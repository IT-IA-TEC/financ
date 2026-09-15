-- Cobrancas de pacotes e de servicos.
--
-- Quatro regras que o desenho protege:
--   1. Toda cobranca aponta para a ORIGEM: a contratacao do pacote ou o
--      servico realizado que a gerou. Cobranca sem origem nao existe.
--   2. Servico incluido no pacote fica vinculado a contratacao e NAO gera
--      cobranca adicional.
--   3. Gratuidade fica registrada com valor final zero, sem virar valor a
--      receber.
--   4. O valor a receber de verdade e o titulo. A cobranca aprovada gera o
--      titulo e guarda o numero dele; o pagamento e lido de la, nunca digitado
--      duas vezes.

create table cobranca (
    id                 uuid           primary key,
    empresa_id         uuid           not null references empresa (id),
    origem             varchar(20)    not null,
    pagador_id         uuid           not null references pagador (id),
    unidade_id         uuid references cliente_espelho (id),
    contratacao_id     uuid references pacote_contratacao (id),
    realizado_id       uuid references servico_realizado (id),
    descricao          varchar(400)   not null,
    periodo_inicio     date,
    periodo_fim        date,
    referencia         varchar(40),
    valor_original     numeric(14, 2) not null default 0,
    desconto           numeric(14, 2) not null default 0,
    valor_final        numeric(14, 2) not null default 0,
    justificativa      varchar(500),
    aprovada_por       varchar(120),
    aprovada_em        timestamp with time zone,
    vencimento         date,
    situacao           varchar(20)    not null default 'PENDENTE',
    titulo_id          uuid references titulo (id),
    integracao_id      uuid references integracao (id),
    referencia_externa varchar(200),
    enviada_em         timestamp with time zone,
    ultimo_erro        varchar(1000),
    criado_em          timestamp with time zone not null default now(),
    criado_por         varchar(120)
);

create index idx_cobranca_empresa on cobranca (empresa_id, vencimento);
create index idx_cobranca_pagador on cobranca (pagador_id);
create index idx_cobranca_contratacao on cobranca (contratacao_id, referencia);
create index idx_cobranca_realizado on cobranca (realizado_id);

create table cobranca_item (
    id             uuid           primary key,
    empresa_id     uuid           not null references empresa (id),
    cobranca_id    uuid           not null references cobranca (id),
    descricao      varchar(300)   not null,
    quantidade     numeric(10, 2) not null default 1,
    valor_unitario numeric(14, 2),
    item_id        uuid references servico_item (id)
);

create index idx_cobranca_item on cobranca_item (cobranca_id);
