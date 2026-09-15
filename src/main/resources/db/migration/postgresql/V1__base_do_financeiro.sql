-- Banco proprio do modulo Financeiro. Comeca ZERADO.
--
-- Tres regras que valem para o arquivo inteiro:
--   1. Este banco e dono do dinheiro (titulo, pagamento, baixa).
--      O ERP continua dono do cadastro (cliente, usuario, WhatsApp).
--   2. TUDO tem empresa_id. Uma pessoa pode cuidar do financeiro de varias
--      empresas, mas nunca ve as duas misturadas.
--   3. Nada aqui escreve na Conexa.

create table empresa (
    id            uuid primary key,
    apelido       text        not null unique,   -- 'you', '40', 'realizze'
    nome          text        not null,
    cnpj          text,
    chave_pix     text,
    erp_setor_id  uuid,                          -- com quem ela conversa no ERP
    ativa         boolean     not null default true,
    criada_em     timestamptz not null default now()
);

comment on table empresa is
    'Cada empresa atendida. Tudo neste banco pendura numa empresa.';

create table usuario (
    id            uuid primary key,
    nome          text        not null,
    email         text        not null unique,
    erp_usuario_id uuid unique,                  -- mesma pessoa la no ERP
    ativo         boolean     not null default true,
    criado_em     timestamptz not null default now()
);

create table usuario_empresa (
    usuario_id    uuid not null references usuario (id) on delete cascade,
    empresa_id    uuid not null references empresa (id) on delete cascade,
    papel         text not null default 'OPERADOR'
                  check (papel in ('OPERADOR', 'GESTOR', 'DIRETOR')),
    primary key (usuario_id, empresa_id)
);

comment on table usuario_empresa is
    'Quem cuida de qual empresa, e com que alcada. Sem linha aqui, nao ve nada.';

create table cliente_espelho (
    id                uuid primary key,
    empresa_id        uuid        not null references empresa (id),
    erp_cliente_id    uuid,
    razao_social      text        not null,
    cnpj_cpf          text,
    responsavel       text,
    telefone          text,
    email             text,
    ativo             boolean     not null default true,
    sincronizado_em   timestamptz not null default now(),
    constraint uq_cliente_por_empresa unique (empresa_id, erp_cliente_id)
);

comment on table cliente_espelho is
    'Copia so-leitura do cliente que vive no ERP. Nunca editar por aqui.';

create index idx_cliente_empresa on cliente_espelho (empresa_id);
create index idx_cliente_cnpj on cliente_espelho (empresa_id, cnpj_cpf);

create table titulo (
    id                  uuid primary key,
    empresa_id          uuid           not null references empresa (id),
    cliente_id          uuid           not null references cliente_espelho (id),
    numero              bigint         not null,
    competencia         date           not null,
    descricao           text           not null,
    valor               numeric(14, 2) not null check (valor > 0),
    vencimento          date           not null,
    situacao            text           not null default 'ABERTO'
                        check (situacao in ('ABERTO', 'PARCIAL', 'PAGO', 'CANCELADO')),
    origem              text           not null default 'MANUAL',
    identificador_pix   text,
    observacao          text,
    criado_em           timestamptz    not null default now(),
    criado_por          text,
    atualizado_em       timestamptz    not null default now(),
    cancelado_em        timestamptz,
    cancelado_por       text,
    motivo_cancelamento text,
    constraint uq_titulo_numero unique (empresa_id, numero),
    constraint uq_titulo_pix unique (empresa_id, identificador_pix)
);

create index idx_titulo_cliente on titulo (cliente_id);
create index idx_titulo_a_vencer on titulo (empresa_id, vencimento)
    where situacao in ('ABERTO', 'PARCIAL');

create table pagamento (
    id            uuid primary key,
    empresa_id    uuid           not null references empresa (id),
    titulo_id     uuid           not null references titulo (id),
    valor         numeric(14, 2) not null check (valor > 0),
    pago_em       date           not null,
    forma         text           not null default 'PIX',
    transacao_id  text,
    conferido_por text,
    observacao    text,
    criado_em     timestamptz    not null default now()
);

create index idx_pagamento_titulo on pagamento (titulo_id);

-- O mesmo PIX nunca entra duas vezes na mesma empresa.
create unique index uq_pagamento_transacao on pagamento (empresa_id, transacao_id)
    where transacao_id is not null;

create table evento (
    id           bigserial primary key,
    empresa_id   uuid        not null references empresa (id),
    entidade     text        not null,
    entidade_id  uuid        not null,
    acao         text        not null,
    autor        text,
    detalhe      jsonb,
    ocorrido_em  timestamptz not null default now()
);

comment on table evento is
    'Trilha de auditoria. Toda mudanca de dinheiro grava uma linha aqui.';

create index idx_evento_entidade on evento (empresa_id, entidade, entidade_id);

create table fila_espelho_erp (
    id            bigserial primary key,
    empresa_id    uuid        not null references empresa (id),
    tipo          text        not null,
    referencia_id uuid        not null,
    carga         jsonb       not null,
    tentativas    int         not null default 0,
    enviado_em    timestamptz,
    erro          text,
    criado_em     timestamptz not null default now()
);

comment on table fila_espelho_erp is
    'O que este sistema precisa refletir de volta no ERP. Fila, para nunca perder envio.';

create index idx_fila_pendente on fila_espelho_erp (criado_em) where enviado_em is null;
