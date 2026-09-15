-- Contas a pagar, separado por empresa.
--
-- O principio que o desenho protege: todo total precisa ser explicavel.
--   visao geral -> composicao do valor -> lancamento -> documentos e historico
--
-- Por isso as dimensoes ficam separadas, cada uma respondendo uma pergunta:
--   entidade responsavel  de quem e a obrigacao
--   favorecido            quem vai receber
--   natureza              o que esta sendo pago
--   centro de custo       qual area absorve o custo
--   tipo de operacao      como a obrigacao surgiu
--   pessoa relacionada    a quem o gasto se refere
--   cliente ou projeto    para qual trabalho
--   bem                   qual equipamento
--   conta financeira      de onde saiu o dinheiro
--   origem                de onde veio a informacao
--
-- Reembolso e tipo de operacao, e NAO natureza: a fonte comprada pelo Lucas
-- aparece como reembolso ao Lucas e como manutencao de equipamento do Fiscal,
-- ao mesmo tempo, sem cadastrar o gasto duas vezes.

-- ------------------------------------------------------- plano gerencial
create table natureza (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    codigo     varchar(20)  not null,
    nome       varchar(200) not null,
    pai_id     uuid references natureza (id),
    -- DESPESA, AQUISICAO, FINANCIAMENTO, TRANSFERENCIA ou RECEITA: separa o
    -- que e custo do que e compra de bem, emprestimo ou troco entre contas.
    grupo      varchar(30)  not null default 'DESPESA',
    ativo      boolean      not null default true,
    criado_em  timestamp with time zone not null default now(),
    constraint uq_natureza_codigo unique (empresa_id, codigo)
);

create index idx_natureza_empresa on natureza (empresa_id);

create table centro_de_custo (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    nome       varchar(200) not null,
    descricao  varchar(500),
    ativo      boolean      not null default true,
    criado_em  timestamp with time zone not null default now(),
    constraint uq_centro_nome unique (empresa_id, nome)
);

-- ------------------------------------------------------------ favorecido
create table favorecido (
    id         uuid primary key,
    empresa_id uuid         not null references empresa (id),
    nome       varchar(300) not null,
    documento  varchar(20),
    tipo       varchar(20)  not null default 'EMPRESA',
    chave_pix  varchar(200),
    banco      varchar(120),
    agencia    varchar(20),
    conta      varchar(30),
    observacao varchar(500),
    ativo      boolean      not null default true,
    criado_em  timestamp with time zone not null default now()
);

create index idx_favorecido_empresa on favorecido (empresa_id, nome);

-- ----------------------------------------------------- conta financeira
create table conta_financeira (
    id            uuid primary key,
    empresa_id    uuid         not null references empresa (id),
    nome          varchar(200) not null,
    tipo          varchar(30)  not null default 'CORRENTE',
    banco         varchar(120),
    agencia       varchar(20),
    numero        varchar(30),
    titular       varchar(300),
    saldo_inicial numeric(14, 2) not null default 0,
    ativo         boolean      not null default true,
    criado_em     timestamp with time zone not null default now()
);

create index idx_conta_empresa on conta_financeira (empresa_id);

-- ------------------------------------------------------------ patrimonio
create table bem (
    id                  uuid primary key,
    empresa_id          uuid         not null references empresa (id),
    numero_patrimonial  varchar(40)  not null,
    descricao           varchar(300) not null,
    tipo                varchar(60),
    marca               varchar(120),
    modelo              varchar(120),
    numero_serie        varchar(120),
    centro_custo_id     uuid references centro_de_custo (id),
    responsavel         varchar(200),
    localizacao         varchar(200),
    situacao            varchar(30)  not null default 'EM_USO',
    aquisicao_em        date,
    valor_aquisicao     numeric(14, 2),
    observacao          varchar(1000),
    ativo               boolean      not null default true,
    criado_em           timestamp with time zone not null default now(),
    constraint uq_bem_numero unique (empresa_id, numero_patrimonial)
);

