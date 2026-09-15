-- Quais colunas cada empresa quer ver na tela de clientes.
create table carteira_colunas (
    empresa_id uuid primary key references empresa (id),
    colunas    text        not null,
    salvo_em   timestamptz not null default now(),
    salvo_por  text
);

comment on table carteira_colunas is
    'Escolha de colunas da tela de clientes, por empresa.';
