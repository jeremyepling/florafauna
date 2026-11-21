"""Sample Resonant Bloom in a disposable, fixed-seed normal 26.3 server world.

Run through Invoke-MinecraftCheck.ps1 -Kind Headless so Gradle owns this checkout.
The server world is unique for every invocation; no existing save is changed.
"""

from __future__ import annotations

import os
import secrets
import socket
import struct
import subprocess
import sys
import time
import uuid
from pathlib import Path


SEED = 6867536138357942029
TARGET = "26.3-neoforge"
TIMEOUT_SECONDS = 360


def free_port() -> int:
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def read_exact(sock: socket.socket, count: int) -> bytes:
    data = bytearray()
    while len(data) < count:
        part = sock.recv(count - len(data))
        if not part:
            raise ConnectionError("RCON connection closed")
        data.extend(part)
    return bytes(data)


def rcon_stop(port: int, password: str) -> None:
    with socket.create_connection(("127.0.0.1", port), timeout=5) as sock:
        sock.settimeout(5)

        def send(request_id: int, kind: int, message: str) -> None:
            body = struct.pack("<ii", request_id, kind) + message.encode("utf-8") + b"\0\0"
            sock.sendall(struct.pack("<i", len(body)) + body)

        def receive() -> tuple[int, int, str]:
            length = struct.unpack("<i", read_exact(sock, 4))[0]
            body = read_exact(sock, length)
            request_id, kind = struct.unpack("<ii", body[:8])
            return request_id, kind, body[8:-2].decode("utf-8", errors="replace")

        send(101, 3, password)
        for _ in range(3):
            request_id, _, _ = receive()
            if request_id == -1:
                raise RuntimeError("RCON authentication failed")
            if request_id == 101:
                break
        else:
            raise RuntimeError("RCON did not acknowledge authentication")
        send(102, 2, "stop")


def main() -> int:
    checkout = Path(__file__).resolve().parent.parent
    target = checkout / "versions" / TARGET
    game_dir = target / "run" / f"server-{checkout.name}-core"
    marker = game_dir / ".florafauna-bloom-density"
    if game_dir.exists() and not marker.is_file():
        raise SystemExit(f"Refusing to change an existing non-probe server directory: {game_dir}")
    game_dir.mkdir(parents=True, exist_ok=True)
    marker.write_text("Disposable normal-world density probe\n", encoding="utf-8")

    run_id = uuid.uuid4().hex
    output = target / ".tmp" / "bloom-density" / run_id
    output.mkdir(parents=True, exist_ok=False)
    world_name = f"bloom-density-{run_id}"
    world = game_dir / world_name
    if world.exists():
        raise SystemExit(f"Fresh density world already exists: {world}")
    report = output / "density.txt"
    password = secrets.token_urlsafe(20)
    server_port = free_port()
    rcon_port = free_port()
    while rcon_port == server_port:
        rcon_port = free_port()
    (game_dir / "server.properties").write_text(
        "\n".join(
            (
                "server-ip=127.0.0.1",
                f"server-port={server_port}",
                "online-mode=false",
                "enforce-secure-profile=false",
                f"level-name={world_name}",
                "level-type=minecraft:normal",
                f"level-seed={SEED}",
                "generate-structures=true",
                "view-distance=2",
                "simulation-distance=2",
                "max-players=1",
                "max-tick-time=180000",
                "enable-rcon=true",
                f"rcon.port={rcon_port}",
                f"rcon.password={password}",
            )
        ) + "\n",
        encoding="utf-8",
    )

    gradle = checkout / "gradlew.bat"
    env = os.environ.copy()
    env["FLORAFAUNA_BLOOM_DENSITY_REPORT"] = str(report)
    print(f"Fresh world: {world}", flush=True)
    print(f"Report and logs: {output}", flush=True)
    with (output / "datagen.log").open("wb") as log:
        result = subprocess.run(
            [str(gradle), "--console=plain", f":{TARGET}:runData"],
            cwd=checkout, stdout=log, stderr=subprocess.STDOUT, env=env,
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
    if result.returncode:
        raise SystemExit(f"Datagen failed; see {output / 'datagen.log'}")

    command = [
        str(gradle), "--console=plain", f"-PplaytestId={run_id}",
        "-PplaytestScenario=density", "-PplaytestPhase=1", f":{TARGET}:runServer",
    ]
    with (output / "server.log").open("wb") as log:
        server = subprocess.Popen(
            command, cwd=checkout, stdout=log, stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL, env=env, creationflags=subprocess.CREATE_NO_WINDOW,
        )
        try:
            deadline = time.monotonic() + TIMEOUT_SECONDS
            while not report.exists() and time.monotonic() < deadline:
                if server.poll() is not None:
                    raise RuntimeError(f"Server exited before the report; see {output / 'server.log'}")
                time.sleep(0.5)
            if not report.exists():
                raise TimeoutError(f"Density report timed out; see {output / 'server.log'}")
            content = report.read_text(encoding="utf-8")
            print(content, flush=True)
            if content.startswith("FAIL:"):
                raise RuntimeError(f"Density probe failed; see {output / 'server.log'}")
            for attempt in range(30):
                try:
                    rcon_stop(rcon_port, password)
                    break
                except (ConnectionError, OSError):
                    if attempt == 29:
                        raise
                    time.sleep(1)
            server.wait(timeout=60)
            if server.returncode:
                raise RuntimeError(f"Server exited {server.returncode}; see {output / 'server.log'}")
            return 0
        finally:
            if server.poll() is None:
                try:
                    rcon_stop(rcon_port, password)
                    server.wait(timeout=15)
                except (ConnectionError, OSError, RuntimeError, subprocess.TimeoutExpired):
                    server.terminate()
                    try:
                        server.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        server.kill()


if __name__ == "__main__":
    sys.exit(main())
