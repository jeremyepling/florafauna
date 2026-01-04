# WARNING: Run this script ONLY from the main repo checkout (C:\code\florafauna-workspace\florafauna).
# Do NOT run from the workspace root or from inside a worktree.
#
# Regular merge (fast-forward when possible). Branch name = worktree folder name.
# Use -f or --force to skip merge and forcefully delete worktree/branch (discards all changes).

param(
  [Parameter(Mandatory=$true, Position=0)][string]$Branch,
  [Alias("f")][switch]$Force
)

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$WorktreePath = "..\$Branch"

git checkout main
git pull origin main

if ($Force) {
  Write-Host "Force mode: skipping merge, deleting worktree and branch..." -ForegroundColor Yellow
} else {
  git merge $Branch --no-edit
  if ($LASTEXITCODE -ne 0) {
    throw "Merge failed. Resolve conflicts, then run: git commit and re-run cleanup steps."
  }
  git push origin main
}

# Remove worktree
git worktree remove $WorktreePath $(if ($Force) { "--force" })
if ($LASTEXITCODE -ne 0) {
  if (-not $Force) {
    Write-Host ""
    Write-Host "Worktree removal failed. If you want to discard all changes, use:" -ForegroundColor Red
    Write-Host "  .\scripts\merge_and_clean.ps1 $Branch -f" -ForegroundColor Cyan
    Write-Host ""
    throw "Worktree removal failed."
  }
  # Force mode but still failed - try harder
  Write-Host "Worktree remove --force failed, removing directory manually..." -ForegroundColor Yellow
  if (Test-Path $WorktreePath) {
    Remove-Item -Recurse -Force $WorktreePath
  }
  git worktree prune
}

# Delete local branch
git branch -D $Branch
if ($LASTEXITCODE -ne 0) {
  Write-Host "Warning: Could not delete local branch '$Branch' (may not exist)" -ForegroundColor Yellow
}

# Delete remote branch
git push origin --delete $Branch 2>$null
if ($LASTEXITCODE -ne 0) {
  Write-Host "Warning: Could not delete remote branch '$Branch' (may not exist)" -ForegroundColor Yellow
}

Write-Host "Cleanup complete for '$Branch'" -ForegroundColor Green
