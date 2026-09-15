-- Modelo de inteligencia artificial ligado ao sistema, e a ficha do caso
-- montada por cada empresa.
--
-- A ideia central: o modelo nao tem acesso a nada por padrao. Cada coisa que
-- ele pode LER e cada coisa que ele pode FAZER e uma permissao ligada na mao,
-- por empresa. A permissao nao e um aviso na tela: e conferida no momento de
-- montar o contexto e no momento de executar a acao. O que nao esta ligado
-- simplesmente nao entra no texto que vai para o modelo, e a acao nao roda.
--
-- Cinco regras que o desenho protege:
--   1. Nada de acesso total. Sem permissao ligada, o modelo nao ve nem age.
--   2. O que o modelo leu e o que ele fez ficam gravados, com a lista de
--      permissoes usadas. Auditoria e o que torna isso confiavel.
--   3. Existe teto de valor e teto de mensagens por dia. Modelo nenhum negocia
--      acima do que a empresa autorizou.
--   4. Da para deixar o modelo so sugerindo, sem falar com cliente nenhum.
--      Esse e o modo em que ele nasce.
--   5. Desligar a chave derruba tudo na hora, sem apagar o historico.

create table agente_ia (
    empresa_id        uuid         primary key references empresa (id),
    integracao_id     uuid references integracao (id),
    -- o nome do modelo na outra ponta, como esta na documentacao do provedor
    modelo            varchar(120),
    ativo             boolean      not null default false,
    -- SO_SUGERE, RESPONDE_COM_REVISAO ou RESPONDE_SOZINHO
    modo              varchar(30)  not null default 'SO_SUGERE',
    -- o que a empresa quer que ele seja: tom, limites, jeito de falar
    instrucao         varchar(4000),
    -- teto de valor que ele pode negociar ou citar como proposta
    teto_valor        numeric(14, 2) not null default 0,
    -- quantas mensagens ele pode mandar por dia para o mesmo cliente
    teto_mensagens_dia integer     not null default 1,
    atualizado_em     timestamp with time zone not null default now(),
    atualizado_por    varchar(120)
);

-- Cada permissao ligada, uma linha. O que nao esta aqui, nao pode.
create table permissao_ia (
    id         uuid        primary key,
    empresa_id uuid        not null references empresa (id),
    -- LEITURA ou ACAO
    especie    varchar(10) not null,
    chave      varchar(60) not null,
    ligada     boolean     not null default false,
    ligada_em  timestamp with time zone,
    ligada_por varchar(120),
    constraint uq_permissao_ia unique (empresa_id, chave)
);

create index idx_permissao_ia on permissao_ia (empresa_id, especie);

-- Tudo que o modelo leu, respondeu e fez.
create table uso_da_ia (
    id           uuid    primary key,
    empresa_id   uuid    not null references empresa (id),
    unidade_id   uuid references cliente_espelho (id),
    -- LEITURA_DO_CASO, RESPOSTA_SUGERIDA ou ACAO
    tipo         varchar(30) not null,
    permissoes   varchar(500),
    pergunta     varchar(4000),
    resposta     varchar(4000),
    acao         varchar(60),
    executada    boolean not null default false,
    recusa       varchar(300),
    aprovada_por varchar(120),
    ocorrido_em  timestamp with time zone not null default now()
);

create index idx_uso_ia on uso_da_ia (empresa_id, ocorrido_em);

-- A ficha do caso, montada por cada empresa.
create table linha_da_ficha (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    ordem      integer      not null default 1,
    rotulo     varchar(60)  not null,
    -- de onde sai o conteudo desta linha
    fonte      varchar(40)  not null,
    ativa      boolean      not null default true
);

create index idx_linha_ficha on linha_da_ficha (empresa_id, ordem);