-- ------------------------------------------------------------- obrigacao
create table obrigacao (
    id                  uuid primary key,
    empresa_id          uuid           not null references empresa (id),
    -- Quando outra empresa do grupo paga por esta, a relacao fica registrada
    -- em vez de trocar o nome da empresa no lancamento.
    empresa_pagadora_id uuid references empresa (id),
    numero              bigint         not null,
    favorecido_id       uuid           not null references favorecido (id),
    descricao           varchar(400)   not null,
    tipo_operacao       varchar(30)    not null default 'DESPESA',
    emissao             date,
    competencia         date,
    vencimento          date           not null,
    valor               numeric(14, 2) not null default 0,
    pessoa_relacionada  varchar(200),
    pagador_id          uuid references pagador (id),
    bem_id              uuid references bem (id),
    conta_id            uuid references conta_financeira (id),
    -- Os cinco controles andam separados de proposito: aprovado, pago em
    -- parte e ainda nao conciliado e uma situacao possivel, e um campo so
    -- nao consegue dizer isso.
    qualidade           varchar(20)    not null default 'COMPLETO',
    aprovacao           varchar(20)    not null default 'NAO_EXIGIDA',
    execucao            varchar(20)    not null default 'NAO_ENCAMINHADA',
    liquidacao          varchar(20)    not null default 'EM_ABERTO',
    conciliacao         varchar(20)    not null default 'NAO_CONCILIADA',
    origem              varchar(30)    not null default 'MANUAL',
    origem_referencia   varchar(400),
    grupo_parcelas      uuid,
    parcela             int,
    total_parcelas      int,
    recorrencia_id      uuid,
    solicitante         varchar(200),
    observacao          varchar(1000),
    motivo_cancelamento varchar(500),
    cancelada           boolean        not null default false,
    aprovada_por        varchar(120),
    aprovada_em         timestamp with time zone,
    criado_em           timestamp with time zone not null default now(),
    criado_por          varchar(120),
    constraint uq_obrigacao_numero unique (empresa_id, numero)
);

create index idx_obrigacao_vencimento on obrigacao (empresa_id, vencimento);
create index idx_obrigacao_favorecido on obrigacao (favorecido_id);
create index idx_obrigacao_grupo on obrigacao (grupo_parcelas);

-- A composicao: e daqui que sai a explicacao de qualquer total.
create table obrigacao_item (
    id              uuid           primary key,
    empresa_id      uuid           not null references empresa (id),
    obrigacao_id    uuid           not null references obrigacao (id),
    descricao       varchar(300)   not null,
    quantidade      numeric(10, 2) not null default 1,
    valor           numeric(14, 2) not null default 0,
    natureza_id     uuid references natureza (id),
    centro_custo_id uuid references centro_de_custo (id),
    pessoa          varchar(200),
    bem_id          uuid references bem (id),
    -- Quando o valor veio de rateio, o criterio fica escrito aqui.
    criterio_rateio varchar(300),
    percentual      numeric(7, 4),
    ordem           int            not null default 1
);

create index idx_obrigacao_item on obrigacao_item (obrigacao_id, ordem);

-- O pagamento e um registro separado da obrigacao: uma conta pode ser paga
-- em varias vezes, e um estorno nao apaga a obrigacao.
create table pagamento_obrigacao (
    id           uuid           primary key,
    empresa_id   uuid           not null references empresa (id),
    obrigacao_id uuid           not null references obrigacao (id),
    pago_em      date           not null,
    valor        numeric(14, 2) not null,
    juros        numeric(14, 2) not null default 0,
    multa        numeric(14, 2) not null default 0,
    desconto     numeric(14, 2) not null default 0,
    retencao     numeric(14, 2) not null default 0,
    conta_id     uuid references conta_financeira (id),
    forma        varchar(40),
    documento_id uuid references documento (id),
    observacao   varchar(500),
    estornado    boolean        not null default false,
    motivo_estorno varchar(500),
    criado_em    timestamp with time zone not null default now(),
    criado_por   varchar(120)
);

create index idx_pagamento_obrigacao on pagamento_obrigacao (obrigacao_id);

-- Recorrencia: o molde que gera obrigacoes periodicas.
create table recorrencia (
    id              uuid primary key,
    empresa_id      uuid         not null references empresa (id),
    descricao       varchar(300) not null,
    favorecido_id   uuid         not null references favorecido (id),
    natureza_id     uuid references natureza (id),
    centro_custo_id uuid references centro_de_custo (id),
    tipo_operacao   varchar(30)  not null default 'DESPESA',
    periodicidade   varchar(30)  not null default 'MENSAL',
    dia_vencimento  int          not null default 10,
    valor_previsto  numeric(14, 2),
    -- Valor variavel nasce como previsao e pede conferencia antes de virar
    -- obrigacao de verdade.
    valor_variavel  boolean      not null default false,
    inicio          date,
    fim             date,
    ativa           boolean      not null default true,
    criado_em       timestamp with time zone not null default now(),
    criado_por      varchar(120)
);

create index idx_recorrencia_empresa on recorrencia (empresa_id);

-- O comprovante e o documento de origem usam o mesmo lugar dos outros anexos.
alter table documento add column obrigacao_id uuid references obrigacao (id);
