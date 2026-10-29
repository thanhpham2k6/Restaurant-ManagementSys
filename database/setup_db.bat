@echo off
chcp 65001 >nul
title Khoi Tao Co So Du Lieu - Restaurant Management System v3.0

echo =====================================================================
echo    HE THONG CAI DAT VA KHOI TAO CO SO DU LIEU POS RESTAURANT v3.0
echo =====================================================================
echo.

set "SQL_FILE=%~dp0setup_database.sql"
if not exist "%SQL_FILE%" (
    echo [LOI] Khong tim thay file setup_database.sql tai: %SQL_FILE%
    pause
    exit /b 1
)

REM Tim kiem sqlcmd
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
        echo [LOI] Khong tim thay tien ich sqlcmd.exe tren he thong!
        echo Vui long kiem tra xem may da cai dat SQL Server chua.
        pause
        exit /b 1
    )
)

echo [1/3] Dang tu dong kiem tra ket noi SQL Server...

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
    goto :found_conn
)

"%SQLCMD_CMD%" -S .\SQLEXPRESS -U sa -P 12345 -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=.\SQLEXPRESS"
    set "AUTH_FLAGS=-U sa -P 12345"
    set "DETECTED_HOST=localhost\SQLEXPRESS"
    goto :found_conn
)

"%SQLCMD_CMD%" -S localhost\SQLEXPRESS -U sa -P 12345 -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=localhost\SQLEXPRESS"
    set "AUTH_FLAGS=-U sa -P 12345"
    set "DETECTED_HOST=localhost\SQLEXPRESS"
    goto :found_conn
)

"%SQLCMD_CMD%" -S localhost -E -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=localhost"
    set "AUTH_FLAGS=-E"
    set "DETECTED_HOST=localhost"
    goto :found_conn
)

"%SQLCMD_CMD%" -S .\SQLEXPRESS -E -C -Q "SELECT 1" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "SERVER=.\SQLEXPRESS"
    set "AUTH_FLAGS=-E"
    set "DETECTED_HOST=localhost\SQLEXPRESS"
    goto :found_conn
)

:manual_config
echo [!] Khong the tu dong ket noi voi thong tin mac dinh.
set /p SERVER="Nhap SQL Server Instance [Mac dinh: localhost]: "
if "%SERVER%"=="" set "SERVER=localhost"

echo.
echo Chon phuong thuc xac thuc:
echo   1. Windows Authentication (Khong can mat khau)
echo   2. SQL Server Authentication (sa / Mat khau)
set /p AUTH_CHOICE="Chon [1 hoac 2, Mac dinh: 2]: "
if "%AUTH_CHOICE%"=="" set AUTH_CHOICE=2

if "%AUTH_CHOICE%"=="1" (
    set "AUTH_FLAGS=-E"
    set "DETECTED_HOST=%SERVER%"
) else (
    set /p DETECTED_USER="Tai khoan SQL [Mac dinh: sa]: "
    if "%DETECTED_USER%"=="" set DETECTED_USER=sa
    set /p DETECTED_PASS="Mat khau SQL [Mac dinh: 12345]: "
    if "%DETECTED_PASS%"=="" set DETECTED_PASS=12345
    set "AUTH_FLAGS=-U %DETECTED_USER% -P %DETECTED_PASS%"
    set "DETECTED_HOST=%SERVER%"
)
goto :do_install

:found_conn
echo.
echo =====================================================================
echo  [OK] PHAT HIEN KET NOI SQL SERVER THANH CONG!
echo       May chu:   %SERVER%
echo       Xac thuc:  %AUTH_FLAGS%
echo =====================================================================
echo.

:do_install
echo [2/3] Dang thuc thi script setup_database.sql...
echo Vui long doi trong giay lat...
echo.

"%SQLCMD_CMD%" -S %SERVER% %AUTH_FLAGS% -C -f 65001 -i "%SQL_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo =====================================================================
    echo [LOI] Qua trinh khoi tao CSDL khong thanh cong!
    echo Vui long kiem tra lai dich vu SQL Server va quyen truy cap.
    echo =====================================================================
    pause
    exit /b 1
)

echo.
echo [3/3] Dang dong bo cau hinh vao tep db.properties...
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

echo.
echo =====================================================================
echo  [OK] KHOI TAO CO SO DU LIEU HOAN TAT THANH CONG!
echo =====================================================================
echo  He thong da tao du 17 bang va nap du lieu mau hoan chinh:
echo    - 53 mon an thuc don kem danh muc chuan
echo    - 20 mat hang kho va dinh luong tieu hao
echo    - 15 ban an so do thoi gian thuc
echo    - Don hang mau hom nay cho Thu ngan, Bep va Dashboard
echo.
echo  Tai khoan dang nhap he thong:
echo    1. Quan ly:  Username: admin    ^| Mat khau: admin123
echo    2. Thu ngan: Username: cashier1 ^| Mat khau: cashier123
echo    3. Nha bep:  Username: kitchen1 ^| Mat khau: kitchen123
echo =====================================================================
echo.
pause
