-- O perfil completo do cliente, no nivel que cada informacao pertence.
--
-- Pessoa (CPF): quem responde, contratos, risco, documentos, consentimentos.
-- Unidade (CNPJ): dados fiscais, enderecos, preferencia de cobranca.
--
-- Todo campo que pode vir de fora carrega a FONTE e a data em que chegou.
-- Assim da para saber se o dado foi digitado aqui, veio da Receita pela CNPJa,
-- veio da Conexa ou veio direto do banco, e nunca se sobrescreve dado bom por
-- dado velho sem alguem ver.

-- ------------------------------------------------------------------ pessoa
alter table pagador add column data_nascimento date;
alter table pagador add column nome_social text;
alter table pagador add column cliente_desde date;
alter table pagador add column situacao text default 'ATIVO';

-- ----------------------------------------------------------------- unidade
alter table cliente_espelho add column nome_fantasia text;
alter table cliente_espelho add column inscricao_estadual text;
alter table cliente_espelho add column inscricao_municipal text;
alter table cliente_espelho add column regime_tributario text;
alter table cliente_espelho add column porte text;
alter table cliente_espelho add column cnae_principal text;
alter table cliente_espelho add column cnae_descricao text;
alter table cliente_espelho add column data_abertura date;
alter table cliente_espelho add column situacao_cadastral text;
alter table cliente_espelho add column optante_simples boolean;
alter table cliente_espelho add column retencoes text;
alter table cliente_espelho add column fonte text default 'MANUAL';
alter table cliente_espelho add column sincronizado_de_fora_em timestamptz;

-- ---------------------------------------------------------------- endereco
create table endereco (
    id          uuid primary key,
    empresa_id  uuid        not null references empresa (id),
    unidade_id  uuid        not null references cliente_espelho (id),
    tipo        text not null default 'PRINCIPAL',
    cep         text,
    logradouro  text,
    numero      text,
    complemento text,
    bairro      text,
    cidade      text,
    uf          text,
    pais        text default 'Brasil',
    fonte       text not null default 'MANUAL',
    criado_em   timestamptz not null default now()
);

create index idx_endereco_unidade on endereco (unidade_id);

-- ----------------------------------------------------------------- contato
create table contato (
    id              uuid primary key,
    empresa_id      uuid         not null references empresa (id),
    pagador_id      uuid references pagador (id),
    unidade_id      uuid references cliente_espelho (id),
    nome            text not null,
    telefone        text,
    whatsapp        text,
    email           text,
    prioridade      int          not null default 1,
    aceita_cobranca boolean      not null default true,
    melhor_horario  text,
    observacao      text,
    ativo           boolean      not null default true,
    criado_em       timestamptz not null default now()
);

create index idx_contato_pagador on contato (pagador_id);
create index idx_contato_unidade on contato (unidade_id);

-- -------------------------------------------------------------------- tags
-- O papel de cada pessoa vira etiqueta, nao campo fixo. Cada etiqueta tem um
-- codigo estavel, que e o que permite espelhar a mesma etiqueta em outro banco
-- do grupo sem depender do texto escrito na tela.
create table etiqueta (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    codigo     text  not null,
    nome       text not null,
    descricao  text,
    escopo     text  not null default 'CONTATO',
    cor        text,
    ativo      boolean      not null default true,
    criado_em  timestamptz not null default now(),
    constraint uq_etiqueta_codigo unique (empresa_id, codigo)
);

create table etiqueta_vinculo (
    id          uuid primary key,
    empresa_id  uuid        not null references empresa (id),
    etiqueta_id uuid        not null references etiqueta (id),
    entidade    text not null,
    entidade_id uuid        not null,
    criado_em   timestamptz not null default now(),
    criado_por  text,
    constraint uq_etiqueta_vinculo unique (etiqueta_id, entidade, entidade_id)
);

create index idx_vinculo_entidade on etiqueta_vinculo (entidade, entidade_id);

