@echo off
chcp 65001 >nul
echo =====================================================================
echo  HỆ THỐNG KHỞI TẠO CƠ SỞ DỮ LIỆU POS RESTAURANT (SQL SCRIPT)
echo =====================================================================
echo.
set /p SERVER="Nhập tên SQL Server Instance [Mặc định: localhost]: "
if "%SERVER%"=="" set SERVER=localhost

set /p USER="Nhập tài khoản SQL [Mặc định: sa]: "
if "%USER%"=="" set USER=sa

set /p PWD="Nhập mật khẩu SQL [Mặc định: 12345]: "
if "%PWD%"=="" set PWD=12345

echo.
echo Đang thực thi setup_database.sql trên %SERVER% (hỗ trợ mã hóa UTF-8 tiếng Việt)...
sqlcmd -S %SERVER% -U %USER% -P %PWD% -C -f 65001 -i "%~dp0setup_database.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================================
    echo  >>> KHỞI TẠO CƠ SỞ DỮ LIỆU THÀNH CÔNG!
    echo =====================================================================
) else (
    echo.
    echo [LỖI] Khởi tạo thất bại! Vui lòng kiểm tra lại kết nối SQL Server và mật khẩu.
)
pause
