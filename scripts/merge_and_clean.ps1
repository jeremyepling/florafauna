# Merges a worktree branch to main and cleans up
# WARNING: Run this script ONLY from the main repo checkout.
# Use -f to force delete without merging.

param(
  [Parameter(Mandatory=$true, Position=0)][string]$Branch,
  [Alias("f")][switch]$Force
)

. (Join-Path $PSScriptRoot "config.ps1")

& (Join-Path $SharedScriptsDir "merge_and_clean.ps1") `
  -Branch $Branch `
  -Force:$Force
