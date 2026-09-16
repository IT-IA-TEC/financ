# Estado da operação · IT.FC

Fechamento de 16/09/2026. Este arquivo é a porta de entrada do projeto: quem
ler isto entende onde o sistema está, o que já foi provado, o que ainda não
foi, e o que falta, sem precisar perguntar nada a ninguém.

---

## 1. O que o sistema é

O IT.FC é o financeiro da YOU Contabilidade em forma de **programa de janela**,
instalado na máquina. Não é site e não abre no navegador: a pessoa clica no
ícone e o sistema abre como qualquer outro programa do computador.

Ele cuida do dinheiro do escritório de ponta a ponta:

- **contas a receber**: o que os clientes devem, com vencimento, baixa e saldo;
- **contas a pagar**: o que a empresa deve, com aprovação, pagamento e
  conciliação com o extrato;
- **cobrança**: a mesa de atendimento por WhatsApp, a régua de mensagens, os
  acordos e a esteira de quem está atrasado;
- **clientes, pacotes e serviços**: quem é cliente, o que ele contratou e o que
  isso gera de cobrança;
- **tarefas e fechamento do mês**: o que precisa ser feito e o que já fechou;
- **painel**: a posição de hoje, calculada na hora a partir do que está lançado.

Uma regra que atravessa o sistema inteiro: **número não se digita**. Todo valor
que aparece na tela é calculado a partir dos lançamentos. Por isso dois lugares
do sistema nunca divergem entre si.

O banco é **separado do ERP da Blanco & Lisboa**. Os dois sistemas conversam por
API quando o dono mandar, nunca por banco compartilhado.

---

## 2. Como ele é montado, camada por camada

Tudo vive em `src/main/java/br/com/itia/financeiro/`, e cada camada só conhece a
de baixo.

| Camada | Onde fica | O que faz | Tamanho |
|---|---|---|---|
| Entrada | `Inicio.java`, `FinanceiroApplication.java` | liga o programa: sobe o Spring sem servidor web e abre a janela | 2 arquivos |
| Janela | `tela/Janela.java` | a moldura: coluna lateral, faixa de cima, histórico de navegação, avisos | 1 arquivo |
| Telas | `tela/` | uma classe por tela do sistema, cada uma sabendo só montar a si mesma | 57 arquivos |
| Peças | `tela/Pecas.java`, `tela/Tabela.java`, `tela/JanelaFlutuante.java` | os blocos repetidos: cabeçalho, cartão, botão, tabela com filtro, janela por cima | 3 arquivos |
| Marca | `marca/` | fontes, cores, ícones de traço e os valores da identidade | Clash Display e Geist embutidas |
| Serviços | `servico/` | as regras de verdade: o que pode, o que não pode, o que cada número significa | 44 arquivos |
| Domínio | `dominio/` | as coisas do mundo real: título, obrigação, cliente, acordo, plano de conta | 102 arquivos |
| Repositórios | `repositorio/` | a conversa com o banco, via Spring Data JPA | 71 arquivos |
| Ponte | `ponte/` | a ligação com o ERP, ainda desligada por configuração | pasta pequena |
| Banco | `src/main/resources/db/migration/` | a estrutura do banco em passos versionados pelo Flyway | 28 passos em H2 e 28 em PostgreSQL |
| Visual | `src/main/resources/estilo/itia.css`, `identidade/identidade.json` | a aparência inteira; o JSON manda no visual sem mexer em código | 2 arquivos |

Por baixo: **Java 21 (Temurin)**, **JavaFX 21.0.4** para a janela, **Spring Boot
3.3.4** em modo sem servidor web, **Flyway** para o banco, **H2 em arquivo** na
máquina e **PostgreSQL (Supabase)** para o banco de verdade.

---

## 3. O que está pronto, e o que provou

