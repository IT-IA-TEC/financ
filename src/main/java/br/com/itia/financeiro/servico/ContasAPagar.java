package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Aprovacao;
import br.com.itia.financeiro.dominio.Bem;
import br.com.itia.financeiro.dominio.CentroDeCusto;
import br.com.itia.financeiro.dominio.Conciliacao;
import br.com.itia.financeiro.dominio.ContaFinanceira;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Execucao;
import br.com.itia.financeiro.dominio.Favorecido;
import br.com.itia.financeiro.dominio.ItemDaObrigacao;
import br.com.itia.financeiro.dominio.Natureza;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.OrigemDoRegistro;
import br.com.itia.financeiro.dominio.PagamentoDeObrigacao;
import br.com.itia.financeiro.dominio.QualidadeDoCadastro;
import br.com.itia.financeiro.dominio.Recorrencia;
import br.com.itia.financeiro.dominio.TipoDeOperacao;
import br.com.itia.financeiro.repositorio.BemRepositorio;
import br.com.itia.financeiro.repositorio.CentroDeCustoRepositorio;
import br.com.itia.financeiro.repositorio.ContaFinanceiraRepositorio;
import br.com.itia.financeiro.repositorio.EmpresaRepositorio;
import br.com.itia.financeiro.repositorio.FavorecidoRepositorio;
import br.com.itia.financeiro.repositorio.NaturezaRepositorio;
import br.com.itia.financeiro.repositorio.ObrigacaoRepositorio;
import br.com.itia.financeiro.repositorio.PagadorRepositorio;
import br.com.itia.financeiro.repositorio.RecorrenciaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O contas a pagar de cada empresa.
 *
 * As regras que este serviço protege:
 *   1. Todo total é explicável. O valor da conta sai da composição, e a
 *      composição precisa fechar com ele.
 *   2. Reembolso é tipo de operação, e não natureza. A fonte comprada pelo
 *      Lucas aparece como reembolso ao Lucas E como manutenção do Fiscal.
 *   3. Os cinco controles andam separados, e vencido é conta, não campo.
 *   4. Mudar valor ou favorecido derruba a aprovação anterior.
 *   5. Rateio guarda critério e proporções, e a soma fecha com o valor.
 *   6. Empresa que paga conta de outra registra a relação, em vez de trocar o
 *      nome da empresa no lançamento.
 */
@Service
public class ContasAPagar {

    private final ObrigacaoRepositorio obrigacoes;
    private final FavorecidoRepositorio favorecidos;
    private final NaturezaRepositorio naturezas;
    private final CentroDeCustoRepositorio centros;
    private final ContaFinanceiraRepositorio contas;
    private final BemRepositorio bens;
    private final RecorrenciaRepositorio recorrencias;
    private final PagadorRepositorio clientes;
    private final EmpresaRepositorio empresas;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;

