-- As regras de cada empresa, ligadas e desligadas por quem usa.
--
-- O sistema nao decide por ninguem: cada empresa escolhe se usa identificador
-- proprio no PIX, se oferece Pix Automatico, se o agente se apresenta como
-- robo e se cobra juros e multa. O padrao vem desligado, e nada muda no
-- comportamento de quem nao mexer aqui.

create table regra_da_empresa (
    empresa_id uuid primary key references empresa (id),

    -- Cada cobranca com identificador proprio no PIX: e o que permite a baixa
    -- automatica sem ninguem ler comprovante.
    pix_identificador boolean not null default false,

    -- Pix Automatico: o cliente autoriza uma vez e deixa de ser cobrado todo mes.
    pix_automatico boolean not null default false,

    -- O agente diz que e robo quando perguntam.
    agente_se_identifica boolean not null default false,

    -- Juros e multa por atraso. Desligado, nada e somado ao valor.
    cobrar_juros boolean not null default false,
    juros_ao_mes numeric(7, 4) not null default 1,
    multa_por_atraso numeric(7, 4) not null default 2,
    carencia_dias int not null default 0,

    atualizado_em timestamp with time zone not null default now(),
    atualizado_por varchar(120)
);
