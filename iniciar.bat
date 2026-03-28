@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem Credenciais MySQL (opcional): copie mysql-local.env.example para mysql-local.env e preencha MYSQL_PASSWORD
if exist "%~dp0mysql-local.env" (
  for /f "usebackq tokens=1* eol=# delims==" %%a in ("%~dp0mysql-local.env") do (
    if not "%%a"=="" set "%%a=%%b"
  )
)

set "PORTA_APP=8080"
set "APP_VERSION=20260327b"
set "URL_APP=http://localhost:%PORTA_APP%/?v=%APP_VERSION%"
set "TITULO_JANELA=Contab360"

echo.
echo [%TIME%] Liberando porta %PORTA_APP%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $p=%PORTA_APP%; Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue | ForEach-Object { try { Stop-Process -Id $_.OwningProcess -Force -ErrorAction Stop } catch {} } }"

echo [%TIME%] Encerrando execucoes antigas do Contab360...
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { ($_.Name -match 'java|javaw|cmd|powershell') -and $_.CommandLine -and ($_.CommandLine -match 'contab360') -and ($_.CommandLine -match 'spring-boot:run') } | ForEach-Object { try { Stop-Process -Id $_.ProcessId -Force -ErrorAction Stop } catch {} } }"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 1" >nul

echo [%TIME%] Subindo Spring Boot em nova janela (clean + run, logs aparecem la^)...
start "%TITULO_JANELA%" /D "%~dp0" cmd /k "mvn clean spring-boot:run"

echo [%TIME%] Aguardando servidor responder em %PORTA_APP%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$port=%PORTA_APP%; $max=240; for ($i=0; $i -lt $max; $i++) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1"

if errorlevel 1 (
  echo.
  echo Nao foi possivel detectar o servidor na porta %PORTA_APP% a tempo. Verifique a janela "%TITULO_JANELA%".
  pause
  exit /b 1
)

echo [%TIME%] Abrindo navegador em %URL_APP%
start "" "%URL_APP%"

echo.
echo Pronto. Servidor na janela "%TITULO_JANELA%" - feche-a para encerrar. Esta janela fecha em 5 segundos...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 5" >nul
exit /b 0
