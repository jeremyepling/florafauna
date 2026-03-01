# Creates a new worktree for mob-system
# WARNING: Run this script ONLY from the main repo checkout.

param(
  [Parameter(Mandatory=$true)][string]$Name,
  [string]$Base = "origin/main",
  [switch]$SkipDataGen
)

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "new_worktree.ps1") `
  -Name $Name `
  -Base $Base `
  -WorldTemplatePath $WorldTemplatePath `
  -OptionsTemplatePath $OptionsTemplatePath `
  -RunDirPattern $RunDirPattern `
  -SkipDataGen:$SkipDataGen
