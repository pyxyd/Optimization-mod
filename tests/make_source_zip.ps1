
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

Add-Type -AssemblyName System.IO.Compression

$ToolsDir  = $PSScriptRoot
$RepoRoot  = Split-Path $ToolsDir -Parent
$OutDir    = Split-Path $RepoRoot -Parent

$toml = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'src\resources\mods.toml'))
$m = [regex]::Match($toml, 'version\s*=\s*"([^"]+)"')
if (-not $m.Success) { throw "could not read version from src\resources\mods.toml" }
$version = $m.Groups[1].Value

$excludeDirs = @('classes', 'staging', 'check')
$vcsDirs = @('.git', '.idea', '.vs', '__pycache__')

$zipName = "Optimization-for-Tacz-" + $version + "-src.zip"
$zipPath = Join-Path $OutDir $zipName
$inner   = "Optimization-for-Tacz-" + $version + "-src"

foreach ($need in @('build.ps1', 'LICENSE', 'README.md', 'icon.png',
                    'src\resources\mods.toml',
                    'src\resources\README.txt', 'src\resources\MANIFEST.MF')) {
    if (-not (Test-Path -LiteralPath (Join-Path $RepoRoot $need))) {
        throw ("repository is incomplete: missing " + $need)
    }
}
$docFile = Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'docs') -File -Filter '*.md' -ErrorAction SilentlyContinue |
    Select-Object -First 1
if (-not $docFile) { throw "repository is incomplete: docs\ has no .md file" }
$srcFileCount = @(Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'src') -Recurse -Filter '*.java').Count
if ($srcFileCount -lt 20) { throw ("src\ looks wrong: only " + $srcFileCount + " .java files") }
$testFileCount = @(Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'tests') -File -Filter '*.java').Count
if ($testFileCount -lt 4) { throw ("tests\ looks wrong: only " + $testFileCount + " .java checkers") }

$staging = Join-Path ([System.IO.Path]::GetTempPath()) ("srcpack_" + [guid]::NewGuid().ToString('N'))
$root = Join-Path $staging $inner
New-Item -ItemType Directory -Force -Path $root | Out-Null

$copied = 0
$skipped = 0
try {
    Get-ChildItem -LiteralPath $RepoRoot -Recurse -Force | ForEach-Object {
        $rel = $_.FullName.Substring($RepoRoot.Length).TrimStart('\')

        $parts = $rel -split '\\'
        if ($parts | Where-Object { $excludeDirs -contains $_ }) { $skipped++; return }
        if ($parts | Where-Object { $vcsDirs -contains $_ }) { $skipped++; return }

        $dest = Join-Path $root $rel
        if ($_.PSIsContainer) {
            New-Item -ItemType Directory -Force -Path $dest | Out-Null
        } else {
            $destDir = Split-Path $dest -Parent
            if (-not (Test-Path -LiteralPath $destDir)) {
                New-Item -ItemType Directory -Force -Path $destDir | Out-Null
            }
            Copy-Item -LiteralPath $_.FullName -Destination $dest -Force
            $copied++
        }
    }

    if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }

    $zipStream = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::CreateNew)
    $archive = New-Object System.IO.Compression.ZipArchive -ArgumentList $zipStream, ([System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem -LiteralPath $root -Recurse -File -Force | ForEach-Object {
            $rel = $_.FullName.Substring($staging.Length).TrimStart('\')
            $entryName = $rel.Replace('\', '/')
            $entry = $archive.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
            $es = $entry.Open()
            $fs = [System.IO.File]::OpenRead($_.FullName)
            try { $fs.CopyTo($es) } finally { $fs.Close(); $es.Close() }
        }
    } finally {
        $archive.Dispose()
        $zipStream.Dispose()
    }

    $problems = @()
    $zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $names = @($zip.Entries | ForEach-Object { $_.FullName })

        $required = @(
            "$inner/build.ps1",
            "$inner/README.md",
            "$inner/LICENSE",
            "$inner/icon.png",
            "$inner/src/resources/mods.toml",
            "$inner/src/resources/README.txt",
            "$inner/src/com/taczopt/TaczOptimizeMod.java",
            "$inner/tests/ModsTomlCheck.java"
        )
        foreach ($need in $required) {
            if (-not ($names -contains $need)) { $problems += ("zip is missing " + $need) }
        }

        if (-not ($names | Where-Object { $_ -match ('^' + [regex]::Escape($inner) + '/docs/[^/]+\.md$') })) {
            $problems += "zip is missing the docs/*.md documentation"
        }
        foreach ($bad in $excludeDirs) {
            if ($names | Where-Object { $_ -match ('/' + $bad + '/') }) {
                $problems += ("zip still contains build output: " + $bad + "/")
            }
        }

        $backslash = @($names | Where-Object { $_ -match '\\' })
        if ($backslash.Count -gt 0) {
            $problems += ("zip has " + $backslash.Count + " entry names using backslashes (must be '/')")
        }

        if ($names | Where-Object { $_ -match '/$' }) {
            $problems += "zip contains empty directory entries"
        }

        $iconEntry = $zip.Entries | Where-Object { $_.FullName -eq ($inner + '/icon.png') }
        $ms = New-Object System.IO.MemoryStream
        $es = $iconEntry.Open(); $es.CopyTo($ms); $es.Close()
        $sha = [System.Security.Cryptography.SHA256]::Create()
        $inZip = ([System.BitConverter]::ToString($sha.ComputeHash($ms.ToArray())) -replace '-', '')
        $ms.Dispose(); $sha.Dispose()
        $onDisk = (Get-FileHash (Join-Path $RepoRoot 'icon.png') -Algorithm SHA256).Hash
        if ($inZip -ne $onDisk) { $problems += "icon.png inside the zip differs from the repository-root icon.png" }
    } finally { $zip.Dispose() }

    Write-Host ("files packed: " + $copied + "  (entries skipped from build dirs: " + $skipped + ")")
    Write-Host ("zip: " + $zipPath)
    Write-Host ("size: " + [Math]::Round((Get-Item -LiteralPath $zipPath).Length / 1KB, 1) + " KB")
    if ($problems.Count -gt 0) {
        foreach ($p in $problems) { Write-Host ("  [FAIL] " + $p) }
        throw "source zip check failed -- the zip was written but it is wrong, see FAIL lines above"
    }
    Write-Host "  [OK]   build script, sources, resources, icon, docs and tests all present; no build output"
} finally {
    Remove-Item -LiteralPath $staging -Recurse -Force -ErrorAction SilentlyContinue
}
