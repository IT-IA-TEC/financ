package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Natureza;
import br.com.itia.financeiro.dominio.ItemDaObrigacao;
import br.com.itia.financeiro.dominio.CentroDeCusto;
import br.com.itia.financeiro.dominio.Bem;
import br.com.itia.financeiro.dominio.Aprovacao;
import br.com.itia.financeiro.dominio.ContaFinanceira;
import br.com.itia.financeiro.dominio.Bem;
import br.com.itia.financeiro.dominio.Conciliacao;
import br.com.itia.financeiro.dominio.ContaFinanceira;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Execucao;
import br.com.itia.financeiro.dominio.Favorecido;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.TipoDeOperacao;
import br.com.itia.financeiro.dominio.PagamentoDeObrigacao;
import br.com.itia.financeiro.servico.ContasAPagar;
import br.com.itia.financeiro.servico.DocumentoServico;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A ficha de uma conta a pagar: o que é, quem aprovou, o que já foi pago e o
 * que ainda falta.
 *
 * Conta que espera aprovação não recebe pagamento: a trava é do sistema, não da
 * tela.
 */
@Component
public class TelaObrigacao implements TelaDeUmSo {

    private static final java.time.format.DateTimeFormatter CRIADO =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ContasAPagar contas;
    private final DocumentoServico documentos;
    private final Janela janela;

    private UUID qual;

    public TelaObrigacao(ContasAPagar contas, DocumentoServico documentos, @Lazy Janela janela) {
        this.contas = contas;
        this.documentos = documentos;
        this.janela = janela;
    }

    @Override
    public void escolher(UUID id) {
        this.qual = id;
    }

    @Override
    public String secao() {
        return "pagar";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        Obrigacao conta = contas.obrigacao(qual);

        Label situacao = new Label(conta.isCancelada() ? "cancelada"
                : conta.getSaldo().signum() == 0 ? "paga"
                : conta.estaVencida(hoje) ? "vencida há " + conta.diasDeAtraso(hoje) + " dias"
                : "em aberto");
        situacao.getStyleClass().addAll("marca-situacao", conta.isCancelada() ? "s-cancelado"
                : conta.getSaldo().signum() == 0 ? "s-pago"
                : conta.estaVencida(hoje) ? "s-vencido" : "");

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("conta nº " + conta.getNumero(),
                conta.getFavorecido() == null ? conta.getDescricao()
                        : conta.getFavorecido().getNome(),
                conta.getDescricao() + " · vence em " + Pecas.data(conta.getVencimento()),
                situacao, Pecas.botaoVazado("Voltar", () -> janela.ir(TelaPagar.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Valor", Pecas.dinheiro(conta.getValor()),
                        conta.getTipoOperacao() == null ? "" : conta.getTipoOperacao().name().toLowerCase(),
                        true, false),
                Pecas.quadro("Já pago", Pecas.dinheiro(conta.getTotalPago()),
                        conta.getPagamentos().size() + " pagamento(s)"),
                Pecas.quadro("Saldo", Pecas.dinheiro(conta.getSaldo()), "o que ainda falta",
                        false, conta.getSaldo().signum() > 0),
                Pecas.quadro("Aprovação", conta.getAprovacao().getRotulo(),
                        conta.getAprovadaPor() != null ? "por " + conta.getAprovadaPor()
                                : conta.getAprovacao().getComoFunciona()),
                Pecas.quadro("Cadastro", conta.getQualidade().getRotulo(),
                        conta.getQualidade().getComoFunciona()),
                Pecas.quadro("No banco", conta.getExecucao().getRotulo(),
                        conta.getExecucao().getComoFunciona()),
                Pecas.quadro("Liquidação", conta.getLiquidacao().getRotulo(),
                        conta.estaVencida(hoje)
                                ? "vencida há " + conta.diasDeAtraso(hoje) + " dias"
                                : "vence em " + Pecas.data(conta.getVencimento())),
                Pecas.quadro("Conciliação", conta.getConciliacao().getRotulo(),
                        conta.getConciliacao().getComoFunciona()))));


