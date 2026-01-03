# WARNING: Run this script ONLY from the main repo checkout (C:\code\florafauna-workspace\florafauna).
# Do NOT run from the workspace root or from inside a worktree.

param(
  [Parameter(Mandatory=$true)][string]$Branch,
  [Parameter(Mandatory=$true)][string]$Message,
  [string]$WorktreePath = "",    # e.g. ..\feature-xyz
  [string]$Main = "main",
  [switch]$DeleteRemoteBranch
)

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

git checkout $Main
git pull origin $Main

git merge --squash $Branch
if ($LASTEXITCODE -ne 0) {
  throw "Squash merge failed. Resolve conflicts, then run: git commit -m `"$Message`" and re-run cleanup steps."
}

git commit -m $Message
git push origin $Main

if ($WorktreePath -ne "") {
  git worktree remove $WorktreePath
}

git branch -D $Branch

if ($DeleteRemoteBranch) {
  git push origin --delete $Branch
}
