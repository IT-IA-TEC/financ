package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Comprovante;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.ComprovanteRepositorio;
import br.com.itia.financeiro.repositorio.PagadorRepositorio;
import br.com.itia.financeiro.repositorio.PagamentoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.itia.financeiro.dominio.ArquivoRecebido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fila de comprovantes.
 *
 * As regras que este serviço protege:
 *   1. Só dá baixa se o destino do pagamento for a conta da empresa. A
 *      conferência é pela chave PIX ou pelo CNPJ, nunca pelo nome digitado.
 *   2. O mesmo identificador de transação não paga duas vezes.
 *   3. Comprovante recusado fica na fila com o motivo escrito, porque o
 *      cliente vai perguntar.
 *   4. A leitura automática é do TEXTO do comprovante. Ler imagem depende de
 *      um serviço de fora, e o sistema diz isso em vez de fingir que leu.
 */
@Service
public class Comprovantes {

    private static final Pattern VALOR = Pattern.compile(
            "(?i)(?:r\\$\\s*)([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2}|[0-9]+[.,][0-9]{2})");
    private static final Pattern DATA = Pattern.compile("([0-3]?\\d/[0-1]?\\d/\\d{4})");
    /** Documento com pontuação: é assim que comprovante escreve CNPJ e CPF. */
    private static final Pattern DOCUMENTO = Pattern.compile(
            "(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}");
    /** A linha que diz para onde o dinheiro foi. */
    private static final Pattern LINHA_DESTINO = Pattern.compile(
            "(?im)^.*(?:destino|favorecido|recebedor|benefici[áa]rio|chave)\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern TRANSACAO = Pattern.compile(
            "(?i)(?:e2e|end.?to.?end|id da transa[çc][ãa]o|identificador|autentica[çc][ãa]o)"
                    + "\\s*[:\\-]?\\s*([A-Za-z0-9]{8,40})");
    private static final Pattern CODIGO_SOLTO = Pattern.compile("\\b(E\\d{25,})\\b");

    private final ComprovanteRepositorio comprovantes;
    private final TituloRepositorio titulos;
    private final PagamentoRepositorio pagamentos;
    private final PagadorRepositorio pagadores;
    private final DocumentoServico documentos;
    private final FinanceiroServico financeiro;
    private final ContextoEmpresa contexto;

    public Comprovantes(ComprovanteRepositorio comprovantes, TituloRepositorio titulos,
                        PagamentoRepositorio pagamentos, PagadorRepositorio pagadores,
                        DocumentoServico documentos, FinanceiroServico financeiro,
                        ContextoEmpresa contexto) {
        this.comprovantes = comprovantes;
        this.titulos = titulos;
        this.pagamentos = pagamentos;
        this.pagadores = pagadores;
        this.documentos = documentos;
        this.financeiro = financeiro;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ fila

    public List<Comprovante> fila() {
        return comprovantes.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId());
    }

    public List<Comprovante> esperando() {
        return fila().stream().filter(Comprovante::naFila).toList();
    }

    public Comprovante comprovante(UUID id) {
        return comprovantes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Comprovante não encontrado."));
    }

    public List<Pagador> clientes() {
        return pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<Titulo> titulosEmAberto() {
        return titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(contexto.exigirEmpresaId(),
                List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL));
    }

    // -------------------------------------------------------------- receber

    /**
     * Guarda um comprovante e lê o que der do texto.
     *
     * O arquivo é opcional: às vezes chega só o texto copiado do aplicativo do
     * banco. O que o sistema não conseguir ler fica visível como falta, e não
     * como zero.
     */
    @Transactional
    public Comprovante receber(UUID pagadorId, String texto, ArquivoRecebido arquivo,
                               String origem) {
        if ((texto == null || texto.isBlank()) && (arquivo == null || arquivo.vazio())) {
            throw new IllegalArgumentException(
                    "Cole o texto do comprovante ou anexe o arquivo.");
        }
        Comprovante comprovante = new Comprovante(contexto.exigirEmpresaId(), pagadorId, origem,
                texto, contexto.autor());
        ler(comprovante, texto);

        if (arquivo != null && !arquivo.vazio()) {
            Documento documento = documentos.anexar(arquivo, pagadorId, null, null,
                    "COMPROVANTE DE PAGAMENTO", null, "comprovante em conferência");
            comprovante.guardarArquivo(documento.getId(), null);
        }
        return comprovantes.save(comprovante);
    }

    /** Lê valor, data, destino e identificador do texto do comprovante. */
    private void ler(Comprovante comprovante, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        comprovante.guardarLeitura(valorDe(texto), dataDe(texto), destinoDe(texto),
                transacaoDe(texto));
    }

    // -------------------------------------------------------------- conferir

