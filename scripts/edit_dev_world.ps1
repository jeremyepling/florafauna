# Runs data generation and launches the client to edit the dev world
# After editing, use save_dev_world.ps1 to save changes to the template

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$branch = git rev-parse --abbrev-ref HEAD
Write-Host "Running from: $root"
Write-Host "Branch: $branch"
Write-Host ""

Set-Location $root

# Determine run directory (matches gradle's logic)
$wt = if ($env:WORKTREE_ID) { $env:WORKTREE_ID } else { Split-Path $root -Leaf }
$runDir = Join-Path $root "run\client-$wt"
$savesDir = Join-Path $runDir "saves"
$devWorld = Join-Path $savesDir "dev"
$optionsFile = Join-Path $runDir "options.txt"

Write-Host "Run directory: $runDir"
Write-Host ""

# Ensure run directory exists
New-Item -ItemType Directory -Force -Path $savesDir | Out-Null

# Copy dev world template if not present
$templateWorld = Join-Path $root "dev\world-template\dev"
if ((Test-Path $templateWorld) -and (-not (Test-Path $devWorld))) {
    Write-Host "Copying dev world template..."
    Copy-Item -Path $templateWorld -Destination $devWorld -Recurse -Force
}

# Copy options template if not present or if it's a fresh default
$templateOptions = Join-Path $root "dev\options-template.txt"
if (Test-Path $templateOptions) {
    if (-not (Test-Path $optionsFile)) {
        Write-Host "Copying options template..."
        Copy-Item -Path $templateOptions -Destination $optionsFile -Force
    }
}

Write-Host ""
Write-Host "Running data generation..."
./gradlew runClientData --stacktrace
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Data generation failed, but launching client anyway..."
}

Write-Host ""
Write-Host "Launching client..."
Write-Host ""

./gradlew runClient

Write-Host ""
Write-Host "Done editing? Run: .\scripts\save_dev_world.ps1"
