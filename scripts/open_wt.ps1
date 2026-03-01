# Opens a worktree in IntelliJ by branch name

param(
  [Parameter(Mandatory=$true)][string]$Branch
)

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "open_wt.ps1") -Branch $Branch -ScriptsDir $SharedScriptsDir
