@echo off
chcp 65001 >nul
cd /d "%~dp0"
where node >nul 2>nul || (echo Установите Node.js LTS: https://nodejs.org & pause & exit /b)
echo Устанавливаю зависимости (первый раз 2-5 минут)...
call npm install || (pause & exit /b)
echo Собираю установщик...
call npm run build || (pause & exit /b)
echo.
echo Готово: установщик лежит в папке release
start "" release
pause
