-- A esteira de inadimplencia: por onde cada devedor esta passando.
--
-- A faixa de atraso o sistema calcula sozinho, e ninguem move na mao: ela vem
-- do vencimento. O que a pessoa marca aqui e o que o sistema NAO sabe: se o
-- caso esta em acordo, contestado, parado esperando alguem, ou no juridico.
--
-- Tres regras que o desenho protege:
--   1. Um caso por cliente. Nao existe o mesmo devedor em duas listas.
--   2. Caso contestado ou em acordo NAO recebe cobranca automatica, porque
--      cobrar quem ja combinou pagamento e o jeito mais rapido de perder o
--      cliente.
--   3. Toda mudanca guarda quem mexeu e quando.

create table caso_de_cobranca (
    id             uuid         primary key,
    empresa_id     uuid         not null references empresa (id),
    unidade_id     uuid         not null references cliente_espelho (id),
    -- EM_COBRANCA, EM_ACORDO, PROMESSA, CONTESTADO, PARADO ou JURIDICO
    situacao       varchar(20)  not null default 'EM_COBRANCA',
    responsavel    varchar(120),
    proxima_acao   varchar(300),
    proxima_data   date,
    observacao     varchar(1000),
    criado_em      timestamp with time zone not null default now(),
    atualizado_em  timestamp with time zone not null default now(),
    atualizado_por varchar(120),
    constraint uq_caso_unidade unique (empresa_id, unidade_id)
);

create index idx_caso_empresa on caso_de_cobranca (empresa_id, situacao);
