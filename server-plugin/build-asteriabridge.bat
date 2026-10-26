@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Сборка плагина AsteriaBridge...
where java >nul 2>nul || (echo Не найдена Java. Установите JDK 17: https://adoptium.net/temurin/releases/?version=17 ^(Windows x64, .msi^), затем запустите этот файл ещё раз. & pause & exit /b)
call gradlew.bat build --no-daemon
if errorlevel 1 (
  echo.
  echo Сборка не удалась. Пришлите скриншот этого окна ^(прокрутите вверх до первой красной ошибки^).
  pause & exit /b
)
echo.
echo Готово! Плагин: %~dp0build\libs\AsteriaBridge-1.2.0.jar
echo Положите его на сервер в папку plugins ^(заменив старый^).
explorer "%~dp0build\libs"
pause
