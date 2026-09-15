-- A regua de cadencia: quando falar, o que falar, e por qual canal.
--
-- Cada passo e uma regra simples: tantos dias antes ou depois do vencimento,
-- manda tal texto. O sistema roda a regua uma vez por dia e monta o lote.
--
-- Tres regras que o desenho protege:
--   1. Passo nenhum inventa data. O gatilho e sempre relativo ao vencimento
--      do documento, e roda uma vez so naquele dia exato.
--   2. Passo pode exigir confirmacao. Empresa que quer olhar antes de mandar
--      deixa marcado, e o lote do dia nasce como previa.
--   3. Quem ja recebeu mensagem do mesmo documento hoje nao recebe de novo,
--      nem que dois passos caiam no mesmo dia.

create table passo_da_regua (
    id                 uuid         primary key,
    empresa_id         uuid         not null references empresa (id),
    nome               varchar(120) not null,
    -- ANTES_DE_VENCER, NO_VENCIMENTO ou DEPOIS_DE_VENCER
    gatilho            varchar(20)  not null default 'DEPOIS_DE_VENCER',
    dias               integer      not null default 0,
    modelo_id          uuid         not null references modelo_mensagem (id),
    canal              varchar(20)  not null default 'WHATSAPP',
    ordem              integer      not null default 1,
    exige_confirmacao  boolean      not null default true,
    ativo              boolean      not null default true,
    ultima_rodada      date,
    criado_em          timestamp with time zone not null default now(),
    criado_por         varchar(120)
);

create index idx_passo_empresa on passo_da_regua (empresa_id, ativo, ordem);
