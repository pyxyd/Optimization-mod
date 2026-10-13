# Removes comments from Java and PowerShell sources in this repository.
#
#   powershell -ExecutionPolicy Bypass -File .\tests\strip_comments.ps1 -DryRun
#   powershell -ExecutionPolicy Bypass -File .\tests\strip_comments.ps1
#
# WHY a real scanner instead of a regex: a naive "remove everything after //" destroys
# any line containing a URL or a string with a slash. Examples that exist in this very
# codebase:
#     "Config: config/taczopt.properties | GitHub: https://github.com/..."
#     $svc = "META-INF/services/cpw.mods.modlauncher.api.ITransformationService"
#     'https://modrinth.com/mod/optimization-forge'
# A regex cannot tell a comment from a URL. This walks the text character by character
# and tracks whether it is inside a string, a char literal, or between """ and @"/"@.
#
# What it does NOT touch: string literals, char literals, and the code itself. Only
# // line comments, /* block comments */ and PowerShell's <# block comments #> go away.
#
# Guard rails:
#   * refuses to run on a dirty git tree unless -Force is given, so the change can
#     always be reviewed with "git diff" and reverted with "git checkout";
#   * -DryRun reports what would change without writing;
#   * reports the before/after line counts per file.
#
# ⚠ Comments in the PowerShell scripts are load-bearing documentation (they explain the
#   stderr/exit-code/comma-precedence traps). Stripping them is permanent -- recover them
#   with "git checkout <file>" if needed, or read the matching notes kept by hand in
#   docs\DEVELOPMENT.md and README.md.
$ErrorActionPreference = 'Stop'

$ToolsDir = $PSScriptRoot
$RepoRoot = Split-Path $ToolsDir -Parent

$DryRun = $false
$Force  = $false
foreach ($a in $args) {
    if ($a -eq '-DryRun') { $DryRun = $true }
    if ($a -eq '-Force')  { $Force  = $true }
}

$GitExe = 'C:\Users\50607\AppData\Local\GitHubDesktop\app-3.5.4\resources\app\git\cmd\git.exe'
if (-not (Test-Path $GitExe)) {
    $onPath = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($onPath) { $GitExe = $onPath.Source }
}
if (-not $DryRun -and -not $Force) {
    if (Test-Path $GitExe) {
        $dirty = & $GitExe -C $RepoRoot status --porcelain 2>&1
        if ($dirty) {
            throw "the working tree has uncommitted changes -- commit first so this edit can be reviewed and reverted (or pass -Force)"
        }
    } else {
        Write-Host "WARN: git not found, skipping the clean-tree check"
    }
}

