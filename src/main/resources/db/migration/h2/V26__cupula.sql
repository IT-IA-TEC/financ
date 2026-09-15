-- A Cupula do IT.FC: as tarefas de quem trabalha no financeiro.
--
-- E a mesma ideia da Cupula do sistema matriz, na versao que o pessoal do
-- financeiro usa: o que chegou pra mim, o que eu mandei, o que eu acompanho, e
-- o que esta aberto pro setor esperando alguem pegar.
--
-- Uma tabela so, com pai opcional: a demanda grande e a tarefa pai, e cada
-- ticket dela e uma tarefa filha. Duas tabelas separadas seriam a mesma coisa
-- escrita duas vezes.
--
-- Seis regras que o desenho protege:
--   1. Tarefa nao se apaga: cancela, com motivo e autor. O que ja aconteceu
--      continua podendo ser explicado depois.
--   2. Quem concluiu nao fecha sozinho: fica esperando revisao de quem pediu,
--      e quem pediu aceita ou manda refazer, com o motivo escrito.
--   3. Tarefa de setor nao tem dono ate alguem pegar. Enquanto nao tem, ela
--      aparece pra todo mundo do setor.
--   4. O que chega de outro sistema nunca vira tarefa repetida: a chave e o
--      identificador de la, e a segunda vez atualiza em vez de duplicar.
--   5. A Central some sozinha: assim que a pessoa abre a tarefa, ela sai da
--      fila do que ainda nao foi visto.
--   6. Prazo estourado nao e situacao: e conta de data. Ninguem marca "atrasada"
--      na mao.

create table tarefa (
    id               uuid         primary key,
    empresa_id       uuid         not null references empresa (id),
    numero           integer      not null,
    pai_id           uuid references tarefa (id),
    titulo           varchar(200) not null,
    descricao        varchar(4000),
    -- BAIXA, NORMAL, ALTA ou CRITICA
    prioridade       varchar(10)  not null default 'NORMAL',
    -- A_FAZER, EM_ANDAMENTO, AGUARDANDO_REVISAO, CONCLUIDA ou CANCELADA
    situacao         varchar(20)  not null default 'A_FAZER',
    -- quem pediu
    pedida_por       varchar(120),
    -- quem faz; vazio quer dizer que esta aberta pro setor inteiro
    responsavel      varchar(120),
    setor            varchar(60),
    prazo            date,
    -- MANUAL, API ou SINCRONIZADA
    origem           varchar(20)  not null default 'MANUAL',
    -- de onde veio, quando veio de fora
    fonte_id         uuid,
    id_externo       varchar(200),
    link_externo     varchar(500),
    -- quando a pessoa viu pela primeira vez (a Central usa isto)
    vista_em         timestamp with time zone,
    iniciada_em      timestamp with time zone,
    concluida_em     timestamp with time zone,
    revisada_em      timestamp with time zone,
    revisada_por     varchar(120),
    -- quando volta pra refazer, o motivo fica escrito
    motivo_retrabalho varchar(500),
    cancelada_em     timestamp with time zone,
    cancelada_por    varchar(120),
    motivo_cancelamento varchar(500),
    cobrada_em       timestamp with time zone,
    criado_em        timestamp with time zone not null default now(),
    criado_por       varchar(120),
    constraint uq_tarefa_numero unique (empresa_id, numero)
);

create index idx_tarefa_empresa on tarefa (empresa_id, situacao);
create index idx_tarefa_responsavel on tarefa (empresa_id, responsavel);
create unique index uq_tarefa_externa on tarefa (empresa_id, id_externo);

-- O fio da tarefa: o que foi dito, por quem e quando.
create table anotacao_da_tarefa (
    id         uuid    primary key,
    tarefa_id  uuid    not null references tarefa (id),
    empresa_id uuid    not null references empresa (id),
    texto      varchar(4000) not null,
    -- COMENTARIO, DUVIDA, COBRANCA ou SISTEMA
    tipo       varchar(20) not null default 'COMENTARIO',
    autor      varchar(120),
    criado_em  timestamp with time zone not null default now()
);

create index idx_anotacao_tarefa on anotacao_da_tarefa (tarefa_id, criado_em);

-- Quem mais acompanha a tarefa, alem de quem faz e de quem pediu.
create table acompanha_tarefa (
    id         uuid    primary key,
    tarefa_id  uuid    not null references tarefa (id),
    empresa_id uuid    not null references empresa (id),
    quem       varchar(120) not null,
    constraint uq_acompanha unique (tarefa_id, quem)
);

-- De onde as tarefas de fora chegam.
--
-- Uma fonte e sempre uma integracao ja configurada (o endereco e a chave moram
-- la, cifrados) mais o mapeamento: qual campo de la vira qual campo daqui.
create table fonte_de_tarefas (
    id             uuid         primary key,
    empresa_id     uuid         not null references empresa (id),
    nome           varchar(120) not null,
    integracao_id  uuid         not null references integracao (id),
    -- a operacao daquela integracao que traz a lista
    operacao_id    uuid references integracao_operacao (id),
    -- o mapeamento: campo daqui = campo de la
    campo_id       varchar(60)  not null default 'id',
    campo_titulo   varchar(60)  not null default 'titulo',
    campo_descricao varchar(60),
    campo_situacao varchar(60),
    campo_responsavel varchar(60),
    campo_prazo    varchar(60),
    campo_prioridade varchar(60),
    campo_link     varchar(60),
    -- so traz o que interessa ao financeiro
    filtro         varchar(300),
    setor_padrao   varchar(60),
    ativa          boolean      not null default true,
    ultima_puxada  timestamp with time zone,
    ultimo_resultado varchar(500),
    criado_em      timestamp with time zone not null default now(),
    criado_por     varchar(120)
);

create index idx_fonte_empresa on fonte_de_tarefas (empresa_id, ativa);
