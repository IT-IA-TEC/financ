-- Marcar quais itens do plano de conta entram no livro caixa.
--
-- E uma marca do proprio item, nao do lancamento: quem lanca nao precisa
-- lembrar disso conta a conta, e depois da para separar o que entra do que
-- nao entra sem recontar nada na mao.

alter table natureza add column livro_caixa boolean default false not null;
