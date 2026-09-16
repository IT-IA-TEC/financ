# Abre o IT.FC direto numa tela, para conferir sem precisar clicar.
#   .\conferir-tela.ps1 pagar
param([string]$Tela = "painel")

$java = [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine') + '\bin\java.exe'
$bibliotecas = (Get-ChildItem 'target\bibliotecas' -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'

& $java -cp "target\classes;$bibliotecas" `
  "-Ditfc.entrarComo=William" "-Ditfc.empresa=you" "-Ditfc.tela=$Tela" `
  br.com.itia.financeiro.Inicio `
  --spring.datasource.url="jdbc:h2:file:./dados/financeiro;DB_CLOSE_DELAY=-1;MODE=PostgreSQL" `
  --spring.datasource.driver-class-name=org.h2.Driver `
  --spring.datasource.username=financeiro `
  --spring.datasource.password=financeiro `
  --spring.flyway.locations=classpath:db/migration/h2
