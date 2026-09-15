-- Quais colunas cada empresa quer ver na tela de clientes.
create table carteira_colunas (
    empresa_id uuid primary key references empresa (id),
    colunas    varchar(2000) not null,
    salvo_em   timestamp with time zone not null default now(),
    salvo_por  varchar(120)
);
