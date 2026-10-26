@echo off
chcp 65001 >nul
cd /d "%~dp0"
where node >nul 2>nul || (echo Установите Node.js LTS: https://nodejs.org & pause & exit /b)
if not exist backend\data\ticket-private.pem (
  echo ВНИМАНИЕ: нет ключа подписи backend\data\ticket-private.pem.
  echo Скопируйте папку backend\data из прошлой версии лаунчера - иначе PREMIUM-визуалы не включатся.
  pause & exit /b
)
rem an older Asteria server (e.g. from another folder) may still hold port 8080: stop it, otherwise the launcher keeps talking to the old one
for /f "tokens=2,5" %%a in ('netstat -ano ^| findstr /b /c:"  TCP"') do (
  echo %%a| findstr /e ":8080" >nul && (
    echo Закрываю старый сервер на порту 8080 ^(PID %%b^)...
    taskkill /PID %%b /F >nul 2>nul
  )
)
echo.
echo Сервер Asteria (подписки, скины, плащи, значки). Не закрывайте это окно.
echo Админ-панель: http://127.0.0.1:8080/admin
echo Лаунчер на этом ПК находит этот сервер сам.
echo.
node backend\server.cjs
pause
