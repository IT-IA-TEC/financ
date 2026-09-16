# IT.FC — o sistema financeiro do grupo (Java)

Sistema próprio, em Java, com **banco próprio e zerado**, feito para atender
**uma ou mais empresas ao mesmo tempo, sem misturar nada** entre elas.

Identidade visual: **IT.IA** (preto, branco, vermelho puro, canto reto, sem
sombra), tokens tirados de `IT.IA/identidade/tokens/`.

---

## Como rodar

Precisa de duas coisas instaladas: **Java 21** (já está) e **Maven**.

```
mvn spring-boot:run
```

Depois abrir no navegador: **http://localhost:8090**

A primeira tela pede para cadastrar a empresa. O banco começa vazio, do jeito
que foi combinado — nada é importado de lugar nenhum.

### Onde ficam os dados

Por enquanto, num arquivo dentro da própria pasta do projeto
(`IT.FC/dados/`). É o modo "roda na máquina sem instalar servidor".

Quando for para valer, o banco é PostgreSQL e o sistema sobe assim:

```
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

lendo três variáveis de ambiente: `FINANCEIRO_DB_URL`, `FINANCEIRO_DB_USUARIO`
e `FINANCEIRO_DB_SENHA`. O desenho das tabelas é o mesmo nos dois casos
(`src/main/resources/db/migration/`).

---

## A regra que sustenta o multiempresa

Tudo tem empresa: cliente, título, pagamento, evento. Nenhuma consulta do
sistema atravessa duas empresas — quem quiser buscar qualquer coisa é obrigado
a dizer de qual empresa está falando (ver `ClienteRepositorio` e
`TituloRepositorio`, onde todo método pede o `empresaId`).

Quem escolhe a empresa é a pessoa, na tela de entrada. A escolha fica guardada
na sessão (`ContextoEmpresa`) e é o único lugar do sistema que responde "qual
empresa?". Trocar de empresa é trocar a tela inteira.

O crachá (`UsuarioEmpresa`) diz quem cuida de qual empresa e com qual alçada:
operador, gestor ou diretor.

---

## O que já está de pé

| Parte | Situação |
|---|---|
| Cadastro de empresas e troca de empresa | pronto |
| Cadastro de clientes (por empresa) | pronto |
| Lançamento de título com número e código PIX automáticos | pronto |
| Baixa de pagamento, com trava de pagamento repetido | pronto |
| Saldo, situação e dias de atraso calculados (nunca digitados) | pronto |
| Cancelamento de título com motivo obrigatório | pronto |
| Trilha de auditoria de tudo que mexe em dinheiro | pronto |
| Painel com em aberto, vencido, recebido no mês | pronto |
| Telas na identidade da IT.IA | pronto |
| Trava de pagamento repetido (mesmo identificador de transação) | pronto |
| Tolerância de até R$ 1,00 fechando o título como pago | pronto |
| Pacotes, serviços realizados e cobranças do atendimento | pronto |
| Contas a pagar por empresa, com rateio, parcelas e recorrência | pronto |
| Fechamento do mês, com prévia e trava de competência | pronto |
| Conciliação bancária (importar extrato, casar, ignorar com motivo) | pronto |
| Conferência de comprovantes, com conferência do destino | pronto |
| Disparo de cobrança em lote, com prévia e quem ficou de fora | pronto |
| Conversa de WhatsApp dentro da plataforma | pronto |
| Régua de cobrança, rodando sozinha de manhã | pronto |
| Esteira de inadimplência por faixa de atraso | pronto |
| Acordos e parcelamento de dívida | pronto |
| Estúdio de fluxos (quando, se, faça) | pronto |
| Agente de primeiro atendimento | pronto |

## O que ainda não está

| Parte | Observação |
|---|---|
| Login | hoje qualquer um que abrir o endereço entra; o plano é a ponte com o login do ERP |
| Ponte com o banco atual | a tabela de fila (`fila_espelho_erp`) já existe, o envio não |
| Envio de verdade pelo WhatsApp | a ligação está pronta e configurável; falta a conta oficial da empresa |
| Leitura da imagem do comprovante | hoje o sistema lê o texto colado; a imagem depende de um serviço de fora |
| Testes automatizados | ainda nenhum escrito; a conferência foi feita na tela, passo a passo |

---

## Duas regras que não podem ser quebradas

1. **Este sistema não escreve na Conexa.** Nem direto, nem por tabela
   intermediária. A separação Financeiro/Conexa é ordem permanente do William.
2. **O cadastro de cliente é do ERP.** Aqui existe uma cópia (`cliente_espelho`)
   só para o título ter dono. Quando a ponte entrar, a cópia passa a chegar de
   lá e o cadastro manual desta tela sai.

---

## Mapa dos arquivos

```
src/main/java/br/com/blancoelisboa/financeiro/
  dominio/      as coisas do negócio: Empresa, Cliente, Titulo, Pagamento, Evento
  repositorio/  as buscas no banco (todas pedem a empresa)
  servico/      as regras: FinanceiroServico, PainelServico, ContextoEmpresa
  web/          as telas e os endereços
src/main/resources/
  db/migration/ o desenho das tabelas (postgresql/ e h2/)
  templates/    as telas em HTML
  static/css/   a identidade da IT.IA
```
