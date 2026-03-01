# Runs data generation and launches the client to edit the dev world
# After editing, use save_dev_world.ps1 to save changes to the template

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "edit_dev_world.ps1") `
  -WorldTemplatePath "$WorldTemplatePath\dev" `
  -OptionsTemplatePath $OptionsTemplatePath `
  -RunDirPattern $RunDirPattern `
  -DataGenTask $DataGenTask `
  -ClientTask $ClientTask
