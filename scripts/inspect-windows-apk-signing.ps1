#Requires -Version 5.1
<#
Read-only signing-identity audit for Rubika-Queu on Windows.
Never uploads a keystore, password, APK, or local data.
USB debugging and ADB authorization must be enabled to inspect an installed APK.
#>
[CmdletBinding()]
param(
    [string]$PackageId = 'ir.sinicable.rubikaqueue',
    [string]$DeviceSerial = ''
)

$ErrorActionPreference = 'Stop'
$homeDir = [Environment]::GetFolderPath('UserProfile')
$sdkDir = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
Write-Host '=== Rubika-Queu read-only signing audit ==='
Write-Host "Windows user profile: $homeDir"
Write-Host 'Never post passwords, keystore files, or unredacted private data.'

function Find-Executable([string]$name, [string]$fallback) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmd) { return $cmd.Source }
    if (Test-Path -LiteralPath $fallback -PathType Leaf) { return $fallback }
    return $null
}

$androidDebugKey = Join-Path $homeDir '.android\debug.keystore'
$projectTestKey = Join-Path $homeDir 'AndroidStudioProjects\Rubika-Queu\signing\test-signing.p12'
$roots = @(
    (Join-Path $homeDir 'AndroidStudioProjects'),
    (Join-Path $homeDir 'Desktop'),
    (Join-Path $homeDir 'Documents'),
    (Join-Path $homeDir 'Downloads')
) | Where-Object { Test-Path -LiteralPath $_ -PathType Container }

Write-Host ''
Write-Host '--- Keystore candidates: PATHS ONLY (no file contents) ---'
$keys = @()
foreach ($root in $roots) {
    $keys += @(Get-ChildItem -LiteralPath $root -File -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension.ToLowerInvariant() -in @('.p12', '.pfx', '.jks', '.keystore') })
}
foreach ($path in @($androidDebugKey, $projectTestKey)) {
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        $keys += Get-Item -LiteralPath $path
    }
}
$keys = @($keys | Sort-Object -Property FullName -Unique)
if ($keys.Count -eq 0) {
    Write-Host 'No candidate keystore found in the searched folders.'
} else {
    $keys | Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize
}

$keytool = Find-Executable 'keytool.exe' 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
if (-not $keytool) {
    Write-Host 'keytool was not found; install/open Android Studio or configure JAVA_HOME.'
} elseif (Test-Path -LiteralPath $androidDebugKey -PathType Leaf) {
    Write-Host ''
    Write-Host '--- Default Android Studio debug keystore certificate ---'
    # Only the documented Android default debug-keystore password is used here.
    # Do NOT pass a real signing password via command-line arguments.
    $data = @(& $keytool -list -v -keystore $androidDebugKey -storepass android -alias androiddebugkey 2>&1)
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Default debug key could not be opened using standard debug settings.'
    } else {
        $data | Where-Object { $_ -match 'SHA256:|SHA-256:|Alias name:' }
    }
}

$adb = Find-Executable 'adb.exe' (Join-Path $sdkDir 'platform-tools\adb.exe')
if (-not $adb) {
    Write-Host ''
    Write-Host 'ADB not found. Install Android SDK Platform-Tools and reconnect your phone.'
    return
}
Write-Host ''
Write-Host '--- USB ADB devices ---'
$deviceLines = @(& $adb devices)
$deviceLines | ForEach-Object { Write-Host $_ }
$authorized = @($deviceLines | Where-Object { $_ -match '^\S+\s+device\s*$' } |
    ForEach-Object { ($_ -split '\s+')[0] })
if (-not $DeviceSerial) {
    if ($authorized.Count -ne 1) {
        Write-Host 'Authorize one device, or rerun with -DeviceSerial <serial>.'
        return
    }
    $DeviceSerial = $authorized[0]
}
if ($DeviceSerial -notin $authorized) {
    Write-Host 'Chosen device is not ADB-authorized.'
    return
}

Write-Host ''
Write-Host "--- Installed package $PackageId on selected device ---"
$packageInfo = @(& $adb -s $DeviceSerial shell dumpsys package $PackageId)
$versionInfo = $packageInfo | Select-String -Pattern 'versionCode=\d+' | Select-Object -First 1
if ($versionInfo) { Write-Host $versionInfo.Line.Trim() }
$packagePaths = @(& $adb -s $DeviceSerial shell pm path $PackageId)
$baseApk = @($packagePaths | Where-Object { $_ -match '^package:.*base\.apk' } |
    Select-Object -First 1)
if ($baseApk.Count -eq 0) {
    Write-Host 'App is not installed or ADB cannot access its APK path.'
    return
}
$remotePath = ($baseApk[0] -replace '^package:', '').Trim()

$buildToolsRoot = Join-Path $sdkDir 'build-tools'
$apksigner = $null
if (Test-Path -LiteralPath $buildToolsRoot -PathType Container) {
    $candidate = Get-ChildItem -LiteralPath $buildToolsRoot -Directory -ErrorAction SilentlyContinue |
        Sort-Object -Property Name -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1
    if ($candidate) { $apksigner = $candidate }
}
if (-not $apksigner) {
    Write-Host 'apksigner.bat not found in Android SDK build-tools.'
    return
}

# Pull ONLY the installed APK to a unique local TEMP path; remove it immediately.
# Does not access, erase, or export Android application data.
$tempApk = Join-Path $env:TEMP ('rubika-signing-' + [guid]::NewGuid().ToString('N') + '.apk')
try {
    & $adb -s $DeviceSerial pull $remotePath $tempApk | Out-Null
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $tempApk -PathType Leaf)) {
        Write-Host 'Could not copy installed APK for local signature inspection.'
        return
    }
    Write-Host '--- INSTALLED APK signer certificate (SHA-256) ---'
    $verification = @(& $apksigner verify --print-certs $tempApk 2>&1)
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'apksigner verification failed; do not install an update.'
        return
    }
    $verification | Where-Object { $_ -match 'Signer #\d+ certificate SHA-256 digest:' }
} finally {
    Remove-Item -LiteralPath $tempApk -Force -ErrorAction SilentlyContinue
}
Write-Host ''
Write-Host 'Compare the INSTALLED APK SHA-256 signer with the local keystore signer.'
Write-Host 'Do NOT uninstall the currently installed application.'
