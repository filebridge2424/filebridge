@echo off
setlocal

title FileBridge Launcher

echo ========================================================
echo             FILEBRIDGE DEV ENVIRONMENT
echo ========================================================

:: 1. Auto-detect tool directories
set "MONGO_DIR=D:\tools\mongodb-win32-x86_64-windows-7.0.15\bin"
set "MAVEN_DIR=D:\tools\apache-maven-3.9.6\bin"

:: Prepend to PATH so any launched shell recognizes mongod and mvn
if exist "%MONGO_DIR%" set "PATH=%MONGO_DIR%;%PATH%"
if exist "%MAVEN_DIR%" set "PATH=%MAVEN_DIR%;%PATH%"

:: Ensure required storage and database directories exist
if not exist "D:\MongoData" mkdir "D:\MongoData"
if not exist "D:\FileBridgeStorage" mkdir "D:\FileBridgeStorage"

:: 1. Launch MongoDB
echo [1/3] Starting MongoDB on port 27017...
start "FileBridge - MongoDB (Port 27017)" cmd /k "mongod --dbpath D:\MongoData --port 27017"

:: 2. Launch Spring Boot Backend
echo [2/3] Starting Spring Boot Backend on port 8080...
start "FileBridge - Spring Boot (Port 8080)" cmd /k "cd /d "%~dp0backend" && mvn spring-boot:run"

:: 3. Launch React Frontend
echo [3/3] Starting Vite Frontend on port 5173...
start "FileBridge - Frontend (Port 5173)" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo ========================================================
echo  All 3 services have been launched in separate windows!
echo  - Frontend Web UI : http://localhost:5173
echo  - Backend API     : http://localhost:8080/api/v1/health
echo  - MongoDB Database: localhost:27017
echo ========================================================
echo.
pause
