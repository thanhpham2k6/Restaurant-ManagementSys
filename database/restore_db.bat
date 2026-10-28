@echo off
chcp 65001 >nul
echo =====================================================================
echo  HỆ THỐNG PHỤC HỒI CƠ SỞ DỮ LIỆU TỪ TỆP .BAK (RESTORE DATABASE)
echo =====================================================================
echo.
set /p SERVER="Nhập tên SQL Server Instance [Mặc định: localhost]: "
if "%SERVER%"=="" set SERVER=localhost

set /p USER="Nhập tài khoản SQL [Mặc định: sa]: "
if "%USER%"=="" set USER=sa

set /p PWD="Nhập mật khẩu SQL [Mặc định: 12345]: "
if "%PWD%"=="" set PWD=12345

set BAK_FILE=%~dp0RestaurantManagementSys.bak

echo.
echo Đang phục hồi cơ sở dữ liệu từ tệp: %BAK_FILE% ...
sqlcmd -S %SERVER% -U %USER% -P %PWD% -C -Q "ALTER DATABASE RestaurantManagementSys SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE RestaurantManagementSys FROM DISK = '%BAK_FILE%' WITH REPLACE; ALTER DATABASE RestaurantManagementSys SET MULTI_USER;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================================
    echo  >>> PHỤC HỒI DATABASE THÀNH CÔNG TỪ FILE .BAK!
    echo =====================================================================
) else (
    echo.
    echo [LƯU Ý] Nếu gặp lỗi quyền truy cập file hoặc phiên bản SQL Server khác nhau,
    echo bạn hãy chạy tệp 'setup_db.bat' để khởi tạo trực tiếp bằng mã nguồn SQL!
)
pause