    /**
     * Confere e dá a baixa.
     *
     * Antes de baixar: o destino tem que ser a conta da empresa, o
     * identificador não pode ter entrado antes, e o título tem que ter saldo.
     */
    @Transactional
    public void conferir(UUID comprovanteId, UUID tituloId, BigDecimal valorConfirmado) {
        Comprovante comprovante = comprovante(comprovanteId);
        Empresa empresa = contexto.exigirEmpresa();

        if (!destinoConfere(comprovante.getDestinoLido(), empresa)) {
            throw new IllegalStateException("O destino do comprovante ("
                    + (comprovante.getDestinoLido() == null ? "não lido"
                    : comprovante.getDestinoLido())
                    + ") não é a conta desta empresa. Confira antes de dar baixa.");
        }
        String transacao = comprovante.getIdentificadorLido();
        if (transacao != null && pagamentos.existsByEmpresaIdAndTransacaoId(empresa.getId(),
                transacao)) {
            throw new IllegalStateException(
                    "Este comprovante já foi lançado antes (transação " + transacao + ").");
        }

        Titulo titulo = titulos.findByIdAndEmpresaId(tituloId, empresa.getId())
                .orElseThrow(() -> new IllegalArgumentException("Título não encontrado."));
        BigDecimal valor = valorConfirmado != null ? valorConfirmado : comprovante.getValorLido();
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Informe o valor do comprovante.");
        }

        financeiro.receberPagamento(titulo.getId(), valor,
                comprovante.getDataLida() == null ? LocalDate.now() : comprovante.getDataLida(),
                "COMPROVANTE", transacao);
        comprovante.conferir(titulo.getId(), contexto.autor());
        comprovantes.save(comprovante);
    }

    @Transactional
    public void recusar(UUID comprovanteId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Recusar exige motivo: é o que a pessoa vai responder ao cliente.");
        }
        Comprovante comprovante = comprovante(comprovanteId);
        comprovante.recusar(motivo, contexto.autor());
        comprovantes.save(comprovante);
    }

    @Transactional
    public void voltarParaFila(UUID comprovanteId) {
        Comprovante comprovante = comprovante(comprovanteId);
        comprovante.voltarParaFila();
        comprovantes.save(comprovante);
    }

    /** Os títulos que combinam com o valor lido, para a pessoa escolher rápido. */
    public List<Titulo> sugestoesPara(Comprovante comprovante) {
        BigDecimal valor = comprovante.getValorLido();
        return titulosEmAberto().stream()
                .filter(t -> comprovante.getPagadorId() == null
                        || (t.getCliente().getPagador() != null
                        && t.getCliente().getPagador().getId().equals(comprovante.getPagadorId())))
                .filter(t -> valor == null || t.getSaldo().subtract(valor).abs()
                        .compareTo(Titulo.TOLERANCIA) <= 0)
                .toList();
    }

    // ------------------------------------------------------------------ apoio

    /**
     * O destino é da empresa?
     *
     * Compara com a chave PIX e com o CNPJ, só pelos números. Nome batendo não
     * vale: nome qualquer um escreve.
     */
    private boolean destinoConfere(String destino, Empresa empresa) {
        if (destino == null || destino.isBlank()) {
            return false;
        }
        String lido = soNumeros(destino);
        String chave = soNumeros(empresa.getChavePix());
        String cnpj = soNumeros(empresa.getCnpj());
        if (lido.isEmpty()) {
            // Chave de e-mail ou aleatoria: compara o texto inteiro.
            return destino.trim().equalsIgnoreCase(
                    empresa.getChavePix() == null ? "" : empresa.getChavePix().trim());
        }
        return (!chave.isEmpty() && lido.equals(chave))
                || (!cnpj.isEmpty() && lido.equals(cnpj));
    }

    private BigDecimal valorDe(String texto) {
        Matcher achou = VALOR.matcher(texto);
        if (!achou.find()) {
            return null;
        }
        String bruto = achou.group(1).replace(".", "").replace(",", ".");
        try {
            return new BigDecimal(bruto);
        } catch (NumberFormatException valorIlegivel) {
            return null;
        }
    }

    private LocalDate dataDe(String texto) {
        Matcher achou = DATA.matcher(texto);
        if (!achou.find()) {
            return null;
        }
        try {
            return LocalDate.parse(achou.group(1), DateTimeFormatter.ofPattern("d/M/yyyy"));
        } catch (RuntimeException dataIlegivel) {
            return null;
        }
    }

    /**
     * Para onde o dinheiro foi.
     *
     * A linha que fala em destino manda. Sem ela, vale um e-mail ou um
     * documento com pontuação. Código de transação não vira destino: é número
     * demais, e o sistema barraria a baixa por engano.
     */
    private String destinoDe(String texto) {
        Matcher linha = LINHA_DESTINO.matcher(texto);
        if (linha.find()) {
            String valor = linha.group(1).trim();
            Matcher emailDaLinha = EMAIL.matcher(valor);
            if (emailDaLinha.find()) {
                return emailDaLinha.group();
            }
            Matcher documentoDaLinha = DOCUMENTO.matcher(valor);
            if (documentoDaLinha.find()) {
                return documentoDaLinha.group(1);
            }
            return valor.isEmpty() ? null : valor;
        }
        Matcher email = EMAIL.matcher(texto);
        if (email.find()) {
            return email.group();
        }
        Matcher documento = DOCUMENTO.matcher(texto);
        return documento.find() ? documento.group(1) : null;
    }

    private String transacaoDe(String texto) {
        Matcher achou = TRANSACAO.matcher(texto);
        if (achou.find()) {
            return achou.group(1);
        }
        Matcher solto = CODIGO_SOLTO.matcher(texto);
        return solto.find() ? solto.group(1) : null;
    }

    private String soNumeros(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }
}
