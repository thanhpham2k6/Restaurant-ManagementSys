@echo off
chcp 65001 >nul
title Phuc Hoi Co So Du Lieu - Restaurant Management System
echo =====================================================================
echo  HỆ THỐNG PHỤC HỒI CƠ SỞ DỮ LIỆU TỪ TỆP .BAK (RESTORE DATABASE)
echo  Tương thích: Microsoft SQL Server 2016, 2017, 2019, 2022, Express
echo =====================================================================
echo.

set "BAK_FILE=%~dp0RestaurantManagementSys.bak"
if not exist "%BAK_FILE%" (
    echo [LỖI] Không tìm thấy file backup tại: %BAK_FILE%
    echo Vui lòng kiểm tra lại vị trí file RestaurantManagementSys.bak!
    pause
    exit /b 1
)

REM Tìm kiếm sqlcmd
set "SQLCMD_CMD=sqlcmd"
where sqlcmd >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\180\Tools\Binn\sqlcmd.exe" (
        set "SQLCMD_CMD=C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\180\Tools\Binn\sqlcmd.exe"
    ) else if exist "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn\sqlcmd.exe" (
        set "SQLCMD_CMD=C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn\sqlcmd.exe"
    ) else if exist "C:\Program Files\Microsoft SQL Server\160\Tools\Binn\sqlcmd.exe" (
        set "SQLCMD_CMD=C:\Program Files\Microsoft SQL Server\160\Tools\Binn\sqlcmd.exe"
    ) else if exist "C:\Program Files\Microsoft SQL Server\150\Tools\Binn\sqlcmd.exe" (
        set "SQLCMD_CMD=C:\Program Files\Microsoft SQL Server\150\Tools\Binn\sqlcmd.exe"
    ) else if exist "C:\Program Files\Microsoft SQL Server\140\Tools\Binn\sqlcmd.exe" (
        set "SQLCMD_CMD=C:\Program Files\Microsoft SQL Server\140\Tools\Binn\sqlcmd.exe"
    ) else (
        echo [LỖI] Không tìm thấy tiện ích sqlcmd.exe trên hệ thống!
        echo Vui lòng kiểm tra xem máy tính đã cài đặt SQL Server chưa.
        pause
        exit /b 1
    )
)

echo [1/3] Đang tự động kiểm tra kết nối SQL Server...

set "SERVER="
set "AUTH_FLAGS="
set "DETECTED_HOST=localhost"
set "DETECTED_USER=sa"
set "DETECTED_PASS=12345"

"%SQLCMD_CMD%" -S localhost -U sa -P 12345 -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=localhost"
    set "AUTH_FLAGS=-U sa -P 12345"
    set "DETECTED_HOST=localhost"
    set "DETECTED_USER=sa"
    set "DETECTED_PASS=12345"
    echo   -^> Kết nối thành công tới: localhost (sa / 12345)
    goto :do_restore
)

"%SQLCMD_CMD%" -S .\SQLEXPRESS -U sa -P 12345 -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=.\SQLEXPRESS"
    set "AUTH_FLAGS=-U sa -P 12345"
    set "DETECTED_HOST=localhost\SQLEXPRESS"
    set "DETECTED_USER=sa"
    set "DETECTED_PASS=12345"
    echo   -^> Kết nối thành công tới: .\SQLEXPRESS (sa / 12345)
    goto :do_restore
)

"%SQLCMD_CMD%" -S localhost\SQLEXPRESS -U sa -P 12345 -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=localhost\SQLEXPRESS"
    set "AUTH_FLAGS=-U sa -P 12345"
    set "DETECTED_HOST=localhost\SQLEXPRESS"
    set "DETECTED_USER=sa"
    set "DETECTED_PASS=12345"
    echo   -^> Kết nối thành công tới: localhost\SQLEXPRESS (sa / 12345)
    goto :do_restore
)

"%SQLCMD_CMD%" -S localhost -E -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=localhost"
    set "AUTH_FLAGS=-E"
    set "DETECTED_HOST=localhost"
    echo   -^> Kết nối thành công tới: localhost (Windows Authentication)
    goto :do_restore
)

"%SQLCMD_CMD%" -S .\SQLEXPRESS -E -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=.\SQLEXPRESS"
    set "AUTH_FLAGS=-E"
    set "DETECTED_HOST=localhost\SQLEXPRESS"
    echo   -^> Kết nối thành công tới: .\SQLEXPRESS (Windows Authentication)
    goto :do_restore
)

echo [!] Không thể tự động kết nối với thông tin mặc định.
set /p SERVER="> Nhập tên SQL Server Instance [Mặc định: localhost]: "
if "%SERVER%"=="" set SERVER=localhost

echo.
echo Phương thức xác thực:
echo   1. Windows Authentication (Không cần mật khẩu)
echo   2. SQL Server Authentication (sa / Mật khẩu)
set /p AUTH_CHOICE="> Chọn phương thức [1 hoặc 2, Mặc định: 2]: "
if "%AUTH_CHOICE%"=="" set AUTH_CHOICE=2

