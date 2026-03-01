# Copies the dev world template to all worktrees
# Run this from the main repo after updating the template

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "sync_dev_world.ps1") `
  -WorldTemplatePath "$WorldTemplatePath\dev" `
  -OptionsTemplatePath $OptionsTemplatePath `
  -RunDirPattern $RunDirPattern
