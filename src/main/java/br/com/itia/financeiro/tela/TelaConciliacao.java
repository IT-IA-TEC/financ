package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ArquivoRecebido;
import br.com.itia.financeiro.dominio.ContaFinanceira;
import br.com.itia.financeiro.dominio.ImportacaoDeExtrato;
import br.com.itia.financeiro.dominio.MovimentoBancario;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.ConciliacaoBancaria;
import br.com.itia.financeiro.servico.ContasAPagar;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Conciliação: o extrato do banco de um lado, o que o sistema sabe do outro.
 *
 * O mesmo extrato não entra duas vezes: o sistema guarda a impressão digital do
 * arquivo e recusa o repetido.
 */
@Component
public class TelaConciliacao implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ConciliacaoBancaria conciliacao;
    private final ContasAPagar contas;
    private final Janela janela;

    private java.util.UUID emConferencia;

    /** O período do fechamento dia a dia, para quem quiser olhar outro pedaço. */
    private LocalDate inicioDoPeriodo = LocalDate.now().minusDays(30);
    private LocalDate fimDoPeriodo = LocalDate.now();

    public TelaConciliacao(ConciliacaoBancaria conciliacao, ContasAPagar contas,
                           @Lazy Janela janela) {
        this.conciliacao = conciliacao;
        this.contas = contas;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "conciliacao";
    }

    @Override
    public Node montar() {
        List<MovimentoBancario> todos = conciliacao.todos();
        List<MovimentoBancario> pendentes = conciliacao.pendentes();
        List<ImportacaoDeExtrato> importacoes = conciliacao.ultimasImportacoes();

        // entrada e saída vêm do tipo do lançamento, não do sinal: no extrato os dois
        // chegam com valor positivo
        BigDecimal entradas = todos.stream()
                .filter(MovimentoBancario::entrou)
                .map(MovimentoBancario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saidas = todos.stream()
                .filter(m -> !m.entrou())
                .map(MovimentoBancario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("banco", "Conciliação bancária",
                "O extrato do banco de um lado, o que o sistema sabe do outro.",
                Pecas.botao("Importar extrato", this::abrirImportar),
                Pecas.botaoVazado("Casar pelo identificador", () -> {
                    int quantos = conciliacao.casarSozinho();
                    janela.avisar(quantos == 0
                            ? "Nenhum movimento casou sozinho desta vez."
                            : quantos + " movimento(s) casaram sozinho.");
                    janela.atualizar();
                }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Esperando conferência", String.valueOf(pendentes.size()),
                        "movimentos sem destino", true, false),
                Pecas.quadro("Entradas", Pecas.dinheiro(entradas), "o que caiu na conta"),
                Pecas.quadro("Saídas", Pecas.dinheiro(saidas), "o que saiu da conta"),
                Pecas.quadro("Extratos", String.valueOf(importacoes.size()),
                        "arquivos importados"))));


        tela.getChildren().add(Pecas.secao("Movimentos esperando conferência"));
        tela.getChildren().add(Tabela.de(pendentes)
                .coluna("Quando", m -> Pecas.data(m.getOcorridoEm()), 0.7)
                .coluna("Tipo", m -> m.entrou() ? "entrada" : "saída", 0.7)
                .coluna("Descrição", MovimentoBancario::getDescricao, 3)
                .coluna("Identificador", MovimentoBancario::getIdentificador, 1.2)
                .valor("Valor", m -> Pecas.numero(m.getValor()))
                .comMarca(MovimentoBancario::getSituacao,
                        m -> m.getValor().signum() > 0 ? "s-pago" : "")
                .aoClicar(m -> {
                    emConferencia = m.getId();
                    janela.atualizar();
                })
                .quandoVazia("Nenhum movimento esperando conferência.")
                .montar());

        Label comoConferir = new Label("Clique num movimento da lista para dar o destino dele.");
        comoConferir.getStyleClass().add("dica");
        tela.getChildren().add(comoConferir);

        if (emConferencia != null) {
            pendentes.stream().filter(m -> m.getId().equals(emConferencia)).findFirst()
                    .ifPresent(movimento -> {
                        tela.getChildren().add(Pecas.secao("O que fazer com este movimento"));
                        tela.getChildren().add(conferir(movimento));
                    });
        }

        javafx.scene.control.DatePicker dePeriodo =
                new javafx.scene.control.DatePicker(inicioDoPeriodo);
        javafx.scene.control.DatePicker atePeriodo =
                new javafx.scene.control.DatePicker(fimDoPeriodo);
        HBox periodo = new HBox(12, new Label("De"), dePeriodo, new Label("até"), atePeriodo,
                Pecas.botaoVazado("Ver período", () -> {
                    inicioDoPeriodo = dePeriodo.getValue();
                    fimDoPeriodo = atePeriodo.getValue();
                    janela.atualizar();
                }));
        periodo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        tela.getChildren().add(Pecas.secao("Fechamento dia a dia"));
        tela.getChildren().add(periodo);
        tela.getChildren().add(Tabela.de(
                conciliacao.fechamentoDiario(inicioDoPeriodo, fimDoPeriodo))
                .coluna("Dia", l -> Pecas.data(l.dia()), 0.8)
                .valor("Entrou", l -> Pecas.numero(l.entradas()))
                .valor("Saiu", l -> Pecas.numero(l.saidas()))
                .valor("Sem par", l -> Pecas.numero(l.semPar()))
                .coluna("Situação do dia", l -> l.fecha() ? "fecha"
                        : l.pendentes() + " sem par", 1.2)
                .comMarca(l -> l.fecha() ? "fecha" : l.pendentes() + " sem par",
                        l -> l.fecha() ? "s-pago" : "s-vencido")
                .quandoVazia("Nenhum movimento nos últimos 30 dias.")
                .montar());

        tela.getChildren().add(Pecas.secao("Extratos importados"));
        tela.getChildren().add(Tabela.de(importacoes)
                .coluna("Quando", i -> i.getQuando() == null ? "" : i.getQuando().format(QUANDO))
                .coluna("Arquivo", ImportacaoDeExtrato::getArquivo, 2)
                .valor("Linhas", i -> String.valueOf(i.getLinhas()))
                .valor("Novos", i -> String.valueOf(i.getNovos()))
                .valor("Repetidos", i -> String.valueOf(i.getRepetidos()))
                .coluna("Quem importou", ImportacaoDeExtrato::getQuem)
                .quandoVazia("Nenhum extrato importado ainda.")
                .montar());
        return tela;
    }

    /**
     * O que fazer com um movimento: ligar a um título, ligar a uma conta a pagar,
     * ignorar ou desfazer o que já foi feito.
     */
    private VBox conferir(MovimentoBancario movimento) {
        Label oQueVeio = new Label(Pecas.data(movimento.getOcorridoEm()) + " · "
                + movimento.getDescricao() + " · " + Pecas.dinheiro(movimento.getValor())
                + (movimento.getIdentificador() == null ? ""
                        : " · " + movimento.getIdentificador()));
        oQueVeio.setWrapText(true);

        if (!movimento.pendente()) {
            return Pecas.caixa(oQueVeio, new HBox(Pecas.botaoVazado("Desfazer", () -> {
                conciliacao.desfazer(movimento.getId());
                janela.avisar("Movimento voltou para a fila de conferência.");
                emConferencia = null;
                janela.atualizar();
            })));
        }

        ComboBox<Titulo> titulo = new ComboBox<>();
        titulo.getItems().addAll(conciliacao.titulosEmAberto());
        titulo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Titulo qual) {
                return qual == null ? "" : qual.getNumero() + " · " + qual.getDescricao()
                        + " · " + Pecas.dinheiro(qual.getSaldo());
            }

            @Override
            public Titulo fromString(String texto) {
                return null;
            }
        });
        titulo.setMaxWidth(Double.MAX_VALUE);
        CheckBox darBaixa = new CheckBox("dar baixa no título");
        darBaixa.setSelected(true);

        ComboBox<Obrigacao> conta = new ComboBox<>();
        conta.getItems().addAll(conciliacao.contasEmAberto());
        conta.setConverter(new StringConverter<>() {
            @Override
            public String toString(Obrigacao qual) {
                return qual == null ? "" : qual.getNumero() + " · " + qual.getDescricao()
                        + " · " + Pecas.dinheiro(qual.getSaldo());
            }

            @Override
            public Obrigacao fromString(String texto) {
                return null;
            }
        });
        conta.setMaxWidth(Double.MAX_VALUE);
        CheckBox registrarPagamento = new CheckBox("registrar o pagamento");
        registrarPagamento.setSelected(true);

        TextField motivo = new TextField();
        motivo.setPromptText("por que este movimento não tem par aqui");

        HBox aoTitulo = new HBox(12, Pecas.campo("Ligar ao título", titulo), darBaixa,
                Pecas.botao("Ligar ao título", () -> {
                    if (titulo.getValue() == null) {
                        janela.reclamar("Escolha o título.");
                        return;
                    }
                    conciliacao.ligarAoTitulo(movimento.getId(), titulo.getValue().getId(),
                            darBaixa.isSelected());
                    janela.avisar("Movimento ligado ao título.");
                    emConferencia = null;
                    janela.atualizar();
                }));
        HBox.setHgrow(aoTitulo.getChildren().get(0), Priority.ALWAYS);
        aoTitulo.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        HBox aConta = new HBox(12, Pecas.campo("Ligar à conta a pagar", conta),
                registrarPagamento,
                Pecas.botao("Ligar à conta", () -> {
                    if (conta.getValue() == null) {
                        janela.reclamar("Escolha a conta.");
                        return;
                    }
                    conciliacao.ligarAConta(movimento.getId(), conta.getValue().getId(),
                            registrarPagamento.isSelected());
                    janela.avisar("Movimento ligado à conta a pagar.");
                    emConferencia = null;
                    janela.atualizar();
                }));
        HBox.setHgrow(aConta.getChildren().get(0), Priority.ALWAYS);
        aConta.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        HBox ignorar = new HBox(12, Pecas.campo("Ignorar este movimento", motivo),
                Pecas.botaoVazado("Ignorar", () -> {
                    if (motivo.getText() == null || motivo.getText().isBlank()) {
                        janela.reclamar("Escreva o motivo de ignorar.");
                        return;
                    }
                    conciliacao.ignorar(movimento.getId(), motivo.getText().trim());
                    janela.avisar("Movimento ignorado, com o motivo registrado.");
                    emConferencia = null;
                    janela.atualizar();
                }));
        HBox.setHgrow(ignorar.getChildren().get(0), Priority.ALWAYS);
        ignorar.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        return Pecas.caixa(oQueVeio, aoTitulo, aConta, ignorar);
    }

    /** O extrato do banco entra por uma janela, sem ocupar espaço na página. */
    private void abrirImportar() {
        JanelaFlutuante.estreita(janela.palco(), "Importar extrato",
                        "O arquivo que o banco exporta.")
                .com(importar())
                .abrir();
    }

    private VBox importar() {
        List<ContaFinanceira> disponiveis = contas.contasAtivas();
        if (disponiveis.isEmpty()) {
            return Pecas.vazio("Nenhuma conta bancária cadastrada. Cadastre antes de importar.");
        }

        ComboBox<ContaFinanceira> conta = new ComboBox<>();
        conta.getItems().addAll(disponiveis);
        conta.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContaFinanceira qual) {
                return qual == null ? "" : qual.getNome();
            }

            @Override
            public ContaFinanceira fromString(String texto) {
                return null;
            }
        });
        conta.getSelectionModel().selectFirst();
        conta.setMaxWidth(Double.MAX_VALUE);

        Label escolhido = new Label("nenhum arquivo escolhido");
        escolhido.getStyleClass().add("dica");

        final File[] arquivo = new File[1];

        HBox linha = new HBox(16, Pecas.campo("Conta do extrato", conta),
                Pecas.campo("Arquivo", new HBox(12,
                        Pecas.botaoVazado("Escolher arquivo", () -> {
                            FileChooser escolher = new FileChooser();
                            escolher.setTitle("Extrato do banco");
                            escolher.getExtensionFilters().add(
                                    new FileChooser.ExtensionFilter("Extrato",
                                            "*.csv", "*.ofx", "*.txt"));
                            File qual = escolher.showOpenDialog(janela.palco());
                            if (qual != null) {
                                arquivo[0] = qual;
                                escolhido.setText(qual.getName());
                            }
                        }), escolhido)));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        return Pecas.caixa(linha, new HBox(Pecas.botao("Importar", () -> {
            if (arquivo[0] == null) {
                janela.reclamar("Escolha o arquivo do extrato.");
                return;
            }
            ConciliacaoBancaria.Resultado resultado = conciliacao.importar(
                    ArquivoRecebido.de(arquivo[0]), conta.getValue().getId());
            janela.avisar("Extrato lido: " + resultado.lidas() + " linhas, "
                    + resultado.novos() + " movimentos novos e "
                    + resultado.repetidos() + " que já existiam.");
            janela.ir(TelaConciliacao.class);
        })));
    }
}
