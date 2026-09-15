-- O que faltava no cadastro da unidade, e a memoria das trocas.
--
-- Percentual, plataforma e tom de cobranca sao da UNIDADE, e nao da pessoa:
-- a mesma pessoa pode ter uma loja com 40% e outra com 30%, em plataformas
-- diferentes, e falar de um jeito com cada uma.
--
-- A troca de dono e a troca de telefone ficam registradas: identificar a
-- unidade pelo codigo, e nao pelo nome, so resolve metade do problema. A outra
-- metade e saber quando a unidade mudou de mao.

alter table cliente_espelho add column percentual numeric(7, 4);
alter table cliente_espelho add column plataforma varchar(60);
alter table cliente_espelho add column tom_de_cobranca varchar(30) default 'PADRAO';
alter table cliente_espelho add column aceita_parcelamento boolean default true;
alter table cliente_espelho add column inicio_na_casa date;

create table historico_da_unidade (
    id         uuid        primary key,
    empresa_id uuid        not null references empresa (id),
    unidade_id uuid        not null references cliente_espelho (id),
    -- DONO, TELEFONE, NOME, PERCENTUAL ou SITUACAO
    tipo       varchar(20) not null,
    de         varchar(300),
    para       varchar(300),
    motivo     varchar(500),
    quando     timestamp with time zone not null default now(),
    quem       varchar(120)
);

create index idx_historico_unidade on historico_da_unidade (unidade_id, quando);
