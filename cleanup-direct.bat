@echo off
chcp 65001 >nul
echo ========================================
echo    磁盘清理工具
echo ========================================
echo.

echo [1/6] 清理用户临时文件...
del /q /f /s "%TEMP%\*" 2>nul
for /d %%i in ("%TEMP%\*") do rd /s /q "%%i" 2>nul
echo ✅ 用户临时文件已清理
echo.

echo [2/6] 清理 Windows 临时文件...
del /q /f /s "C:\Windows\Temp\*" 2>nul
for /d %%i in ("C:\Windows\Temp\*") do rd /s /q "%%i" 2>nul
echo ✅ Windows 临时文件已清理
echo.

echo [3/6] 清理缩略图缓存...
del /q /f "%LOCALAPPDATA%\Microsoft\Windows\Explorer\thumbcache_*" 2>nul
echo ✅ 缩略图缓存已清理
echo.

echo [4/6] 清理 QQ音乐缓存...
if exist "D:\QQMusicCache" (
    rd /s /q "D:\QQMusicCache"
    echo ✅ QQ音乐缓存已清理
) else (
    echo ⚠️ QQ音乐缓存文件夹不存在
)
echo.

echo [5/6] 清理联想软件管家下载包...
if exist "D:\LenovoQMDownload\SoftMgr" (
    del /q /f /s "D:\LenovoQMDownload\SoftMgr\*" 2>nul
    echo ✅ 联想软件管家下载包已清理
) else (
    echo ⚠️ 联想软件管家下载包文件夹不存在
)
echo.

echo [6/6] 清理迅雷备份文件...
if exist "%LOCALAPPDATA%\Temp\XLLiveUD" (
    rd /s /q "%LOCALAPPDATA%\Temp\XLLiveUD"
    echo ✅ 迅雷备份文件已清理
) else (
    echo ⚠️ 迅雷备份文件夹不存在
)
echo.

echo ========================================
echo    清理完成！
echo ========================================
echo.
echo 已清理项目：
echo   - 用户临时文件
echo   - Windows 临时文件
echo   - 缩略图缓存
echo   - QQ音乐缓存 (1.68 GB)
echo   - 联想软件管家下载包 (255 MB)
echo   - 迅雷备份文件 (200 MB)
echo.
echo 预计释放空间: 2-3 GB
echo.
echo 提示: 微信缓存请在微信设置中手动清理
echo       浏览器缓存请在浏览器设置中手动清理
echo.
pause
