#Requires -Version 7.0
# From the workspace: .\florafauna-2\scripts\test_worktrees.ps1 -Slot 1,2,3 -Offline
param(
    [Parameter(Mandatory, Position=0)]
    [ValidateNotNullOrEmpty()]
    [ValidateRange(1, 2147483647)]
    [int[]]$Slot,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
if (@($Slot | Sort-Object -Unique).Count -ne $Slot.Count) {
    throw 'Specify each slot only once.'
}

# Git lists the primary checkout first, even when called from a linked worktree.
$entries = git -C $PSScriptRoot -c core.quotePath=false worktree list --porcelain
if ($LASTEXITCODE -ne 0) { throw 'Cannot list worktrees for this repository.' }
$worktrees = @($entries | ForEach-Object {
    if ($_.StartsWith('worktree ')) { [IO.Path]::GetFullPath($_.Substring(9)) }
})
$primary = $worktrees[0]
$runId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)

# Validate the entire selection before any build starts.
$targets = @(foreach ($number in $Slot) {
    $path = "$primary-$number"
    if ($path -notin $worktrees -or !(Test-Path -LiteralPath "$path/gradlew.bat" -PathType Leaf)) {
        throw "Slot $number has no registered worktree with a Gradle wrapper at $path."
    }
    [pscustomobject]@{
        Slot = $number
        Path = $path
        Log = Join-Path $path ".tmp/worktree-tests/$runId.log"
    }
})

$options = @('--max-workers=4', '--console=plain')
if ($Offline) { $options += '--offline' }
foreach ($target in $targets) {
    Write-Host "Slot $($target.Slot) log: $($target.Log)"
}
$results = @($targets | ForEach-Object -Parallel {
    $ErrorActionPreference = 'Stop'
    $PSNativeCommandUseErrorActionPreference = $false
    $target = $_
    $options = $using:options
    $phase = 'setup'
    try {
        Set-Location -LiteralPath $target.Path
        [IO.Directory]::CreateDirectory((Split-Path $target.Log -Parent)) | Out-Null
        $phase = 'runData'
        "Gradle $phase" | Set-Content -LiteralPath $target.Log
        & .\gradlew.bat @options runData *>> $target.Log
        if ($LASTEXITCODE -ne 0) { throw "Gradle exit $LASTEXITCODE" }

        $phase = 'build runGameTestServer'
        "Gradle $phase" | Add-Content -LiteralPath $target.Log
        & .\gradlew.bat @options build runGameTestServer *>> $target.Log
        if ($LASTEXITCODE -ne 0) { throw "Gradle exit $LASTEXITCODE" }
        $result = 'PASS'
    } catch {
        $result = "FAIL ($phase): $($_.Exception.Message)"
    }
    [pscustomobject]@{ Slot = $target.Slot; Result = $result; Log = $target.Log }
} -ThrottleLimit $targets.Count)

foreach ($result in ($results | Sort-Object Slot)) {
    Write-Host "Slot $($result.Slot) $($result.Result)  $($result.Log)"
}
if ($results.Count -ne $targets.Count -or ($results | Where-Object Result -ne 'PASS')) { exit 1 }
exit 0
