-- Fechamento do mes: a leva de cobrancas de um periodo.
--
-- Duas regras que o desenho protege:
--   1. Competencia fechada nao gera nem recalcula nada. Quem precisa mexer
--      reabre, com nome e data registrados.
--   2. O valor faturado que vem de fora fica guardado por competencia, com a
--      origem do arquivo, para o total da leva poder ser explicado depois.

create table competencia (
    id           uuid        primary key,
    empresa_id   uuid        not null references empresa (id),
    referencia   varchar(7)  not null,
    situacao     varchar(20) not null default 'ABERTA',
    fechada_em   timestamp with time zone,
    fechada_por  varchar(120),
    reaberta_em  timestamp with time zone,
    reaberta_por varchar(120),
    motivo       varchar(500),
    criado_em    timestamp with time zone not null default now(),
    constraint uq_competencia unique (empresa_id, referencia)
);

create table faturado_do_periodo (
    id           uuid           primary key,
    empresa_id   uuid           not null references empresa (id),
    referencia   varchar(7)     not null,
    documento    varchar(20)    not null,
    pagador_id   uuid references pagador (id),
    unidade_id   uuid references cliente_espelho (id),
    valor        numeric(14, 2) not null,
    observacao   varchar(300),
    origem       varchar(200),
    importado_em timestamp with time zone not null default now(),
    importado_por varchar(120)
);

create index idx_faturado_periodo on faturado_do_periodo (empresa_id, referencia);
