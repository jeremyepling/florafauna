# Runs data generation and launches the client to edit the dev world
# After editing, use save_dev_world.ps1 to save changes to the template

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$branch = git rev-parse --abbrev-ref HEAD
Write-Host "Running from: $root"
Write-Host "Branch: $branch"
Write-Host ""

Set-Location $root

Write-Host "Running data generation..."
./gradlew runClientData --stacktrace
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Data generation failed, but launching client anyway..."
}

Write-Host ""
Write-Host "Launching client..."
Write-Host "After editing the dev world, run: .\scripts\save_dev_world.ps1"
Write-Host ""

./gradlew runClient
