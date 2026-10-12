# Build script for "Optimization for Tacz" -- an optimization-only add-on for
# Timeless and Classics Zero (TaCZ). Minecraft 1.20.1 / Forge.
#
#   powershell -ExecutionPolicy Bypass -File .\build.ps1
#   powershell -ExecutionPolicy Bypass -File .\build.ps1 -GameDir "D:\pcl2\.minecraft\versions\1.20.1-Forge_47.4.26"
#
# Defaults to a sibling folder of this repository that looks like a Minecraft instance
# (a version json plus a libraries\ folder). Pass -GameDir when that guess is wrong.
#
# ⚠ KEEP THIS FILE ASCII-ONLY. Windows PowerShell 5.1 reads a BOM-less .ps1 as ANSI, so
#   a literal non-ASCII character here arrives as mojibake and lands on disk wrong.
#   That is also why the output jar name is built from code points instead of typed out.
#
# This mod only changes how things look and feel. It does NOT add attachment slots, does
# NOT add items, and does NOT write into the gun packs. The old "barrel slot" feature
# (loader jar + enum injection + a gun pack) was removed on purpose -- that was creating
# content, not optimizing. Do not reintroduce it.
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------------
# paths
# ---------------------------------------------------------------------------------
# No param() block on purpose: param() must be the very first statement in a script, and
# a stray $ErrorActionPreference above it would break the file. -GameDir is read from
# $args instead, which works the same way for "-File build.ps1 -GameDir X".
$RepoRoot  = $PSScriptRoot
$SrcRoot   = Join-Path $RepoRoot "src"
$CheckRoot = Join-Path $RepoRoot "tests"

$GameArg = $null
for ($i = 0; $i -lt $args.Count; $i++) {
    if ($args[$i] -eq '-GameDir' -and ($i + 1) -lt $args.Count) { $GameArg = $args[$i + 1] }
}
if ($GameArg) { $GameDir = $GameArg } else { $GameDir = Split-Path $RepoRoot -Parent }

# The output file name is deliberately NOT the display name. Forge style:
#   [<display prefix>]<english>-<mcver>-<modver>-forge.jar
# U+FF1A below is a FULLWIDTH COLON and it is intentional -- do not "fix" it to a plain
# ':'. The display name inside src\resources\mods.toml stays "Optimization for Tacz".
$OutPrefix = "[TaCZ" + [char]0xFF1A + [char]0x4F18 + [char]0x5316 + "]"
$OutName   = $OutPrefix + "Optimization-1.20.1-2.0.0-forge.jar"
$OutJar    = [System.IO.Path]::Combine((Join-Path $GameDir "mods"), $OutName)

# ---------------------------------------------------------------------------------
# locate a JDK 17 and the Minecraft/Forge jars
# ---------------------------------------------------------------------------------
if (-not (Test-Path (Join-Path $GameDir "mods"))) { throw ("not a game folder: " + $GameDir) }
$Minecraft = Split-Path (Split-Path $GameDir -Parent) -Parent
$Libraries = Join-Path $Minecraft "libraries"
if (-not (Test-Path $Libraries)) { throw ("libraries not found at " + $Libraries + " -- pass -GameDir") }

# Find a JDK 17. Search the whole drive set for an Adoptium build first (that is what
# this project was developed against), then fall back to whatever javac is on PATH.
$Javac = Get-ChildItem "D:\", "C:\" -Directory -ErrorAction SilentlyContinue |
    ForEach-Object { Join-Path $_.FullName "Eclipse Adoptium" } |
    Where-Object { Test-Path $_ } |
    ForEach-Object { Get-ChildItem $_ -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue } |
    ForEach-Object { Join-Path $_.FullName "bin\javac.exe" } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1
if (-not $Javac) {
    $onPath = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($onPath) { $Javac = $onPath.Source }
}
if (-not $Javac) { throw "JDK 17 (javac.exe) not found -- install a JDK 17 and re-run" }
# NOTE: $Javac is "...\jdk-17\bin\javac.exe". Go up TWICE for the JDK root; stopping at
# "bin" makes Join-Path silently yield nothing and the checkers cannot run.
$JdkBin  = Split-Path $Javac -Parent
$Jdk     = Split-Path $JdkBin -Parent
$JarExe  = Join-Path $JdkBin "jar.exe"
$JavaExe = Join-Path $JdkBin "java.exe"

