-- Pacotes: a oferta que reune servicos ja cadastrados.
--
-- Tres regras que o desenho protege:
--   1. O pacote NAO cadastra servico de novo. Ele aponta para o catalogo.
--   2. Limite sem numero quer dizer ilimitado, e nao zero.
--   3. Contratacao guarda o valor acordado com aquele cliente, que pode ser
--      diferente do valor do pacote. Mudar o pacote nao mexe no que ja foi
--      acordado antes.

create table pacote (
    id                  uuid primary key,
    empresa_id          uuid         not null references empresa (id),
    codigo              varchar(20)  not null,
    nome                varchar(200) not null,
    descricao           varchar(2000),
    valor               numeric(14, 2),
    periodicidade       varchar(30)  not null default 'MENSAL',
    periodicidade_outra varchar(100),
    ativo               boolean      not null default true,
    criado_em           timestamp with time zone not null default now(),
    criado_por          varchar(120),
    atualizado_em       timestamp with time zone not null default now(),
    constraint uq_pacote_codigo unique (empresa_id, codigo)
);

create index idx_pacote_empresa on pacote (empresa_id);

create table pacote_composicao (
    id                   uuid           primary key,
    empresa_id           uuid           not null references empresa (id),
    pacote_id            uuid           not null references pacote (id),
    servico_id           uuid           not null references servico (id),
    abrangencia          varchar(30)    not null default 'SERVICO_COMPLETO',
    item_id              uuid references servico_item (id),
    ilimitado            boolean        not null default false,
    quantidade_incluida  numeric(10, 2),
    periodo_limite       varchar(30)    not null default 'POR_PERIODO_DO_PACOTE',
    tratamento_excedente varchar(30)    not null default 'PRECO_DO_CATALOGO',
    observacao           varchar(500),
    ordem                int            not null default 1,
    criado_em            timestamp with time zone not null default now()
);

create index idx_composicao_pacote on pacote_composicao (pacote_id, ordem);

create table pacote_contratacao (
    id             uuid        primary key,
    empresa_id     uuid        not null references empresa (id),
    pacote_id      uuid        not null references pacote (id),
    pagador_id     uuid        not null references pagador (id),
    unidade_id     uuid references cliente_espelho (id),
    valor_acordado numeric(14, 2),
    inicio         date,
    fim            date,
    situacao       varchar(20) not null default 'ATIVA',
    observacao     varchar(1000),
    criado_em      timestamp with time zone not null default now(),
    criado_por     varchar(120)
);

create index idx_contratacao_pacote on pacote_contratacao (pacote_id);
create index idx_contratacao_pagador on pacote_contratacao (pagador_id);
