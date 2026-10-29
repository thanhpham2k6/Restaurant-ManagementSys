@echo off
chcp 65001 >nul
title Build & Run - Restaurant POS Enterprise v3.0
echo =====================================================================
echo      BIÊN DỊCH VÀ KHỞI CHẠY HỆ THỐNG RESTAURANT POS v3.0
echo =====================================================================
echo.

REM Tự động nhận diện JDK
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Apache NetBeans\jdk" (
        set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
    ) else if exist "C:\Program Files\Java\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
    )
)

REM Tự động nhận diện Maven
set MVN_CMD=mvn
if exist "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
)

echo [1/2] Đang biên dịch và đóng gói bằng Maven...
call "%MVN_CMD%" clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================================
    echo  >>> BIÊN DỊCH THÀNH CÔNG! ĐANG KHỞI CHẠY ỨNG DỤNG...
    echo =====================================================================
    echo.
    java -Dfile.encoding=UTF-8 -jar "%~dp0target\restaurant-pos-1.0-SNAPSHOT-jar-with-dependencies.jar"
) else (
    echo.
    echo =====================================================================
    echo [LỖI] Quá trình biên dịch thất bại! Vui lòng kiểm tra log bên trên.
    echo =====================================================================
    pause
)
