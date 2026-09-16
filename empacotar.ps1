# Gera o IT.FC como programa do Windows.
#
# Sem nenhum programa extra instalado, sai uma pasta pronta com o IT.FC.exe
# dentro, que abre com dois cliques e ja leva o Java junto: quem receber nao
# precisa instalar Java nenhum.
#
# Se o WiX Toolset estiver instalado na maquina, sai tambem o instalador de
# verdade (.msi), aquele que pergunta onde instalar e cria atalho no menu.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine')

Write-Host "Montando o sistema..." -ForegroundColor Cyan
& .\ferramentas\apache-maven-3.9.16\bin\mvn.cmd -q -DskipTests package

# tudo o que o programa precisa fica em uma pasta so
$entrega = Join-Path $PSScriptRoot 'target\entrega'
if (Test-Path $entrega) { Remove-Item $entrega -Recurse -Force }
New-Item -ItemType Directory -Force -Path $entrega | Out-Null
Copy-Item 'target\financeiro-0.1.0.jar' $entrega
Copy-Item 'target\bibliotecas\*.jar' $entrega

$saida = Join-Path $PSScriptRoot 'target\programa'
if (Test-Path $saida) { Remove-Item $saida -Recurse -Force }
New-Item -ItemType Directory -Force -Path $saida | Out-Null

$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'

Write-Host "Gerando o programa..." -ForegroundColor Cyan
& $jpackage --type app-image --name 'IT.FC' --app-version '0.1.0' `
  --vendor 'IT.IA' --description 'Gestao financeira' `
  --input $entrega --main-jar 'financeiro-0.1.0.jar' `
  --main-class 'br.com.itia.financeiro.Inicio' `
  --dest $saida

$temWix = $null -ne (Get-Command candle.exe -ErrorAction SilentlyContinue)
if ($temWix) {
  Write-Host "Gerando o instalador..." -ForegroundColor Cyan
  & $jpackage --type msi --name 'IT.FC' --app-version '0.1.0' `
    --vendor 'IT.IA' --description 'Gestao financeira' `
    --input $entrega --main-jar 'financeiro-0.1.0.jar' `
    --main-class 'br.com.itia.financeiro.Inicio' `
    --dest $saida `
    --win-dir-chooser --win-menu --win-shortcut
} else {
  Write-Host "Sem o WiX Toolset nesta maquina, entao o instalador .msi nao foi gerado." -ForegroundColor Yellow
  Write-Host "A pasta com o IT.FC.exe esta pronta e funciona do mesmo jeito." -ForegroundColor Yellow
}

Write-Host "Pronto. Resultado em: $saida" -ForegroundColor Green
Write-Host "Lembre de copiar dados\banco.env para a pasta de dados do programa" -ForegroundColor Green
Write-Host "  (%LOCALAPPDATA%\IT.FC\banco.env), senao ele abre no banco desta maquina." -ForegroundColor Green