function Remove-SlashComments {
    <#
      Strips // and /* */ comments from a C-like file (Java).
      Tracks string and char literals so a "//" inside them is left alone.
      Text blocks (""" ... """) are handled too: they may contain anything.
      Returns @{ Text; Removed }.
    #>
    param([string]$Text)

    $sb = New-Object System.Text.StringBuilder
    $n = $Text.Length
    $i = 0
    $inStr = $false
    $inChar = $false
    $inTextBlock = $false
    $inLine = $false
    $inBlock = $false
    $removed = 0

    while ($i -lt $n) {
        $c = $Text[$i]
        $c2 = if ($i + 1 -lt $n) { $Text[$i + 1] } else { [char]0 }

        if ($inLine) {
            if ($c -eq "`n") { $inLine = $false; [void]$sb.Append($c); $i++; continue }
            $removed++; $i++; continue
        }
        if ($inBlock) {
            if ($c -eq '*' -and $c2 -eq '/') { $inBlock = $false; $removed += 2; $i += 2; continue }
            if ($c -eq "`n") { [void]$sb.Append($c) }   # keep line numbering sane
            else { $removed++ }
            $i++; continue
        }
        if ($inTextBlock) {
            if ($c -eq '"' -and $c2 -eq '"' -and ($i + 2 -lt $n) -and $Text[$i + 2] -eq '"') {
                [void]$sb.Append('"""'); $inTextBlock = $false; $i += 3; continue
            }
            [void]$sb.Append($c); $i++; continue
        }
        if ($inStr) {
            [void]$sb.Append($c)
            if ($c -eq '\') { if ($i + 1 -lt $n) { [void]$sb.Append($c2); $i += 2; continue } }
            elseif ($c -eq '"') { $inStr = $false }
            $i++; continue
        }
        if ($inChar) {
            [void]$sb.Append($c)
            if ($c -eq '\') { if ($i + 1 -lt $n) { [void]$sb.Append($c2); $i += 2; continue } }
            elseif ($c -eq "'") { $inChar = $false }
            $i++; continue
        }

        # not inside anything
        if ($c -eq '"' -and $c2 -eq '"' -and ($i + 2 -lt $n) -and $Text[$i + 2] -eq '"') {
            [void]$sb.Append('"""'); $inTextBlock = $true; $i += 3; continue
        }
        if ($c -eq '"') { $inStr = $true; [void]$sb.Append($c); $i++; continue }
        if ($c -eq "'") { $inChar = $true; [void]$sb.Append($c); $i++; continue }
        if ($c -eq '/' -and $c2 -eq '/') { $inLine = $true; $i += 2; continue }
        if ($c -eq '/' -and $c2 -eq '*') { $inBlock = $true; $i += 2; continue }

        [void]$sb.Append($c); $i++
    }
    return @{ Text = $sb.ToString(); Removed = $removed }
}

function Remove-PowerShellComments {
    <#
      Strips hash and angle-bracket block comments from a PowerShell script.
      Tracks single-quoted and double-quoted strings (including doubled-quote escapes)
      so a hash sign inside a string -- a colour code, a URL fragment -- is left alone.
      PowerShell has no C-style block comments; a "//" only ever appears inside a URL
      here, never as a comment, so this does not treat it as one.
    #>
    param([string]$Text)

    $sb = New-Object System.Text.StringBuilder
    $n = $Text.Length
    $i = 0
    $inSingle = $false
    $inDouble = $false
    $inLine = $false
    $inBlock = $false
    $removed = 0

    while ($i -lt $n) {
        $c = $Text[$i]
        $c2 = if ($i + 1 -lt $n) { $Text[$i + 1] } else { [char]0 }
        $c3 = if ($i + 2 -lt $n) { $Text[$i + 2] } else { [char]0 }

        if ($inLine) {
            if ($c -eq "`n") { $inLine = $false; [void]$sb.Append($c); $i++; continue }
            $removed++; $i++; continue
        }
        if ($inBlock) {
            if ($c -eq '#' -and $c2 -eq '>') { $inBlock = $false; $removed += 2; $i += 2; continue }
            if ($c -eq "`n") { [void]$sb.Append($c) } else { $removed++ }
            $i++; continue
        }
        if ($inSingle) {
            [void]$sb.Append($c)
            if ($c -eq "'" -and $c2 -eq "'") { [void]$sb.Append($c2); $i += 2; continue }
            elseif ($c -eq "'") { $inSingle = $false }
            $i++; continue
        }
        if ($inDouble) {
            [void]$sb.Append($c)
            if ($c -eq '`') { if ($i + 1 -lt $n) { [void]$sb.Append($c2); $i += 2; continue } }
            elseif ($c -eq '"' -and $c2 -eq '"') { [void]$sb.Append($c2); $i += 2; continue }
            elseif ($c -eq '"') { $inDouble = $false }
            $i++; continue
        }

        if ($c -eq '<' -and $c2 -eq '#') { $inBlock = $true; $i += 2; continue }
        if ($c -eq '#') { $inLine = $true; $i++; continue }
        if ($c -eq "'") { $inSingle = $true; [void]$sb.Append($c); $i++; continue }
        if ($c -eq '"') { $inDouble = $true; [void]$sb.Append($c); $i++; continue }

        [void]$sb.Append($c); $i++
    }
    return @{ Text = $sb.ToString(); Removed = $removed }
}

function Compress-BlankLines {
    <# Collapses runs of 3+ blank lines down to one, and trims trailing blank lines. #>
    param([string]$Text)
    $lines = $Text -split "`n"
    $out = New-Object System.Collections.ArrayList
    $blank = 0
    foreach ($l in $lines) {
        if ($l.Trim().Length -eq 0) {
            $blank++
            if ($blank -le 1) { [void]$out.Add('') }
        } else {
            $blank = 0
            [void]$out.Add($l.TrimEnd())
        }
    }
    while ($out.Count -gt 0 -and $out[$out.Count - 1] -eq '') { $out.RemoveAt($out.Count - 1) }
    return (($out -join "`n") + "`n")
}

$targets = @()
$targets += Get-ChildItem (Join-Path $RepoRoot 'src')   -Recurse -Filter '*.java'
$targets += Get-ChildItem (Join-Path $RepoRoot 'tests') -Filter '*.java'
$targets += Get-ChildItem $RepoRoot -Recurse -Filter '*.ps1'

$totalBefore = 0
$totalAfter = 0
$changed = 0

foreach ($f in $targets) {
    $before = [System.IO.File]::ReadAllText($f.FullName)
    $beforeLines = ($before -split "`n").Count

    $res = if ($f.Extension -eq '.ps1') { Remove-PowerShellComments -Text $before } else { Remove-SlashComments -Text $before }
    $after = Compress-BlankLines -Text $res.Text
    $afterLines = ($after -split "`n").Count

    $totalBefore += $beforeLines
    $totalAfter += $afterLines

    if ($after -ne $before) {
        $changed++
        if (-not $DryRun) {
            [System.IO.File]::WriteAllText($f.FullName, $after, (New-Object System.Text.UTF8Encoding($false)))
        }
        Write-Host ("  {0,-42} {1,4} -> {2,4} 行  (-{3})" -f $f.Name, $beforeLines, $afterLines, ($beforeLines - $afterLines))
    }
}

Write-Host ""
Write-Host ("文件: " + $changed + " / " + $targets.Count + " 个有变化")
Write-Host ("行数: " + $totalBefore + " -> " + $totalAfter + "  (-" + ($totalBefore - $totalAfter) + ")")
if ($DryRun) { Write-Host "DRY RUN -- nothing was written" }