if "%AUTH_CHOICE%"=="1" (
    set AUTH_FLAGS=-E
    set "DETECTED_HOST=%SERVER%"
    echo   -^> Đang sử dụng Windows Authentication.
) else (
    set /p USER="> Nhập tài khoản SQL [Mặc định: sa]: "
    if "%USER%"=="" set USER=sa
    set /p PWD="> Nhập mật khẩu SQL [Mặc định: 12345]: "
    if "%PWD%"=="" set PWD=12345
    set AUTH_FLAGS=-U %USER% -P %PWD%
    set "DETECTED_HOST=%SERVER%"
    set "DETECTED_USER=%USER%"
    set "DETECTED_PASS=%PWD%"
)

:do_restore
echo.
echo [2/3] Đang tiến hành phục hồi cơ sở dữ liệu...
echo Tệp backup: %BAK_FILE%
echo Máy chủ:    %SERVER%
echo.

"%SQLCMD_CMD%" -S %SERVER% %AUTH_FLAGS% -C -Q "SET NOCOUNT ON; DECLARE @bak NVARCHAR(512) = N'%BAK_FILE%'; DECLARE @defaultData NVARCHAR(512) = CAST(SERVERPROPERTY('InstanceDefaultDataPath') AS NVARCHAR(512)); DECLARE @defaultLog NVARCHAR(512) = CAST(SERVERPROPERTY('InstanceDefaultLogPath') AS NVARCHAR(512)); IF @defaultData IS NULL OR @defaultData = '' BEGIN SELECT TOP 1 @defaultData = SUBSTRING(physical_name, 1, CHARINDEX('master.mdf', LOWER(physical_name)) - 1) FROM master.sys.master_files WHERE database_id = 1 AND file_id = 1; END; IF @defaultLog IS NULL OR @defaultLog = '' BEGIN SET @defaultLog = @defaultData; END; DECLARE @dataFile NVARCHAR(512) = @defaultData + 'RestaurantManagementSys.mdf'; DECLARE @logFile NVARCHAR(512) = @defaultLog + 'RestaurantManagementSys_log.ldf'; IF EXISTS (SELECT name FROM sys.databases WHERE name = 'RestaurantManagementSys') BEGIN PRINT N'>>> Đang đóng các kết nối cũ tới RestaurantManagementSys...'; ALTER DATABASE RestaurantManagementSys SET SINGLE_USER WITH ROLLBACK IMMEDIATE; END; PRINT N'>>> Đang khôi phục cơ sở dữ liệu và tự động ánh xạ thư mục dữ liệu...'; RESTORE DATABASE RestaurantManagementSys FROM DISK = @bak WITH REPLACE, MOVE 'RestaurantManagementSys' TO @dataFile, MOVE 'RestaurantManagementSys_log' TO @logFile; ALTER DATABASE RestaurantManagementSys SET MULTI_USER; PRINT N'>>> Hoàn tất Restore thành công!';"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================================
    echo  >>> PHỤC HỒI DATABASE THÀNH CÔNG TỪ FILE .BAK!
    echo =====================================================================
    echo [3/3] Đang đồng bộ cấu hình vào tệp db.properties...
    set "ROOT_DIR=%~dp0.."
    (
        echo # Cau hinh ket noi SQL Server cho Restaurant POS Enterprise
        echo db.host=%DETECTED_HOST%
        echo db.port=1433
        echo db.name=RestaurantManagementSys
        echo db.user=%DETECTED_USER%
        echo db.password=%DETECTED_PASS%
    ) > "%ROOT_DIR%\db.properties"

    if exist "%ROOT_DIR%\src\main\resources" (
        (
            echo # Cau hinh ket noi SQL Server cho Restaurant POS Enterprise
            echo db.host=%DETECTED_HOST%
            echo db.port=1433
            echo db.name=RestaurantManagementSys
            echo db.user=%DETECTED_USER%
            echo db.password=%DETECTED_PASS%
        ) > "%ROOT_DIR%\src\main\resources\db.properties"
    )

    echo  Đã tự động cập nhật db.properties với máy chủ: %DETECTED_HOST%
    echo.
    echo  Tài khoản đăng nhập hệ thống:
    echo    1. Quản lý:  Username: admin    ^| Mật khẩu: admin123
    echo    2. Thu ngân: Username: cashier1 ^| Mật khẩu: cashier123
    echo    3. Nhà bếp:  Username: kitchen1 ^| Mật khẩu: kitchen123
    echo =====================================================================
) else (
    echo.
    echo =====================================================================
    echo [LỖI] Phục hồi từ file .bak không thành công!
    echo Nguyên nhân có thể do:
    echo  1. Phiên bản SQL Server máy này thấp hơn phiên bản tạo file .bak (vd: SQL 2019 không đọc được .bak tạo từ SQL 2022).
    echo  2. Sai tên Server Instance hoặc sai mật khẩu sa.
    echo =====================================================================
    echo.
    set /p RUN_SETUP="Bạn có muốn tự động chạy 'setup_db.bat' để khởi tạo trực tiếp bằng mã nguồn SQL không? (Y/N) [Mặc định: Y]: "
    if "%RUN_SETUP%"=="" set RUN_SETUP=Y
    if /i "%RUN_SETUP%"=="Y" (
        call "%~dp0setup_db.bat"
    )
)
echo.
pause
