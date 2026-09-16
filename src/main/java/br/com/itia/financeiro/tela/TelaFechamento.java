package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Competencia;
import br.com.itia.financeiro.dominio.FaturadoDoPeriodo;
import br.com.itia.financeiro.servico.FechamentoDoMes;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Fechamento do mês: gerar de uma vez as cobranças dos pacotes contratados.
 *
 * A prévia mostra quem entra e quem fica de fora, com o motivo, antes de
 * qualquer coisa ser gerada.
 */
@Component
public class TelaFechamento implements Tela {

    private final FechamentoDoMes fechamento;
    private final Janela janela;

    private YearMonth periodo = YearMonth.now();

    public TelaFechamento(FechamentoDoMes fechamento, @Lazy Janela janela) {
        this.fechamento = fechamento;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    @Override
    public Node montar() {
        FechamentoDoMes.Previa previa = fechamento.previa(periodo);
        List<FechamentoDoMes.Linha> prontas = previa.prontas();
        List<FechamentoDoMes.Linha> fora = previa.foraDaLeva();

        BigDecimal total = prontas.stream()
                .map(FechamentoDoMes.Linha::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ComboBox<YearMonth> mes = new ComboBox<>();
        List<YearMonth> meses = new ArrayList<>();
        for (int atras = 0; atras < 12; atras++) {
            meses.add(YearMonth.now().minusMonths(atras));
        }
        mes.getItems().addAll(meses);
        mes.getSelectionModel().select(periodo);
        mes.setOnAction(acao -> {
            periodo = mes.getValue();
            janela.atualizar();
        });

        TextField dia = new TextField("20");
        dia.setPrefWidth(80);

        boolean fechada = previa.fechada();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("catálogo", "Fechamento do mês",
                "Quem entra, quem fica de fora e por quê, antes de gerar qualquer cobrança.",
                mes, Pecas.botaoVazado("Ver este mês", () -> janela.atualizar()),
                Pecas.botaoVazado("Importar o faturado do mês", this::importar),
                previa.fechada()
                        ? Pecas.botaoVazado("Reabrir", this::reabrir)
                        : Pecas.botaoVazado("Fechar o mês", this::fechar),
                Pecas.botao("Gerar a leva", () -> gerar(dia.getText())),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Total da leva", Pecas.dinheiro(total),
                        prontas.size() + " contratações prontas", true, false),
                Pecas.quadro("Fica de fora", String.valueOf(fora.size()),
                        "cada um com o motivo", false, !fora.isEmpty()),
                Pecas.quadro("Arquivo sem dono", String.valueOf(previa.semDono().size()),
                        "linhas do faturado sem contratação", false,
                        !previa.semDono().isEmpty()),
                Pecas.quadro("Situação do mês", fechada ? "fechado" : "aberto",
                        fechada ? "não aceita lançamento" : "aceita lançamento"))));

        Label vencimento = new Label("Dia do vencimento das cobranças geradas:");
        vencimento.getStyleClass().add("dica");
        HBox linhaDoDia = new HBox(12, vencimento, dia);
        HBox.setHgrow(vencimento, Priority.NEVER);
        tela.getChildren().add(linhaDoDia);

        tela.getChildren().add(Pecas.secao("Quem entra"));
        tela.getChildren().add(Tabela.de(prontas)
                .coluna("Cliente", FechamentoDoMes.Linha::cliente, 2)
                .coluna("Pacote", FechamentoDoMes.Linha::pacote, 1.6)
                .coluna("Unidade", FechamentoDoMes.Linha::unidade, 1.4)
                .valor("Valor", l -> Pecas.numero(l.valor()))
                .coluna("De onde vem o valor", l -> l.valorVeioDeFora() ? "arquivo importado"
                        : "valor da contratação", 1.4)
                .quandoVazia("Nenhuma contratação pronta para este mês.")
                .montar());

        tela.getChildren().add(Pecas.secao("Quem fica de fora"));
        tela.getChildren().add(Tabela.de(fora)
                .coluna("Cliente", FechamentoDoMes.Linha::cliente, 2)
                .coluna("Pacote", FechamentoDoMes.Linha::pacote, 1.6)
                .coluna("Por quê", FechamentoDoMes.Linha::motivoDeFicarDeFora, 2.4)
                .quandoVazia("Ninguém fica de fora neste mês.")
                .montar());

        List<FaturadoDoPeriodo> faturado = fechamento.faturadoDe(periodo);
        tela.getChildren().add(Pecas.secao("Faturado importado deste mês"));
        tela.getChildren().add(Tabela.de(faturado)
                .coluna("Documento", FaturadoDoPeriodo::getDocumento, 1)
                .coluna("Cliente", f -> f.getPagador() == null ? "" : f.getPagador().getNome(), 2)
                .coluna("Unidade", f -> f.getUnidade() == null ? ""
                        : f.getUnidade().getRazaoSocial(), 1.6)
                .valor("Valor", f -> Pecas.numero(f.getValor()))
                .coluna("Arquivo", FaturadoDoPeriodo::getOrigem, 1.4)
                .comMarca(f -> f.semDono() ? "sem dono" : "amarrado",
                        f -> f.semDono() ? "s-vencido" : "s-pago")
                .quandoVazia("Nenhum faturado importado para este mês.")
                .montar());

