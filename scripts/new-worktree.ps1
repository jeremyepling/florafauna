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

@"
`$env:GRADLE_USER_HOME   = "`$PSScriptRoot\..\..\.gradle-user-home"
`$env:WORKTREE_ID        = "$Name"
`$env:JAVA_TOOL_OPTIONS  = "-Dorg.gradle.vfs.watch=false"
"@ | Out-File (Join-Path $wtPath "tools\agent_env.ps1") -Encoding utf8 -Force

Write-Host ""
Write-Host "Created:"
Write-Host "  Branch: $Name"
Write-Host "  Path:   $wtPath"
Write-Host ""
Write-Host "Next:"
Write-Host "  scripts\open-idea.ps1 `"$wtPath`""
Write-Host "  (then in IntelliJ terminal: .\tools\agent_env.ps1 ; claude)"
