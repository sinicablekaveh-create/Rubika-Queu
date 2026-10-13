# Standalone plugin-core verification for Windows PowerShell.
# Requires a JDK (java and javac); does not need Gradle, Android SDK, or AI providers.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$pluginDirectory = Join-Path $root 'overrides\plugin-system'
$testFile = Join-Path $root 'tests\PluginSystemTests.java'

$java = Get-Command 'java' -ErrorAction SilentlyContinue
$javac = Get-Command 'javac' -ErrorAction SilentlyContinue
if ($null -eq $java -or $null -eq $javac) {
    throw 'JDK not found in PATH. Install JDK 17 or newer and reopen PowerShell.'
}
if (-not (Test-Path -LiteralPath $pluginDirectory -PathType Container) -or
    -not (Test-Path -LiteralPath $testFile -PathType Leaf)) {
    throw 'Plugin core or test source is missing; run this script from a complete repository checkout.'
}

$sources = @(Get-ChildItem -LiteralPath $pluginDirectory -Filter '*.java' -File |
    Select-Object -ExpandProperty FullName)
$sources += $testFile
if ($sources.Count -lt 4) { throw 'Expected three plugin-core Java files and one test file.' }

$buildDirectory = Join-Path ([System.IO.Path]::GetTempPath()) (
    'rubika-plugin-tests-' + [Guid]::NewGuid().ToString('N')
)
New-Item -ItemType Directory -Path $buildDirectory -Force | Out-Null
try {
    Write-Host 'Compiling independent Java plugin core...'
    & $javac.Source -d $buildDirectory @sources
    if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

    Write-Host 'Running lifecycle, fallback and workflow tests...'
    & $java.Source -cp $buildDirectory 'ir.sinicable.rubikaqueue.PluginSystemTests'
    if ($LASTEXITCODE -ne 0) { throw "Java tests failed with exit code $LASTEXITCODE" }

    Write-Host 'PASS: plugin core tests completed without Android SDK or external providers.'
}
finally {
    Remove-Item -LiteralPath $buildDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
