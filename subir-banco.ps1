# Cria no banco de verdade (Supabase) tudo o que o IT.FC precisa.
#
# Quem cria as tabelas e o proprio sistema: ele le os 27 passos de
# src/main/resources/db/migration/postgresql e vai aplicando em ordem, um por
# um, guardando o que ja aplicou. Rodar duas vezes nao duplica nada.
#
# Antes de rodar: copiar dados/banco.env.modelo para dados/banco.env e colar
# ali a linha do painel do Supabase. A pasta dados/ nunca sobe para o GitHub.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$arquivo = Join-Path $PSScriptRoot 'dados\banco.env'
if (-not (Test-Path $arquivo)) {
  Write-Host "Faltou o arquivo dados\banco.env. Copie dados\banco.env.modelo e cole a linha do painel." -ForegroundColor Red
  exit 1
}

$linhaBanco = $null
Get-Content $arquivo | ForEach-Object {
  $linha = $_.Trim()
  if ($linha -and -not $linha.StartsWith('#')) {
    $corte = $linha.IndexOf('=')
    if ($corte -gt 0) {
      $nome = $linha.Substring(0, $corte).Trim()
      $valor = $linha.Substring($corte + 1).Trim()
      if ($nome -eq 'BANCO') { $linhaBanco = $valor } else { Set-Item -Path ("Env:" + $nome) -Value $valor }
    }
  }
}

# A linha do painel vem inteira: postgresql://usuario:senha@endereco:porta/banco
# Aqui ela e separada nas tres partes que o sistema espera.
if ($linhaBanco) {
  if ($linhaBanco -notmatch '^postgres(ql)?://([^:]+):(.+)@([^/]+)/(.+)$') {
    Write-Host "A linha BANCO nao esta no formato do painel do Supabase." -ForegroundColor Red
    exit 1
  }
  $usuario = $Matches[2]
  $senha = [uri]::UnescapeDataString($Matches[3])
  $endereco = $Matches[4]
  $nomeDoBanco = ($Matches[5] -split '\?')[0]
  if ($senha -match '^\[?YOUR-PASSWORD\]?$' -or $senha -eq 'SENHA') {
    Write-Host "A senha ainda nao foi trocada em dados\banco.env." -ForegroundColor Red
    exit 1
  }
  $env:FINANCEIRO_DB_URL = "jdbc:postgresql://$endereco/$nomeDoBanco" + "?sslmode=require"
  $env:FINANCEIRO_DB_USUARIO = $usuario
  $env:FINANCEIRO_DB_SENHA = $senha
}

foreach ($obrigatorio in 'FINANCEIRO_DB_URL','FINANCEIRO_DB_USUARIO','FINANCEIRO_DB_SENHA') {
  if (-not (Get-Item -Path ("Env:" + $obrigatorio) -ErrorAction SilentlyContinue)) {
    Write-Host "Faltou preencher $obrigatorio em dados\banco.env." -ForegroundColor Red
    exit 1
  }
}

Write-Host ("Banco: " + $env:FINANCEIRO_DB_URL + " (usuario " + $env:FINANCEIRO_DB_USUARIO + ")") -ForegroundColor DarkGray

if (-not (Test-Path 'target\financeiro-0.1.0.jar')) {
  Write-Host "O sistema ainda nao foi montado. Rodando o build antes." -ForegroundColor Yellow
  $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine')
  & .\ferramentas\apache-maven-3.9.16\bin\mvn.cmd -q -DskipTests package
}

$java = [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine') + '\bin\java.exe'
Write-Host "Ligando no banco e criando as tabelas..." -ForegroundColor Cyan
& $java -jar target\financeiro-0.1.0.jar --spring.profiles.active=postgres --server.port=8091
