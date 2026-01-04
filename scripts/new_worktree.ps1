# WARNING: Run this script ONLY from the main repo checkout (C:\code\florafauna-workspace\florafauna).
# Do NOT run from the workspace root or from inside a worktree.

param(
  [Parameter(Mandatory=$true)][string]$Name,
  [string]$Base = "origin/main",
  [string]$ParentDir = ".."   # where worktrees live relative to repo root
)

$root = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw "Not in a git repo." }

$parent = Resolve-Path (Join-Path $root $ParentDir)
$wtPath = Join-Path $parent.Path $Name

if (Test-Path $wtPath) { throw "Worktree path already exists: $wtPath" }

Write-Host "Creating worktree '$Name' at $wtPath (base: $Base)"

git worktree add -b $Name $wtPath $Base
if ($LASTEXITCODE -ne 0) { throw "git worktree add failed" }

New-Item -ItemType Directory -Force -Path (Join-Path $wtPath "tools") | Out-Null

# Copy dev world and options to worktree's run directory
$wtRunDir = Join-Path $wtPath "run\client-$Name"
$devWorldSrc = Join-Path $root "dev\world-template"
$optionsSrc = Join-Path $root "dev\options-template.txt"

if (Test-Path $devWorldSrc) {
    Write-Host "Copying dev world to worktree..."
    $wtSaves = Join-Path $wtRunDir "saves"
    New-Item -ItemType Directory -Force -Path $wtSaves | Out-Null
    Get-ChildItem -Path $devWorldSrc -Directory | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $wtSaves -Recurse -Force
    }
}

if (Test-Path $optionsSrc) {
    Write-Host "Copying options template to worktree..."
    New-Item -ItemType Directory -Force -Path $wtRunDir | Out-Null
    Copy-Item -Path $optionsSrc -Destination (Join-Path $wtRunDir "options.txt") -Force
}

@"
`$env:GRADLE_USER_HOME   = "`$PSScriptRoot\..\..\.gradle-user-home"
`$env:WORKTREE_ID        = "$Name"
`$env:JAVA_TOOL_OPTIONS  = "-Dorg.gradle.vfs.watch=false"
"@ | Out-File (Join-Path $wtPath "tools\agent_env.ps1") -Encoding utf8 -Force

# Run data generation in the new worktree (in isolated subprocess to avoid polluting current env)
Write-Host ""
Write-Host "Running data generation..."
$gradleHome = Join-Path $parent.Path ".gradle-user-home"
& pwsh -NoProfile -Command "Set-Location '$wtPath'; `$env:GRADLE_USER_HOME='$gradleHome'; `$env:WORKTREE_ID='$Name'; `$env:JAVA_TOOL_OPTIONS='-Dorg.gradle.vfs.watch=false'; ./gradlew runData"
if ($LASTEXITCODE -ne 0) { Write-Warning "runData failed, but continuing..." }

Write-Host ""
Write-Host "Created:"
Write-Host "  Branch: $Name"
Write-Host "  Path:   $wtPath"
Write-Host ""
Write-Host "Next:"
Write-Host "  scripts\open_idea.ps1 `"$wtPath`""
Write-Host "  (then in IntelliJ terminal: .\tools\agent_env.ps1 ; claude)"
