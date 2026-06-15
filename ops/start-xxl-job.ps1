[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Console]::InputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
Set-Location "D:\Agent\xxl-job-admin"
java -jar xxl-job-admin.jar --spring.config.location=classpath:/application.properties,./application.properties
