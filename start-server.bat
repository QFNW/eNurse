@echo off
cd /d "%~dp0server"

echo Stopping old processes...
taskkill /F /IM node.exe >nul 2>nul
ping -n 3 127.0.0.1 >nul

echo Building...
call npx prisma generate
if %errorlevel% neq 0 (
    echo Prisma generate failed
    pause
    exit /b 1
)
call npx nest build
if %errorlevel% neq 0 (
    echo Build failed
    pause
    exit /b 1
)

echo Starting server...
start "eNurse" /MIN node dist/src/main.js
ping -n 5 127.0.0.1 >nul

echo.
echo Server running at http://localhost:3000
echo Verification codes printed in this window [EmailService]
echo.
pause
