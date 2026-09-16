# O que a janela tem da versão web (fechamento de 16/09/2026)

A varredura comparou as 42 páginas da versão web com as 56 telas da janela, item
por item: títulos, seções, botões, colunas de tabela, campos e janelas.

**Antes desta leva: 293 pedaços faltando em 32 telas. Depois: 47, e nenhum deles
é função que falte.** O que sobrou está listado no fim, com o motivo.

## O que entrou nesta leva

### Integrações
- Lista com as colunas da página: como funciona, acesso, ambiente, última checagem.
- Ficha da ligação: tipo de ligação, forma de acesso, endereço base, tempo limite,
  tentativas, espera entre tentativas, conferir assinatura do aviso, para que serve.
- Operações: cadastrar, executar (com corpo da chamada) e remover.
- Testar conexão, com o resultado da última checagem à mostra.
- Endereço de recebimento, com troca do trecho secreto.
- Movimento da ligação: tudo o que entrou e saiu por ela.

### Inteligência
- O que ele nunca faz, com chave nenhuma.
- Como ele está ligado: modelo (integração), nome do modelo, até onde ele vai,
  teto de valor, mensagens por dia, ligado, e o que a empresa quer dele.
- As duas tabelas de chaves separadas: o que ele pode ver e o que ele pode fazer,
  cada uma com o que é e o porquê.
- O porteiro: o que aconteceria agora se o modelo tentasse cada ação.
- A ficha do caso: as linhas que aparecem na mesa, com ordem, rótulo, de onde sai,
  mostra ou não, guardar, tirar e acrescentar linha.
- O que o modelo leu e fez, com as chaves usadas e o resultado.

### Fluxos
- Cada fluxo com a receita dele: as condições e as ações, na ordem, com tirar.
- Nova condição (o que conferir, como, valor) e nova ação (o que fazer, texto da
  mensagem, em quantos dias, o que escrever).

### De onde vêm as tarefas
- Cada fonte aberta para ajuste, com prazo, prioridade, link, setor e ativa.
- Fonte nova com todos os campos, e busca por fonte.

### Contas a pagar
- Filtro por coluna (o funil) em vencimento, nº, favorecido, descrição, natureza,
  centro de custo, valor devido, pago, saldo, pendências e situação.
- Aviso de filtro aplicado e limpar filtros.
- Colunas de natureza, centro de custo e pendências na tabela.
- Cadastro de bem com fica dentro de, data e valor da aquisição.

### Conta a pagar (a ficha)
- Cartões de cadastro, no banco, liquidação e conciliação.
- Dados gerais abertos para ajuste: favorecido, descrição, tipo de operação,
  emissão, competência, vencimento, valor devido, conta financeira, quem paga,
  pessoa relacionada, bem, solicitante e observação.
- Marcar como completa e enviar para aprovação.
- Pagamento com retenção, e estorno de pagamento com motivo.
- Andamento no banco e conciliação.
- Documentos e origem, com anexar documento.

### Lançar conta
- Quem paga, natureza, centro de custo, pessoa relacionada, bem, solicitante,
  observação e situação do cadastro.

### Clientes
- Escolher as colunas da tela, separadas por pessoa e por unidade.
- Importar a base de clientes, com o formato do arquivo à mostra.
- CPF repetido: o sistema pergunta antes de somar as unidades ao cliente que já existe.
- Filtro por coluna (o funil) em toda a carteira, por texto, faixa, data e situação.

### Mesa de cobrança (Conversas)
- Já estava 100% desde a leva anterior: fila, sem cliente ligado, ferramentas com
  as teclas, comandos prontos, abas do cliente, de um clique, história e as janelas.

### Acordos, comprovantes, esteira, régua, disparos e realizados
- Acordos: novo acordo por cima da tela, com juros e multa, primeira vence em e observação.
- Comprovantes: receber comprovante (cliente, de onde veio, arquivo, texto) e
  voltar para a fila.
- Esteira: clicar em quem está na faixa abre o caso, com quem cuida, próxima data,
  próxima ação e observação.
- Régua: canal do passo, olhar antes de mandar, e os nomes da página.
- Disparos: novo disparo por cima, com canal e faixa de dias.
- Disparo: sair em (dia e hora), marcar como enviada, não entregue e tirar do disparo.
- Serviços realizados: registrar atendimento com todos os campos.

### Configurar empresas, serviços, conciliação, fechamento e tarefas
- Empresas: cadastrar nova, salvar alterações, desativar, reativar e excluir.
- Serviços: gerenciar departamentos e unidades de cobrança.
- Conciliação: importar extrato por janela, situação do dia e ver período.
- Fechamento: importar o faturado do mês com troca ou soma, ver este mês e gerar a leva.
- Tarefas: demanda com vários tickets, e os nomes das janelas da página.

### Tags, regras, análises, pacotes, agente e as fichas
- Tags: editar tag existente, com o código que nunca muda.
- Regras: as três seções da página, com os nomes dela.
- Análises, pacotes, agente, pacote, serviço e acordo: nomes e colunas iguais aos da página.

## O que sobrou, e por quê

São 47 apontamentos, nenhum deles função que falte:

- **33 são o próprio texto do filtro de coluna** (o comparador lê o conteúdo do
  funil como se fosse nome de coluna). O filtro está feito.
- **9 são nome de bloco** (o "olho" acima do título de uma janela), que na janela
  aparece como título da janela.
- **3 são coluna de ação** ("O que fazer", "Situação"), que na janela virou clique
  na linha ou botão no topo.
- **2 são botão de busca** em telas onde a lista já filtra na hora que a pessoa escolhe.

## Como conferir

O sistema monta e abre sem erro: `mvn -DskipTests package` e depois o
`ABRIR IT.FC.bat`. A versão web de referência fica em http://localhost:8090.
