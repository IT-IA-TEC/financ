-- Modulo de integracoes: o sistema conversando com o mundo de fora.
--
-- Uma integracao e uma ligacao configurada por dentro do proprio sistema:
-- um banco (Inter), um sistema de cobranca (Conexa), um banco de dados
-- (Supabase) ou uma API qualquer. As credenciais ficam CIFRADAS.

create table integracao (
    id                  uuid primary key,
    empresa_id          uuid        not null references empresa (id),
    provedor            text        not null,
    nome                text        not null,
    ambiente            text        not null default 'PRODUCAO'
                        check (ambiente in ('PRODUCAO', 'TESTE')),
    base_url            text,
    credenciais_cifradas text,
    webhook_segredo     text        not null,
    ativa               boolean     not null default false,
    ultima_checagem_em  timestamptz,
    ultima_checagem_ok  boolean,
    ultima_checagem_erro text,
    observacao          text,
    criada_em           timestamptz not null default now(),
    criada_por          text,
    atualizada_em       timestamptz not null default now(),
    constraint uq_integracao_nome unique (empresa_id, nome)
);

comment on table integracao is
    'Cada ligacao com um sistema de fora, configurada por empresa.';

create index idx_integracao_empresa on integracao (empresa_id);

create table integracao_evento (
    id            bigserial primary key,
    integracao_id uuid        not null references integracao (id) on delete cascade,
    empresa_id    uuid        not null references empresa (id),
    direcao       text        not null check (direcao in ('ENTRADA', 'SAIDA')),
    tipo          text        not null,
    referencia    text,
    carga         text,
    status        text        not null default 'RECEBIDO'
                  check (status in ('RECEBIDO', 'PROCESSADO', 'ERRO', 'IGNORADO')),
    erro          text,
    ocorrido_em   timestamptz not null default now(),
    processado_em timestamptz
);

comment on table integracao_evento is
    'Tudo que entra e sai por uma integracao, guardado bruto. Nada e apagado.';

create index idx_evento_integracao on integracao_evento (integracao_id, ocorrido_em desc);
create index idx_evento_pendente on integracao_evento (ocorrido_em)
    where status = 'RECEBIDO';
