# Run manually from the feature worktree to land its committed changes on main.
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

function Invoke-Git([string]$checkout) {
    git -C $checkout @args
    if ($LASTEXITCODE -ne 0) { throw "Failed in '$checkout': git $args" }
}

function Assert-CleanCheckout([string]$checkout) {
    if (Invoke-Git $checkout status --porcelain --untracked-files=normal) {
        throw "Commit or stash changes in '$checkout' before merging."
    }
    $gitDir = Invoke-Git $checkout rev-parse --absolute-git-dir
    foreach ($state in 'MERGE_HEAD', 'rebase-merge', 'rebase-apply', 'CHERRY_PICK_HEAD', 'REVERT_HEAD', 'sequencer') {
        if (Test-Path -LiteralPath (Join-Path $gitDir $state)) {
            throw "Finish the in-progress Git operation in '$checkout' before retrying."
        }
    }
}

$worker = Invoke-Git . rev-parse --show-toplevel
$branchRef = git -C $worker symbolic-ref --quiet HEAD
if ($LASTEXITCODE -ne 0 -or $branchRef -eq 'refs/heads/main') {
    throw 'Run this from a worktree on a named feature branch.'
}
$branch = $branchRef.Substring('refs/heads/'.Length)
$featureTip = Invoke-Git $worker rev-parse HEAD
$main = Invoke-Git $worker for-each-ref '--format=%(worktreepath)' refs/heads/main
if (!$main) { throw 'No worktree has main checked out.' }

# Serialize invocations across workers while updating the shared main checkout.
$commonDir = Invoke-Git $worker rev-parse --path-format=absolute --git-common-dir
$lockPath = Join-Path $commonDir 'florafauna-merge.lock'
try {
    $lock = [IO.File]::Open($lockPath, 'OpenOrCreate', 'ReadWrite', 'None')
} catch [IO.IOException] {
    throw "Cannot lock '$lockPath'. If another merge is running, retry when it finishes."
}

try {
    Assert-CleanCheckout $worker
    Assert-CleanCheckout $main
    if ((Invoke-Git $main symbolic-ref HEAD) -ne 'refs/heads/main') {
        throw "Expected main to be checked out in '$main'."
    }
    Write-Host "Merging '$branch' via '$main'. Resolve any conflicts there, commit and rerun from '$worker'."
    Invoke-Git $main pull --no-rebase --no-edit origin main
    Invoke-Git $main merge --no-edit $featureTip
    $mainTip = Invoke-Git $main rev-parse refs/heads/main
    Invoke-Git $main merge-base --is-ancestor $featureTip $mainTip

    # Delete a published feature ref only if its observed tip is included.
    # The lease also protects against a push made after this check.
    $remoteRef = Invoke-Git $worker ls-remote --refs origin $branchRef
    $keepBranch = $false
    if ($remoteRef) {
        $remoteTip = ($remoteRef -split '\s+')[0]
        git -C $worker merge-base --is-ancestor $remoteTip $featureTip
        if ($LASTEXITCODE -ne 0) {
            $keepBranch = $true
            Write-Warning "Keeping '$branch': its remote tip is outside the feature being landed."
        }
    }
    Assert-CleanCheckout $worker
    if ((Invoke-Git $worker symbolic-ref HEAD) -ne $branchRef -or (Invoke-Git $worker rev-parse HEAD) -ne $featureTip) {
        throw "The worker branch changed. Review '$worker' and retry."
    }
    Invoke-Git $main push origin "${mainTip}:refs/heads/main"
    Write-Host "Published main at $mainTip."
    if ($keepBranch) { return }

    # Detach at the landed feature commit so the worker and its saves stay in place.
    Assert-CleanCheckout $worker
    if ((Invoke-Git $worker symbolic-ref HEAD) -ne $branchRef -or (Invoke-Git $worker rev-parse HEAD) -ne $featureTip) {
        throw "Main was published. Worker changed; keeping its local branch."
    }
    if ($remoteRef) {
        git -C $worker push "--force-with-lease=${branchRef}:$remoteTip" origin ":$branchRef"
        if ($LASTEXITCODE -ne 0) {
            throw "Main was published. Remote cleanup failed; keeping the local branch for retry."
        }
    }
    Invoke-Git $worker switch --no-overwrite-ignore --detach $featureTip
    if (Invoke-Git $worker for-each-ref '--format=%(worktreepath)' $branchRef | Where-Object { $_ }) {
        throw "Main was published. '$branch' is in use by another worktree; keeping it."
    }
    Invoke-Git $worker update-ref -d $branchRef $featureTip
    $configKeys = '^branch\.' + [regex]::Escape($branch) + '\.'
    if (git -C $worker config --local --name-only --get-regexp $configKeys) {
        Invoke-Git $worker config --local --remove-section "branch.$branch"
    }
    Write-Host "Published '$branch' on main. Start the next feature in '$worker' with git switch -c <name> main."
} finally {
    $lock.Dispose()
}
