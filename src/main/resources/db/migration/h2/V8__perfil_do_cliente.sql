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
alter table pagador add column nome_social varchar(200);
alter table pagador add column cliente_desde date;
alter table pagador add column situacao varchar(20) default 'ATIVO';

-- ----------------------------------------------------------------- unidade
alter table cliente_espelho add column nome_fantasia varchar(300);
alter table cliente_espelho add column inscricao_estadual varchar(40);
alter table cliente_espelho add column inscricao_municipal varchar(40);
alter table cliente_espelho add column regime_tributario varchar(60);
alter table cliente_espelho add column porte varchar(60);
alter table cliente_espelho add column cnae_principal varchar(20);
alter table cliente_espelho add column cnae_descricao varchar(300);
alter table cliente_espelho add column data_abertura date;
alter table cliente_espelho add column situacao_cadastral varchar(60);
alter table cliente_espelho add column optante_simples boolean;
alter table cliente_espelho add column retencoes varchar(300);
alter table cliente_espelho add column fonte varchar(30) default 'MANUAL';
alter table cliente_espelho add column sincronizado_de_fora_em timestamp with time zone;

-- ---------------------------------------------------------------- endereco
create table endereco (
    id          uuid primary key,
    empresa_id  uuid        not null references empresa (id),
    unidade_id  uuid        not null references cliente_espelho (id),
    tipo        varchar(20) not null default 'PRINCIPAL',
    cep         varchar(12),
    logradouro  varchar(300),
    numero      varchar(20),
    complemento varchar(120),
    bairro      varchar(120),
    cidade      varchar(120),
    uf          varchar(2),
    pais        varchar(60) default 'Brasil',
    fonte       varchar(30) not null default 'MANUAL',
    criado_em   timestamp with time zone not null default now()
);

create index idx_endereco_unidade on endereco (unidade_id);

-- ----------------------------------------------------------------- contato
create table contato (
    id              uuid primary key,
    empresa_id      uuid         not null references empresa (id),
    pagador_id      uuid references pagador (id),
    unidade_id      uuid references cliente_espelho (id),
    nome            varchar(200) not null,
    telefone        varchar(40),
    whatsapp        varchar(40),
    email           varchar(200),
    prioridade      int          not null default 1,
    aceita_cobranca boolean      not null default true,
    melhor_horario  varchar(80),
    observacao      varchar(500),
    ativo           boolean      not null default true,
    criado_em       timestamp with time zone not null default now()
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
    codigo     varchar(60)  not null,
    nome       varchar(120) not null,
    descricao  varchar(500),
    escopo     varchar(20)  not null default 'CONTATO',
    cor        varchar(20),
    ativo      boolean      not null default true,
    criado_em  timestamp with time zone not null default now(),
    constraint uq_etiqueta_codigo unique (empresa_id, codigo)
);

create table etiqueta_vinculo (
    id          uuid primary key,
    empresa_id  uuid        not null references empresa (id),
    etiqueta_id uuid        not null references etiqueta (id),
    entidade    varchar(30) not null,
    entidade_id uuid        not null,
    criado_em   timestamp with time zone not null default now(),
    criado_por  varchar(120),
    constraint uq_etiqueta_vinculo unique (etiqueta_id, entidade, entidade_id)
);

create index idx_vinculo_entidade on etiqueta_vinculo (entidade, entidade_id);

-- ------------------------------------------------------ cobranca da unidade
create table preferencia_cobranca (
    unidade_id                uuid primary key references cliente_espelho (id),
    empresa_id                uuid         not null references empresa (id),
    forma_preferida           varchar(30),
    chave_pix                 varchar(200),
    banco                     varchar(120),
    agencia                   varchar(20),
    conta                     varchar(30),
    titular                   varchar(200),
    dia_vencimento            int,
    periodicidade             varchar(30),
    email_cobranca            varchar(200),
    aceita_debito_recorrente  boolean      not null default false,
    juros_ao_mes              numeric(6, 3),
    multa_percentual          numeric(6, 3),
    desconto_antecipacao      numeric(6, 3),
    dias_carencia             int,
    protestar_apos_dias       int,
    instrucoes                varchar(1000),
    fonte                     varchar(30)  not null default 'MANUAL',
    sincronizado_de_fora_em   timestamp with time zone,
    atualizado_em             timestamp with time zone not null default now()
);

