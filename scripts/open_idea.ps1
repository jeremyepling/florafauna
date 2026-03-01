# Opens IntelliJ IDEA for the specified path

param(
  [Parameter(Mandatory=$true)][string]$Path
)

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "open_idea.ps1") -Path $Path
