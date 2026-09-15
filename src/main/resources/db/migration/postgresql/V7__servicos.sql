-- Modulo de Servicos: o catalogo do que a empresa faz, com as partes de cada
-- servico e o preco de cada coisa.
--
-- Tres regras que o desenho protege:
--   1. Preco NAO DEFINIDO e diferente de preco ZERO. Por isso valor aceita nulo.
--   2. Toda troca de preco escreve uma linha no historico, com data e autor.
--   3. Servico e item que ja foram usados sao INATIVADOS, nunca apagados.

create table departamento (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    nome       text not null,
    descricao  text,
    ativo      boolean      not null default true,
    criado_em  timestamptz not null default now(),
    constraint uq_departamento_nome unique (empresa_id, nome)
);

create table unidade_cobranca (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    nome       text not null,
    ativo      boolean      not null default true,
    criado_em  timestamptz not null default now(),
    constraint uq_unidade_nome unique (empresa_id, nome)
);

create table servico (
    id                 uuid primary key,
    empresa_id         uuid           not null references empresa (id),
    codigo             text    not null,
    nome               text   not null,
    descricao          text,
    departamento_id    uuid references departamento (id),
    responsavel_padrao text,
    forma_preco        text    not null default 'VALOR_UNICO',
    valor              numeric(14, 2),
    unidade_id         uuid references unidade_cobranca (id),
    vigencia_inicio    date,
    ativo              boolean        not null default true,
    criado_em          timestamptz not null default now(),
    criado_por         text,
    atualizado_em      timestamptz not null default now(),
    constraint uq_servico_codigo unique (empresa_id, codigo)
);

create index idx_servico_empresa on servico (empresa_id);
create index idx_servico_departamento on servico (departamento_id);

create table servico_item (
    id                uuid primary key,
    empresa_id        uuid           not null references empresa (id),
    servico_id        uuid           not null references servico (id),
    nome              text   not null,
    descricao         text,
    obrigatorio       boolean        not null default true,
    quantidade_padrao numeric(10, 2) not null default 1,
    tratamento_preco  text    not null default 'INCLUIDO',
    valor             numeric(14, 2),
    unidade_id        uuid references unidade_cobranca (id),
    ordem             int            not null default 1,
    ativo             boolean        not null default true,
    criado_em         timestamptz not null default now()
);

create index idx_item_servico on servico_item (servico_id, ordem);

create table servico_preco_historico (
    id              bigserial primary key,
    empresa_id      uuid          not null references empresa (id),
    servico_id      uuid          not null references servico (id),
    item_id         uuid references servico_item (id),
    o_que_mudou     text  not null,
    forma_preco     text,
    valor_anterior  numeric(14, 2),
    valor_novo      numeric(14, 2),
    unidade         text,
    vigencia_inicio date,
    justificativa   text,
    quando          timestamptz not null default now(),
    quem            text
);

create index idx_preco_historico on servico_preco_historico (servico_id, quando);