        tela.getChildren().add(Pecas.secao("Meses já fechados"));
        tela.getChildren().add(Tabela.de(fechamento.competencias())
                .coluna("Mês", Competencia::getReferencia, 0.8)
                .coluna("Fechado por", Competencia::getFechadaPor, 1.4)
                .coluna("Reaberto por", Competencia::getReabertaPor, 1.4)
                .coluna("Motivo da reabertura", Competencia::getMotivo, 2)
                .comMarca(Competencia::getSituacao,
                        c -> c.estaFechada() ? "s-pago" : "s-aberto")
                .quandoVazia("Nenhum mês fechado ainda.")
                .montar());
        return tela;
    }

    /** Fecha o mês: depois disso nada mais entra nele. */
    private void fechar() {
        fechamento.fechar(periodo);
        janela.avisar("Mês " + periodo + " fechado. Nada mais entra nele.");
        janela.atualizar();
    }

    /** Reabre o mês, sempre com o motivo registrado. */
    private void reabrir() {
        javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
        motivo.setPromptText("por que este mês está sendo reaberto");
        JanelaFlutuante.estreita(janela.palco(), "Reabrir o mês",
                        "O motivo fica registrado junto com quem reabriu.")
                .com(Pecas.campo("Motivo", motivo))
                .acao("Reabrir", () -> {
                    if (motivo.getText().isBlank()) {
                        janela.reclamar("Escreva o motivo da reabertura.");
                        return false;
                    }
                    fechamento.reabrir(periodo, motivo.getText().trim());
                    janela.avisar("Mês " + periodo + " reaberto.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Traz o faturado do mês de um arquivo. */
    /** O faturado do mês vem de um arquivo de fora, com duas colunas. */
    private void importar() {
        javafx.scene.control.Label comoE = new javafx.scene.control.Label(
                "Arquivo de texto com duas colunas: documento e valor, separados por ponto e "
                        + "vírgula. Uma linha por cliente ou por unidade. Exemplo:");
        comoE.getStyleClass().add("dica");
        comoE.setWrapText(true);

        javafx.scene.control.Label exemplo = new javafx.scene.control.Label(
                "12.345.678/0001-99;1450,00\n987.654.321-00;450,00");
        exemplo.getStyleClass().add("texto");

        javafx.scene.control.Label semDono = new javafx.scene.control.Label(
                "Linha que não achar cliente pelo documento fica visível como sem dono, e não "
                        + "vira cobrança de ninguém.");
        semDono.getStyleClass().add("dica");
        semDono.setWrapText(true);

        javafx.scene.control.ComboBox<String> oQueFazer =
                new javafx.scene.control.ComboBox<>();
        oQueFazer.getItems().addAll("trocar tudo por este arquivo", "somar a este arquivo");
        oQueFazer.getSelectionModel().selectFirst();
        oQueFazer.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.Label escolhido = new javafx.scene.control.Label(
                "nenhum arquivo escolhido");
        escolhido.getStyleClass().add("dica");
        java.io.File[] arquivo = new java.io.File[1];

        HBox anexo = new HBox(12, Pecas.botaoVazado("Escolher arquivo", () -> {
            javafx.stage.FileChooser escolher = new javafx.stage.FileChooser();
            escolher.setTitle("Arquivo do faturado do mês");
            escolher.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Texto", "*.csv", "*.txt"));
            java.io.File qual = escolher.showOpenDialog(janela.palco());
            if (qual != null) {
                arquivo[0] = qual;
                escolhido.setText(qual.getName());
            }
        }), escolhido);
        anexo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        JanelaFlutuante.estreita(janela.palco(), "Importar o faturado do mês",
                        "Entrada do faturado · " + periodo)
                .com(comoE, Pecas.caixa(exemplo), Pecas.campo("Arquivo", anexo),
                        Pecas.campo("O que fazer com o que já foi importado", oQueFazer), semDono)
                .acao("Importar", () -> {
                    if (arquivo[0] == null) {
                        janela.reclamar("Escolha o arquivo do faturado.");
                        return false;
                    }
                    try {
                        byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo[0].toPath());
                        FechamentoDoMes.Importacao resultado = fechamento.importar(periodo,
                                new br.com.itia.financeiro.dominio.ArquivoRecebido(
                                        arquivo[0].getName(), conteudo),
                                oQueFazer.getSelectionModel().getSelectedIndex() == 0);
                        janela.avisar(resultado.lidas() + " linha(s) lidas, "
                                + resultado.amarradas() + " amarradas e " + resultado.semDono()
                                + " sem dono.");
                        janela.atualizar();
                        return true;
                    } catch (java.io.IOException naoLeu) {
                        janela.reclamar("Não deu para ler o arquivo: " + naoLeu.getMessage());
                        return false;
                    }
                })
                .abrir();
    }

    private void gerar(String dia) {
        int diaDoVencimento;
        try {
            diaDoVencimento = Integer.parseInt(dia.trim());
        } catch (RuntimeException naoEhNumero) {
            janela.reclamar("Escreva o dia do vencimento, de 1 a 28.");
            return;
        }
        int quantas = fechamento.gerar(periodo, diaDoVencimento);
        janela.avisar(quantas == 0
                ? "Nada foi gerado: nenhuma contratação estava pronta."
                : quantas + " cobrança(s) geradas para " + periodo + ".");
        janela.atualizar();
    }
}
