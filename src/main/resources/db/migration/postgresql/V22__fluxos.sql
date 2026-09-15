-- Estudio de fluxos: automacao montada pelo usuario, sem programar.
--
-- Um fluxo e sempre a mesma frase: QUANDO acontecer tal coisa, SE tais
-- condicoes valerem, FACA tais acoes. Nada de caixa preta: cada rodada grava
-- o que fez e em quem, para alguem poder conferir depois.
--
-- Quatro regras que o desenho protege:
--   1. Fluxo nao roda escondido. Toda acao vira uma linha de execucao.
--   2. Fluxo desligado nao roda, e desligar nao apaga o historico.
--   3. O mesmo fluxo nao age duas vezes no mesmo cliente no mesmo dia.
--   4. Acao que manda mensagem passa pelas mesmas travas da cobranca: quem
--      pediu para nao ser cobrado continua sem receber.

create table fluxo (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    nome       varchar(120) not null,
    descricao  varchar(300),
    -- VENCE_EM_DIAS, VENCEU_HOJE, ATRASO_DE_DIAS, CLIENTE_RESPONDEU,
    -- PAGAMENTO_ENTROU ou ACORDO_QUEBRADO
    gatilho    varchar(30)  not null,
    dias       integer      not null default 0,
    ativo      boolean      not null default true,
    ultima_rodada date,
    criado_em  timestamp with time zone not null default now(),
    criado_por varchar(120)
);

create index idx_fluxo_empresa on fluxo (empresa_id, ativo);

create table passo_do_fluxo (
    id        uuid        primary key,
    fluxo_id  uuid        not null references fluxo (id),
    -- CONDICAO ou ACAO
    tipo      varchar(10) not null,
    ordem     integer     not null default 1,
    -- para CONDICAO: VALOR_EM_ABERTO, DIAS_ATRASO, SITUACAO_DO_CASO ou TOM_DO_CLIENTE
    campo     varchar(30),
    -- MAIOR, MENOR, IGUAL ou DIFERENTE
    operador  varchar(10),
    valor     varchar(120),
    -- para ACAO: MANDAR_MENSAGEM, MARCAR_CASO, ANOTAR_PROXIMA_ACAO ou REGISTRAR_OBSERVACAO
    acao      varchar(30),
    modelo_id uuid references modelo_mensagem (id),
    texto     varchar(500),
    dias      integer not null default 0
);

create index idx_passo_fluxo on passo_do_fluxo (fluxo_id, ordem);

create table execucao_do_fluxo (
    id         uuid    primary key,
    empresa_id uuid    not null references empresa (id),
    fluxo_id   uuid    not null references fluxo (id),
    unidade_id uuid references cliente_espelho (id),
    titulo_id  uuid references titulo (id),
    -- FEZ ou NAO_FEZ
    resultado  varchar(10)  not null,
    detalhe    varchar(500),
    ocorrido_em timestamp with time zone not null default now()
);

create index idx_execucao_fluxo on execucao_do_fluxo (empresa_id, fluxo_id, ocorrido_em);
