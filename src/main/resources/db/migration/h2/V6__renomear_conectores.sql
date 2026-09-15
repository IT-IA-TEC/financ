-- Os modelos de partida foram renomeados quando o modulo virou plataforma.
-- Aqui as integracoes que ja existiam passam a apontar para o nome novo.
update integracao set provedor = 'BANCO_API'              where provedor = 'INTER';
update integracao set provedor = 'SISTEMA_DE_COBRANCA'    where provedor = 'CONEXA';
update integracao set provedor = 'BANCO_DE_DADOS_EXTERNO' where provedor = 'SUPABASE';
update integracao set provedor = 'PERSONALIZADA'          where provedor = 'REST';
update integracao set provedor = 'SO_RECEBIMENTO'         where provedor = 'WEBHOOK';

update integracao set tipo = 'WEBHOOK' where provedor = 'SO_RECEBIMENTO';
update integracao set autenticacao = 'CERTIFICADO' where provedor = 'BANCO_API' and autenticacao = 'NENHUMA';
