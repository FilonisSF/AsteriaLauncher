@echo off
chcp 65001 >nul
cd /d "%~dp0"
where node >nul 2>nul || (echo Установите Node.js LTS: https://nodejs.org & pause & exit /b)
if not exist backend\data\ticket-private.pem (
  echo ВНИМАНИЕ: нет ключа подписи backend\data\ticket-private.pem.
  echo Скопируйте папку backend\data из прошлой версии лаунчера - иначе PREMIUM-визуалы не включатся.
  pause & exit /b
)
echo.
echo Сервер Asteria (подписки, скины, плащи, значки). Не закрывайте это окно.
echo Админ-панель: http://127.0.0.1:8080/admin
echo Лаунчер на этом ПК находит этот сервер сам.
echo.
node backend\server.cjs
pause
