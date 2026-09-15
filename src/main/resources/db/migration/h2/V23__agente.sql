-- Agente de primeiro atendimento: responde a primeira mensagem do cliente.
--
-- O agente so fala o que o sistema sabe: saldo, vencimento, chave PIX e o que
-- ja foi combinado. Ele nao inventa valor, nao da desconto e nao fecha acordo.
-- Quando nao entende, ou quando o assunto e dinheiro negociado, ele passa para
-- uma pessoa e diz que passou.
--
-- Quatro regras que o desenho protege:
--   1. Toda resposta do agente fica registrada junto com a pergunta e com a
--      intencao que ele entendeu. Assim da para conferir se ele errou.
--   2. Assunto que envolve negociar valor sempre vai para gente. Sempre.
--   3. Se a empresa mandar, ele se identifica como robo na primeira fala.
--   4. Fora do horario combinado ele nao responde: guarda e avisa a equipe.

create table agente (
    empresa_id       uuid         primary key references empresa (id),
    ativo            boolean      not null default false,
    saudacao         varchar(500),
    assinatura       varchar(120),
    -- horario em que ele pode responder
    comeca_as        integer      not null default 8,
    termina_as       integer      not null default 18,
    responde_sabado  boolean      not null default false,
    atualizado_em    timestamp with time zone not null default now(),
    atualizado_por   varchar(120)
);

create table atendimento_do_agente (
    id           uuid    primary key,
    empresa_id   uuid    not null references empresa (id),
    unidade_id   uuid references cliente_espelho (id),
    entrada_id   uuid references mensagem (id),
    resposta_id  uuid references mensagem (id),
    pergunta     varchar(1000),
    -- QUANTO_DEVO, COMO_PAGO, JA_PAGUEI, VOU_PAGAR, QUERO_PARCELAR,
    -- RECLAMACAO ou NAO_ENTENDI
    intencao     varchar(20) not null,
    escalado     boolean not null default false,
    motivo       varchar(300),
    ocorrido_em  timestamp with time zone not null default now()
);

create index idx_atendimento_agente on atendimento_do_agente (empresa_id, ocorrido_em);
