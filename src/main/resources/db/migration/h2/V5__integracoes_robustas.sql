-- A integracao deixa de ser "um provedor" e passa a ter estrutura:
-- COMO ela funciona (tipo), COMO ela se autentica, e QUAIS operacoes ela pode
-- fazer. O conector vira so um modelo de partida.

alter table integracao add column tipo varchar(30);
alter table integracao add column autenticacao varchar(30);
alter table integracao add column tempo_limite_segundos int default 20;
alter table integracao add column tentativas int default 3;
alter table integracao add column espera_entre_tentativas_ms int default 2000;
alter table integracao add column verificar_assinatura boolean default false;

update integracao set tipo = 'API_REST', autenticacao = 'NENHUMA' where tipo is null;
alter table integracao alter column tipo set not null;
alter table integracao alter column autenticacao set not null;

create table integracao_operacao (
    id            uuid primary key,
    integracao_id uuid          not null references integracao (id),
    nome          varchar(200)  not null,
    verbo         varchar(10)   not null default 'GET',
    caminho       varchar(500)  not null,
    para_que      varchar(500),
    corpo_modelo  varchar(4000),
    ativa         boolean       not null default true,
    criada_em     timestamp with time zone not null default now()
);

create index idx_operacao_integracao on integracao_operacao (integracao_id);
