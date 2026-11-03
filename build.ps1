<#
  Builds noor.apk without Gradle, using only Android Studio's bundled JDK and the installed Android SDK:
  aapt2 -> javac -> d8 -> zipalign -> apksigner. Output: noor\build\noor.apk
#>
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$bt = Get-ChildItem "$sdk\build-tools" -Directory | Where-Object Name -like '35.*' | Select-Object -First 1 -ExpandProperty FullName
$jar = "$sdk\platforms\android-33\android.jar"
$jbr = Get-ChildItem 'C:\Program Files\Android' -Directory | ForEach-Object { Join-Path $_.FullName 'jbr' } | Where-Object { Test-Path "$_\bin\javac.exe" } | Select-Object -First 1
if (-not $jbr) { throw 'No JDK with javac found in Android Studio' }
$env:JAVA_HOME = $jbr; $env:PATH = "$jbr\bin;$env:PATH"
$b = Join-Path $here 'build'
if (Test-Path $b) { Remove-Item -Recurse -Force $b }
New-Item -ItemType Directory -Force "$b\gen", "$b\classes", "$b\dex" | Out-Null

function Run($exe) { $ErrorActionPreference = 'Continue'; & $exe @args 2>&1 | ForEach-Object { "$_" }; if ($LASTEXITCODE) { throw "$exe failed ($LASTEXITCODE)" } }

Run "$bt\aapt2.exe" compile --dir "$here\app\res" -o "$b\res.zip"
Run "$bt\aapt2.exe" link -o "$b\base.apk" -I $jar --manifest "$here\app\AndroidManifest.xml" -A "$here\app\assets" `
  --java "$b\gen" --min-sdk-version 26 --target-sdk-version 33 --version-code 1 --version-name 1.0 "$b\res.zip"
$src = @(Get-ChildItem -Recurse "$here\app\src", "$b\gen" -Filter *.java | ForEach-Object FullName)
Run "$jbr\bin\javac.exe" -encoding UTF-8 -source 11 -target 11 '-Xlint:-options' -classpath $jar -d "$b\classes" @src
$classes = @(Get-ChildItem -Recurse "$b\classes" -Filter *.class | ForEach-Object FullName)
Run "$bt\d8.bat" --release --min-api 26 --lib $jar --output "$b\dex" @classes

Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$b\base.apk", 'Update')
[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$b\dex\classes.dex", 'classes.dex') | Out-Null
$zip.Dispose()

Run "$bt\zipalign.exe" -p -f 4 "$b\base.apk" "$b\aligned.apk"
$ks = Join-Path $here 'noor.jks'
if (-not (Test-Path $ks)) {
  Run "$jbr\bin\keytool.exe" -genkeypair -keystore $ks -storepass noor-tv -keypass noor-tv -alias noor -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Noor TV'
}
Run "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:noor-tv --key-pass pass:noor-tv --out "$b\noor.apk" "$b\aligned.apk"
Run "$bt\apksigner.bat" verify "$b\noor.apk"
"Built $b\noor.apk ($([int]((Get-Item "$b\noor.apk").Length / 1KB)) KB)"



