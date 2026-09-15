-- O pagador (a pessoa, pelo CPF) e as unidades dela.
-- Uma pessoa paga por uma ou mais unidades: CNPJ, loja, filial, contrato.
-- A divida nasce na unidade. A cobranca vai para a pessoa.

create table pagador (
    id         uuid primary key,
    empresa_id uuid        not null references empresa (id),
    nome       text        not null,
    cpf        text,
    whatsapp   text,
    telefone   text,
    email      text,
    observacao text,
    ativo      boolean     not null default true,
    criado_em  timestamptz not null default now(),
    criado_por text,
    constraint uq_pagador_cpf_por_empresa unique (empresa_id, cpf)
);

comment on table pagador is
    'A pessoa que paga. Uma pessoa pode responder por varias unidades.';

create index idx_pagador_empresa on pagador (empresa_id);

alter table cliente_espelho add column pagador_id uuid references pagador (id);
alter table cliente_espelho add column codigo_externo text;

create index idx_cliente_pagador on cliente_espelho (pagador_id);
