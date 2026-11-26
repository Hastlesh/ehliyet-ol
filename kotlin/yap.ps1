# Ehliyet Ol (Kotlin) — Android derlemesi: APK (telefona) + AAB (Play Console'a)
# Kullanım: powershell -ExecutionPolicy Bypass -File yap.ps1 [-deneme] [-kur]
#   -deneme: yalnız deneme sürümü (Ehliyet Ol K) derlenir; -kur ile USB'deki telefona kurulur
# Araçlar (JDK, Gradle, Android SDK) Memur Ol'un klasöründen kullanılır. iOS derlemesi bulut Mac'te.
param([switch]$deneme, [switch]$kur)
$ErrorActionPreference = 'Continue'
$K = $PSScriptRoot
$A = "$K\..\..\..\SartimTamam\android\arac"
$env:JAVA_HOME = (Get-ChildItem "$A\jdk" -Directory)[0].FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME = "$A\sdk"

# Sorular ve resimler web sürümünden (tek kaynak: ..\uygulama)
$F = "$K\composeApp\src\commonMain\composeResources\files"
New-Item -ItemType Directory -Force "$F\r" | Out-Null
Copy-Item "$K\..\..\uygulama\veri\sorular.json", "$K\..\..\uygulama\veri\dersler.json" $F -Force
robocopy "$K\..\..\uygulama\r" "$F\r" /MIR /NJH /NJS /NFL /NDL | Out-Null

Push-Location $K
if ($deneme) {
  & "$A\gradle\gradle-8.13\bin\gradle.bat" --no-daemon -q :composeApp:assembleDebug
  $kod = $LASTEXITCODE; Pop-Location
  if ($kod) { throw "gradle hata: $kod" }
  "APK: $K\composeApp\build\outputs\apk\debug\composeApp-debug.apk"
  if ($kur) { & "$A\sdk\platform-tools\adb.exe" install -r "$K\composeApp\build\outputs\apk\debug\composeApp-debug.apk" }
  return
}
& "$A\gradle\gradle-8.13\bin\gradle.bat" --no-daemon -q :composeApp:assembleRelease :composeApp:bundleRelease
$kod = $LASTEXITCODE
Pop-Location
if ($kod) { throw "gradle hata: $kod" }
Copy-Item "$K\composeApp\build\outputs\apk\release\composeApp-release.apk" "$K\EhliyetOl.apk" -Force
Copy-Item "$K\composeApp\build\outputs\bundle\release\composeApp-release.aab" "$K\EhliyetOl.aab" -Force
"APK: $K\EhliyetOl.apk ($([math]::Round((Get-Item "$K\EhliyetOl.apk").Length/1MB,1)) MB)"
"AAB: $K\EhliyetOl.aab ($([math]::Round((Get-Item "$K\EhliyetOl.aab").Length/1MB,1)) MB)"
