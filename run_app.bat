@echo off
chcp 65001 >nul
title Restaurant POS Enterprise v3.0
echo =====================================================================
echo    KHỞI ĐỘNG HỆ THỐNG QUẢN LÝ NHÀ HÀNG POS ENTERPRISE v3.0
echo =====================================================================
echo.

set JAR_FILE=%~dp0target\restaurant-pos-1.0-SNAPSHOT-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo [CẢNH BÁO] Chưa tìm thấy tệp JAR đóng gói sẵn tại target\
    echo Đang tự động chuyển sang biên dịch và khởi chạy...
    call "%~dp0build_and_run.bat"
    exit /b
)

echo Đang khởi chạy ứng dụng Restaurant POS Enterprise...
start "" javaw -Dfile.encoding=UTF-8 -jar "%JAR_FILE%" 2>nul || java -Dfile.encoding=UTF-8 -jar "%JAR_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LỖI] Không thể khởi chạy ứng dụng!
    echo Vui lòng kiểm tra xem máy đã cài đặt Java 17+ (JRE/JDK) chưa.
    pause
)
