# WARNING: Run this script ONLY from the main repo checkout (C:\code\florafauna-workspace\florafauna).
# Do NOT run from the workspace root or from inside a worktree.
#
# Regular merge (fast-forward when possible). Branch name = worktree folder name.

param(
  [Parameter(Mandatory=$true, Position=0)][string]$Branch
)

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$WorktreePath = "..\$Branch"

git checkout main
git pull origin main

git merge $Branch --no-edit
if ($LASTEXITCODE -ne 0) {
  throw "Merge failed. Resolve conflicts, then run: git commit and re-run cleanup steps."
}

git push origin main

git worktree remove $WorktreePath

git branch -D $Branch
git push origin --delete $Branch
