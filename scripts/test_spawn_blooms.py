"""Check new-world spawn blooms and save/reopen on a disposable dedicated server.

Run through Invoke-MinecraftCheck.ps1 -Kind Headless to lock the checkout.
"""

import argparse
import json
from pathlib import Path
import socket
import subprocess
import uuid


TARGETS = ("26.1-neoforge", "26.1-fabric", "26.3-neoforge", "26.3-fabric")


def free_port() -> int:
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def configure_world(directory: Path, terrain: str) -> None:
    biome = "plains" if terrain == "grass" else "ocean"
    layers = (
        [(1, "bedrock"), (2, "dirt"), (1, "grass_block")]
        if terrain == "grass"
        else [(1, "bedrock"), (3, "stone"), (16, "water")]
    )
    generator = {
        "biome": "minecraft:" + biome,
        "layers": [{"height": height, "block": "minecraft:" + block} for height, block in layers],
        "features": False,
        "lakes": False,
        "structure_overrides": [],
    }
    properties = {
        "server-ip": "127.0.0.1",
        "server-port": free_port(),
        "online-mode": "false",
        "generate-structures": "false",
        "level-name": "world",
        "level-seed": 6867536138357942029,
        "level-type": "minecraft:flat",
        "generator-settings": json.dumps(generator, separators=(",", ":")),
        "view-distance": 2,
        "simulation-distance": 2,
        "max-tick-time": 180000,
    }
    (directory / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (directory / "server.properties").write_text(
        "".join(f"{key}={value}\n" for key, value in properties.items()), encoding="utf-8"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=TARGETS, default="26.3-neoforge")
    parser.add_argument("--terrain", choices=("grass", "ocean"), default="grass")
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()

    checkout = Path(__file__).resolve().parents[1]
    run_id = uuid.uuid4().hex
    directory = checkout / "versions" / args.target / ".tmp" / "world-playtests" / run_id / "spawnblooms"
    directory.mkdir(parents=True, exist_ok=False)
    configure_world(directory, args.terrain)
    print(f"Disposable {args.terrain} world and logs: {directory}", flush=True)

    command = [str(checkout / "gradlew.bat"), "--console=plain"]
    if args.offline:
        command.append("--offline")
    command.extend([f"-PplaytestId={run_id}", "-PplaytestScenario=spawnblooms"])
    for phase in (1, 2):
        log_path = directory / f"phase-{phase}.log"
        report_path = directory / f"phase-{phase}.txt"
        try:
            with log_path.open("wb") as log:
                result = subprocess.run(
                    command + [f"-PplaytestPhase={phase}", f":{args.target}:runSpawnBloomProbe"],
                    cwd=checkout,
                    stdout=log,
                    stderr=subprocess.STDOUT,
                    stdin=subprocess.DEVNULL,
                    timeout=360,
                    creationflags=subprocess.CREATE_NO_WINDOW,
                )
        except subprocess.TimeoutExpired:
            raise SystemExit(f"Phase {phase} timed out; see {log_path}") from None
        report = report_path.read_text(encoding="utf-8").strip() if report_path.is_file() else "No report"
        if result.returncode or not report.startswith("PASS "):
            raise SystemExit(f"Phase {phase} failed: {report}; see {log_path}")
        if not (directory / "world" / "level.dat").is_file():
            raise SystemExit(f"Phase {phase} ended without saving the disposable world; see {log_path}")
        print(report, flush=True)


if __name__ == "__main__":
    main()
