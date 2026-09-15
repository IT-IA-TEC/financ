-- O pagador (a pessoa, pelo CPF) e as unidades dela.
-- Uma pessoa paga por uma ou mais unidades: CNPJ, loja, filial, contrato.
-- A divida nasce na unidade. A cobranca vai para a pessoa.

create table pagador (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    nome       varchar(200) not null,
    cpf        varchar(20),
    whatsapp   varchar(40),
    telefone   varchar(40),
    email      varchar(200),
    observacao varchar(1000),
    ativo      boolean      not null default true,
    criado_em  timestamp with time zone not null default now(),
    criado_por varchar(120),
    constraint uq_pagador_cpf_por_empresa unique (empresa_id, cpf)
);

create index idx_pagador_empresa on pagador (empresa_id);

alter table cliente_espelho add column pagador_id uuid references pagador (id);
alter table cliente_espelho add column codigo_externo varchar(60);

create index idx_cliente_pagador on cliente_espelho (pagador_id);
