# WARNING: Run this script ONLY from the main repo checkout (C:\code\florafauna-workspace\florafauna).
# Do NOT run from the workspace root or from inside a worktree.
#
# This does a regular merge (fast-forward when possible, no extra commit for single commits).
# Use squash-merge-and-clean.ps1 if you want to combine multiple commits into one.

param(
  [Parameter(Mandatory=$true)][string]$Branch,
  [string]$WorktreePath = "",    # e.g. ..\feature-xyz
  [string]$Main = "main",
  [switch]$DeleteRemoteBranch
)

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

git checkout $Main
git pull origin $Main

git merge $Branch
if ($LASTEXITCODE -ne 0) {
  throw "Merge failed. Resolve conflicts, then run: git commit and re-run cleanup steps."
}

git push origin $Main

if ($WorktreePath -ne "") {
  git worktree remove $WorktreePath
}

git branch -D $Branch

if ($DeleteRemoteBranch) {
  git push origin --delete $Branch
}
