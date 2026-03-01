# Copies the current dev world to the template
# Best run from the main repo, but can use -Force from worktrees

param(
    [Alias("f")]
    [switch]$Force
)

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "save_dev_world.ps1") `
  -Force:$Force `
  -WorldTemplatePath "$WorldTemplatePath\dev" `
  -RunDirPattern $RunDirPattern
