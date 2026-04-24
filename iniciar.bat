@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem Credenciais MySQL (opcional): copie mysql-local.env.example para mysql-local.env.
rem Aceita MYSQL_USER/MYSQL_PASSWORD e mapeia para CONTAB360_MYSQL_USER/CONTAB360_MYSQL_PASSWORD.
if exist "%~dp0mysql-local.env" (
  for /f "usebackq tokens=1* eol=# delims==" %%a in ("%~dp0mysql-local.env") do (
    if not "%%a"=="" (
      set "%%a=%%b"
      call :trimVar %%a
    )
  )
)
call :trimVar MYSQL_USER
call :trimVar MYSQL_PASSWORD
call :trimVar CONTAB360_MYSQL_USER
call :trimVar CONTAB360_MYSQL_PASSWORD
rem Prioriza CONTAB360_* (ex.: mysql-local.env). MYSQL_* so entra como fallback.
if not defined CONTAB360_MYSQL_USER if defined MYSQL_USER set "CONTAB360_MYSQL_USER=%MYSQL_USER%"
if not defined CONTAB360_MYSQL_PASSWORD if defined MYSQL_PASSWORD set "CONTAB360_MYSQL_PASSWORD=%MYSQL_PASSWORD%"
call :trimVar CONTAB360_MYSQL_USER
call :trimVar CONTAB360_MYSQL_PASSWORD

if not defined CONTAB360_MYSQL_USER set "CONTAB360_MYSQL_USER=root"

rem Forca credenciais no ambiente do processo Java/Spring para evitar fallback inesperado.
set "SPRING_DATASOURCE_USERNAME=%CONTAB360_MYSQL_USER%"
set "SPRING_DATASOURCE_PASSWORD=%CONTAB360_MYSQL_PASSWORD%"

echo [INFO] MySQL user carregado: "%CONTAB360_MYSQL_USER%"
if defined CONTAB360_MYSQL_PASSWORD (
  echo [INFO] MySQL password carregado: [definido]
) else (
  echo [INFO] MySQL password carregado: [vazio]
)

set "PORTA_APP=8080"
set "PORTA_FRONT=3000"
set "APP_VERSION=20260327b"
set "URL_APP=http://localhost:%PORTA_APP%/?v=%APP_VERSION%"
set "URL_FRONT=http://localhost:%PORTA_FRONT%"
set "TITULO_JANELA=Contab360"
set "TITULO_JANELA_FRONT=Contab360 Frontend"
set "FRONTEND_DIR="
if exist "%~dp0frontend\package.json" set "FRONTEND_DIR=%~dp0frontend"

echo.
echo [%TIME%] Liberando porta %PORTA_APP%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $p=%PORTA_APP%; Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue | ForEach-Object { try { Stop-Process -Id $_.OwningProcess -Force -ErrorAction Stop } catch {} } }"
if not "%FRONTEND_DIR%"=="" (
  echo [%TIME%] Liberando porta %PORTA_FRONT% do frontend...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $p=%PORTA_FRONT%; Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue | ForEach-Object { try { Stop-Process -Id $_.OwningProcess -Force -ErrorAction Stop } catch {} } }"
)

echo [%TIME%] Encerrando execucoes antigas do Contab360...
taskkill /FI "WINDOWTITLE eq %TITULO_JANELA%" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq %TITULO_JANELA_FRONT%" /T /F >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $root='%~dp0'; $front='%FRONTEND_DIR%'; Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -and ( (($_.Name -match 'java|javaw|mvn|mvn\.cmd') -and ($_.CommandLine -match 'spring-boot:run|mvn(\.cmd)?\s+clean\s+spring-boot:run|-jar\s+.*contab360.*\.jar') -and $_.CommandLine.Contains($root)) -or (($front -ne '') -and ($_.Name -match 'node|npm|npm\.cmd') -and ($_.CommandLine -match 'npm\s+run\s+dev|next\s+dev|vite') -and $_.CommandLine.Contains($front)) ) } | ForEach-Object { try { Stop-Process -Id $_.ProcessId -Force -ErrorAction Stop } catch {} } }"
echo [%TIME%] Aguardando encerramento completo da execucao anterior...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "& { $ok=$false; for ($i=0; $i -lt 40; $i++) { $appBusy = Get-NetTCPConnection -LocalPort %PORTA_APP% -ErrorAction SilentlyContinue; if (-not $appBusy) { $ok=$true; break }; Start-Sleep -Milliseconds 500 }; if ($ok) { exit 0 } else { exit 1 } }"

if errorlevel 1 (
  echo.
  echo Nao foi possivel encerrar completamente a execucao anterior. Tente fechar manualmente e execute novamente.
  pause
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 1" >nul

echo [%TIME%] Subindo Spring Boot em nova janela (clean + run, logs aparecem la^)...
start "%TITULO_JANELA%" /D "%~dp0" cmd /k "mvn clean spring-boot:run"
if not "%FRONTEND_DIR%"=="" (
  echo [%TIME%] Subindo Frontend em nova janela...
  start "%TITULO_JANELA_FRONT%" /D "%FRONTEND_DIR%" cmd /k "npm run dev"
) else (
  echo [%TIME%] Frontend do Contab360 nao encontrado em "%~dp0frontend". Seguindo apenas com backend.
)

echo [%TIME%] Aguardando servidor responder em %PORTA_APP%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$port=%PORTA_APP%; $max=240; for ($i=0; $i -lt $max; $i++) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1"

if errorlevel 1 (
  echo.
  echo Nao foi possivel detectar o servidor na porta %PORTA_APP% a tempo. Verifique a janela "%TITULO_JANELA%".
  pause
  exit /b 1
)

if not "%FRONTEND_DIR%"=="" (
  echo [%TIME%] Aguardando frontend responder em %PORTA_FRONT%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$port=%PORTA_FRONT%; $max=240; for ($i=0; $i -lt $max; $i++) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1"
  if errorlevel 1 (
    echo.
    echo Nao foi possivel detectar o frontend na porta %PORTA_FRONT% a tempo. Verifique a janela "%TITULO_JANELA_FRONT%".
    pause
    exit /b 1
  )
)

if not "%FRONTEND_DIR%"=="" (
  echo [%TIME%] Abrindo Chrome em modo anonimo em %URL_FRONT%
  start "" chrome --incognito "%URL_FRONT%"
) else (
  echo [%TIME%] Abrindo Chrome em modo anonimo em %URL_APP%
  start "" chrome --incognito "%URL_APP%"
)

echo.
echo Pronto. Servidor na janela "%TITULO_JANELA%" - feche-a para encerrar. Esta janela fecha em 5 segundos...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 5" >nul
exit /b 0

:trimVar
set "TMP=!%~1!"
if not defined TMP exit /b 0
for /f "tokens=* delims= " %%T in ("!TMP!") do set "TMP=%%T"
:trimVarLoop
if not defined TMP goto trimVarSet
if not "!TMP:~-1!"==" " goto trimVarSet
set "TMP=!TMP:~0,-1!"
goto trimVarLoop
:trimVarSet
set "%~1=!TMP!"
exit /b 0
