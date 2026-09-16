# Abre o IT.FC ligado no banco de arquivo desta maquina, com os dados de teste.
# Serve para conferir as telas durante a conversao, sem tocar no banco de verdade.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$java = [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine') + '\bin\java.exe'
$bibliotecas = (Get-ChildItem 'target\bibliotecas' -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
$caminho = "target\classes;$bibliotecas"

& $java -cp $caminho "-Ditfc.entrarComo=William" "-Ditfc.empresa=you" br.com.itia.financeiro.Inicio `
  --spring.datasource.url="jdbc:h2:file:./dados/financeiro;DB_CLOSE_DELAY=-1;MODE=PostgreSQL" `
  --spring.datasource.driver-class-name=org.h2.Driver `
  --spring.datasource.username=financeiro `
  --spring.datasource.password=financeiro `
  --spring.flyway.locations=classpath:db/migration/h2
