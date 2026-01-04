# Copies the dev world template to all worktrees (overwrites existing dev worlds)
# Run this from the main florafauna repo after updating the template

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

# Ensure we're in the main repo, not a worktree
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne "main") {
    throw "This script should only be run from the main florafauna repo (on main branch), not from a worktree."
}

$templateWorld = Join-Path $root "dev\world-template\dev"
$templateOptions = Join-Path $root "dev\options-template.txt"

if (-not (Test-Path $templateWorld)) {
    throw "Template world not found: $templateWorld"
}

# Get all worktrees
$worktrees = git worktree list --porcelain | Select-String "^worktree " | ForEach-Object {
    $_.Line -replace "^worktree ", ""
}

$synced = 0
foreach ($wt in $worktrees) {
    # Skip the main repo
    if ($wt -eq $root) { continue }

    $wtName = Split-Path $wt -Leaf
    $wtSaves = Join-Path $wt "run\client-$wtName\saves"
    $wtDev = Join-Path $wtSaves "dev"
    $wtOptions = Join-Path $wt "run\client-$wtName\options.txt"

    Write-Host "Syncing to: $wtName"

    # Copy world
    if (Test-Path $wtDev) {
        Remove-Item -Path $wtDev -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $wtSaves | Out-Null
    Copy-Item -Path $templateWorld -Destination $wtDev -Recurse -Force
    Write-Host "  Copied dev world"

    # Copy options
    if (Test-Path $templateOptions) {
        Copy-Item -Path $templateOptions -Destination $wtOptions -Force
        Write-Host "  Copied options"
    }

    $synced++
}

Write-Host ""
Write-Host "Synced dev world to $synced worktree(s)"