# ---------------------------------------------------------------------------------
# helper: run a native tool and return its exit code without aborting the script
# ---------------------------------------------------------------------------------
#
# WHY THIS EXISTS (every point below was an actual failure, not a precaution):
#
#  * javac writes ordinary NOTES to stderr ("uses unchecked or unsafe operations",
#    "[removal] ... has been deprecated"). This script runs with
#    $ErrorActionPreference = "Stop", and PowerShell turns a native command's stderr into
#    NativeCommandError records -- so a build that COMPILES FINE died at the javac line
#    with the mojibake text of a harmless note and nothing was packaged. Suppressing
#    stderr is not an option either: real compile errors live there.
#
#  * Going through cmd.exe (to use "2>file") is not an option: cmd refuses command lines
#    over 8191 characters and this project's javac classpath alone is ~11300 characters,
#    so even batching the sources cannot fit. javac's own "@argfile" escape hatch was
#    tried and abandoned as well -- it is read with the platform default charset, and no
#    encoding we tried reproduced the non-ASCII TaCZ jar name, so javac kept reporting
#    "invalid flag: tacz-1.20.1-...jar".
#
#  * A native command's STDOUT is pipeline output, and everything a function emits
#    becomes part of its return value. "& $Exe @args; return $LASTEXITCODE" therefore
#    returns the tool's output lines PLUS the exit code, and "$rc -ne 0" is then always
#    true -- that is how a successful ModsTomlCheck once reported "validation failed".
#    Capture stdout into a variable instead of letting it flow to the pipeline.
#
#  * Do not trust $LASTEXITCODE alone: a tool that prints nothing can leave a stale value
#    behind (MixinsCheck.class was never produced while the exit code looked fine).
#    Callers below verify the produced artifact as well.
function Invoke-Native {
    param([string]$Exe, [string[]]$Arguments)
    $errFile = [System.IO.Path]::GetTempFileName()
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        # "@Arguments" splatting makes PowerShell quote every argument itself: no manual
        # quoting, no Invoke-Expression, and no cmd.exe length limit.
        $out = & $Exe @Arguments 2> $errFile
        $code = $LASTEXITCODE
        if ($out) { Write-Host (($out | Out-String).TrimEnd()) }
        if (Test-Path $errFile) {
            $stderr = [System.IO.File]::ReadAllText($errFile)
            if ($stderr.Trim().Length -gt 0) { Write-Host $stderr.TrimEnd() }
        }
        if ($code -ne 0) { Write-Host ("[Invoke-Native] exit " + $code + " <- " + $Exe) }
        return $code
    } finally {
        $ErrorActionPreference = $prev
        Remove-Item $errFile -Force -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------------------------
# 1) classpath
# ---------------------------------------------------------------------------------
$verJson = Get-ChildItem $GameDir -Filter "*.json" | Where-Object { $_.Name -match "Forge" } | Select-Object -First 1
if (-not $verJson) { throw ("no Forge version json in " + $GameDir) }
$libs = (Get-Content $verJson.FullName -Raw -Encoding UTF8 | ConvertFrom-Json).libraries
$cp = New-Object System.Collections.Generic.List[string]
foreach ($l in $libs) {
    $rel = $l.downloads.artifact.path
    if (-not $rel) { continue }
    $p = Join-Path $Libraries ($rel -replace "/", "\")
    if (Test-Path $p) { $cp.Add($p) }
}
# The forge client jar is patched Minecraft: real member names plus Forge's extended
# KeyMapping constructor. Prepend it so it wins over the plain SRG jar.
$forgeClient = (Get-ChildItem (Join-Path $Libraries "net\minecraftforge\forge") -Recurse -Filter "*-client.jar" |
    Where-Object { $_.FullName -match "47\.4\.26" } | Select-Object -First 1).FullName
if ($forgeClient) { $cp.Insert(0, $forgeClient) }
$cp.Add((Get-ChildItem (Join-Path $Libraries "net\minecraft\client") -Recurse -Filter "*-srg.jar" | Select-Object -First 1).FullName)
$cp.Add((Get-ChildItem (Join-Path $Libraries "net\minecraftforge\forge") -Recurse -Filter "*-universal.jar" | Select-Object -First 1).FullName)
$cp.Add((Get-ChildItem (Join-Path $Libraries "net\minecraftforge\javafmllanguage") -Recurse -Filter "*.jar" | Select-Object -First 1).FullName)
$cp.Add((Get-ChildItem (Join-Path $Libraries "net\minecraftforge\fmlcore") -Recurse -Filter "*.jar" | Select-Object -First 1).FullName)
$cp.Add((Get-ChildItem (Join-Path $Libraries "net\minecraftforge\fmlloader") -Recurse -Filter "*.jar" | Select-Object -First 1).FullName)
$tacz = (Get-ChildItem (Join-Path $GameDir "mods") -Filter "*tacz-1.20.1*.jar" | Select-Object -First 1).FullName
if (-not $tacz) { throw "TaCZ jar not found in mods\ -- this mod compiles against it" }
$cp.Add($tacz)

# ---------------------------------------------------------------------------------
# 2) compile
# ---------------------------------------------------------------------------------
$classes = Join-Path $RepoRoot "classes"
Remove-Item $classes -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$srcs = Get-ChildItem $SrcRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$rc = Invoke-Native $Javac (@("-encoding", "UTF-8", "-nowarn", "-proc:none", "-cp", ($cp -join ";"), "-d", $classes) + @($srcs))
if ($rc -ne 0) { throw "javac failed" }
if (-not (Test-Path (Join-Path $classes "com\taczopt\TaczOptimizeMod.class"))) {
    throw "javac produced no TaczOptimizeMod.class -- the main sources did not compile"
}

# ---------------------------------------------------------------------------------
# 3) package (Forge only recognises META-INF/mods.toml)
# ---------------------------------------------------------------------------------
$ResRoot  = Join-Path $SrcRoot "resources"
$staging  = Join-Path $RepoRoot "staging"
Remove-Item $staging -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $staging | Out-Null
Copy-Item (Join-Path $classes "*") $staging -Recurse -Force
Copy-Item (Join-Path $ResRoot "taczopt.mixins.json") $staging -Force
Copy-Item (Join-Path $ResRoot "pack.mcmeta") $staging -Force
Copy-Item (Join-Path $ResRoot "assets") $staging -Recurse -Force

# The mod-list icon goes at the jar ROOT (Forge reads icon.png from there, not from
# assets\). It lives at the REPOSITORY ROOT rather than under src\resources\ so the
# README can display it with a simple relative path on GitHub. Copy it VERBATIM: it is a
# hand-made PNG, and re-encoding it through any image library changes the file even when
# every pixel stays identical.
$iconSrc = Join-Path $RepoRoot "icon.png"
if (-not (Test-Path $iconSrc)) {
    throw "icon.png is missing from the repository root -- that is the mod-list icon"
}
Copy-Item $iconSrc $staging -Force

# README.txt also sits at the jar root so anyone who opens the jar finds the project
# links. Forge supports only one URL (displayURL in mods.toml), so the rest live here.
$readmeSrc = Join-Path $ResRoot "README.txt"
if (-not (Test-Path $readmeSrc)) {
    throw "src\resources\README.txt is missing -- it carries the project links"
}
Copy-Item $readmeSrc $staging -Force

$metaInf = Join-Path $staging "META-INF"
New-Item -ItemType Directory -Force -Path $metaInf | Out-Null
Copy-Item (Join-Path $ResRoot "mods.toml") (Join-Path $metaInf "mods.toml") -Force

# ---------------------------------------------------------------------------------
# 3b) validate mods.toml with the SAME toml library Forge uses (night-config)
# ---------------------------------------------------------------------------------
# A stray extra quote ('''' instead of ''') is not a Java compile error and not caught by
# counting quotes, but makes Forge abort at startup with "contains a corrupt or
# misconfigured toml". So parse it for real here.
$tomlJar = (Get-ChildItem (Join-Path $Libraries "com\electronwill\night-config\toml") -Recurse -Filter "*.jar" |
    Sort-Object FullName -Descending | Select-Object -First 1).FullName
$coreJar = (Get-ChildItem (Join-Path $Libraries "com\electronwill\night-config\core") -Recurse -Filter "*.jar" |
    Sort-Object FullName -Descending | Select-Object -First 1).FullName
if (-not $tomlJar -or -not $coreJar) { throw "night-config toml/core jars not found under libraries" }
$checkDir = Join-Path $RepoRoot "check"
Remove-Item $checkDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $checkDir | Out-Null
$rc = Invoke-Native $Javac @("-encoding", "UTF-8", "-nowarn", "-cp", "$tomlJar;$coreJar", "-d", $checkDir, (Join-Path $CheckRoot "ModsTomlCheck.java"))
if ($rc -ne 0) { throw "ModsTomlCheck compile failed" }
$rc = Invoke-Native $JavaExe @("-Dfile.encoding=UTF-8", "-cp", "$checkDir;$tomlJar;$coreJar", "ModsTomlCheck", (Join-Path $metaInf "mods.toml"))
if ($rc -ne 0) { throw "mods.toml validation failed (see FAIL line above)" }

# ---------------------------------------------------------------------------------
# 3c) every @Mixin class must be listed in the mixins json
# ---------------------------------------------------------------------------------
# Missing one is neither a compile error nor a startup error: it throws
# IllegalClassLoadError the first time that code path runs and crashes the game. Two
# @Accessor interfaces were dropped when this mod was merged from four smaller ones and
# the result was "the game crashes the moment a bullet hole appears".
$rc = Invoke-Native $Javac @("-encoding", "UTF-8", "-nowarn", "-d", $checkDir, (Join-Path $CheckRoot "MixinsCheck.java"))
# NOTE: do NOT trust the exit code here -- a javac that prints nothing can leave a stale
# value behind, so this check once passed while MixinsCheck.class was never produced and
# the next step died with "ClassNotFoundException: MixinsCheck". Verify the artifact.
if (-not (Test-Path (Join-Path $checkDir "MixinsCheck.class"))) {
    Write-Host "MixinsCheck.class missing after javac; retrying with output shown"
    $rc = Invoke-Native $Javac @("-encoding", "UTF-8", "-nowarn", "-d", $checkDir, (Join-Path $CheckRoot "MixinsCheck.java"))
    if (-not (Test-Path (Join-Path $checkDir "MixinsCheck.class"))) {
        throw "MixinsCheck did not compile (see javac output above)"
    }
}
$rc = Invoke-Native $JavaExe @("-Dfile.encoding=UTF-8", "-cp", $checkDir, "MixinsCheck", $SrcRoot, (Join-Path $ResRoot "taczopt.mixins.json"))
if ($rc -ne 0) { throw "mixins.json check failed: some @Mixin class is not registered" }

# ---------------------------------------------------------------------------------
# 3d) the mixin target and its argument order must still match TaCZ's bytecode
# ---------------------------------------------------------------------------------
# MixinsCheck only proves "the class is registered". It cannot see a target that no
# longer exists, and it cannot see swapped arguments. Both happened:
#   1) @ModifyVariable(ordinal=10) located a local slot that moved -> silently did
#      nothing (require = 0 does not warn)
#   2) the handler was written against TaCZ's formal parameter NAMES -- the last two
#      floats are named the other way round, so the bullet VELOCITY was zeroed instead
#      of the spread (bullets dropped to the ground, knockback went to zero)
# InjectionCheck walks the operand stack at the Projectile.m_37251_ call site with ASM
# and derives which method parameter feeds each argument, so it does not depend on names.
$asmCp = New-Object System.Collections.Generic.List[string]
foreach ($a in @("asm", "asm-tree")) {
    $j = (Get-ChildItem (Join-Path $Libraries "org\ow2\asm\$a") -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1)
    if ($j) { $asmCp.Add($j.FullName) }
}
if ($asmCp.Count -ge 2) {
    $rc = Invoke-Native $Javac @("-encoding", "UTF-8", "-nowarn", "-cp", ($asmCp -join ";"), "-d", $checkDir, (Join-Path $CheckRoot "InjectionCheck.java"))
    if ($rc -ne 0) { throw "InjectionCheck compile failed" }
    $rc = Invoke-Native $JavaExe @("-Dfile.encoding=UTF-8", "-cp", ($checkDir + ";" + ($asmCp -join ";")), "InjectionCheck", $tacz)
    if ($rc -ne 0) { throw "InjectionCheck failed: a mixin target or its parameter order no longer matches TaCZ" }
} else {
    Write-Host "WARN: asm jars not found, skipped InjectionCheck"
}

# ---------------------------------------------------------------------------------
# 3e) offline unit checks for the pure-logic classes
# ---------------------------------------------------------------------------------
# Crosshair geometry, config migration rules, hit-marker geometry and colours. These
# depend only on the game-free classes under src\, so they run with $classes as the
# whole classpath -- no Minecraft needed.
$logicChecks = @("CrosshairCheck", "ConfigUpgradeCheck", "HitFeedbackCheck")
foreach ($name in $logicChecks) {
    $file = Join-Path $CheckRoot ($name + ".java")
    if (-not (Test-Path $file)) { throw ("missing logic checker: " + $name + ".java") }
    $rc = Invoke-Native $Javac @("-encoding", "UTF-8", "-nowarn", "-cp", $classes, "-d", $checkDir, $file)
    if ($rc -ne 0) { throw ($name + " compile failed") }
    if (-not (Test-Path (Join-Path $checkDir ($name + ".class")))) {
        throw ($name + " did not compile (no .class produced)")
    }
    $rc = Invoke-Native $JavaExe @("-Dfile.encoding=UTF-8", "-cp", ($checkDir + ";" + $classes), $name)
    if ($rc -ne 0) { throw ($name + " failed (see FAIL lines above)") }
    Write-Host ($name + " passed")
}

Remove-Item $OutJar -Force -ErrorAction SilentlyContinue
$rc = Invoke-Native $JarExe @("--create", "--file", $OutJar, "--manifest", (Join-Path $ResRoot "MANIFEST.MF"), "-C", $staging, ".")
if ($rc -ne 0) { throw "jar packaging failed" }

# ---------------------------------------------------------------------------------
# 4) verify the packaged jar really contains what Forge needs
# ---------------------------------------------------------------------------------
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($OutJar)
try {
    $names = @($zip.Entries | ForEach-Object { $_.FullName })
    # The collection goes on the LEFT of -contains. This list uses plain literals on
    # purpose: a list built as @($v + '/a', $v + '/b') is silently ONE element, because
    # the comma binds tighter than "+" in PowerShell.
    foreach ($need in @("META-INF/mods.toml", "pack.mcmeta", "taczopt.mixins.json")) {
        if (-not ($names -contains $need)) { throw ("jar is missing " + $need) }
    }
    if (-not ($names -contains "icon.png")) { throw "jar is missing icon.png at the root" }

    # The icon must be the hand-made file byte for byte.
    $srcHash = (Get-FileHash $iconSrc -Algorithm SHA256).Hash
    $entry = $zip.Entries | Where-Object { $_.FullName -eq "icon.png" }
    $ms = New-Object System.IO.MemoryStream
    $es = $entry.Open()
    $es.CopyTo($ms); $es.Close()
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $jarHash = ([System.BitConverter]::ToString($sha.ComputeHash($ms.ToArray())) -replace '-', '')
    $ms.Dispose(); $sha.Dispose()
    if ($srcHash -ne $jarHash) {
        throw "packaged icon.png differs from the repository-root icon.png -- something re-encoded it"
    }
    Write-Host ("icon.png in jar: byte-identical (" + $srcHash.Substring(0, 16) + "...)")

    # The project links must survive into the packaged README.
    if (-not ($names -contains "README.txt")) { throw "jar is missing README.txt at the root" }
    $readmeInJar = $zip.Entries | Where-Object { $_.FullName -eq "README.txt" }
    $rs = New-Object System.IO.StreamReader($readmeInJar.Open(), [System.Text.Encoding]::UTF8)
    $readmeText = $rs.ReadToEnd(); $rs.Close()
    foreach ($link in @("https://github.com/pyxyd/Optimization-mod",
                        "https://modrinth.com/mod/optimization-forge")) {
        if ($readmeText -notmatch [regex]::Escape($link)) {
            throw ("README.txt is missing the link " + $link)
        }
    }
    Write-Host "README.txt in jar: both project links present"

    $e = $zip.Entries | Where-Object { $_.FullName -eq "META-INF/mods.toml" }
    $sr = New-Object System.IO.StreamReader($e.Open(), [System.Text.Encoding]::UTF8)
    $inJar = $sr.ReadToEnd(); $sr.Close()
    if ($inJar -match "''''") { throw "packaged mods.toml still has four quotes" }
    if ($inJar -notmatch 'modId="taczopt"') { throw "packaged mods.toml has no modId=taczopt" }
    if ($inJar -notmatch [regex]::Escape("displayURL=")) { throw "packaged mods.toml has no displayURL" }

    # This mod must NOT be a transformation service: Forge's ModsFolderLocator silently
    # drops every jar that provides ITransformationService from the mod list -- no log,
    # no error. That is exactly how the whole mod once vanished from the mod list.
    $svc = "META-INF/services/cpw.mods.modlauncher.api.ITransformationService"
    if ($names -contains $svc) {
        throw "mod jar contains the ITransformationService file: Forge would exclude this jar"
    }

    # Optimization-only: no attachment-slot content may sneak back in.
    $banned = @("assets/tacz/textures/gui/refit_slot_icons.png",
                "assets/taczopt/textures/gui/refit_slot_barrel.png",
                "com/taczopt/barrelslot/BarrelSlot.class",
                "com/taczopt/mixin/BarrelSlotButtonMixin.class",
                "com/taczopt/mixin/BarrelCreativeTabMixin.class",
                "com/taczopt/slot/BarrelSlotTransformer.class")
    foreach ($bad in $banned) {
        if ($names -contains $bad) {
            throw ("jar still contains barrel-slot leftover " + $bad)
        }
    }
} finally { $zip.Dispose() }
Write-Host "mod jar content check passed"

# ---------------------------------------------------------------------------------
# 5) tidy up leftovers in the game folder
# ---------------------------------------------------------------------------------
# A loader jar providing ITransformationService would keep injecting the removed BARREL
# enum constant into TaCZ even though this mod no longer wants it.
$staleLoader = Get-ChildItem (Join-Path $GameDir "mods") -Filter "*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "*" + [char]0x67AA + [char]0x7BA1 + [char]0x69FD + [char]0x4F4D + "*" }
foreach ($f in $staleLoader) {
    Remove-Item $f.FullName -Force
    Write-Host ("removed stale loader jar: " + $f.Name)
}
# ...and the gun pack that only existed to provide the barrel attachment.
$stalePack = Join-Path (Join-Path $GameDir "tacz") ([char]0x6025 + [char]0x901F + [char]0x63E1 + [char]0x628A + [char]0x4E00 + [char]0x4F53 + [char]0x67AA + [char]0x7BA1)
if (Test-Path $stalePack) {
    Remove-Item $stalePack -Recurse -Force
    Write-Host "removed stale barrel gun pack"
}
# Two jars with modId "taczopt" make Forge abort, and the file name has changed twice
# already, so an older copy under the old name is likely to be lying around. The match
# is deliberately narrow ("optimization" in the name) so a mod that merely mentions TaCZ
# is never touched.
$staleMine = Get-ChildItem (Join-Path $GameDir "mods") -Filter "*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "(?i)optimization" -and $_.FullName -ne $OutJar }
foreach ($f in $staleMine) {
    Remove-Item $f.FullName -Force
    Write-Host ("removed stale copy of this mod: " + $f.Name)
}

Write-Host ("OK -> " + $OutJar)
