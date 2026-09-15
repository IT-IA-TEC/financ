-- Mensagens de cobranca: modelo, lote e a mensagem em si.
--
-- Esta e a base de tres coisas que vem juntas: o disparo em lote, a regua de
-- cadencia e a conversa de WhatsApp dentro da plataforma. Por isso a mensagem
-- guarda de onde ela nasceu (lote ou regua), para quem foi, por qual canal, e
-- em que pe esta.
--
-- Cinco regras que o desenho protege:
--   1. Nada sai sem alguem confirmar. O lote nasce como PREVIA.
--   2. Quem ficou de fora fica registrado com o motivo, e nao some.
--   3. A mesma cobranca nao vai duas vezes no mesmo dia pelo mesmo canal.
--   4. A mensagem guarda o texto ja montado, e nao so o modelo, porque o
--      modelo muda e a prova do que foi dito nao pode mudar junto.
--   5. Resposta do cliente entra na mesma tabela, com direcao ENTRADA.

create table modelo_mensagem (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    nome       varchar(120) not null,
    -- WHATSAPP ou EMAIL
    canal      varchar(20)  not null default 'WHATSAPP',
    assunto    varchar(200),
    corpo      varchar(4000) not null,
    -- AMIGAVEL, FIRME ou FORMAL
    tom        varchar(20)  not null default 'AMIGAVEL',
    -- ANTES_VENCER, NO_DIA, APOS_VENCER ou LIVRE
    momento    varchar(20)  not null default 'LIVRE',
    ativo      boolean      not null default true,
    criado_em  timestamp with time zone not null default now(),
    criado_por varchar(120)
);

create index idx_modelo_empresa on modelo_mensagem (empresa_id, ativo);

create table lote_mensagem (
    id            uuid         primary key,
    empresa_id    uuid         not null references empresa (id),
    nome          varchar(200) not null,
    modelo_id     uuid references modelo_mensagem (id),
    canal         varchar(20)  not null default 'WHATSAPP',
    -- o filtro escrito em portugues, para quem abrir depois entender o recorte
    filtro        varchar(500),
    -- PREVIA, CONFIRMADO, ENVIANDO, CONCLUIDO ou CANCELADO
    situacao      varchar(20)  not null default 'PREVIA',
    quantidade    integer      not null default 0,
    fora          integer      not null default 0,
    valor_total   numeric(14, 2) not null default 0,
    agendado_para timestamp with time zone,
    confirmado_em timestamp with time zone,
    confirmado_por varchar(120),
    criado_em     timestamp with time zone not null default now(),
    criado_por    varchar(120)
);

create index idx_lote_empresa on lote_mensagem (empresa_id, situacao);

create table mensagem (
    id            uuid        primary key,
    empresa_id    uuid        not null references empresa (id),
    lote_id       uuid references lote_mensagem (id),
    modelo_id     uuid references modelo_mensagem (id),
    pagador_id    uuid references pagador (id),
    unidade_id    uuid references cliente_espelho (id),
    contato_id    uuid references contato (id),
    titulo_id     uuid references titulo (id),
    cobranca_id   uuid references cobranca (id),
    canal         varchar(20) not null default 'WHATSAPP',
    -- SAIDA (nos para o cliente) ou ENTRADA (resposta do cliente)
    direcao       varchar(10) not null default 'SAIDA',
    destino       varchar(200),
    assunto       varchar(200),
    corpo         varchar(4000) not null,
    -- NA_FILA, ENVIADA, ENTREGUE, LIDA, RESPONDIDA, FALHOU ou CANCELADA
    situacao      varchar(20) not null default 'NA_FILA',
    motivo        varchar(500),
    tentativas    integer     not null default 0,
    agendada_para timestamp with time zone,
    enviada_em    timestamp with time zone,
    entregue_em   timestamp with time zone,
    lida_em       timestamp with time zone,
    respondida_em timestamp with time zone,
    id_externo    varchar(200),
    criado_em     timestamp with time zone not null default now(),
    criado_por    varchar(120)
);

create index idx_mensagem_empresa on mensagem (empresa_id, situacao);
create index idx_mensagem_lote on mensagem (lote_id);
create index idx_mensagem_unidade on mensagem (empresa_id, unidade_id, criado_em);
create index idx_mensagem_titulo on mensagem (empresa_id, titulo_id, canal);

-- Quem ficou de fora do lote, com o motivo. Nao some, porque a pergunta
-- "por que fulano nao recebeu" chega sempre.
create table fora_do_lote (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    lote_id    uuid         not null references lote_mensagem (id),
    unidade_id uuid references cliente_espelho (id),
    titulo_id  uuid references titulo (id),
    quem       varchar(200),
    motivo     varchar(300) not null,
    criado_em  timestamp with time zone not null default now()
);

create index idx_fora_lote on fora_do_lote (lote_id);
