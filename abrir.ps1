# Abre o IT.FC direto do que foi montado, sem precisar empacotar.
# Serve para trabalhar: o programa instalável é gerado pelo empacotar.ps1.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$java = [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine') + '\bin\java.exe'
$bibliotecas = (Get-ChildItem 'target\bibliotecas' -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
$caminho = "target\classes;$bibliotecas"

& $java -cp $caminho br.com.itia.financeiro.Inicio
