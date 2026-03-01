# Merges current branch to main and pushes
# Run from inside any worktree.

$branch = git rev-parse --abbrev-ref HEAD
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

if ($branch -eq "main") {
    throw "Already on main. Switch to a feature branch first."
}

Write-Host "Merging '$branch' to main..."

git checkout main
if ($LASTEXITCODE -ne 0) { throw "Failed to checkout main." }

git pull origin main
if ($LASTEXITCODE -ne 0) { throw "Failed to pull main." }

git merge $branch --no-edit
if ($LASTEXITCODE -ne 0) {
    throw "Merge failed. Resolve conflicts, commit, then re-run."
}

git push origin main
if ($LASTEXITCODE -ne 0) { throw "Failed to push main." }

Write-Host "Deleting local branch '$branch'..."
git branch -d $branch
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Could not delete local branch '$branch'"
}

Write-Host "Deleting remote branch '$branch'..."
git push origin --delete $branch 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Could not delete remote branch '$branch' (may not exist)"
}

Write-Host ""
Write-Host "Merged '$branch' to main." -ForegroundColor Green
Write-Host "Ready for: git checkout -b <next-feature>"
