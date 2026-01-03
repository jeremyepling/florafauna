param([Parameter(Mandatory=$true)][string]$Branch)

# Find the worktree path that has this branch checked out
$lines = git worktree list --porcelain
$wt = $null
$br = $null

foreach ($line in $lines) {
  if ($line -like "worktree *") { $wt = $line.Substring(9).Trim() }
  if ($line -like "branch *")   { $br = $line.Substring(7).Trim() }

  if ($wt -and $br -and $br.EndsWith("/$Branch")) {
    & (Join-Path $PSScriptRoot "open-idea.ps1") -Path $wt
    exit 0
  }

  if ($line -eq "") { $wt = $null; $br = $null }
}

throw "Branch '$Branch' not found as a checked-out worktree."
