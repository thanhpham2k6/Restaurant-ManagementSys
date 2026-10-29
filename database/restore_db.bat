@echo off
chcp 65001 >nul
title Phuc Hoi Co So Du Lieu - Restaurant Management System
echo =====================================================================
echo  HỆ THỐNG PHỤC HỒI CƠ SỞ DỮ LIỆU TỪ TỆP .BAK (RESTORE DATABASE)
echo  Tương thích: Microsoft SQL Server 2016, 2017, 2019, 2022, Express
echo =====================================================================
echo.

set BAK_FILE=%~dp0RestaurantManagementSys.bak
if not exist "%BAK_FILE%" (
    echo [LỖI] Không tìm thấy file backup tại: %BAK_FILE%
    echo Vui lòng kiểm tra lại vị trí file RestaurantManagementSys.bak!
    pause
    exit /b 1
)

echo [1/3] Cấu hình máy chủ SQL Server:
set /p SERVER="> Nhập tên SQL Server Instance [Mặc định: localhost]: "
if "%SERVER%"=="" set SERVER=localhost

echo.
echo [2/3] Phương thức xác thực:
echo   1. Windows Authentication (Không cần mật khẩu - Khuyên dùng)
echo   2. SQL Server Authentication (sa / Mật khẩu)
set /p AUTH_CHOICE="> Chọn phương thức [1 hoặc 2, Mặc định: 2]: "
if "%AUTH_CHOICE%"=="" set AUTH_CHOICE=2

if "%AUTH_CHOICE%"=="1" (
    set AUTH_FLAGS=-E
    echo   -^> Đang sử dụng Windows Authentication.
) else (
    set /p USER="> Nhập tài khoản SQL [Mặc định: sa]: "
    if "%USER%"=="" set USER=sa
    set /p PWD="> Nhập mật khẩu SQL [Mặc định: 12345]: "
    if "%PWD%"=="" set PWD=12345
    set AUTH_FLAGS=-U %USER% -P %PWD%
)

echo.
echo [3/3] Đang tiến hành phục hồi cơ sở dữ liệu...
echo Tệp backup: %BAK_FILE%
echo Máy chủ:    %SERVER%
echo.

sqlcmd -S %SERVER% %AUTH_FLAGS% -C -Q "SET NOCOUNT ON; DECLARE @bak NVARCHAR(512) = N'%BAK_FILE%'; DECLARE @defaultData NVARCHAR(512) = CAST(SERVERPROPERTY('InstanceDefaultDataPath') AS NVARCHAR(512)); DECLARE @defaultLog NVARCHAR(512) = CAST(SERVERPROPERTY('InstanceDefaultLogPath') AS NVARCHAR(512)); IF @defaultData IS NULL OR @defaultData = '' BEGIN SELECT TOP 1 @defaultData = SUBSTRING(physical_name, 1, CHARINDEX('master.mdf', LOWER(physical_name)) - 1) FROM master.sys.master_files WHERE database_id = 1 AND file_id = 1; END; IF @defaultLog IS NULL OR @defaultLog = '' BEGIN SET @defaultLog = @defaultData; END; DECLARE @dataFile NVARCHAR(512) = @defaultData + 'RestaurantManagementSys.mdf'; DECLARE @logFile NVARCHAR(512) = @defaultLog + 'RestaurantManagementSys_log.ldf'; IF EXISTS (SELECT name FROM sys.databases WHERE name = 'RestaurantManagementSys') BEGIN PRINT N'>>> Đang đóng các kết nối cũ tới RestaurantManagementSys...'; ALTER DATABASE RestaurantManagementSys SET SINGLE_USER WITH ROLLBACK IMMEDIATE; END; PRINT N'>>> Đang khôi phục cơ sở dữ liệu và tự động ánh xạ thư mục dữ liệu...'; RESTORE DATABASE RestaurantManagementSys FROM DISK = @bak WITH REPLACE, MOVE 'RestaurantManagementSys' TO @dataFile, MOVE 'RestaurantManagementSys_log' TO @logFile; ALTER DATABASE RestaurantManagementSys SET MULTI_USER; PRINT N'>>> Hoàn tất Restore thành công!';"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================================
    echo  >>> PHỤC HỒI DATABASE THÀNH CÔNG TỪ FILE .BAK!
    echo =====================================================================
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
