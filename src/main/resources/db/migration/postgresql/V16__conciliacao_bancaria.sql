-- Conciliacao bancaria: o extrato de um lado, o que o sistema registrou do outro.
--
-- Quatro regras que o desenho protege:
--   1. O mesmo arquivo nao entra duas vezes: cada importacao guarda a impressao
--      digital do conteudo, e a segunda tentativa e recusada.
--   2. O mesmo movimento nao entra duas vezes: identificador do banco e unico
--      por conta.
--   3. Movimento sem dono NAO recebe classificacao chutada: fica pendente, e
--      alguem decide.
--   4. Conciliar nao cria nem apaga dinheiro: so liga o que ja existe dos dois
--      lados. O que nao tem par vira lancamento novo de proposito, com clique.

create table importacao_extrato (
    id          uuid         primary key,
    empresa_id  uuid         not null references empresa (id),
    conta_id    uuid references conta_financeira (id),
    arquivo     varchar(300) not null,
    impressao   varchar(64)  not null,
    linhas      int          not null default 0,
    novos       int          not null default 0,
    repetidos   int          not null default 0,
    quando      timestamp with time zone not null default now(),
    quem        varchar(120),
    constraint uq_extrato_impressao unique (empresa_id, impressao)
);

create table movimento_bancario (
    id             uuid           primary key,
    empresa_id     uuid           not null references empresa (id),
    conta_id       uuid references conta_financeira (id),
    importacao_id  uuid references importacao_extrato (id),
    ocorrido_em    date           not null,
    valor          numeric(14, 2) not null,
    -- CREDITO (entrou) ou DEBITO (saiu)
    tipo           varchar(10)    not null,
    descricao      varchar(400),
    identificador  varchar(200),
    -- PENDENTE, CONCILIADO ou IGNORADO
    situacao       varchar(20)    not null default 'PENDENTE',
    titulo_id      uuid references titulo (id),
    obrigacao_id   uuid references obrigacao (id),
    motivo         varchar(500),
    conciliado_em  timestamp with time zone,
    conciliado_por varchar(120),
    criado_em      timestamp with time zone not null default now()
);

create index idx_movimento_empresa on movimento_bancario (empresa_id, ocorrido_em);
create index idx_movimento_situacao on movimento_bancario (empresa_id, situacao);
create unique index uq_movimento_identificador
    on movimento_bancario (empresa_id, identificador);
