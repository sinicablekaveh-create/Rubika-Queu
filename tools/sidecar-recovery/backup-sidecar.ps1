# Run ONLY on an owner-controlled Windows computer with JDK 17+ and Android platform-tools.
# Never executes on a phone without a consciously authorized USB-debugging connection.
param([Parameter(Mandatory=$true)][string]$OutputPath)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { throw "ADB platform-tools is not on PATH." }
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw "JDK javac is not on PATH." }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "JDK java is not on PATH." }
$repository = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$classes = Join-Path $env:TEMP ([System.IO.Path]::GetRandomFileName())
New-Item -ItemType Directory -Path $classes | Out-Null
try {
  & javac -d $classes (Join-Path $repository "overrides/queue-backup/QueueBackupCrypto.java") (Join-Path $repository "tools/sidecar-recovery/SidecarAdbBackup.java")
  if ($LASTEXITCODE -ne 0) { throw "Backup helper compilation failed." }
  & java -cp $classes ir.sinicable.rubikaqueue.SidecarAdbBackup $OutputPath
  if ($LASTEXITCODE -ne 0) { throw "Backup failed; original phone app was not changed." }
} finally {
  Remove-Item -LiteralPath $classes -Recurse -Force -ErrorAction SilentlyContinue
}
