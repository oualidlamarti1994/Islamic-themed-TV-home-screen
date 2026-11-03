<#
  Downloads everything the app bundles but this repository does not ship:
  Amiri / Amiri Quran fonts (SIL OFL), curated verses + 99 Names, the whole Quran (Uthmani, Tanzil via alquran.cloud)
  and the sahih hadith (Sahih al-Bukhari, Sahih Muslim via fawazahmed0/hadith-api).
  Usage: powershell -ExecutionPolicy Bypass -File fetch_assets.ps1
#>
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$a = Join-Path $PSScriptRoot 'app\assets'
New-Item -ItemType Directory -Force $a | Out-Null
foreach ($f in 'amiriquran/AmiriQuran-Regular.ttf', 'amiri/Amiri-Regular.ttf', 'amiri/Amiri-Bold.ttf') {
  Invoke-WebRequest -UseBasicParsing "https://github.com/google/fonts/raw/main/ofl/$f" -OutFile (Join-Path $a (Split-Path $f -Leaf))
}
$tmp = Join-Path $env:TEMP 'noor-hadith'
New-Item -ItemType Directory -Force $tmp | Out-Null
foreach ($e in 'ara-bukhari', 'ara-muslim') {
  Invoke-WebRequest -UseBasicParsing "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/$e.min.json" -OutFile "$tmp\$e.json"
}
python -I (Join-Path $PSScriptRoot 'fetch_data.py') $a
python -I (Join-Path $PSScriptRoot 'fetch_more.py') $a "$tmp\ara-bukhari.json" "$tmp\ara-muslim.json"
python -I (Join-Path $PSScriptRoot 'fetch_hadith.py') $a "$tmp\ara-bukhari.json" "$tmp\ara-muslim.json" "$tmp\report.txt"
"Assets ready in $a"
