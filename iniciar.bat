@echo off
title Gestor de Inventario

set RUTA_APP=%~dp0
cd /d "%RUTA_APP%"

if not exist ".\mysql_portable\bin\mysqld.exe" (
    echo [ERROR] No se encontro la carpeta 'mysql_portable'.
    pause
    exit /b
)

if not exist ".\mysql_portable\data" (
    echo [INFO] Primera ejecucion detectada. Inicializando base de datos local...
    ".\mysql_portable\bin\mysqld.exe" --initialize-insecure --datadir=".\mysql_portable\data"
    timeout /t 3 /nobreak >nul
)

tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [INFO] El servidor MySQL ya esta activo.
) else (
    echo [INFO] Iniciando servidor MySQL local...
    start "" /b ".\mysql_portable\bin\mysqld.exe" --datadir=".\mysql_portable\data" --port=3306
    timeout /t 5 /nobreak >nul
)

echo [INFO] Verificando esquema e importando tablas SQL...
".\mysql_portable\bin\mysql.exe" -u root -e "CREATE DATABASE IF NOT EXISTS inventario_vidrieria;" 2>nul
".\mysql_portable\bin\mysql.exe" -u root inventario_vidrieria < ".\BaseDatos\inventario_vidrieria Progra II.sql" 2>nul

if exist ".\GestorInventario-1.0-SNAPSHOT.jar" (
    echo [INFO] Lanzando aplicacion Java...
    start "" javaw -jar ".\GestorInventario-1.0-SNAPSHOT.jar"
) else (
    echo [ERROR] No se encontro el archivo JAR de la aplicacion.
    pause
)