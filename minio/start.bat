@echo off
set MINIO_HOME=D:\Agent\minio\config
set HOME=D:\Agent\minio\config
set USERPROFILE=D:\Agent\minio\config
echo Starting MinIO Server...
echo Console: http://localhost:9001
echo API: http://localhost:9000
echo User: minioadmin / minioadmin
echo.
cd /d D:\Agent\minio
minio.exe server .\data --console-address ":9001"