        List<String> pendencias = conta.pendencias();
        if (!pendencias.isEmpty()) {
            tela.getChildren().add(Pecas.secao("Falta resolver"));
            VBox caixa = new VBox(6);
            caixa.getStyleClass().add("caixa");
            for (String pendencia : pendencias) {
                Label linha = new Label("· " + pendencia);
                linha.setWrapText(true);
                caixa.getChildren().add(linha);
            }
            tela.getChildren().add(caixa);
        }

        tela.getChildren().add(Pecas.secao("O que dá para fazer agora"));
        tela.getChildren().add(acoes(conta));

        tela.getChildren().add(Pecas.secao("Dados gerais"));
        tela.getChildren().add(dadosGerais(conta));

        tela.getChildren().add(Pecas.cabecalho("composição", "Composição do valor",
                "Cada item diz o que está sendo pago e de qual área sai o dinheiro.",
                Pecas.botao("Adicionar à composição", () -> janelaDoItem(conta))));
        tela.getChildren().add(Tabela.de(conta.getItens())
                .coluna("O que é", ItemDaObrigacao::getDescricao, 2.4)
                .coluna("Natureza", i -> i.getNatureza() == null ? ""
                        : i.getNatureza().getNome(), 1.4)
                .coluna("Centro de custo", i -> i.getCentroDeCusto() == null ? ""
                        : i.getCentroDeCusto().getNome(), 1.4)
                .coluna("Pessoa", ItemDaObrigacao::getPessoa, 1.2)
                .coluna("Bem", i -> i.getBem() == null ? "" : i.getBem().getDescricao(), 1.2)
                .valor("Quantidade", i -> Pecas.numero(i.getQuantidade()))
                .valor("Valor", i -> Pecas.numero(i.getTotal()))
                .coluna("Critério do rateio", ItemDaObrigacao::getCriterioRateio, 1.2)
                .aoClicar(item -> tirarItem(conta, item))
                .quandoVazia("Esta conta não foi detalhada em itens.")
                .montar());

        Label somaDosItens = new Label("Soma dos itens: "
                + Pecas.dinheiro(conta.getTotalDosItens())
                + " · valor da conta: " + Pecas.dinheiro(conta.getValor()));
        somaDosItens.getStyleClass().add("dica");
        tela.getChildren().add(somaDosItens);

        Label comoTirar = new Label("Clique num item para tirar da composição.");
        comoTirar.getStyleClass().add("dica");
        tela.getChildren().add(comoTirar);

        tela.getChildren().add(Pecas.secao("Ratear entre áreas"));
        tela.getChildren().add(Pecas.caixa(
                new Label("O rateio divide o valor da conta entre centros de custo, "
                        + "por percentual."),
                new HBox(Pecas.botaoVazado("Aplicar rateio", () -> janelaDoRateio(conta)))));

        tela.getChildren().add(Pecas.secao("Pagamentos"));
        tela.getChildren().add(Tabela.de(conta.getPagamentos())
                .coluna("Pago em", p -> Pecas.data(p.getPagoEm()))
                .valor("Valor", p -> Pecas.numero(p.getValor()))
                .valor("Juros e multa", p -> Pecas.numero(p.getJuros().add(p.getMulta())))
                .valor("Desconto e retenção",
                        p -> Pecas.numero(p.getDesconto().add(p.getRetencao())))
                .valor("Saiu do banco", p -> Pecas.numero(p.getSaidaDoBanco()))
                .coluna("Conta", p -> p.getConta() == null ? "" : p.getConta().getNome())
                .coluna("Forma", PagamentoDeObrigacao::getForma)
                .comMarca(p -> p.isEstornado() ? "estornado" : "válido",
                        p -> p.isEstornado() ? "s-cancelado" : "s-pago")
                .aoClicar(p -> estornar(conta, p))
                .quandoVazia("Nenhum pagamento registrado nesta conta.")
                .montar());

        Label comoEstornar = new Label("Clique num pagamento para estornar.");
        comoEstornar.getStyleClass().add("dica");
        tela.getChildren().add(comoEstornar);

        if (!conta.isCancelada() && conta.getSaldo().signum() > 0
                && conta.getAprovacao().liberaPagamento()) {
            tela.getChildren().add(Pecas.secao("Registrar pagamento"));
            tela.getChildren().add(formularioDePagamento(conta, hoje));
        }

        tela.getChildren().add(Pecas.secao("Andamento no banco e conciliação"));
        tela.getChildren().add(andamento(conta));

