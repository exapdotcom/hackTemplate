#requires -Version 5.1
<#
.SYNOPSIS
    Renames the template package, applicationId, app label, root project name and Kotlin package directories.

.DESCRIPTION
    The script is a dry run by default. It prints every file and directory it would touch.
    Use -Apply to actually rewrite files and move source directories.

    It skips research/, scripts/, build directories, .gradle and local.properties.

.EXAMPLE
    .\scripts\rename-project.ps1 -NewName "MyHack" -NewPackage "com.example.myhack"

.EXAMPLE
    .\scripts\rename-project.ps1 -NewName "MyHack" -NewPackage "com.example.myhack" -Apply
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z][A-Za-z0-9]*$')]
    [string]$NewName,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$')]
    [string]$NewPackage,

    [string]$NewAppLabel = '',

    [switch]$Apply
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($NewAppLabel)) {
    $NewAppLabel = $NewName
}

$root = Split-Path -Parent $PSScriptRoot
$oldPackage = 'com.hustle'
$oldAppId = 'com.hustle.hacktemplate'
$newPackagePath = $NewPackage -replace '\.', '\'
$utf8 = [System.Text.UTF8Encoding]::new($false)

if ($NewPackage -eq $oldPackage -or $NewPackage -eq $oldAppId) {
    throw "NewPackage must differ from the template package: $NewPackage"
}

$replacements = [ordered]@{
    $oldAppId                  = $NewPackage
    $oldPackage                = $NewPackage
    'HackTemplateApplication'  = "${NewName}Application"
    'HackTemplateTheme'        = "${NewName}Theme"
    'Theme.HackTemplate'       = "Theme.$NewName"
    'Hack Template'            = $NewAppLabel
    'HackTemplate'             = $NewName
}

$textExtensions = @(
    '.kt', '.kts', '.xml', '.toml', '.properties', '.md',
    '.yml', '.yaml', '.json', '.pro', '.txt'
)

$files = Get-ChildItem -Path $root -Recurse -File | Where-Object {
    $relative = $_.FullName.Substring($root.Length).TrimStart('\')
    $extension = $_.Extension.ToLowerInvariant()
    $relative -notmatch '(^|\\)(\.git|\.gradle|build|\.idea|research|scripts)(\\|$)' -and
    $_.Name -ne 'local.properties' -and
    $textExtensions -contains $extension
}

$changedFiles = New-Object System.Collections.Generic.List[string]

foreach ($file in $files) {
    $text = [System.IO.File]::ReadAllText($file.FullName)
    $updated = $text

    foreach ($entry in $replacements.GetEnumerator()) {
        $updated = $updated.Replace($entry.Key, $entry.Value)
    }

    if ($updated -ne $text) {
        $changedFiles.Add($file.FullName)

        if ($Apply) {
            [System.IO.File]::WriteAllText($file.FullName, $updated, $utf8)
        }
    }
}

$renamedFiles = New-Object System.Collections.Generic.List[object]
$applicationFile = Get-ChildItem -Path $root -Recurse -File -Filter 'HackTemplateApplication.kt' | Where-Object {
    $_.FullName -notmatch '(^|\\)(\.git|\.gradle|build|\.idea|research|scripts)(\\|$)'
} | Select-Object -First 1

if ($null -ne $applicationFile) {
    $applicationTarget = Join-Path $applicationFile.DirectoryName "${NewName}Application.kt"
    if (Test-Path $applicationTarget) {
        throw "Target file already exists: $applicationTarget"
    }

    $renamedFiles.Add([pscustomobject]@{
        Source = $applicationFile.FullName
        Target = $applicationTarget
    })
}

$packageRoots = Get-ChildItem -Path $root -Recurse -Directory -Filter 'hustle' | Where-Object {
    $_.Parent.Name -eq 'com' -and
    $_.FullName -match '\\src\\(main|test|androidTest)\\java\\com\\hustle$'
}

$moves = New-Object System.Collections.Generic.List[object]

foreach ($source in $packageRoots) {
    # source is <module>/src/<variant>/java/com/hustle; the package root is java/.
    $javaRoot = Split-Path -Parent $source.Parent.FullName
    $target = Join-Path $javaRoot $newPackagePath
    $targetParent = Split-Path -Parent $target

    if ($target.Equals($source.FullName, [StringComparison]::OrdinalIgnoreCase)) {
        continue
    }

    if ($target.StartsWith($source.FullName + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "New package cannot be a child of the old package: $target"
    }

    if (Test-Path $target) {
        throw "Target package directory already exists: $target"
    }

    $moves.Add([pscustomobject]@{
        Source = $source.FullName
        Target = $target
        TargetParent = $targetParent
    })
}

Write-Host ""
Write-Host "Template rename" -ForegroundColor Cyan
Write-Host "  Root:        $root"
Write-Host "  Name:        $NewName"
Write-Host "  Package:     $NewPackage"
Write-Host "  App label:   $NewAppLabel"
Write-Host "  Mode:        $(if ($Apply) { 'APPLY' } else { 'DRY RUN' })"
Write-Host ""

Write-Host "Files to update: $($changedFiles.Count)" -ForegroundColor Yellow
$changedFiles | ForEach-Object {
    Write-Host "  $($_.Substring($root.Length).TrimStart('\'))"
}

Write-Host ""
Write-Host "Files to rename: $($renamedFiles.Count)" -ForegroundColor Yellow
$renamedFiles | ForEach-Object {
    Write-Host "  $($_.Source.Substring($root.Length).TrimStart('\'))"
    Write-Host "    -> $($_.Target.Substring($root.Length).TrimStart('\'))"
}

Write-Host ""
Write-Host "Package directories to move: $($moves.Count)" -ForegroundColor Yellow
$moves | ForEach-Object {
    Write-Host "  $($_.Source.Substring($root.Length).TrimStart('\'))"
    Write-Host "    -> $($_.Target.Substring($root.Length).TrimStart('\'))"
}

if (-not $Apply) {
    Write-Host ""
    Write-Host "Dry run only. Re-run with -Apply to make changes." -ForegroundColor Green
    exit 0
}

foreach ($renamed in $renamedFiles) {
    Move-Item -Path $renamed.Source -Destination $renamed.Target
}

foreach ($move in $moves) {
    New-Item -ItemType Directory -Force -Path $move.TargetParent | Out-Null
    Move-Item -Path $move.Source -Destination $move.Target
}

# Remove empty com/ directories left behind.
Get-ChildItem -Path $root -Recurse -Directory -Filter 'com' | Where-Object {
    (Get-ChildItem -Path $_.FullName -Force | Measure-Object).Count -eq 0
} | Sort-Object { $_.FullName.Length } -Descending | Remove-Item -Force

Write-Host ""
Write-Host "Done. Open the project in Android Studio and sync Gradle." -ForegroundColor Green
Write-Host "Check: settings.gradle.kts, app/build.gradle.kts, AndroidManifest.xml, Theme.HackTemplate -> Theme.$NewName." -ForegroundColor DarkGray
