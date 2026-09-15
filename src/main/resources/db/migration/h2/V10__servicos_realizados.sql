-- Servicos realizados: o registro do que a equipe fez para o cliente.
--
-- Tres regras que o desenho protege:
--   1. O registro guarda o TRATAMENTO ja decidido (incluido no pacote, cobrado
--      a parte, com desconto ou gratuito). "A decidir" nao fica gravado.
--   2. O que conta contra o limite do pacote e so o que foi marcado como
--      incluido no pacote. Por isso a contagem nunca precisa adivinhar.
--   3. Desconto dado num atendimento fica aqui, e nunca mexe no preco do
--      catalogo.

create table servico_realizado (
    id             uuid           primary key,
    empresa_id     uuid           not null references empresa (id),
    pagador_id     uuid           not null references pagador (id),
    unidade_id     uuid references cliente_espelho (id),
    servico_id     uuid           not null references servico (id),
    contratacao_id uuid references pacote_contratacao (id),
    realizado_em   date           not null,
    quantidade     numeric(10, 2) not null default 1,
    responsavel    varchar(200),
    tratamento     varchar(30)    not null default 'COBRADO_A_PARTE',
    valor_cobrado  numeric(14, 2),
    desconto       numeric(14, 2),
    observacao     varchar(1000),
    situacao       varchar(20)    not null default 'REGISTRADO',
    criado_em      timestamp with time zone not null default now(),
    criado_por     varchar(120)
);

create index idx_realizado_empresa on servico_realizado (empresa_id, realizado_em);
create index idx_realizado_pagador on servico_realizado (pagador_id, servico_id);

create table servico_realizado_item (
    id           uuid           primary key,
    empresa_id   uuid           not null references empresa (id),
    realizado_id uuid           not null references servico_realizado (id),
    item_id      uuid           not null references servico_item (id),
    quantidade   numeric(10, 2) not null default 1,
    observacao   varchar(500)
);

create index idx_realizado_item on servico_realizado_item (realizado_id);

-- O comprovante do atendimento usa o mesmo lugar dos outros anexos do cliente.
alter table documento add column realizado_id uuid references servico_realizado (id);
