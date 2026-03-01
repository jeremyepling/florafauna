# Copies the dev world template to all worktrees (overwrites existing dev worlds)
# Run from the main repo after updating the template.

$repoRoot = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne "main") {
    throw "Run this from the main repo (on main branch), not from a worktree."
}

$templateWorld = Join-Path $repoRoot "dev\world-template\dev"
$templateOptions = Join-Path $repoRoot "dev\options-template.txt"

if (-not (Test-Path $templateWorld)) {
    throw "Template world not found: $templateWorld"
}

$worktrees = git worktree list --porcelain | Select-String "^worktree " | ForEach-Object {
    $_.Line -replace "^worktree ", ""
}

$synced = 0
foreach ($wt in $worktrees) {
    if ($wt -eq $repoRoot) { continue }

    $wtName = Split-Path $wt -Leaf
    $runDir = Join-Path $wt "run\client-$wtName"
    $wtSaves = Join-Path $runDir "saves"
    $wtDev = Join-Path $wtSaves "dev"
    $wtOptions = Join-Path $runDir "options.txt"

    Write-Host "Syncing to: $wtName"

    if (Test-Path $wtDev) {
        Remove-Item -Path $wtDev -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $wtSaves | Out-Null
    Copy-Item -Path $templateWorld -Destination $wtDev -Recurse -Force
    Write-Host "  Copied dev world"

    if (Test-Path $templateOptions) {
        Copy-Item -Path $templateOptions -Destination $wtOptions -Force
        Write-Host "  Copied options"
    }

    $synced++
}

Write-Host ""
Write-Host "Synced dev world to $synced worktree(s)"
