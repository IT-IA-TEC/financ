-- Acordo de divida: quando o cliente nao consegue pagar de uma vez.
--
-- O acordo nao apaga o que era devido. Os documentos originais ficam
-- guardados, marcados como EM_ACORDO, e saem da cobranca porque quem passa a
-- ser cobrado sao as parcelas. Se o acordo quebrar, eles voltam.
--
-- Quatro regras que o desenho protege:
--   1. A soma das parcelas fecha com o valor combinado, ate o ultimo centavo.
--   2. Nenhum documento entra em dois acordos ao mesmo tempo.
--   3. Desconto dado fica escrito, com quem autorizou. Desconto sem nome e
--      como dinheiro sumido.
--   4. Acordo quebrado devolve os documentos originais para a cobranca, e as
--      parcelas que ainda nao foram pagas sao canceladas.

create table acordo (
    id               uuid         primary key,
    empresa_id       uuid         not null references empresa (id),
    unidade_id       uuid         not null references cliente_espelho (id),
    numero           integer      not null,
    -- quanto estava devendo antes de qualquer conta
    valor_original   numeric(14, 2) not null,
    -- juros e multa que entraram
    acrescimo        numeric(14, 2) not null default 0,
    desconto         numeric(14, 2) not null default 0,
    -- o que o cliente vai pagar no fim das contas
    valor_combinado  numeric(14, 2) not null,
    entrada          numeric(14, 2) not null default 0,
    parcelas         integer      not null default 1,
    primeiro_vencimento date      not null,
    -- ATIVO, CUMPRIDO, QUEBRADO ou CANCELADO
    situacao         varchar(20)  not null default 'ATIVO',
    motivo_desconto  varchar(300),
    autorizado_por   varchar(120),
    observacao       varchar(1000),
    quebrado_em      timestamp with time zone,
    motivo_quebra    varchar(300),
    criado_em        timestamp with time zone not null default now(),
    criado_por       varchar(120),
    constraint uq_acordo_numero unique (empresa_id, numero)
);

create index idx_acordo_empresa on acordo (empresa_id, situacao);

-- Quais documentos entraram neste acordo, e com que saldo cada um entrou.
create table documento_do_acordo (
    id          uuid           primary key,
    acordo_id   uuid           not null references acordo (id),
    titulo_id   uuid           not null references titulo (id),
    saldo       numeric(14, 2) not null,
    constraint uq_documento_acordo unique (acordo_id, titulo_id)
);

-- As parcelas do acordo sao titulos de verdade, para caírem no contas a
-- receber, na conciliacao e na cobranca como qualquer outro.
create table parcela_do_acordo (
    id         uuid    primary key,
    acordo_id  uuid    not null references acordo (id),
    titulo_id  uuid    not null references titulo (id),
    ordem      integer not null,
    constraint uq_parcela_acordo unique (acordo_id, ordem)
);
