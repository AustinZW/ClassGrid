[CmdletBinding()]
param(
    [switch]$Bundle
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $repoRoot
try {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot "keystore.properties"))) {
        throw "请先配置 keystore.properties，参见 RELEASE-SIGNING.md。"
    }

    $tasks = @("testDebugUnitTest", "lintDebug", "assembleDebug", "assembleRelease")
    if ($Bundle) { $tasks += "bundleRelease" }
    & (Join-Path $repoRoot "gradlew.bat") @tasks
    if ($LASTEXITCODE -ne 0) { throw "正式版构建失败（退出码 $LASTEXITCODE）。" }

    Write-Host "签名 APK：app/build/outputs/apk/release/app-release.apk"
    if ($Bundle) { Write-Host "签名 AAB：app/build/outputs/bundle/release/app-release.aab" }
} finally {
    Pop-Location
}
