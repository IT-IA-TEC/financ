package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.servico.Analises;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Análises: quanto do que foi cobrado virou dinheiro, e o que está por vir.
 *
 * A previsão usa a taxa de conversão dos meses anteriores. Ela é uma
 * estimativa, e a tela diz isso.
 */
@Component
public class TelaAnalises implements Tela {

    private final Analises analises;
    private final Janela janela;

    public TelaAnalises(Analises analises, @Lazy Janela janela) {
        this.analises = analises;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "titulos";
    }

    @Override
    public Node montar() {
        List<Analises.LinhaDoMes> curva = analises.curvaDeRecuperacao(12);
        Analises.Previsao previsao = analises.previsao(6);
        List<Analises.LinhaDeRisco> ranking = analises.ranking(10);
        BigDecimal concentracao = analises.concentracao(ranking);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("livro", "Análises",
                "Quanto do que foi cobrado virou dinheiro, e o que está por vir.",
                Pecas.botaoVazado("Exportar títulos", this::exportar),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Taxa de conversão", previsao.taxaEmPorcento() + "%",
                        "do cobrado que vira dinheiro", true, false),
                Pecas.quadro("A vencer", Pecas.dinheiro(previsao.totalAVencer()),
                        "nos próximos meses"),
                Pecas.quadro("Estimativa de entrada", Pecas.dinheiro(previsao.totalEstimado()),
                        "usando a taxa acima"),
                Pecas.quadro("Vencido hoje", Pecas.dinheiro(previsao.vencido()),
                        "já passou do prazo", false, previsao.vencido().signum() > 0))));

        java.util.LinkedHashMap<String, Runnable> partes = new java.util.LinkedHashMap<>();
        partes.put("Contas a receber", () -> janela.ir(TelaTitulos.class));
        partes.put("Comprovantes", () -> janela.ir(TelaComprovantes.class));
        partes.put("Análises", () -> janela.ir(TelaAnalises.class));
        tela.getChildren().add(Pecas.abas("Análises", partes));


        tela.getChildren().add(Pecas.secao("Mês a mês: cobrado e recebido"));
        tela.getChildren().add(Tabela.de(curva)
                .coluna("Mês", Analises.LinhaDoMes::mes)
                .valor("Cobrado, pela competência", m -> Pecas.numero(m.cobrado()))
                .valor("Recebido, pela data do dinheiro", m -> Pecas.numero(m.recebido()))
                .valor("Diferença", m -> Pecas.numero(m.diferenca()))
                .valor("Conversão", m -> m.conversao() + "%")
                .quandoVazia("Ainda não há meses fechados para comparar.")
                .montar());

        tela.getChildren().add(Pecas.secao("O que deve entrar"));
        tela.getChildren().add(Tabela.de(previsao.meses())
                .coluna("Mês", Analises.LinhaDoMes::mes)
                .valor("A vencer", m -> Pecas.numero(m.cobrado()))
                .valor("Estimado", m -> Pecas.numero(m.recebido()))
                .quandoVazia("Nada a vencer nos próximos meses.")
                .montar());

        tela.getChildren().add(Pecas.secao("Quem concentra o risco · "
                + concentracao + "% nos dez maiores"));
        tela.getChildren().add(Tabela.de(ranking)
                .coluna("Cliente", Analises.LinhaDeRisco::cliente, 2)
                .valor("Em aberto", r -> Pecas.numero(r.emAberto()))
                .valor("Do qual vencido", r -> Pecas.numero(r.vencido()))
                .valor("Maior atraso", r -> r.maiorAtraso() + " dias")
                .valor("Parte do total", r -> r.participacao() + "%")
                .quandoVazia("Nenhum cliente com saldo em aberto.")
                .montar());
        return tela;
    }

    private void exportar() {
        FileChooser onde = new FileChooser();
        onde.setTitle("Salvar os títulos");
        onde.setInitialFileName("titulos.csv");
        onde.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha", "*.csv"));
        File arquivo = onde.showSaveDialog(janela.palco());
        if (arquivo == null) {
            return;
        }
        try {
            Files.writeString(arquivo.toPath(), analises.exportarTitulos(),
                    StandardCharsets.UTF_8);
            janela.avisar("Títulos salvos em " + arquivo.getName() + ".");
        } catch (java.io.IOException naoSalvou) {
            janela.reclamar("Não consegui salvar o arquivo: " + naoSalvou.getMessage());
        }
        janela.atualizar();
    }
}
