@echo off
rem Abre o IT.FC sempre na versao mais nova, para conferir o que acabou de ser feito.
rem Fecha a janela que estiver aberta, monta o que mudou e abre de novo.

cd /d "%~dp0"
echo Fechando a janela anterior, se houver...
taskkill /FI "WINDOWTITLE eq IT.FC*" /IM java.exe /F >nul 2>&1

echo Montando a versao mais nova...
powershell -ExecutionPolicy Bypass -NoProfile -Command "$env:JAVA_HOME=[Environment]::GetEnvironmentVariable('JAVA_HOME','Machine'); & '.\ferramentas\apache-maven-3.9.16\bin\mvn.cmd' -q -DskipTests package"

if errorlevel 1 (
  echo.
  echo Algo quebrou ao montar. Avise o Claude.
  pause
  exit /b 1
)

echo Abrindo o IT.FC...
start "" powershell -ExecutionPolicy Bypass -NoProfile -WindowStyle Hidden -File "abrir-com-dados-de-teste.ps1"
exit /b 0
