@echo off
chcp 65001 >nul
cd /d "%~dp0"
where git >nul 2>nul || (echo Установите Git: https://git-scm.com/download/win & pause & exit /b)

if not exist .git git init -b main
git remote get-url origin >nul 2>nul || git remote add origin https://github.com/FilonisSF/AsteriaLauncher.git

rem Git needs an author name for commits. If none is set, use the GitHub name (email = GitHub's private address, not yours).
git config user.name >nul 2>nul || git config user.name "FilonisSF"
git config user.email >nul 2>nul || git config user.email "FilonisSF@users.noreply.github.com"

for /f "tokens=2 delims=:, " %%v in ('findstr /c:"\"version\"" package.json') do set VER=%%~v
git checkout -B main >nul 2>nul
git add -A
git commit -m "AsteriaLauncher %VER%" || echo (новых изменений нет — отправляю то, что уже есть)
git rev-parse --verify HEAD >nul 2>nul || (echo Не удалось создать коммит — пришлите скриншот этого окна. & pause & exit /b)

echo.
echo Отправляю на GitHub... Если откроется окно входа — войдите в аккаунт FilonisSF.
git push -u origin main && goto pushed
rem GitHub already has files we don't (README/LICENSE made when the repo was created): merge them in, our files win on conflicts
echo На GitHub уже есть файлы ^(например README^) — объединяю с ними...
git pull --no-rebase --allow-unrelated-histories -X ours --no-edit origin main
git push -u origin main || (
  echo.
  echo Не получилось отправить. Проверьте:
  echo  1. На github.com создан репозиторий FilonisSF/AsteriaLauncher ^(New repository, пустой^).
  echo  2. Вы вошли в окне входа GitHub ^(если окна не было — запустите этот файл ещё раз^).
  pause & exit /b
)
:pushed
echo Готово: https://github.com/FilonisSF/AsteriaLauncher

set /p REL=Выпустить обновление лаунчера %VER% для всех игроков? (y/n):
if /i "%REL%"=="y" (
  git tag -f v%VER%
  git push -f origin v%VER%
  echo Обновление собирается: https://github.com/FilonisSF/AsteriaLauncher/actions
  echo Через ~10 минут все лаунчеры обновятся сами.
)
pause
