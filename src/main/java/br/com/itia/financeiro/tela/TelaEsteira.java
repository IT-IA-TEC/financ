package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.servico.Esteira;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A esteira: quem está devendo, separado por faixa de atraso.
 *
 * A faixa não é um rótulo que alguém escolhe: ela sai do maior atraso de cada
 * unidade, calculado na hora.
 */
@Component
public class TelaEsteira implements Tela {

    private final Esteira esteira;
    private final Janela janela;

    public TelaEsteira(Esteira esteira, @Lazy Janela janela) {
        this.esteira = esteira;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        Map<Esteira.Faixa, List<Esteira.NaEsteira>> quadro = esteira.quadro(hoje);
        List<CasoDeCobranca> paraHoje = esteira.paraHoje(hoje);
        Map<java.util.UUID, String> nomes = esteira.nomes();

        BigDecimal vencido = BigDecimal.ZERO;
        int quantosVencidos = 0;
        for (Map.Entry<Esteira.Faixa, List<Esteira.NaEsteira>> coluna : quadro.entrySet()) {
            if (coluna.getKey().de() > 0) {
                quantosVencidos += coluna.getValue().size();
                vencido = vencido.add(esteira.totalDe(coluna.getValue()));
            }
        }

        BigDecimal ate15 = BigDecimal.ZERO;
        BigDecimal mais30 = BigDecimal.ZERO;
        for (Map.Entry<Esteira.Faixa, List<Esteira.NaEsteira>> coluna : quadro.entrySet()) {
            int de = coluna.getKey().de();
            if (de > 0 && de <= 15) {
                ate15 = ate15.add(esteira.totalDe(coluna.getValue()));
            }
            if (de > 30) {
                mais30 = mais30.add(esteira.totalDe(coluna.getValue()));
            }
        }

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("quem deve, e há quanto tempo", "Esteira de inadimplência",
                "Cada coluna é uma faixa de atraso. A faixa sai do maior atraso da unidade.",
                Pecas.quadrosDoTopo(
                Pecas.quadro("Vencido", Pecas.dinheiro(vencido),
                        quantosVencidos + " unidades em atraso", true, false),
                Pecas.quadro("Até 15 dias", Pecas.dinheiro(ate15),
                        "ainda dá para resolver na conversa"),
                Pecas.quadro("Mais de 30 dias", Pecas.dinheiro(mais30),
                        "o dinheiro que está mais difícil", false, mais30.signum() > 0),
                Pecas.quadro("Para hoje", String.valueOf(paraHoje.size()),
                        "ações marcadas, vencidas ou de hoje"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Esteira"));

        HBox colunas = new HBox(12);
        for (Map.Entry<Esteira.Faixa, List<Esteira.NaEsteira>> coluna : quadro.entrySet()) {
            colunas.getChildren().add(coluna(coluna.getKey(), coluna.getValue(), hoje));
        }
        colunas.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        tela.getChildren().add(colunas);

        if (!paraHoje.isEmpty()) {
            tela.getChildren().add(Pecas.secao("Marcados para hoje"));
            tela.getChildren().add(Tabela.de(paraHoje)
                    .coluna("Quem", c -> nomes.getOrDefault(c.getUnidadeId(), ""), 2)
                    .coluna("Situação", CasoDeCobranca::getSituacao)
                    .coluna("O que fazer", CasoDeCobranca::getProximaAcao, 2)
                    .coluna("Responsável", CasoDeCobranca::getResponsavel)
                    .quandoVazia("Ninguém marcado para hoje.")
                    .montar());
        }
        return tela;
    }

    /**
     * O caso de cobrança daquela unidade: em que pé está, quem cuida, o que
     * fazer e quando.
     */
    private void abrirCaso(Esteira.NaEsteira quem) {
        CasoDeCobranca caso = quem.caso();

        javafx.scene.control.ComboBox<String> situacao = new javafx.scene.control.ComboBox<>();
        situacao.getItems().addAll(CasoDeCobranca.SITUACOES);
        situacao.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String qual) {
                return qual == null ? "" : qual.toLowerCase().replace('_', ' ');
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        });
        situacao.getSelectionModel().select(caso == null ? CasoDeCobranca.SITUACOES.get(0)
                : caso.getSituacao());
        situacao.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.TextField responsavel = new javafx.scene.control.TextField(
                caso == null ? "" : caso.getResponsavel());
        javafx.scene.control.DatePicker proximaData = new javafx.scene.control.DatePicker(
                caso == null ? null : caso.getProximaData());
        proximaData.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.TextField proximaAcao = new javafx.scene.control.TextField(
                caso == null ? "" : caso.getProximaAcao());
        proximaAcao.setPromptText("ligar para combinar a data do pagamento");

        javafx.scene.control.TextArea observacao = new javafx.scene.control.TextArea(
                caso == null ? "" : caso.getObservacao());
        observacao.setPrefRowCount(3);

        HBox campos = new HBox(16, Pecas.campo("Situação", situacao),
                Pecas.campo("Quem está cuidando", responsavel),
                Pecas.campo("Próxima data", proximaData));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), quem.quem(), "Caso de cobrança.")
                .com(campos, Pecas.campo("Próxima ação", proximaAcao),
                        Pecas.campo("Observação", observacao))
                .outraAcao("Anotar e abrir a conversa", () -> {
                    esteira.anotar(quem.unidadeId(), situacao.getValue(), responsavel.getText(),
                            proximaAcao.getText(), proximaData.getValue(), observacao.getText());
                    janela.ir(TelaConversas.class);
                })
                .acao("Guardar", () -> {
                    esteira.anotar(quem.unidadeId(), situacao.getValue(), responsavel.getText(),
                            proximaAcao.getText(), proximaData.getValue(), observacao.getText());
                    janela.avisar("Caso atualizado.");
                    janela.ir(TelaEsteira.class);
                    return true;
                })
                .abrir();
    }

    /** Uma faixa de atraso, com quem está nela. */
    private VBox coluna(Esteira.Faixa faixa, List<Esteira.NaEsteira> gente, LocalDate hoje) {
        VBox coluna = new VBox(0);
        coluna.getStyleClass().add("coluna-lista");
        coluna.setMinWidth(220);

        Label nome = new Label(faixa.nome().toUpperCase() + "  ·  " + gente.size());
        nome.getStyleClass().add("nome-coluna");
        nome.setMaxWidth(Double.MAX_VALUE);
        coluna.getChildren().add(nome);

        Label total = new Label(Pecas.dinheiro(esteira.totalDe(gente)));
        total.getStyleClass().add("apoio-item");
        total.setStyle("-fx-padding: 8 14 8 14; -fx-text-fill: #FFFFFF; -fx-font-size: 15px;");
        coluna.getChildren().add(total);

        for (Esteira.NaEsteira quem : gente) {
            Label pessoa = new Label(quem.quem());
            pessoa.getStyleClass().add("titulo-item");
            pessoa.setWrapText(true);

            Label valor = new Label(Pecas.dinheiro(quem.vencido()) + " · "
                    + quem.maiorAtraso() + " dias");
            valor.getStyleClass().add("apoio-item");

            VBox item = new VBox(4, pessoa, valor);
            item.getStyleClass().add("item-lista");
            item.setOnMouseClicked(clique -> abrirCaso(quem));
            if (quem.semFalarHaMuito(hoje)) {
                item.getChildren().add(marca("sem falar há muito tempo"));
            }
            coluna.getChildren().add(item);
        }
        if (gente.isEmpty()) {
            Label vazio = new Label("ninguém nesta faixa");
            vazio.getStyleClass().add("apoio-item");
            vazio.setStyle("-fx-padding: 16 14 16 14;");
            coluna.getChildren().add(vazio);
        }
        return coluna;
    }

    private Label marca(String texto) {
        Label marca = new Label(texto);
        marca.getStyleClass().addAll("marca-situacao", "s-vencido");
        return marca;
    }
}
