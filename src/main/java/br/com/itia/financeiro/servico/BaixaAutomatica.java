package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Evento;
import br.com.itia.financeiro.dominio.EventoIntegracao;
import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.EventoIntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.EventoRepositorio;
import br.com.itia.financeiro.repositorio.PagamentoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * O tradutor do aviso do banco: transforma o que chegou pelo webhook em baixa.
 *
 * O que este serviço protege:
 *   1. O aviso é guardado bruto antes de ser interpretado. Se a tradução
 *      falhar, o aviso continua lá para alguém olhar.
 *   2. O mesmo identificador de transação nunca paga duas vezes.
 *   3. Aviso sem cobrança correspondente não é chutado em título nenhum:
 *      fica na fila de investigação, com o motivo escrito.
 *   4. Roda sem ninguém logado, porque quem chama é o banco. O autor da baixa
 *      fica registrado como a integração.
 */
@Service
public class BaixaAutomatica {

    /** O identificador que o sistema cria em cada título: APELIDO + 8 dígitos. */
    private static final Pattern IDENTIFICADOR = Pattern.compile("([A-Z][A-Z0-9]{1,11}\\d{8})");

    private static final Pattern VALOR = Pattern.compile(
            "\"(valor|valor_pago|amount|value)\"\\s*:\\s*\"?([0-9]+[.,]?[0-9]*)");
    private static final Pattern TRANSACAO = Pattern.compile(
            "\"(endToEndId|end_to_end_id|e2eid|txid|transacao|transaction_id|id)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DATA = Pattern.compile(
            "\"(data|data_pagamento|pago_em|horario|paid_at|date)\"\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2})");

    private final TituloRepositorio titulos;
    private final PagamentoRepositorio pagamentos;
    private final EventoIntegracaoRepositorio avisosDeIntegracao;
    private final EventoRepositorio eventos;
    private final Avisos avisos;

    public BaixaAutomatica(TituloRepositorio titulos, PagamentoRepositorio pagamentos,
                           EventoIntegracaoRepositorio avisosDeIntegracao,
                           EventoRepositorio eventos, Avisos avisos) {
        this.titulos = titulos;
        this.pagamentos = pagamentos;
        this.avisosDeIntegracao = avisosDeIntegracao;
        this.eventos = eventos;
        this.avisos = avisos;
    }

    /**
     * Tenta dar baixa com um aviso recém-chegado.
     *
     * Nunca deixa a exceção subir: o banco não pode receber erro nosso por
     * causa de um aviso que não soubemos ler.
     */
    @Transactional
    public Resultado processar(EventoIntegracao aviso, UUID empresaId) {
        try {
            return traduzir(aviso, empresaId);
        } catch (RuntimeException falha) {
            aviso.marcarErro("não consegui ler o aviso: " + falha.getMessage());
            avisosDeIntegracao.save(aviso);
            return new Resultado(false, "não consegui ler o aviso", null);
        }
    }

    private Resultado traduzir(EventoIntegracao aviso, UUID empresaId) {
        String corpo = aviso.getCarga() == null ? "" : aviso.getCarga();

        if (!pareceRecebimento(aviso.getTipo(), corpo)) {
            // Aviso de outra natureza: fica guardado, sem virar baixa.
            aviso.marcarProcessado();
            avisosDeIntegracao.save(aviso);
            return new Resultado(false, "aviso guardado, não é recebimento", null);
        }

        String identificador = achar(IDENTIFICADOR, corpo, 1);
        if (identificador == null) {
            return naoCasou(aviso, "aviso sem o identificador da cobrança");
        }

        Optional<Titulo> achado = titulos.findByEmpresaIdAndIdentificadorPix(empresaId,
                identificador);
        if (achado.isEmpty()) {
            return naoCasou(aviso, "nenhuma cobrança com o identificador " + identificador);
        }
        Titulo titulo = achado.get();

        String transacao = achar(TRANSACAO, corpo, 2);
        if (transacao != null && pagamentos.existsByEmpresaIdAndTransacaoId(empresaId, transacao)) {
            // Mesmo PIX chegando de novo: guarda e nao paga duas vezes.
            aviso.marcarProcessado();
            avisosDeIntegracao.save(aviso);
            return new Resultado(false, "este pagamento já tinha entrado antes", titulo);
        }

        BigDecimal valor = valorDe(corpo);
        if (valor.signum() <= 0) {
            return naoCasou(aviso, "aviso sem valor de pagamento");
        }

        Pagamento pagamento = new Pagamento(valor, dataDe(corpo), "PIX", transacao,
                "integração");
        // O pagamento entra pelo titulo, que se encarrega de gravar.
        titulo.receber(pagamento);
        titulos.save(titulo);

        eventos.save(new Evento(empresaId, "titulo", titulo.getId(), "BAIXA_AUTOMATICA",
                "integração", "{\"valor\": " + valor + ", \"situacao\": \""
                + titulo.getSituacao() + "\", \"transacao\": \"" + transacao + "\"}"));

        aviso.marcarProcessado();
        avisosDeIntegracao.save(aviso);
        avisos.avisar(empresaId, "PAGAMENTO_RECEBIDO");
        return new Resultado(true, "título " + titulo.getNumero() + " baixado em "
                + titulo.getSituacao().name().toLowerCase(), titulo);
    }

    private Resultado naoCasou(EventoIntegracao aviso, String motivo) {
        aviso.marcarErro(motivo);
        avisosDeIntegracao.save(aviso);
        return new Resultado(false, motivo, null);
    }

    /** Reprocessa os avisos que ficaram parados, depois de alguém arrumar algo. */
    @Transactional
    public int reprocessarParados(UUID empresaId) {
        List<EventoIntegracao> parados = avisosDeIntegracao
                .findTop30ByEmpresaIdOrderByOcorridoEmDesc(empresaId).stream()
                .filter(e -> "ENTRADA".equals(e.getDirecao()))
                .filter(e -> !"PROCESSADO".equals(e.getStatus()))
                .toList();

        int baixados = 0;
        for (EventoIntegracao aviso : parados) {
            if (processar(aviso, empresaId).baixou()) {
                baixados = baixados + 1;
            }
        }
        return baixados;
    }

    // ------------------------------------------------------------------ apoio

    private boolean pareceRecebimento(String tipo, String corpo) {
        String junto = ((tipo == null ? "" : tipo) + " " + corpo).toUpperCase();
        return junto.contains("PIX") || junto.contains("RECEBI") || junto.contains("PAGAMENTO")
                || junto.contains("LIQUIDA") || junto.contains("CREDITO")
                || junto.contains("PAID");
    }

    private String achar(Pattern padrao, String texto, int grupo) {
        Matcher achou = padrao.matcher(texto);
        return achou.find() ? achou.group(grupo).trim() : null;
    }

    /**
     * O valor do aviso.
     *
     * Sem valor legível o saldo não é chutado: o aviso vai para a fila de
     * investigação. Quem pagou a mais continua com o crédito registrado no
     * próprio título, que é onde ele já é calculado hoje.
     */
    private BigDecimal valorDe(String corpo) {
        String bruto = achar(VALOR, corpo, 2);
        if (bruto == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(bruto.replace(",", "."));
        } catch (NumberFormatException valorIlegivel) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate dataDe(String corpo) {
        String bruto = achar(DATA, corpo, 2);
        if (bruto == null) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(bruto);
        } catch (DateTimeParseException dataIlegivel) {
            return LocalDate.now();
        }
    }

    /** O que aconteceu com o aviso, para a tela contar em uma linha. */
    public record Resultado(boolean baixou, String explicacao, Titulo titulo) {
    }
}
