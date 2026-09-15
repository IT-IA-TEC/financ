-- Pausar a regua sem perder o caso de vista.
--
-- Quando o cliente combina uma data, ou quando um acordo esta em dia, cobrar
-- de novo e o jeito mais rapido de estragar o que ja estava resolvido. A pausa
-- tem data de fim de proposito: regua parada para sempre e divida esquecida.

alter table caso_de_cobranca add column pausada_ate date;
alter table caso_de_cobranca add column motivo_da_pausa varchar(300);
