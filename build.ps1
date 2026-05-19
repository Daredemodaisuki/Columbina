param(
    [switch]$Install,
    [switch]$Isolated
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildDir = Join-Path $projectDir "Ant\build"
$distDir = Join-Path $projectDir "Ant"
$srcDir = Join-Path $projectDir "src"
$libDir = Join-Path $projectDir "lib"

if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null

$classpath = "$libDir\josm-tested.jar;$libDir\utilsplugin2.jar"
$javaFiles = Get-ChildItem -Path $srcDir -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

Write-Output "[1/3] Compiling $($javaFiles.Count) source files..."
javac --release 11 -cp $classpath -d $buildDir $javaFiles
if ($LASTEXITCODE -ne 0) { Write-Error "Compilation failed"; exit 1 }

Write-Output "[2/3] Packaging..."
if (-not (Test-Path "$projectDir\images")) { New-Item -ItemType Directory -Force -Path "$projectDir\images" | Out-Null }
Copy-Item -Path "$projectDir\images" -Destination "$buildDir\images" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item -Path "$projectDir\data" -Destination "$buildDir\data" -Recurse -Force -ErrorAction SilentlyContinue

$manifestContent = "Plugin-Mainversion: 18600`r`nPlugin-Version: 1.0.4`r`nPlugin-Class: yakxin.columbina.Columbina`r`nPlugin-Description: Columbina`r`nAuthor: Nandaiji Yakxin`r`nPlugin-Icon: images/Columbina.png`r`nPlugin-Requires: utilsplugin2`r`n"
$manifestFile = Join-Path $buildDir "MANIFEST.MF"
[System.IO.File]::WriteAllText($manifestFile, $manifestContent, [System.Text.ASCIIEncoding]::new())

jar cfm "$distDir\Columbina.jar" $manifestFile -C $buildDir .
if ($LASTEXITCODE -ne 0) { Write-Error "Jar creation failed"; exit 1 }

Write-Output "[3/3] Build complete: $distDir\Columbina.jar"

if ($Install) {
    if ($Isolated) {
        $pluginDir = Join-Path $projectDir ".josm-debug\plugins"
    } else {
        $pluginDir = Join-Path $env:APPDATA "JOSM\plugins"
    }
    New-Item -ItemType Directory -Force -Path $pluginDir | Out-Null
    Copy-Item "$distDir\Columbina.jar" $pluginDir -Force
    if ($Isolated) {
        Copy-Item "$libDir\utilsplugin2.jar" $pluginDir -Force
    }
    Write-Output "[OK] Plugin installed to $pluginDir\Columbina.jar"
}
