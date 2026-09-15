-- Conferencia de comprovantes: a fila de tudo que chegou como prova de pagamento.
--
-- Quatro regras que o desenho protege:
--   1. So da baixa se o dinheiro caiu na conta certa. O destino lido e
--      comparado com a chave PIX da empresa, e nao com o nome digitado.
--   2. O mesmo identificador de transacao nao entra duas vezes.
--   3. Comprovante recusado NAO some: fica na fila com o motivo, porque o
--      cliente vai perguntar.
--   4. O arquivo fica guardado junto da baixa, sempre.

create table comprovante (
    id                 uuid        primary key,
    empresa_id         uuid        not null references empresa (id),
    pagador_id         uuid references pagador (id),
    unidade_id         uuid references cliente_espelho (id),
    documento_id       uuid references documento (id),
    -- De onde veio: DIGITADO, WHATSAPP ou IMPORTADO
    origem             varchar(20) not null default 'DIGITADO',
    texto              varchar(4000),
    valor_lido         numeric(14, 2),
    data_lida          date,
    destino_lido       varchar(200),
    identificador_lido varchar(200),
    -- NA_FILA, CONFERIDO ou RECUSADO
    situacao           varchar(20) not null default 'NA_FILA',
    titulo_id          uuid references titulo (id),
    motivo             varchar(500),
    conferido_em       timestamp with time zone,
    conferido_por      varchar(120),
    criado_em          timestamp with time zone not null default now(),
    criado_por         varchar(120)
);

create index idx_comprovante_empresa on comprovante (empresa_id, situacao);