| O que | Como foi provado | Resultado |
|---|---|---|
| O sistema compila e empacota inteiro | `ferramentas\apache-maven-3.9.16\bin\mvn.cmd clean package` na pasta nova | BUILD SUCCESS, gerou `target/financeiro-0.1.0.jar` |
| O banco bate com o modelo | o programa sobe com conferência de estrutura ligada (`ddl-auto: validate`) e o Flyway aplica os 28 passos | abriu sem erro depois do passo novo do livro caixa |
| As 57 telas existem e abrem | `abrir-com-dados-de-teste.ps1` e navegação pelas telas, fotografadas | fotos em `provas/telas/` |
| A janela tem o que a versão web tinha | comparação item por item: `provas/varredura/comparar.py` | de 293 pedaços faltando para 47, todos cosméticos · detalhe em `O QUE FALTA NO DESKTOP.md` |
| Painel com valores escondidos por padrão | aberto o painel e clicado o olho | `provas/telas/painel-valores-escondidos.png` e `painel-valores-a-mostra.png` |
| Quem paga travado na empresa aberta | tela de lançar conta aberta com a YOU Contabilidade | `provas/telas/lancar-conta-quem-paga-travado.png` |
| Plano de conta com categoria, subcategoria e livro caixa | criada a categoria "Assinaturas e software" com a marca de livro caixa | `provas/telas/janela-novo-item-do-plano.png` e `lancar-conta-item-livro-caixa.png` |
| Rascunho de conta a pagar | salvo um rascunho e aberto o recorte Rascunhos | `provas/telas/ficha-da-conta-rascunho-guardado.png` e `contas-a-pagar-rascunhos.png` |
| Aviso de inadimplência no menu | contagem de clientes com título vencido na coluna lateral | `provas/telas/menu-aviso-de-inadimplencia.png` |
| Cards do topo dentro da borda | defeito reproduzido e corrigido na ficha da conta | antes: `defeito-cards-por-cima-do-titulo.png` · depois: `corrigido-cards-em-duas-linhas.png` |
| Instalável para Windows | `empacotar.ps1` (rodado em 16/09/2026, antes das últimas mudanças) | gerou `target/programa/IT.FC/IT.FC.exe`, 211 MB |

---

## 4. A linha entre o que foi provado e o que só foi escrito

**O sistema está provado do nosso lado: ele compila, abre, monta as telas, grava
e lê no banco de arquivo desta máquina. Nenhuma chamada real foi feita a serviço
de fora: nem a banco (Inter), nem a WhatsApp, nem ao banco de verdade no
Supabase em uso do dia a dia, nem a qualquer modelo de inteligência.** As telas
de integração, de inteligência e de disparo existem e gravam a configuração, mas
o que sai delas para o mundo nunca foi disparado de verdade.

O mesmo vale para o sino de avisos: ele está na tela, e não é aviso de verdade
ainda.

---

## 5. O que falta

### Decisão do dono

- **Aba Inadimplência**: está em branco por ordem expressa. Falta dizer o que
  entra na lista (nome, quanto deve, dias de atraso, ação).
- **Banco de verdade**: ligar o sistema no Supabase no dia a dia, ou continuar
  no banco de arquivo da máquina.
- **Banco Inter**: levantamento e prévia prontos, parados esperando decisão.
- **Pasta antiga de referência**: `C:\Projetos\ERP-BLANCO-&-LISBOA\FINANCEIRO-JAVA-ANTIGO`
  guarda a versão web que roda em http://localhost:8090, usada para comparar.
  Apagar ou manter.
- **Conta de teste**: a conta nº 2, "Teste de rascunho", de R$ 100,00, ficou no
  banco desta máquina como prova do caminho do rascunho. Apagar ou manter.

### Acesso que depende de terceiro

- **WhatsApp pela API oficial da Meta**: depende de conta e aprovação da Meta.
- **Banco Inter**: depende de certificado e credencial do banco.
- **Instalador `.msi`**: só sai nesta máquina depois de instalar o WiX Toolset.
  O `.exe` funciona sem ele.

### Trabalho do agente

- Refazer o instalável, que foi gerado antes das últimas mudanças.
- Transformar o sino em aviso de verdade para o pessoal do financeiro (entra
  junto com as integrações).
- Fechar os 47 pontos cosméticos que sobraram da comparação com a versão web.
- Escrever teste automático: hoje o projeto não tem nenhum (`src/test` não
  existe). Toda prova até aqui é de build e de tela.

### Limpeza de segurança

- `dados/banco.env` guarda a senha do banco de verdade e **não sobe** para o
  GitHub: a pasta `dados/` inteira está fora do repositório. O modelo sem senha
  é o `dados/banco.env.modelo`.
- A senha `financeiro`, que aparece em `application.yml` e nos scripts de abrir,
  é a do banco de arquivo local, de teste. Não é credencial de produção. Quando
  o sistema for para o banco de verdade, essa senha some da configuração.
- `dados/chave-cofre` fica fora do repositório pela mesma regra.

---

## 6. Decisões já fechadas

Não se reabrem sem motivo novo.

1. **Programa de janela, nunca navegador.** O IT.FC é aplicativo instalável em
   Java/JavaFX. A versão web ficou só como referência de comparação.
2. **Banco do IT.FC separado do ERP.** Projeto próprio no Supabase
   (`ubrzrxtjlxemjefjbyze`). A conversa entre os sistemas é por API, e só quando
   o dono mandar.
3. **O visual sai do `itfc-design-system.json`**, entregue pela identidade da
   marca. Medida, cor e canto vêm de lá, não de improviso.
4. **Botão sólido escuro** com as letras acendendo em laranja no mouse, no lugar
   do botão laranja antigo.
