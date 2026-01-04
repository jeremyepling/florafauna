# Copies the current dev world to the template (for distribution to new worktrees)
# Best run from the main florafauna repo, but can use -Force from worktrees

param(
    [Alias("f")]
    [switch]$Force
)

$currentRoot = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

# Find the main repo (the worktree on 'main' branch)
$mainRepo = $null
git worktree list --porcelain | ForEach-Object -Begin { $wt = @{} } -Process {
    if ($_ -match "^worktree (.+)") { $wt.path = $Matches[1] }
    if ($_ -match "^branch refs/heads/(.+)") { $wt.branch = $Matches[1] }
    if ($_ -eq "") {
        if ($wt.branch -eq "main") { $mainRepo = $wt.path }
        $wt = @{}
    }
} -End {
    if ($wt.branch -eq "main") { $mainRepo = $wt.path }
}

if (-not $mainRepo) { throw "Could not find main repo (worktree on 'main' branch)" }

# Warn if not on main branch
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne "main") {
    if (-not $Force) {
        Write-Warning "You're on branch '$branch', not main."
        Write-Warning "This will copy YOUR dev world to the main repo's template."
        Write-Warning "Use -Force (-f) to proceed."
        exit 1
    }
    Write-Warning "Proceeding from branch '$branch' (forced)"
}

# Source is current repo's dev world, destination is main repo's template
$srcWorld = Join-Path $currentRoot "run\client-$branch\saves\dev"
# Fallback to run/saves/dev for main repo
if (-not (Test-Path $srcWorld)) {
    $srcWorld = Join-Path $currentRoot "run\saves\dev"
}
$dstWorld = Join-Path $mainRepo "dev\world-template\dev"

if (-not (Test-Path $srcWorld)) {
    throw "Source world not found: $srcWorld`nRun the client and create/modify the dev world first."
}

Write-Host "Copying dev world to template..."
Write-Host "  From: $srcWorld"
Write-Host "  To:   $dstWorld"

# Remove old template and copy new
if (Test-Path $dstWorld) {
    Remove-Item -Path $dstWorld -Recurse -Force
}
Copy-Item -Path $srcWorld -Destination $dstWorld -Recurse -Force

Write-Host ""
Write-Host "Done! Changes saved to: $mainRepo"
Write-Host "Don't forget to commit dev/world-template/ in the main repo."
