# Packages the repository into a shareable source zip (the thing you attach to a
# release, separate from the built mod jar).
#
# Usage:  powershell -ExecutionPolicy Bypass -File .\tests\make_source_zip.ps1
# Output: <parent of the repository>\Optimization-for-Tacz-<version>-src.zip
#
# WHY a script instead of "right click > send to zip":
#   * classes\ staging\ check\ must NOT ship -- that is compiler output, and an archive
#     containing them looks like a build tree nobody cleaned;
#   * src\resources\icon.png MUST ship, because build.ps1 refuses to build without it;
#   * entry names must use FORWARD SLASHES. ZipFile.CreateFromDirectory on Windows writes
#     them with backslashes, and then tools on Linux/macOS either show one flat file or
#     fail to extract;
#   * the archive needs one top-level folder so unzipping does not scatter files into
#     whatever directory the user happens to be in.
# This script asserts all of those before it writes the zip.
#
# KEEP THIS FILE ASCII-ONLY (Windows PowerShell 5.1 reads a BOM-less .ps1 as ANSI).
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem
# ZipArchive / ZipArchiveMode live in System.IO.Compression itself; FileSystem alone
# only provides ZipFile. Without this line: "type ZipArchiveMode not found".
Add-Type -AssemblyName System.IO.Compression

$ToolsDir  = $PSScriptRoot                  # ...\tests
$RepoRoot  = Split-Path $ToolsDir -Parent   # the repository root
$OutDir    = Split-Path $RepoRoot -Parent   # put the zip next to the repo

# Version comes from mods.toml so the zip name can never disagree with the mod.
$toml = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'src\resources\mods.toml'))
$m = [regex]::Match($toml, 'version\s*=\s*"([^"]+)"')
if (-not $m.Success) { throw "could not read version from src\resources\mods.toml" }
$version = $m.Groups[1].Value

# Intermediate build output. Never ship these.
$excludeDirs = @('classes', 'staging', 'check')

$zipName = "Optimization-for-Tacz-" + $version + "-src.zip"
$zipPath = Join-Path $OutDir $zipName
$inner   = "Optimization-for-Tacz-" + $version + "-src"

# Sanity: the things that make the source usable must exist.
# NOTE: keep every literal here ASCII-only. A literal non-ASCII file name arrives as
# mojibake and the check fails on a file that is right there, so docs\ is checked by
# pattern instead of by name.
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
        # skip anything inside an excluded directory (and the directories themselves)
        $parts = $rel -split '\\'
        if ($parts | Where-Object { $excludeDirs -contains $_ }) { $skipped++; return }

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

    # VCS metadata would bloat the zip and leak history
    foreach ($junk in @('.git', '.idea', '.vs', '__pycache__')) {
        $p = Join-Path $root $junk
        if (Test-Path -LiteralPath $p) { Remove-Item $p -Recurse -Force; Write-Host ("  dropped " + $junk) }
    }

    if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }

    # Build the zip entry by entry instead of ZipFile.CreateFromDirectory, so the entry
    # names use '/' (see the header) and empty directories are skipped entirely.
    # NOTE: CreateEntry's name must use '/' even here.
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

    # ---- verify ---------------------------------------------------------------------
    $problems = @()
    $zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $names = @($zip.Entries | ForEach-Object { $_.FullName })
        # Build the required-name list with string interpolation, NOT "@($inner + '/a', $inner + '/b')".
        # In PowerShell the COMMA BINDS TIGHTER THAN "+", so that form parses as
        #   $inner + ('/a', '/b')  -> an ARRAY CONCATENATION -> ONE string "/a /b",
        #   i.e. a one-element array. The -contains check then always fails and the error
        #   lists every path joined by spaces. Measured: @($v+'/x', $v+'/y').Count is 1,
        #   while @("$v/x", "$v/y").Count is 2.
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
        # docs\, checked by extension so no non-ASCII literal is needed
        if (-not ($names | Where-Object { $_ -match ('^' + [regex]::Escape($inner) + '/docs/[^/]+\.md$') })) {
            $problems += "zip is missing the docs/*.md documentation"
        }
        foreach ($bad in $excludeDirs) {
            if ($names | Where-Object { $_ -match ('/' + $bad + '/') }) {
                $problems += ("zip still contains build output: " + $bad + "/")
            }
        }
        # every entry must use forward slashes, or non-Windows tools choke on the archive
        $backslash = @($names | Where-Object { $_ -match '\\' })
        if ($backslash.Count -gt 0) {
            $problems += ("zip has " + $backslash.Count + " entry names using backslashes (must be '/')")
        }
        # no stray empty directory entries either
        if ($names | Where-Object { $_ -match '/$' }) {
            $problems += "zip contains empty directory entries"
        }
        # the icon inside the zip must still be byte-identical to the working copy
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