    public ContasAPagar(ObrigacaoRepositorio obrigacoes, FavorecidoRepositorio favorecidos,
                        NaturezaRepositorio naturezas, CentroDeCustoRepositorio centros,
                        ContaFinanceiraRepositorio contas, BemRepositorio bens,
                        RecorrenciaRepositorio recorrencias, PagadorRepositorio clientes,
                        EmpresaRepositorio empresas, ContextoEmpresa contexto, Avisos avisos) {
        this.obrigacoes = obrigacoes;
        this.favorecidos = favorecidos;
        this.naturezas = naturezas;
        this.centros = centros;
        this.contas = contas;
        this.bens = bens;
        this.recorrencias = recorrencias;
        this.clientes = clientes;
        this.empresas = empresas;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------------ lista

    public List<Obrigacao> todas() {
        return obrigacoes.findByEmpresaIdOrderByVencimento(contexto.exigirEmpresaId());
    }

    /**
     * A lista da tela, com a visão escolhida e os filtros.
     *
     * As visões são as perguntas que a rotina faz todo dia, e não filtros
     * soltos que alguém precisa remontar de manhã.
     */
    public List<Obrigacao> lista(String visao, FiltroDeContas filtro) {
        LocalDate hoje = LocalDate.now();
        return todas().stream()
                .filter(o -> cabeNaVisao(o, visao, hoje))
                .filter(o -> filtro == null || filtro.aceita(o, hoje))
                .toList();
    }

    private boolean cabeNaVisao(Obrigacao o, String visao, LocalDate hoje) {
        if (visao == null || visao.isBlank() || "todas".equals(visao)) {
            return !o.isCancelada();
        }
        return switch (visao) {
            case "proximos7" -> !o.isCancelada() && o.getSaldo().signum() > 0
                    && !o.getVencimento().isBefore(hoje)
                    && !o.getVencimento().isAfter(hoje.plusDays(7));
            case "vencidas" -> o.estaVencida(hoje);
            case "aprovacao" -> !o.isCancelada() && o.getAprovacao() == Aprovacao.PENDENTE;
            case "prontas" -> o.podeIrParaPagamento()
                    && o.getExecucao() == Execucao.NAO_ENCAMINHADA;
            case "encaminhadas" -> !o.isCancelada()
                    && (o.getExecucao() == Execucao.ENCAMINHADA
                    || o.getExecucao() == Execucao.AGENDADA)
                    && o.getSaldo().signum() > 0;
            case "reembolsos" -> !o.isCancelada()
                    && o.getTipoOperacao() == TipoDeOperacao.REEMBOLSO
                    && o.getSaldo().signum() > 0;
            case "parciais" -> !o.isCancelada()
                    && o.getLiquidacao() == br.com.itia.financeiro.dominio.Liquidacao.PARCIAL;
            case "incompletas" -> !o.isCancelada() && !o.pendencias().isEmpty();
            case "naoconciliadas" -> !o.isCancelada()
                    && o.getLiquidacao() == br.com.itia.financeiro.dominio.Liquidacao.LIQUIDADA
                    && o.getConciliacao() != Conciliacao.CONCILIADA;
            case "rascunhos" -> !o.isCancelada()
                    && o.getQualidade() == br.com.itia.financeiro.dominio
                    .QualidadeDoCadastro.RASCUNHO;
            case "canceladas" -> o.isCancelada();
            default -> !o.isCancelada();
        };
    }

    /** As visões salvas que aparecem na tela, com a contagem de cada uma. */
    public Map<String, Integer> contagemDasVisoes() {
        LocalDate hoje = LocalDate.now();
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (String visao : List.of("proximos7", "vencidas", "aprovacao", "prontas",
                "encaminhadas", "reembolsos", "parciais", "incompletas", "naoconciliadas",
                "rascunhos")) {
            contagem.put(visao, (int) todas().stream()
                    .filter(o -> cabeNaVisao(o, visao, hoje)).count());
        }
        return contagem;
    }

    public Obrigacao obrigacao(UUID id) {
        return obrigacoes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conta não encontrada nesta empresa."));
    }

    /** Os números do topo da tela, todos explicáveis pela lista. */
    public Resumo resumo(List<Obrigacao> lista) {
        LocalDate hoje = LocalDate.now();
        BigDecimal aberto = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        BigDecimal aVencer7 = BigDecimal.ZERO;
        BigDecimal pago = BigDecimal.ZERO;
        BigDecimal semClassificacao = BigDecimal.ZERO;
        int pendentes = 0;

        for (Obrigacao o : lista) {
            if (o.isCancelada()) {
                continue;
            }
            pago = pago.add(o.getTotalPago());
            aberto = aberto.add(o.getSaldo());
            if (o.estaVencida(hoje)) {
                vencido = vencido.add(o.getSaldo());
            } else if (!o.getVencimento().isAfter(hoje.plusDays(7))) {
                aVencer7 = aVencer7.add(o.getSaldo());
            }
            if (!o.pendencias().isEmpty()) {
                pendentes = pendentes + 1;
            }
            if (o.getItens().isEmpty()) {
                semClassificacao = semClassificacao.add(o.getValor());
            }
        }
        return new Resumo(aberto, vencido, aVencer7, pago, semClassificacao, pendentes);
    }

    /** Quanto cada área absorveu, separando custo direto de custo rateado. */
    public List<LinhaDeCusto> custoPorCentro(List<Obrigacao> lista) {
        Map<String, BigDecimal[]> soma = new LinkedHashMap<>();
        for (Obrigacao o : lista) {
            if (o.isCancelada()) {
                continue;
            }
            for (ItemDaObrigacao item : o.getItens()) {
                String area = item.getCentroDeCusto() == null
                        ? "sem centro de custo" : item.getCentroDeCusto().getNome();
                BigDecimal[] valores = soma.computeIfAbsent(area,
                        chave -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                if (item.veioDeRateio()) {
                    valores[1] = valores[1].add(item.getTotal());
                } else {
                    valores[0] = valores[0].add(item.getTotal());
                }
            }
        }
        List<LinhaDeCusto> linhas = new ArrayList<>();
        soma.forEach((area, valores) ->
                linhas.add(new LinhaDeCusto(area, valores[0], valores[1])));
        linhas.sort((a, b) -> b.total().compareTo(a.total()));
        return linhas;
    }

    // ------------------------------------------------------------- cadastro

    @Transactional
    public Obrigacao lancar(UUID favorecidoId, String descricao, TipoDeOperacao tipo,
                            LocalDate emissao, LocalDate competencia, LocalDate vencimento,
                            BigDecimal valor, UUID contaId, UUID empresaPagadoraId,
                            String pessoaRelacionada, UUID clienteId, UUID bemId,
                            String solicitante, String observacao, boolean rascunho,
                            OrigemDoRegistro origem, String origemReferencia) {
        Empresa empresa = contexto.exigirEmpresa();
        Obrigacao nova = new Obrigacao(empresa, obrigacoes.proximoNumero(empresa.getId()),
                favorecido(favorecidoId), descricao,
                vencimento == null ? LocalDate.now() : vencimento, contexto.autor());
        nova.ajustarDadosGerais(favorecido(favorecidoId), descricao,
                tipo == null ? TipoDeOperacao.DESPESA : tipo, emissao, competencia,
                vencimento == null ? LocalDate.now() : vencimento, valor, conta(contaId),
                empresaPagadora(empresaPagadoraId));
        nova.ajustarVinculos(pessoaRelacionada, cliente(clienteId), bem(bemId), solicitante,
                observacao);
        nova.ajustarOrigem(origem == null ? OrigemDoRegistro.MANUAL : origem, origemReferencia);
        nova.ajustarQualidade(rascunho ? QualidadeDoCadastro.RASCUNHO
                : QualidadeDoCadastro.COMPLETO);
        obrigacoes.save(nova);
        avisos.avisar(empresa.getId(), "CONTA_A_PAGAR_LANCADA");
        return nova;
    }

    @Transactional
    public void salvarDadosGerais(UUID id, UUID favorecidoId, String descricao,
                                  TipoDeOperacao tipo, LocalDate emissao, LocalDate competencia,
                                  LocalDate vencimento, BigDecimal valor, UUID contaId,
                                  UUID empresaPagadoraId, String pessoaRelacionada,
                                  UUID clienteId, UUID bemId, String solicitante,
                                  String observacao) {
        Obrigacao obrigacao = obrigacao(id);
        boolean mudouValor = valor != null && obrigacao.getValor().compareTo(valor) != 0;
        boolean mudouFavorecido = !obrigacao.getFavorecido().getId().equals(favorecidoId);

        obrigacao.ajustarDadosGerais(favorecido(favorecidoId), descricao, tipo, emissao,
                competencia, vencimento, valor, conta(contaId),
                empresaPagadora(empresaPagadoraId));
        obrigacao.ajustarVinculos(pessoaRelacionada, cliente(clienteId), bem(bemId), solicitante,
                observacao);

        if (mudouValor || mudouFavorecido) {
            // Quem aprovou aprovou aquele valor para aquela pessoa.
            obrigacao.invalidarAprovacao();
        }
        obrigacao.recalcularLiquidacao();
    }

    @Transactional
    public void marcarCompleta(UUID id) {
        obrigacao(id).ajustarQualidade(QualidadeDoCadastro.COMPLETO);
    }

    @Transactional
    public void cancelar(UUID id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Cancelar exige motivo.");
        }
        obrigacao(id).cancelar(motivo);
    }

    // ----------------------------------------------------------- composicao

    @Transactional
    public void adicionarItem(UUID id, String descricao, BigDecimal quantidade, BigDecimal valor,
                              UUID naturezaId, UUID centroId, String pessoa, UUID bemId) {
        Obrigacao obrigacao = obrigacao(id);
        int ordem = obrigacao.getItens().size() + 1;
        ItemDaObrigacao item = new ItemDaObrigacao(obrigacao, descricao, ordem);
        item.ajustar(descricao, quantidade, valor, natureza(naturezaId), centro(centroId),
                pessoa, bem(bemId), null, null, ordem);
        // O item entra pela obrigacao, que se encarrega de gravar.
        obrigacao.receber(item);
    }

    @Transactional
    public void removerItem(UUID id, UUID itemId) {
        Obrigacao obrigacao = obrigacao(id);
        obrigacao.getItens().removeIf(i -> i.getId().equals(itemId));
    }

    /**
     * Distribui o valor entre áreas, guardando o critério.
     *
     * A soma fecha exatamente com o valor da conta: a diferença de
     * arredondamento vai para a última linha, de propósito.
     */
    @Transactional
    public void ratear(UUID id, String descricao, UUID naturezaId, String criterio,
                       List<UUID> centrosEscolhidos, List<BigDecimal> percentuais) {
        Obrigacao obrigacao = obrigacao(id);
        if (centrosEscolhidos == null || centrosEscolhidos.isEmpty()) {
            throw new IllegalArgumentException("Escolha pelo menos uma área para o rateio.");
        }
        if (criterio == null || criterio.isBlank()) {
            throw new IllegalArgumentException(
                    "Rateio exige critério. Sem ele ninguém consegue conferir a divisão depois.");
        }
        BigDecimal soma = percentuais.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (soma.compareTo(new BigDecimal("100")) != 0) {
            throw new IllegalArgumentException("Os percentuais precisam somar 100.");
        }

        obrigacao.limparItens();
        BigDecimal total = obrigacao.getValor();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (int i = 0; i < centrosEscolhidos.size(); i++) {
            boolean ultima = i == centrosEscolhidos.size() - 1;
            BigDecimal percentual = percentuais.get(i);
            BigDecimal valor = ultima
                    ? total.subtract(acumulado)
                    : total.multiply(percentual)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            acumulado = acumulado.add(valor);

            ItemDaObrigacao item = new ItemDaObrigacao(obrigacao, descricao, i + 1);
            item.ajustar(descricao, BigDecimal.ONE, valor, natureza(naturezaId),
                    centro(centrosEscolhidos.get(i)), null, null, criterio, percentual, i + 1);
            obrigacao.receber(item);
        }
    }

    // ------------------------------------------------------------ aprovacao

    @Transactional
    public void pedirAprovacao(UUID id) {
        obrigacao(id).exigirAprovacao();
    }

    @Transactional
    public void aprovar(UUID id) {
        contexto.exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel.GESTOR);
        Obrigacao obrigacao = obrigacao(id);
        if (obrigacao.getQualidade() == QualidadeDoCadastro.RASCUNHO) {
            throw new IllegalStateException(
                    "Rascunho não vai para aprovação. Complete o cadastro antes.");
        }
        obrigacao.aprovar(contexto.autor());
    }

