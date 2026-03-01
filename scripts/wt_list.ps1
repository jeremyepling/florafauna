# Lists all worktrees for this repo

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "wt_list.ps1")
