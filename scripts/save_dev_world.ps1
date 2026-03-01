# Copies the current dev world to the main repo's template
# Run from any worktree. Use -Force from non-main branches.

param(
    [Alias("f")]
    [switch]$Force
)

$repoRoot = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

# Find the main repo
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

# Warn if not on main
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne "main" -and -not $Force) {
    Write-Warning "You're on branch '$branch', not main."
    Write-Warning "This will copy YOUR dev world to the main repo's template."
    Write-Warning "Use -Force (-f) to proceed."
    exit 1
}

$wtName = Split-Path $repoRoot -Leaf
$srcWorld = Join-Path $repoRoot "run\client-$wtName\saves\dev"
$dstWorld = Join-Path $mainRepo "dev\world-template\dev"

if (-not (Test-Path $srcWorld)) {
    throw "Source world not found: $srcWorld`nRun the client and create/modify the dev world first."
}

Write-Host "Copying dev world to template..."
Write-Host "  From: $srcWorld"
Write-Host "  To:   $dstWorld"

if (Test-Path $dstWorld) {
    Remove-Item -Path $dstWorld -Recurse -Force
}
Copy-Item -Path $srcWorld -Destination $dstWorld -Recurse -Force

Write-Host ""
Write-Host "Done! Template updated in: $mainRepo"
Write-Host "Run .\scripts\sync_dev_world.ps1 to push to existing worktrees."