-- --------------------------------------------------------------- contratos
-- O contrato mora na PESSOA, e pode cobrir uma unidade especifica ou todas.
create table contrato (
    id                    uuid primary key,
    empresa_id            uuid         not null references empresa (id),
    pagador_id            uuid         not null references pagador (id),
    unidade_id            uuid references cliente_espelho (id),
    numero                varchar(40)  not null,
    descricao             varchar(500),
    inicio                date,
    fim                   date,
    valor                 numeric(14, 2),
    indice_reajuste       varchar(40),
    mes_reajuste          int,
    dia_vencimento        int,
    periodicidade         varchar(30),
    responsavel_comercial varchar(200),
    situacao              varchar(20)  not null default 'ATIVO',
    observacao            varchar(1000),
    fonte                 varchar(30)  not null default 'MANUAL',
    criado_em             timestamp with time zone not null default now(),
    criado_por            varchar(120),
    constraint uq_contrato_numero unique (empresa_id, numero)
);

create index idx_contrato_pagador on contrato (pagador_id);

create table contrato_servico (
    id          uuid primary key,
    contrato_id uuid           not null references contrato (id),
    servico_id  uuid references servico (id),
    descricao   varchar(300),
    quantidade  numeric(10, 2) not null default 1,
    valor       numeric(14, 2)
);

create index idx_contrato_servico on contrato_servico (contrato_id);

-- ---------------------------------------------------------- risco e credito
create table analise_credito (
    pagador_id       uuid primary key references pagador (id),
    empresa_id       uuid        not null references empresa (id),
    classificacao    varchar(20),
    limite_credito   numeric(14, 2),
    bloqueado        boolean     not null default false,
    motivo_bloqueio  varchar(500),
    ultima_analise_em timestamp with time zone,
    analisado_por    varchar(120),
    observacao       varchar(1000)
);

create table restricao (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    pagador_id uuid         not null references pagador (id),
    tipo       varchar(30)  not null,
    origem     varchar(200),
    valor      numeric(14, 2),
    data       date,
    situacao   varchar(30)  not null default 'ATIVA',
    observacao varchar(500),
    criado_em  timestamp with time zone not null default now()
);

create index idx_restricao_pagador on restricao (pagador_id);

-- --------------------------------------------------------- linha do tempo
create table interacao (
    id             uuid primary key,
    empresa_id     uuid         not null references empresa (id),
    pagador_id     uuid references pagador (id),
    unidade_id     uuid references cliente_espelho (id),
    tipo           varchar(40)  not null,
    descricao      varchar(2000),
    valor          numeric(14, 2),
    data_prometida date,
    situacao       varchar(30),
    canal          varchar(30),
    autor          varchar(120),
    ocorrido_em    timestamp with time zone not null default now()
);

create index idx_interacao_pagador on interacao (pagador_id, ocorrido_em);

-- ------------------------------------------------------------- documentos
create table documento (
    id           uuid primary key,
    empresa_id   uuid         not null references empresa (id),
    pagador_id   uuid references pagador (id),
    unidade_id   uuid references cliente_espelho (id),
    contrato_id  uuid references contrato (id),
    tipo         varchar(60)  not null,
    nome_arquivo varchar(300) not null,
    caminho      varchar(500) not null,
    tamanho      bigint,
    validade     date,
    observacao   varchar(500),
    anexado_por  varchar(120),
    anexado_em   timestamp with time zone not null default now()
);

create index idx_documento_pagador on documento (pagador_id);
create index idx_documento_unidade on documento (unidade_id);

-- ------------------------------------------------------------ conformidade
create table consentimento (
    id          uuid primary key,
    empresa_id  uuid        not null references empresa (id),
    pagador_id  uuid        not null references pagador (id),
    contato_id  uuid references contato (id),
    canal       varchar(30) not null,
    base_legal  varchar(60) not null,
    situacao    varchar(20) not null default 'ACEITO',
    origem      varchar(200),
    observacao  varchar(500),
    registrado_em timestamp with time zone not null default now(),
    registrado_por varchar(120)
);

create index idx_consentimento_pagador on consentimento (pagador_id);