-- ------------------------------------------------------ cobranca da unidade
create table preferencia_cobranca (
    unidade_id                uuid primary key references cliente_espelho (id),
    empresa_id                uuid         not null references empresa (id),
    forma_preferida           text,
    chave_pix                 text,
    banco                     text,
    agencia                   text,
    conta                     text,
    titular                   text,
    dia_vencimento            int,
    periodicidade             text,
    email_cobranca            text,
    aceita_debito_recorrente  boolean      not null default false,
    juros_ao_mes              numeric(6, 3),
    multa_percentual          numeric(6, 3),
    desconto_antecipacao      numeric(6, 3),
    dias_carencia             int,
    protestar_apos_dias       int,
    instrucoes                text,
    fonte                     text  not null default 'MANUAL',
    sincronizado_de_fora_em   timestamptz,
    atualizado_em             timestamptz not null default now()
);

-- --------------------------------------------------------------- contratos
-- O contrato mora na PESSOA, e pode cobrir uma unidade especifica ou todas.
create table contrato (
    id                    uuid primary key,
    empresa_id            uuid         not null references empresa (id),
    pagador_id            uuid         not null references pagador (id),
    unidade_id            uuid references cliente_espelho (id),
    numero                text  not null,
    descricao             text,
    inicio                date,
    fim                   date,
    valor                 numeric(14, 2),
    indice_reajuste       text,
    mes_reajuste          int,
    dia_vencimento        int,
    periodicidade         text,
    responsavel_comercial text,
    situacao              text  not null default 'ATIVO',
    observacao            text,
    fonte                 text  not null default 'MANUAL',
    criado_em             timestamptz not null default now(),
    criado_por            text,
    constraint uq_contrato_numero unique (empresa_id, numero)
);

create index idx_contrato_pagador on contrato (pagador_id);

create table contrato_servico (
    id          uuid primary key,
    contrato_id uuid           not null references contrato (id),
    servico_id  uuid references servico (id),
    descricao   text,
    quantidade  numeric(10, 2) not null default 1,
    valor       numeric(14, 2)
);

create index idx_contrato_servico on contrato_servico (contrato_id);

-- ---------------------------------------------------------- risco e credito
create table analise_credito (
    pagador_id       uuid primary key references pagador (id),
    empresa_id       uuid        not null references empresa (id),
    classificacao    text,
    limite_credito   numeric(14, 2),
    bloqueado        boolean     not null default false,
    motivo_bloqueio  text,
    ultima_analise_em timestamptz,
    analisado_por    text,
    observacao       text
);

create table restricao (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    pagador_id uuid         not null references pagador (id),
    tipo       text  not null,
    origem     text,
    valor      numeric(14, 2),
    data       date,
    situacao   text  not null default 'ATIVA',
    observacao text,
    criado_em  timestamptz not null default now()
);

create index idx_restricao_pagador on restricao (pagador_id);

-- --------------------------------------------------------- linha do tempo
create table interacao (
    id             uuid primary key,
    empresa_id     uuid         not null references empresa (id),
    pagador_id     uuid references pagador (id),
    unidade_id     uuid references cliente_espelho (id),
    tipo           text  not null,
    descricao      text,
    valor          numeric(14, 2),
    data_prometida date,
    situacao       text,
    canal          text,
    autor          text,
    ocorrido_em    timestamptz not null default now()
);

create index idx_interacao_pagador on interacao (pagador_id, ocorrido_em);

-- ------------------------------------------------------------- documentos
create table documento (
    id           uuid primary key,
    empresa_id   uuid         not null references empresa (id),
    pagador_id   uuid references pagador (id),
    unidade_id   uuid references cliente_espelho (id),
    contrato_id  uuid references contrato (id),
    tipo         text  not null,
    nome_arquivo text not null,
    caminho      text not null,
    tamanho      bigint,
    validade     date,
    observacao   text,
    anexado_por  text,
    anexado_em   timestamptz not null default now()
);

create index idx_documento_pagador on documento (pagador_id);
create index idx_documento_unidade on documento (unidade_id);

-- ------------------------------------------------------------ conformidade
create table consentimento (
    id          uuid primary key,
    empresa_id  uuid        not null references empresa (id),
    pagador_id  uuid        not null references pagador (id),
    contato_id  uuid references contato (id),
    canal       text not null,
    base_legal  text not null,
    situacao    text not null default 'ACEITO',
    origem      text,
    observacao  text,
    registrado_em timestamptz not null default now(),
    registrado_por text
);

create index idx_consentimento_pagador on consentimento (pagador_id);
