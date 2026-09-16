package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Pacote;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.servico.PacoteServico;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Pacotes: um conjunto de serviços vendido junto, com um valor só.
 *
 * O pacote diz o que está incluído e quanto cada coisa passa a custar quando o
 * cliente estoura o que foi combinado.
 */
@Component
public class TelaPacotes implements Tela {

    private final PacoteServico pacotes;
    private final Janela janela;

    private String busca = "";
    private String situacaoEscolhida = "todas";

    public TelaPacotes(PacoteServico pacotes, @Lazy Janela janela) {
        this.pacotes = pacotes;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    /** A lista de situação da barra, igual à da web. */
    private ComboBox<String> filtroDeSituacao() {
        ComboBox<String> filtro = new ComboBox<>();
        filtro.getItems().addAll("todas", "ativo", "inativo");
        filtro.getSelectionModel().select(situacaoEscolhida);
        filtro.setOnAction(acao -> {
            situacaoEscolhida = filtro.getValue();
            janela.atualizar();
        });
        return filtro;
    }

    @Override
    public Node montar() {
        List<Pacote> lista = pacotes.lista(busca, "todas".equals(situacaoEscolhida) ? null : situacaoEscolhida);

        TextField procurar = new TextField(busca);
        procurar.setPromptText("procurar pacote");
        procurar.setPrefWidth(280);
        procurar.setOnAction(acao -> {
            busca = procurar.getText();
            janela.atualizar();
        });

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("gestão de pacotes", "Pacotes",
                "Serviços vendidos juntos, com um valor só.",
                Pecas.botao("+ Novo pacote", this::abrirNovoPacote),
                filtroDeSituacao(), procurar,
                Pecas.botaoVazado("Buscar", () -> {
                    busca = procurar.getText();
                    janela.atualizar();
                })));

        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Serviços", () -> janela.ir(TelaServicos.class));
        partes.put("Pacotes", () -> janela.ir(TelaPacotes.class));
        partes.put("Serviços realizados", () -> janela.ir(TelaRealizados.class));
        partes.put("Cobranças", () -> janela.ir(TelaCobrancas.class));
        partes.put("Fechamento", () -> janela.ir(TelaFechamento.class));
        tela.getChildren().add(Pecas.abas("Pacotes", partes));

        tela.getChildren().add(Pecas.secao("Pacotes cadastrados"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Código", Pacote::getCodigo, 0.6)
                .coluna("Nome", Pacote::getNome, 2)
                .valor("Serviços incluídos", p -> String.valueOf(p.getComposicao().size()))
                .coluna("Valor", Pacote::getValorResumido, 1)
                .coluna("Cobrança", Pacote::getPeriodicidadeResumida, 1)
                .valor("Contratações", p -> String.valueOf(pacotes.quantasContratacoes(p.getId())))
                .comMarca(p -> p.isAtivo() ? "ativo" : "inativo",
                        p -> p.isAtivo() ? "" : "s-cancelado")
                .aoClicar(qualPacote -> janela.ir(TelaPacote.class, qualPacote.getId()))
                .quandoVazia("Nenhum pacote cadastrado ainda.")
                .montar());

        return tela;
    }

    /** O pacote novo abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovoPacote() {
        TextField nome = new TextField();
        nome.setPromptText("pacote essencial");
        TextField descricao = new TextField();
        TextField valor = new TextField();
        valor.setPromptText("0,00");

        ComboBox<Periodicidade> periodicidade = new ComboBox<>();
        periodicidade.getItems().addAll(Periodicidade.values());
        periodicidade.getSelectionModel().selectFirst();
        periodicidade.setMaxWidth(Double.MAX_VALUE);

        TextField outra = new TextField();
        outra.setPromptText("a cada 45 dias");

        HBox campos = new HBox(16,
                Pecas.campo("Nome", nome),
                Pecas.campo("Descrição", descricao),
                Pecas.campo("Valor do pacote (R$)", valor),
                Pecas.campo("Periodicidade", periodicidade),
                Pecas.campo("Se for outra, qual", outra));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Novo pacote",
                        "Serviços vendidos juntos, com um valor só.")
                .com(campos)
                .acao("Cadastrar pacote", () -> {
            if (nome.getText() == null || nome.getText().isBlank()) {
                janela.reclamar("Escreva o nome do pacote.");
                return false;
            }
            BigDecimal quanto = TelaTituloNovo.dinheiro(valor.getText());
            Pacote novo = pacotes.cadastrar(nome.getText().trim(),
                    descricao.getText() == null || descricao.getText().isBlank() ? null
                            : descricao.getText().trim(),
                    quanto, periodicidade.getValue(),
                    outra.getText() == null || outra.getText().isBlank() ? null
                            : outra.getText().trim());
            janela.avisar("Pacote " + novo.getCodigo() + " cadastrado.");
            janela.ir(TelaPacotes.class);
            return true;
                })
                .abrir();
    }
}
