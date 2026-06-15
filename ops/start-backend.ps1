[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Console]::InputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
Set-Location "D:\Agent"
$jar = Get-ChildItem "blog-backend\target\blog-backend-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jar) {
    java -jar $jar.FullName
} else {
    Write-Host "JAR not found, using mvn spring-boot:run"
    Set-Location "D:\Agent\blog-backend"
    mvn spring-boot:run
}
