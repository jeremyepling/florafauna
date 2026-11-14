#Requires -Version 7.0
# Run Minecraft clients against fresh copies of a closed save. Each scenario has its own copy.
param(
    [ValidateSet('recovery', 'hammer', 'pods', 'transport', 'frenchie', 'frontpack')]
    [string[]]$Scenario = @('recovery', 'hammer', 'pods', 'transport', 'frenchie', 'frontpack'),
    [string]$SourceWorld,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$checkout = Split-Path $PSScriptRoot -Parent
if (!$SourceWorld) {
    $SourceWorld = Join-Path $checkout "run/client-$(Split-Path $checkout -Leaf)/saves/dev"
}
$world = (Resolve-Path -LiteralPath $SourceWorld).Path
$level = Join-Path $world 'level.dat'
if (!(Test-Path -LiteralPath $level -PathType Leaf)) { throw "Missing level.dat in $world" }
if (@($Scenario | Sort-Object -Unique).Count -ne $Scenario.Count) { throw 'Select each scenario only once.' }
$runId = [guid]::NewGuid().ToString('N')
$runRoot = Join-Path $checkout ".tmp/world-playtests/$runId"
if ($runRoot.StartsWith($world + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The test output cannot be inside the source save.'
}
if ((Get-Item -LiteralPath $world).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or
        (Get-ChildItem -LiteralPath $world -Recurse -Force -Attributes ReparsePoint | Select-Object -First 1)) {
    throw 'Choose a self-contained save without symbolic links or junctions.'
}
$options = @('--console=plain', '--max-workers=4')
if ($Offline) { $options += '--offline' }
$hash = (Get-FileHash -LiteralPath $level).Hash
Write-Host "Source save: $world"
Write-Host "Test worlds, logs and results: $runRoot"

Push-Location -LiteralPath $checkout
try {
    $lock = $null
    try {
        $lockPath = Join-Path $world 'session.lock'
        if (Test-Path -LiteralPath $lockPath) {
            try { $lock = [IO.File]::Open($lockPath, 'Open', 'Read', 'Read') }
            catch [IO.IOException] { throw "Close the source world before copying it: $world" }
        }
        foreach ($name in $Scenario) {
            $gameDir = Join-Path $runRoot $name
            $copy = Join-Path $gameDir 'saves/playtest'
            [IO.Directory]::CreateDirectory($copy) | Out-Null
            & robocopy $world $copy /E /XJ /XF session.lock /R:0 /W:0 /NFL /NDL /NJH /NJS /NP | Out-Null
            if ($LASTEXITCODE -ge 8) { throw "Could not copy $world into $copy (robocopy exit $LASTEXITCODE)." }
            if ((Get-FileHash -LiteralPath (Join-Path $copy 'level.dat')).Hash -ne $hash) {
                throw 'Source save changed during the copy.'
            }
            Set-Content -LiteralPath (Join-Path $gameDir '.florafauna-playtest') -Value $name
            $gameOptions = (Get-Content -LiteralPath 'dev/options-template.txt') -replace '^pauseOnLostFocus:.*','pauseOnLostFocus:false'
            $gameOptions | Set-Content -LiteralPath (Join-Path $gameDir 'options.txt')
        }
    } finally { if ($lock) { $lock.Dispose() } }

    & .\gradlew.bat @options runData *> (Join-Path $runRoot 'datagen.log')
    if ($LASTEXITCODE -ne 0) { throw "Data generation failed. See $runRoot/datagen.log" }
    $failed = @()
    foreach ($name in $Scenario) {
        $gameDir = Join-Path $runRoot $name
        $phases = if ($name -eq 'frenchie') { @(1) } else { @(1, 2) }
        foreach ($phase in $phases) {
            Write-Host "Launching $name phase $phase ..."
            $log = Join-Path $gameDir "phase-$phase.log"
            & .\gradlew.bat @options "-PplaytestId=$runId" "-PplaytestScenario=$name" "-PplaytestPhase=$phase" runWorldPlaytest *> $log
            $exitCode = $LASTEXITCODE
            $resultPath = Join-Path $gameDir "phase-$phase.result"
            $result = if (Test-Path -LiteralPath $resultPath) { (Get-Content -Raw -LiteralPath $resultPath).Trim() } else { 'Missing client result' }
            Write-Host "$name phase $phase (Gradle $exitCode): $result  $log"
            if ($exitCode -ne 0 -or $result -ne 'PASS') {
                $failed += "$name phase $phase"
                break
            }
        }
    }
    if ($failed.Count) { throw "Failed playtests: $($failed -join ', '). Logs: $runRoot" }
} finally { Pop-Location }