        tela.getChildren().add(Pecas.cabecalho("documentos e origem",
                "Documentos e origem",
                "De onde esta conta veio e o que está anexado nela.",
                Pecas.botaoVazado("Anexar documento", () -> anexar(conta))));
        tela.getChildren().add(origem(conta));
        return tela;
    }

    /** Aprovar, rejeitar e cancelar ficam juntos, cada um com o seu peso. */
    private FlowPane acoes(Obrigacao conta) {
        FlowPane linha = new FlowPane(12, 12);
        if (conta.isCancelada()) {
            Label nada = new Label("Conta cancelada. Nada mais acontece nela.");
            nada.getStyleClass().add("dica");
            linha.getChildren().add(nada);
            return linha;
        }
        if (!conta.pendencias().isEmpty()) {
            linha.getChildren().add(Pecas.botaoVazado("Marcar como completa", () -> {
                contas.marcarCompleta(conta.getId());
                janela.avisar("Cadastro marcado como completo.");
                janela.ir(TelaObrigacao.class, conta.getId());
            }));
        }
        if (conta.getAprovacao() == Aprovacao.NAO_EXIGIDA && conta.getSaldo().signum() > 0) {
            linha.getChildren().add(Pecas.botao("Enviar para aprovação", () -> {
                contas.pedirAprovacao(conta.getId());
                janela.avisar("Aprovação pedida.");
                janela.ir(TelaObrigacao.class, conta.getId());
            }));
        }
        if (conta.getAprovacao() == Aprovacao.PENDENTE) {
            linha.getChildren().add(Pecas.botao("Aprovar", () -> {
                contas.aprovar(conta.getId());
                janela.avisar("Conta aprovada.");
                janela.ir(TelaObrigacao.class, conta.getId());
            }));
            linha.getChildren().add(Pecas.botaoPerigo("Rejeitar",
                    () -> pedirMotivo("Rejeitar conta", "por que está rejeitando",
                            motivo -> {
                                contas.rejeitar(conta.getId(), motivo);
                                janela.avisar("Conta rejeitada, com o motivo guardado.");
                                janela.ir(TelaObrigacao.class, conta.getId());
                            })));
        }
        if (conta.getTotalPago().signum() == 0) {
            linha.getChildren().add(Pecas.botaoPerigo("Cancelar conta",
                    () -> pedirMotivo("Cancelar conta", "por que está cancelando",
                            motivo -> {
                                contas.cancelar(conta.getId(), motivo);
                                janela.avisar("Conta cancelada, com o motivo guardado.");
                                janela.ir(TelaObrigacao.class, conta.getId());
                            })));
        }
        if (linha.getChildren().isEmpty()) {
            Label nada = new Label("Nada pendente nesta conta.");
            nada.getStyleClass().add("dica");
            linha.getChildren().add(nada);
        }
        return linha;
    }

    /**
     * Os dados gerais da conta, abertos para ajuste. Mudar o valor ou o
     * favorecido derruba a aprovação: quem aprovou aprovou aquele valor para
     * aquela pessoa.
     */
    private VBox dadosGerais(Obrigacao conta) {
        ComboBox<Favorecido> favorecido = new ComboBox<>();
        favorecido.getItems().addAll(contas.favorecidosAtivos());
        favorecido.setConverter(nome(Favorecido::getNome));
        favorecido.getSelectionModel().select(contas.favorecidosAtivos().stream()
                .filter(f -> conta.getFavorecido() != null
                        && f.getId().equals(conta.getFavorecido().getId()))
                .findFirst().orElse(null));
        favorecido.setMaxWidth(Double.MAX_VALUE);

        TextField descricao = new TextField(conta.getDescricao());

        ComboBox<TipoDeOperacao> tipo = new ComboBox<>();
        tipo.getItems().addAll(TipoDeOperacao.values());
        tipo.getSelectionModel().select(conta.getTipoOperacao());
        tipo.setMaxWidth(Double.MAX_VALUE);

        DatePicker emissao = new DatePicker(conta.getEmissao());
        DatePicker competencia = new DatePicker(conta.getCompetencia());
        DatePicker vencimento = new DatePicker(conta.getVencimento());
        emissao.setMaxWidth(Double.MAX_VALUE);
        competencia.setMaxWidth(Double.MAX_VALUE);
        vencimento.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField(Pecas.numero(conta.getValor()));

        ComboBox<ContaFinanceira> banco = new ComboBox<>();
        banco.getItems().add(null);
        banco.getItems().addAll(contas.contasAtivas());
        banco.setConverter(nomeOuPadrao(ContaFinanceira::getNome, "definir na hora de pagar"));
        banco.getSelectionModel().select(conta.getConta());
        banco.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Empresa> pagadora = new ComboBox<>();
        pagadora.getItems().add(null);
        pagadora.getItems().addAll(contas.empresasDoGrupo());
        pagadora.setConverter(nomeOuPadrao(Empresa::getNome, "a própria empresa"));
        pagadora.getSelectionModel().select(conta.getEmpresaPagadora());
        pagadora.setMaxWidth(Double.MAX_VALUE);

        TextField pessoa = new TextField(conta.getPessoaRelacionada());

        ComboBox<Bem> bem = new ComboBox<>();
        bem.getItems().add(null);
        bem.getItems().addAll(contas.bensAtivos());
        bem.setConverter(nomeOuPadrao(Bem::getDescricao, "nenhum"));
        bem.getSelectionModel().select(conta.getBem());
        bem.setMaxWidth(Double.MAX_VALUE);

        TextField solicitante = new TextField(conta.getSolicitante());
        TextField observacao = new TextField(conta.getObservacao());

        Label recado = new Label("Mudar o valor ou o favorecido derruba a aprovação: quem "
                + "aprovou aprovou aquele valor para aquela pessoa.");
        recado.getStyleClass().add("dica");
        recado.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Favorecido", favorecido),
                Pecas.campo("Descrição", descricao), Pecas.campo("Tipo de operação", tipo));
        HBox linha2 = new HBox(16, Pecas.campo("Emissão", emissao),
                Pecas.campo("Competência", competencia),
                Pecas.campo("Vencimento", vencimento),
                Pecas.campo("Valor devido (R$)", valor));
        HBox linha3 = new HBox(16, Pecas.campo("Conta financeira", banco),
                Pecas.campo("Quem paga", pagadora),
                Pecas.campo("Pessoa relacionada", pessoa));
        HBox linha4 = new HBox(16, Pecas.campo("Bem relacionado", bem),
                Pecas.campo("Solicitante", solicitante),
                Pecas.campo("Observação", observacao));
        for (HBox linha : List.of(linha1, linha2, linha3, linha4)) {
            linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        }

        return Pecas.caixa(recado, linha1, linha2, linha3, linha4,
                new HBox(Pecas.botao("Salvar dados", () -> {
                    if (favorecido.getValue() == null || vencimento.getValue() == null) {
                        janela.reclamar("Escolha o favorecido e o vencimento.");
                        return;
                    }
                    contas.salvarDadosGerais(conta.getId(), favorecido.getValue().getId(),
                            descricao.getText(), tipo.getValue(), emissao.getValue(),
                            competencia.getValue(), vencimento.getValue(),
                            TelaTituloNovo.dinheiro(valor.getText()),
                            banco.getValue() == null ? null : banco.getValue().getId(),
                            pagadora.getValue() == null ? null : pagadora.getValue().getId(),
                            pessoa.getText(), null,
                            bem.getValue() == null ? null : bem.getValue().getId(),
                            solicitante.getText(), observacao.getText());
                    janela.avisar("Dados salvos.");
                    janela.ir(TelaObrigacao.class, conta.getId());
                })));
    }

    /** Andamento no banco, conciliação e o cancelamento, cada um separado. */
    private VBox andamento(Obrigacao conta) {
        Label recado = new Label("Encaminhar não comprova pagamento, e agendar no banco também "
                + "não. Por isso estes controles são separados do valor pago.");
        recado.getStyleClass().add("dica");
        recado.setWrapText(true);

        ComboBox<Execucao> execucao = new ComboBox<>();
        execucao.getItems().addAll(Execucao.values());
        execucao.getSelectionModel().select(conta.getExecucao());
        execucao.setMaxWidth(Double.MAX_VALUE);
        execucao.setOnAction(acao -> {
            contas.marcarExecucao(conta.getId(), execucao.getValue());
            janela.avisar("Andamento no banco atualizado.");
            janela.ir(TelaObrigacao.class, conta.getId());
        });

        ComboBox<Conciliacao> conciliacao = new ComboBox<>();
        conciliacao.getItems().addAll(Conciliacao.values());
        conciliacao.getSelectionModel().select(conta.getConciliacao());
        conciliacao.setMaxWidth(Double.MAX_VALUE);
        conciliacao.setOnAction(acao -> {
            contas.marcarConciliacao(conta.getId(), conciliacao.getValue());
            janela.avisar("Conciliação atualizada.");
            janela.ir(TelaObrigacao.class, conta.getId());
        });

        HBox linha = new HBox(16, Pecas.campo("Andamento no banco", execucao),
                Pecas.campo("Conciliação", conciliacao));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        return Pecas.caixa(recado, linha);
    }

    /** De onde a conta veio e os documentos anexados nela. */
    private VBox origem(Obrigacao conta) {
        Label deOndeVeio = new Label("De onde veio: " + conta.getOrigem().getRotulo());
        Label referencia = new Label("Referência da origem: "
                + (conta.getOrigemReferencia() == null ? "sem referência"
                        : conta.getOrigemReferencia()));
        Label quem = new Label("Registrado por " + conta.getCriadoPor()
                + (conta.getCriadoEm() == null ? ""
                        : " em " + conta.getCriadoEm().format(CRIADO)));
        deOndeVeio.setWrapText(true);
        referencia.setWrapText(true);
        quem.setWrapText(true);

        VBox caixa = new VBox(6, deOndeVeio, referencia, quem);
        caixa.getStyleClass().add("caixa");

        List<Documento> anexos = documentos.daObrigacao(conta.getId());
        if (!anexos.isEmpty()) {
            caixa.getChildren().add(Tabela.de(anexos)
                    .coluna("Arquivo", Documento::getNomeArquivo, 2)
                    .coluna("Tipo", Documento::getTipo)
                    .coluna("Quando", d -> d.getAnexadoEm() == null ? ""
                            : d.getAnexadoEm().format(CRIADO))
                    .quandoVazia("Nenhum documento anexado.")
                    .montar());
        }
        return caixa;
    }

    private void anexar(Obrigacao conta) {
        javafx.stage.FileChooser escolher = new javafx.stage.FileChooser();
        escolher.setTitle("Documento desta conta");
        java.io.File arquivo = escolher.showOpenDialog(janela.palco());
        if (arquivo == null) {
            return;
        }
        try {
            byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo.toPath());
            Documento guardado = documentos.anexar(
                    new br.com.itia.financeiro.dominio.ArquivoRecebido(arquivo.getName(),
                            conteudo), null, null, null, "NOTA", null, null);
            documentos.vincularAObrigacao(guardado, conta.getId());
            janela.avisar("Documento anexado.");
            janela.ir(TelaObrigacao.class, conta.getId());
        } catch (java.io.IOException naoLeu) {
            janela.reclamar("Não deu para ler o arquivo: " + naoLeu.getMessage());
        }
    }

    private <T> javafx.util.StringConverter<T> nome(
            java.util.function.Function<T, String> comoChama) {
        return nomeOuPadrao(comoChama, "");
    }

    private <T> javafx.util.StringConverter<T> nomeOuPadrao(
            java.util.function.Function<T, String> comoChama, String quandoNulo) {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(T qual) {
                return qual == null ? quandoNulo : comoChama.apply(qual);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        };
    }

    private void estornar(Obrigacao conta, PagamentoDeObrigacao pagamento) {
        if (pagamento.isEstornado()) {
            janela.reclamar("Este pagamento já foi estornado.");
            return;
        }
        pedirMotivo("Estornar pagamento", "motivo do estorno", motivo -> {
            contas.estornar(conta.getId(), pagamento.getId(), motivo);
            janela.avisar("Pagamento estornado, com o motivo guardado.");
            janela.ir(TelaObrigacao.class, conta.getId());
        });
    }

    private VBox formularioDePagamento(Obrigacao conta, LocalDate hoje) {
        TextField valor = new TextField(Pecas.numero(conta.getSaldo()));
        DatePicker quando = new DatePicker(hoje);
        quando.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ContaFinanceira> banco = new ComboBox<>();
        banco.getItems().add(null);
        banco.getItems().addAll(contas.contasAtivas());
        banco.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContaFinanceira qual) {
                return qual == null ? "não definida" : qual.getNome();
            }

            @Override
            public ContaFinanceira fromString(String texto) {
                return null;
            }
        });
        banco.getSelectionModel().selectFirst();
        banco.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> forma = new ComboBox<>();
        forma.getItems().addAll("PIX", "TRANSFERENCIA", "BOLETO", "DINHEIRO", "CARTAO");
        forma.getSelectionModel().selectFirst();
        forma.setMaxWidth(Double.MAX_VALUE);

        TextField juros = new TextField("0,00");
        TextField multa = new TextField("0,00");
        TextField desconto = new TextField("0,00");
        TextField retencao = new TextField("0,00");

        HBox primeira = new HBox(16,
                Pecas.campo("Valor pago (R$)", valor),
                Pecas.campo("Pago em", quando),
                Pecas.campo("Conta usada", banco),
                Pecas.campo("Forma", forma));
        HBox segunda = new HBox(16,
                Pecas.campo("Juros (R$)", juros),
                Pecas.campo("Multa (R$)", multa),
                Pecas.campo("Desconto (R$)", desconto),
                Pecas.campo("Retenção (R$)", retencao));
        primeira.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));
        segunda.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        return Pecas.caixa(primeira, segunda, new HBox(Pecas.botao("Registrar pagamento",
                () -> pagar(conta.getId(), valor.getText(), quando.getValue(), banco.getValue(),
                        forma.getValue(), juros.getText(), multa.getText(),
                        desconto.getText(), retencao.getText()))));
    }

    private void pagar(UUID id, String valor, LocalDate quando, ContaFinanceira banco,
                       String forma, String juros, String multa, String desconto,
                       String retencao) {
        BigDecimal quanto = TelaTituloNovo.dinheiro(valor);
        if (quanto == null || quanto.signum() <= 0) {
            janela.reclamar("Escreva o valor pago, maior que zero.");
            return;
        }
        contas.pagar(id, quando, quanto, banco == null ? null : banco.getId(), forma,
                ouZero(juros), ouZero(multa), ouZero(desconto), ouZero(retencao), null);
        janela.avisar("Pagamento registrado.");
        janela.ir(TelaObrigacao.class, id);
    }

    private BigDecimal ouZero(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** Acrescenta um item na composição do valor da conta. */
    private void janelaDoItem(Obrigacao conta) {
        TextField descricao = new TextField();
        descricao.setPromptText("o que está sendo pago");
        TextField quantidade = new TextField("1");
        TextField valor = new TextField("0,00");
        TextField pessoa = new TextField();
        pessoa.setPromptText("de quem é este item");

        ComboBox<Natureza> natureza = escolha(contas.naturezasAtivas(), Natureza::getNome);
        ComboBox<CentroDeCusto> centro = escolha(contas.centrosAtivos(), CentroDeCusto::getNome);
        ComboBox<Bem> bem = escolha(contas.bensAtivos(), Bem::getDescricao);

        HBox linha1 = new HBox(16, Pecas.campo("O que é", descricao),
                Pecas.campo("Quantidade", quantidade), Pecas.campo("Valor (R$)", valor));
        HBox linha2 = new HBox(16, Pecas.campo("Natureza", natureza),
                Pecas.campo("Centro de custo", centro), Pecas.campo("Pessoa", pessoa),
                Pecas.campo("Bem", bem));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Adicionar à composição",
                        "A soma dos itens não precisa bater com o valor da conta, "
                                + "mas a diferença aparece na tela.")
                .com(linha1, linha2)
                .acao("Adicionar à composição", () -> {
                    if (descricao.getText().isBlank()) {
                        janela.reclamar("Escreva o que é este item.");
                        return false;
                    }
                    contas.adicionarItem(qual, descricao.getText(),
                            numero(quantidade.getText()), numero(valor.getText()),
                            natureza.getValue() == null ? null : natureza.getValue().getId(),
                            centro.getValue() == null ? null : centro.getValue().getId(),
                            pessoa.getText(),
                            bem.getValue() == null ? null : bem.getValue().getId());
                    janela.avisar("Item incluído na composição.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Tira um item da composição. */
    private void tirarItem(Obrigacao conta, ItemDaObrigacao item) {
        JanelaFlutuante.estreita(janela.palco(), "Tirar da composição",
                        item.getDescricao() + " · " + Pecas.dinheiro(item.getTotal()))
                .acao("Tirar", () -> {
                    contas.removerItem(qual, item.getId());
                    janela.avisar("Item tirado da composição.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Divide o valor da conta entre áreas, por percentual. */
    private void janelaDoRateio(Obrigacao conta) {
        TextField descricao = new TextField(conta.getDescricao());
        ComboBox<Natureza> natureza = escolha(contas.naturezasAtivas(), Natureza::getNome);

        ComboBox<String> criterio = new ComboBox<>();
        criterio.getItems().addAll("percentual", "igual", "uso");
        criterio.getSelectionModel().selectFirst();
        criterio.setMaxWidth(Double.MAX_VALUE);

        VBox areas = new VBox(8);
        java.util.List<CentroDeCusto> centros = contas.centrosAtivos();
        java.util.List<CheckBox> marcados = new java.util.ArrayList<>();
        java.util.List<TextField> percentuais = new java.util.ArrayList<>();
        for (CentroDeCusto centro : centros) {
            CheckBox marca = new CheckBox(centro.getNome());
            TextField quanto = new TextField("0");
            quanto.setPrefWidth(90);
            marcados.add(marca);
            percentuais.add(quanto);
            HBox linha = new HBox(12, marca, new Label("%"), quanto);
            linha.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            areas.getChildren().add(linha);
        }
        if (centros.isEmpty()) {
            areas.getChildren().add(new Label("Nenhum centro de custo cadastrado. "
                    + "Cadastre em Contas a pagar, no botão Cadastros."));
        }

        HBox linha = new HBox(16, Pecas.campo("O que está sendo dividido", descricao),
                Pecas.campo("Natureza", natureza), Pecas.campo("Critério", criterio));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Ratear entre áreas",
                        "Marque as áreas e diga quanto cada uma absorve.")
                .com(linha, Pecas.secao("Áreas"), Pecas.caixa(areas))
                .acao("Aplicar rateio", () -> {
                    java.util.List<java.util.UUID> escolhidos = new java.util.ArrayList<>();
                    java.util.List<java.math.BigDecimal> quanto = new java.util.ArrayList<>();
                    for (int i = 0; i < marcados.size(); i++) {
                        if (marcados.get(i).isSelected()) {
                            escolhidos.add(centros.get(i).getId());
                            quanto.add(numero(percentuais.get(i).getText()));
                        }
                    }
                    if (escolhidos.isEmpty()) {
                        janela.reclamar("Marque pelo menos uma área.");
                        return false;
                    }
                    contas.ratear(qual, descricao.getText(),
                            natureza.getValue() == null ? null : natureza.getValue().getId(),
                            criterio.getValue(), escolhidos, quanto);
                    janela.avisar("Rateio aplicado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Uma lista de escolha com o nome de cada opção. */
    private <T> ComboBox<T> escolha(java.util.List<T> itens,
                                    java.util.function.Function<T, String> comoChamar) {
        ComboBox<T> caixa = new ComboBox<>();
        caixa.getItems().add(null);
        caixa.getItems().addAll(itens);
        caixa.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(T qualItem) {
                return qualItem == null ? "não informado" : comoChamar.apply(qualItem);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        });
        caixa.getSelectionModel().selectFirst();
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }

    private java.math.BigDecimal numero(String texto) {
        java.math.BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? java.math.BigDecimal.ZERO : valor;
    }

    /** Pergunta o motivo antes de uma ação que não se desfaz. */
    private void pedirMotivo(String titulo, String exemplo,
                             java.util.function.Consumer<String> entao) {
        javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
        motivo.setPromptText(exemplo);
        JanelaFlutuante.estreita(janela.palco(), titulo,
                        "Sem o motivo escrito, a ação não acontece.")
                .com(Pecas.campo("Motivo", motivo))
                .acao(titulo, () -> {
                    if (motivo.getText().isBlank()) {
                        janela.reclamar("Sem o motivo escrito, a ação não acontece.");
                        return false;
                    }
                    entao.accept(motivo.getText().trim());
                    return true;
                })
                .abrir();
    }
}