5. **Menu em coluna lateral fixa**, que não recolhe.
6. **Sem 2FA por enquanto** na tela de entrada.
7. **Sem gráficos por enquanto** no painel.
8. **Nada de "Blanco e Lisboa" dentro do produto**: a marca no sistema é IT.IA /
   IT.FC.
9. **Valores do painel nascem escondidos** toda vez que o sistema abre.
10. **Quem paga é a empresa aberta**, não é escolha no lançamento.
11. **Plano de conta é a única classificação** do lançamento: tipo de operação e
    natureza saíram da tela.

---

## 7. Tropeços que já custaram caro

Para não repetir:

- **Apóstrofo no nome da pasta** (`YOU-JAVA's`) quebra comando de terminal comum.
  Mover e copiar com PowerShell usando `-LiteralPath`. Atenção: `New-Item` não
  aceita `-LiteralPath`, só `-Path`.
- **PowerShell estraga acento**: gravar texto em português por ele salva em ANSI
  e o menu vira `CONCILIAÃ‡ÃƒO`. Gravar sempre pela ferramenta de escrita do
  agente ou por Python com UTF-8.
- **`\t` em caminho dentro de script Python** vira tabulação: `"provas\telas"`
  virou `"provas<TAB>elas"`. Escrever com barra normal ou string crua.
- **Mover pasta com o sistema aberto não funciona**: processos com a pasta como
  diretório de trabalho seguram o caminho. Fechar tudo antes.
- **`mklink` falha em caminho com `&`**: usar `New-Item -ItemType Junction`.
- **Objeto vindo do banco não se compara por igualdade** no JavaFX: selecionar
  item de lista pelo identificador, nunca pelo objeto, senão a lista fica vazia.
- **Dado preguiçoso fora da transação** quebra a tela: montar o texto da lista
  enquanto o banco ainda está aberto.
- **Cartão com largura mínima não encolhe**: quando a fileira passa de quatro
  cartões, ela desce para uma linha própria, senão passa por cima do título.
- **Empurrar para o GitHub sem permissão** devolve "repositório não encontrado",
  que parece outra coisa. É permissão de escrita na conta conectada.

---

## 8. Mapa: onde cada coisa mora

| Pasta | O que tem |
|---|---|
| `src/main/java/` | o sistema inteiro, nas camadas da seção 2 |
| `src/main/resources/estilo/` | a folha de aparência (`itia.css`) |
| `src/main/resources/db/migration/` | os passos do banco, um para H2 e outro para PostgreSQL |
| `identidade/` | a identidade da marca: `identidade.json` (manda no visual em tempo de execução), `itfc-design-system.json` (a entrega original), a página de identidade e o zip como veio |
| `rascunhos/` | os rascunhos de tela aprovados, como o `itfc-login.html` |
| `provas/telas/` | as fotos que provam cada entrega da seção 3 |
| `provas/varredura/` | a comparação com a versão web: script, resultado bruto e relatório |
| `ferramentas-de-teste/` | os scripts que abrem o sistema, clicam e fotografam a tela |
| `referencia-web/` | a versão web antiga, guardada para consulta |
| `dados/` | o banco de arquivo desta máquina, os documentos anexados e as senhas · **fora do repositório** |
| `ferramentas/` | Maven e Gradle usados pelo projeto · **fora do repositório** |
| `target/` | o que o build gera · **fora do repositório** |
| `O QUE FALTA NO DESKTOP.md` | o relatório da comparação com a versão web |
| `README.md` | como abrir, empacotar e ligar o banco |

---

## 9. O que ficou fora do repositório, de propósito

| O que | Por quê |
|---|---|
| `dados/` | guarda o banco desta máquina, os documentos anexados, a senha do banco de verdade e a chave do cofre. Dado e segredo não são código. |
| `ferramentas/` | Maven e Gradle são download de terceiro, pesados e reinstaláveis. Quem clonar usa o da própria máquina. |
| `target/` | é o que o build gera; nasce de novo a cada construção e guarda o caminho da máquina. |
| `.idea/`, `.vscode/`, `*.iml` | preferência de editor de cada um. |

---

## 10. Commits desta arrumação

| Commit | O que é |
|---|---|
| `7ea6c73` | A leva de mudanças que veio antes desta arrumação: entrada nova, plano de conta e rascunho no contas a pagar |
| `b905d79` | Junta no projeto a identidade, o rascunho da entrada e as provas |
| `f16f06f` | Traz os scripts de conferência de tela e corta o caminho da máquina |
| `1e627a3` | Tira as últimas menções à pasta antiga |
| `e46cbd3` | Documento de entrada: o estado da operação |
| `527e91b` | Registra os números dos commits aqui nesta tabela |
