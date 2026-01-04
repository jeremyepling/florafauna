# WARNING: Run this script ONLY from the main repo checkout (C:\code\florafauna-workspace\florafauna).
# Do NOT run from the workspace root or from inside a worktree.

param(
  [Parameter(Mandatory=$true)][string]$Name,
  [string]$Base = "origin/main",
  [string]$ParentDir = ".."
)

& (Join-Path $PSScriptRoot "new_worktree.ps1") -Name $Name -Base $Base -ParentDir $ParentDir

$root = git rev-parse --show-toplevel
$parent = Resolve-Path (Join-Path $root $ParentDir)
$wtPath = Join-Path $parent.Path $Name

& (Join-Path $PSScriptRoot "open_idea.ps1") -Path $wtPath
