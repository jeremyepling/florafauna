# Runs data generation and launches the client to edit the dev world
# After editing, use save_dev_world.ps1 to save changes to the template.

$repoRoot = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$branch = git rev-parse --abbrev-ref HEAD
$wtName = Split-Path $repoRoot -Leaf
$runDir = Join-Path $repoRoot "run\client-$wtName"
$savesDir = Join-Path $runDir "saves"
$devWorld = Join-Path $savesDir "dev"

Write-Host "Running from: $repoRoot"
Write-Host "Branch: $branch"
Write-Host "Run directory: $runDir"
Write-Host ""

Set-Location $repoRoot

New-Item -ItemType Directory -Force -Path $savesDir | Out-Null

# Copy dev world template if not present
$templateWorld = Join-Path $repoRoot "dev\world-template\dev"
if ((Test-Path $templateWorld) -and (-not (Test-Path $devWorld))) {
    Write-Host "Copying dev world template..."
    Copy-Item -Path $templateWorld -Destination $devWorld -Recurse -Force
}

# Copy options template if not present
$templateOptions = Join-Path $repoRoot "dev\options-template.txt"
if ((Test-Path $templateOptions) -and (-not (Test-Path (Join-Path $runDir "options.txt")))) {
    Write-Host "Copying options template..."
    Copy-Item -Path $templateOptions -Destination (Join-Path $runDir "options.txt") -Force
}

Write-Host ""
Write-Host "Running data generation..."
./gradlew runData --stacktrace
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Data generation failed, but launching client anyway..."
}

Write-Host ""
Write-Host "Launching client..."
Write-Host ""

./gradlew runClient

Write-Host ""
Write-Host "Done editing? Run: .\scripts\save_dev_world.ps1"
