@echo off
cd /d "%~dp0server"

echo Stopping old processes...
taskkill /F /IM node.exe >nul 2>&1
timeout /t 2 /nobreak >nul

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
start /B node dist/src/main.js
timeout /t 3 /nobreak >nul

echo.
echo Server running at http://localhost:3000
echo Verification codes printed in this window [EmailService]
echo.
pause
