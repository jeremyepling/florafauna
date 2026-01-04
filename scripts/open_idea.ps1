param(
  [Parameter(Mandatory=$true)][string]$Path
)

$idea = $env:IDEA_EXE

if (-not $idea -or -not (Test-Path $idea)) {
  # Fallback: try common locations
  $candidates = Get-ChildItem "C:\Program Files\JetBrains\" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "IntelliJ IDEA Community Edition*" } |
    ForEach-Object { Join-Path $_.FullName "bin\idea64.exe" } |
    Where-Object { Test-Path $_ } |
    Sort-Object -Descending

  $idea = $candidates | Select-Object -First 1
}

if (-not $idea -or -not (Test-Path $idea)) {
  throw "Could not find idea64.exe. Set IDEA_EXE env var, e.g. setx IDEA_EXE `"C:\...\idea64.exe`""
}

$full = Resolve-Path $Path
Write-Host "Launching IntelliJ: $idea"
Start-Process -FilePath $idea -ArgumentList "`"$($full.Path)`""
