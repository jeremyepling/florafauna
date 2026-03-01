# Creates a new worktree and opens it in IntelliJ
# WARNING: Run this script ONLY from the main repo checkout.

param(
  [Parameter(Mandatory=$true)][string]$Name,
  [string]$Base = "origin/main",
  [switch]$SkipDataGen
)

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "new_and_open.ps1") `
  -Name $Name `
  -Base $Base `
  -WorldTemplatePath $WorldTemplatePath `
  -OptionsTemplatePath $OptionsTemplatePath `
  -RunDirPattern $RunDirPattern `
  -SkipDataGen:$SkipDataGen