    @Transactional
    public void rejeitar(UUID id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Rejeitar exige motivo.");
        }
        obrigacao(id).rejeitar(contexto.autor(), motivo);
    }

    @Transactional
    public void marcarExecucao(UUID id, Execucao execucao) {
        Obrigacao obrigacao = obrigacao(id);
        if (execucao != Execucao.NAO_ENCAMINHADA && !obrigacao.podeIrParaPagamento()) {
            throw new IllegalStateException(
                    "Esta conta ainda não pode ir para pagamento. Confira aprovação e cadastro.");
        }
        obrigacao.ajustarExecucao(execucao);
    }

    @Transactional
    public void marcarConciliacao(UUID id, Conciliacao conciliacao) {
        obrigacao(id).ajustarConciliacao(conciliacao);
    }

    // ----------------------------------------------------------- pagamentos

    @Transactional
    public void pagar(UUID id, LocalDate quando, BigDecimal valor, UUID contaId, String forma,
                      BigDecimal juros, BigDecimal multa, BigDecimal desconto,
                      BigDecimal retencao, String observacao) {
        Obrigacao obrigacao = obrigacao(id);
        if (obrigacao.isCancelada()) {
            throw new IllegalStateException("Conta cancelada não recebe pagamento.");
        }
        if (!obrigacao.getAprovacao().liberaPagamento()) {
            throw new IllegalStateException(
                    "Esta conta espera aprovação. Aprove antes de registrar o pagamento.");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor do pagamento precisa ser maior que zero.");
        }
        if (valor.compareTo(obrigacao.getSaldo()) > 0) {
            throw new IllegalArgumentException("Pagamento maior que o saldo de "
                    + obrigacao.getSaldo() + ". Confira o valor.");
        }

        PagamentoDeObrigacao pagamento = new PagamentoDeObrigacao(obrigacao, quando, valor,
                conta(contaId), forma, contexto.autor());
        pagamento.ajustarEncargos(juros, multa, desconto, retencao, observacao);
        // O pagamento entra pela obrigacao, que se encarrega de gravar.
        obrigacao.receber(pagamento);
        avisos.avisar(obrigacao.getEmpresa().getId(), "CONTA_A_PAGAR_PAGA");
    }

    @Transactional
    public void estornar(UUID id, UUID pagamentoId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Estorno exige motivo.");
        }
        Obrigacao obrigacao = obrigacao(id);
        obrigacao.getPagamentos().stream()
                .filter(p -> p.getId().equals(pagamentoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado."))
                .estornar(motivo);
        obrigacao.recalcularLiquidacao();
    }

    // ------------------------------------------------ parcelas e recorrencia

    /**
     * Divide uma contratação em parcelas.
     *
     * A soma das parcelas fecha com o total: a sobra dos centavos vai para a
     * primeira, e não some.
     */
    @Transactional
    public List<Obrigacao> parcelar(UUID favorecidoId, String descricao, TipoDeOperacao tipo,
                                    BigDecimal total, int vezes, LocalDate primeiroVencimento,
                                    UUID naturezaId, UUID centroId) {
        if (vezes < 2) {
            throw new IllegalArgumentException("Parcelamento precisa de pelo menos duas vezes.");
        }
        UUID grupo = UUID.randomUUID();
        BigDecimal cada = total.divide(new BigDecimal(vezes), 2, RoundingMode.DOWN);
        BigDecimal sobra = total.subtract(cada.multiply(new BigDecimal(vezes)));
        List<Obrigacao> criadas = new ArrayList<>();

        for (int i = 1; i <= vezes; i++) {
            BigDecimal valor = i == 1 ? cada.add(sobra) : cada;
            LocalDate vencimento = primeiroVencimento.plusMonths(i - 1L);
            Obrigacao parcela = lancar(favorecidoId, descricao + " · parcela " + i + "/" + vezes,
                    tipo, LocalDate.now(), vencimento.withDayOfMonth(1), vencimento, valor,
                    null, null, null, null, null, contexto.autor(), null, false,
                    OrigemDoRegistro.MANUAL, null);
            parcela.marcarComoParcela(grupo, i, vezes);
            if (naturezaId != null || centroId != null) {
                ItemDaObrigacao item = new ItemDaObrigacao(parcela, descricao, 1);
                item.ajustar(descricao, BigDecimal.ONE, valor, natureza(naturezaId),
                        centro(centroId), null, null, null, null, 1);
                parcela.receber(item);
            }
            criadas.add(parcela);
        }
        return criadas;
    }

    public List<Recorrencia> recorrencias() {
        return recorrencias.findByEmpresaIdOrderByDescricao(contexto.exigirEmpresaId());
    }

    @Transactional
    public Recorrencia cadastrarRecorrencia(String descricao, UUID favorecidoId, UUID naturezaId,
                                            UUID centroId, TipoDeOperacao tipo,
                                            br.com.itia.financeiro.dominio.Periodicidade periodicidade,
                                            int diaVencimento, BigDecimal valorPrevisto,
                                            boolean valorVariavel, LocalDate inicio,
                                            LocalDate fim) {
        Recorrencia nova = new Recorrencia(contexto.exigirEmpresa(), descricao,
                favorecido(favorecidoId), contexto.autor());
        nova.ajustar(descricao, favorecido(favorecidoId), natureza(naturezaId), centro(centroId),
                tipo, periodicidade, diaVencimento, valorPrevisto, valorVariavel, inicio, fim,
                true);
        return recorrencias.save(nova);
    }

    /**
     * Gera as contas do mês a partir das recorrências.
     *
     * Roda quantas vezes quiser: recorrência que já gerou naquela competência
     * é pulada. Valor variável nasce como rascunho, para ninguém confundir
     * previsão com conta confirmada.
     */
    @Transactional
    public int gerarRecorrentes(YearMonth competencia) {
        LocalDate primeiroDia = competencia.atDay(1);
        int criadas = 0;

        for (Recorrencia recorrencia : recorrencias()) {
            if (!recorrencia.valeNoMes(primeiroDia)) {
                continue;
            }
            if (!obrigacoes.findByRecorrenciaIdAndCompetencia(recorrencia.getId(), primeiroDia)
                    .isEmpty()) {
                continue;
            }
            int dia = Math.min(recorrencia.getDiaVencimento(), competencia.lengthOfMonth());
            Obrigacao nova = lancar(recorrencia.getFavorecido().getId(),
                    recorrencia.getDescricao() + " · " + competencia,
                    recorrencia.getTipoOperacao(), primeiroDia, primeiroDia,
                    competencia.atDay(dia),
                    recorrencia.getValorPrevisto() == null
                            ? BigDecimal.ZERO : recorrencia.getValorPrevisto(),
                    null, null, null, null, null, contexto.autor(), null,
                    recorrencia.isValorVariavel(), OrigemDoRegistro.RECORRENCIA,
                    "recorrência " + recorrencia.getDescricao());
            nova.veioDaRecorrencia(recorrencia.getId());

            if (recorrencia.getNatureza() != null || recorrencia.getCentroDeCusto() != null) {
                ItemDaObrigacao item = new ItemDaObrigacao(nova, recorrencia.getDescricao(), 1);
                item.ajustar(recorrencia.getDescricao(), BigDecimal.ONE, nova.getValor(),
                        recorrencia.getNatureza(), recorrencia.getCentroDeCusto(), null, null,
                        null, null, 1);
                nova.receber(item);
            }
            criadas = criadas + 1;
        }
        if (criadas > 0) {
            avisos.avisar(contexto.exigirEmpresaId(), "CONTAS_RECORRENTES_GERADAS");
        }
        return criadas;
    }

    // --------------------------------------------------------- cadastros base

    /**
     * Deixa a empresa nova pronta para usar.
     *
     * Toda empresa começa com o próprio plano gerencial e os próprios centros
     * de custo, separados das outras. Nada é compartilhado entre empresas.
     */
    @Transactional
    public void prepararEmpresa() {
        Empresa empresa = contexto.exigirEmpresa();
        if (naturezas.countByEmpresaId(empresa.getId()) == 0) {
            criarPlanoInicial(empresa);
        }
        if (centros.countByEmpresaId(empresa.getId()) == 0) {
            for (String nome : List.of("Fiscal", "Contábil", "Departamento Pessoal",
                    "Societário", "Comercial", "Administrativo", "Financeiro", "Diretoria")) {
                centros.save(new CentroDeCusto(empresa, nome, null));
            }
        }
    }

    private void criarPlanoInicial(Empresa empresa) {
        Map<String, List<String>> plano = new LinkedHashMap<>();
        plano.put("Pessoal", List.of("Salários", "Vale-transporte", "Vale-alimentação",
                "Encargos", "Férias", "Décimo terceiro", "Comissões"));
        plano.put("Ocupação", List.of("Aluguel", "Condomínio", "Energia", "Água", "Limpeza"));
        plano.put("Tecnologia", List.of("Sistemas", "Licenças", "Internet",
                "Manutenção de equipamentos"));
        plano.put("Administrativo", List.of("Material de escritório", "Serviços de apoio",
                "Despesas diversas"));
        plano.put("Comercial", List.of("Publicidade", "Comissões comerciais", "Viagens"));
        plano.put("Financeiro", List.of("Tarifas bancárias", "Juros", "Encargos bancários"));

        int codigo = 1;
        for (Map.Entry<String, List<String>> grupo : plano.entrySet()) {
            Natureza pai = naturezas.save(new Natureza(empresa, String.format("%02d", codigo),
                    grupo.getKey(), br.com.itia.financeiro.dominio.GrupoDeNatureza.DESPESA, null));
            int filho = 1;
            for (String nome : grupo.getValue()) {
                naturezas.save(new Natureza(empresa,
                        String.format("%02d.%02d", codigo, filho), nome,
                        br.com.itia.financeiro.dominio.GrupoDeNatureza.DESPESA, pai));
                filho = filho + 1;
            }
            codigo = codigo + 1;
        }
        // Os grupos que nao sao custo do mes ficam separados de proposito.
        naturezas.save(new Natureza(empresa, "90", "Tributos",
                br.com.itia.financeiro.dominio.GrupoDeNatureza.TRIBUTO, null));
        naturezas.save(new Natureza(empresa, "91", "Aquisições patrimoniais",
                br.com.itia.financeiro.dominio.GrupoDeNatureza.AQUISICAO, null));
        naturezas.save(new Natureza(empresa, "92", "Movimentações com sócios",
                br.com.itia.financeiro.dominio.GrupoDeNatureza.SOCIOS, null));
        naturezas.save(new Natureza(empresa, "93", "Financiamentos",
                br.com.itia.financeiro.dominio.GrupoDeNatureza.FINANCIAMENTO, null));
        naturezas.save(new Natureza(empresa, "94", "Transferências entre contas próprias",
                br.com.itia.financeiro.dominio.GrupoDeNatureza.TRANSFERENCIA, null));
    }

    public List<Natureza> naturezasAtivas() {
        return naturezas.findByEmpresaIdAndAtivoTrueOrderByCodigo(contexto.exigirEmpresaId());
    }

    public List<Natureza> naturezasTodas() {
        return naturezas.findByEmpresaIdOrderByCodigo(contexto.exigirEmpresaId());
    }

    public List<CentroDeCusto> centrosAtivos() {
        return centros.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<CentroDeCusto> centrosTodos() {
        return centros.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public List<Favorecido> favorecidosAtivos() {
        return favorecidos.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<ContaFinanceira> contasAtivas() {
        return contas.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<Bem> bensAtivos() {
        return bens.findByEmpresaIdAndAtivoTrueOrderByNumeroPatrimonial(
                contexto.exigirEmpresaId());
    }

    public List<Empresa> empresasDoGrupo() {
        return empresas.findByAtivaTrueOrderByNome();
    }

    @Transactional
    public Favorecido cadastrarFavorecido(String nome, String documento, String tipo,
                                          String chavePix) {
        Favorecido novo = new Favorecido(contexto.exigirEmpresa(), nome, documento, tipo);
        novo.ajustar(nome, documento, tipo, chavePix, null, null, null, null, true);
        return favorecidos.save(novo);
    }

    @Transactional
    public CentroDeCusto cadastrarCentro(String nome, String descricao) {
        return centros.save(new CentroDeCusto(contexto.exigirEmpresa(), nome, descricao));
    }

    @Transactional
    public Natureza cadastrarNatureza(String codigo, String nome,
                                      br.com.itia.financeiro.dominio.GrupoDeNatureza grupo,
                                      UUID paiId) {
        return cadastrarNatureza(codigo, nome, grupo, paiId, false);
    }

    @Transactional
    public Natureza cadastrarNatureza(String codigo, String nome,
                                      br.com.itia.financeiro.dominio.GrupoDeNatureza grupo,
                                      UUID paiId, boolean livroCaixa) {
        Natureza nova = new Natureza(contexto.exigirEmpresa(), codigo, nome, grupo,
                natureza(paiId));
        nova.marcarLivroCaixa(livroCaixa);
        return naturezas.save(nova);
    }

    @Transactional
    public ContaFinanceira cadastrarConta(String nome, String tipo, String banco, String agencia,
                                          String numero, String titular, BigDecimal saldoInicial) {
        ContaFinanceira nova = new ContaFinanceira(contexto.exigirEmpresa(), nome, titular);
        nova.ajustar(nome, tipo, banco, agencia, numero, titular, saldoInicial, true);
        return contas.save(nova);
    }

    @Transactional
    public Bem cadastrarBem(String numeroPatrimonial, String descricao, String tipo,
                            UUID centroId, String responsavel, LocalDate aquisicao,
                            BigDecimal valor) {
        Bem novo = new Bem(contexto.exigirEmpresa(), numeroPatrimonial, descricao);
        novo.ajustar(descricao, tipo, null, null, null, centro(centroId), responsavel, null,
                "EM_USO", aquisicao, valor, null, true);
        return bens.save(novo);
    }

    // ------------------------------------------------------------------ apoio

    private Favorecido favorecido(UUID id) {
        return favorecidos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Favorecido não encontrado."));
    }

    private Natureza natureza(UUID id) {
        if (id == null) {
            return null;
        }
        return naturezas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Natureza não encontrada."));
    }

    private CentroDeCusto centro(UUID id) {
        if (id == null) {
            return null;
        }
        return centros.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
    }

    private ContaFinanceira conta(UUID id) {
        if (id == null) {
            return null;
        }
        return contas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
    }

    private Bem bem(UUID id) {
        if (id == null) {
            return null;
        }
        return bens.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Bem não encontrado."));
    }

    private br.com.itia.financeiro.dominio.Pagador cliente(UUID id) {
        if (id == null) {
            return null;
        }
        return clientes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    /** A empresa do grupo que paga por esta, quando for o caso. */
    private Empresa empresaPagadora(UUID id) {
        if (id == null) {
            return null;
        }
        return empresas.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
    }

    public record Resumo(BigDecimal aberto, BigDecimal vencido, BigDecimal aVencer7,
                         BigDecimal pago, BigDecimal semClassificacao, int pendentes) {
    }

    public record LinhaDeCusto(String area, BigDecimal direto, BigDecimal rateado) {
        public BigDecimal total() {
            return direto.add(rateado);
        }
    }
}
