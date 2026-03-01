# Creates a new numbered worktree with dev world setup
# Run from the main repo only.
# Usage: .\scripts\new_worktree.ps1 <number>

param(
    [Parameter(Mandatory=$true, Position=0)][int]$Number
)

$repoRoot = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne "main") {
    throw "Run this from the main repo (on main branch), not from a worktree."
}

$repoName = Split-Path $repoRoot -Leaf
$wtName = "$repoName-$Number"
$wsRoot = Split-Path $repoRoot -Parent
$wtPath = Join-Path $wsRoot $wtName

if (Test-Path $wtPath) { throw "Worktree path already exists: $wtPath" }

Write-Host "Creating worktree '$wtName' at $wtPath"

git worktree add $wtPath main
if ($LASTEXITCODE -ne 0) { throw "git worktree add failed" }

# Copy dev world and options to worktree's run directory
$runDir = Join-Path $wtPath "run\client-$wtName"
$templateWorld = Join-Path $repoRoot "dev\world-template\dev"
$templateOptions = Join-Path $repoRoot "dev\options-template.txt"

if (Test-Path $templateWorld) {
    Write-Host "Copying dev world to worktree..."
    $wtSaves = Join-Path $runDir "saves"
    New-Item -ItemType Directory -Force -Path $wtSaves | Out-Null
    Copy-Item -Path $templateWorld -Destination (Join-Path $wtSaves "dev") -Recurse -Force
}

if (Test-Path $templateOptions) {
    Write-Host "Copying options template to worktree..."
    New-Item -ItemType Directory -Force -Path $runDir | Out-Null
    Copy-Item -Path $templateOptions -Destination (Join-Path $runDir "options.txt") -Force
}

Write-Host ""
Write-Host "Created:"
Write-Host "  Worktree: $wtName"
Write-Host "  Path:     $wtPath"
