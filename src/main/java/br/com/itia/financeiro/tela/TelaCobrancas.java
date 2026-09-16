package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Cobranca;
import br.com.itia.financeiro.servico.Cobrancas;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Cobranças: o que saiu dos pacotes e dos atendimentos e virou dinheiro a
 * receber.
 *
 * Cobrança aprovada vira título no contas a receber. Antes disso, ela ainda
 * pode ser ajustada.
 */
@Component
public class TelaCobrancas implements Tela {

    private final Cobrancas cobrancas;

    private String origemEscolhida = "todas";
    private String situacaoEscolhida = "todas";
    private final Janela janela;

    public TelaCobrancas(Cobrancas cobrancas, @Lazy Janela janela) {
        this.cobrancas = cobrancas;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    /** Uma lista de escolha da barra de filtros. */
    private ComboBox<String> filtro(String oQue, String atual, java.util.List<String> opcoes,
                                    java.util.function.Consumer<String> aoEscolher) {
        ComboBox<String> lista = new ComboBox<>();
        lista.getItems().addAll(opcoes);
        lista.getSelectionModel().select(atual);
        lista.setPromptText(oQue);
        lista.setOnAction(acao -> aoEscolher.accept(lista.getValue()));
        return lista;
    }

    @Override
    public Node montar() {
        List<Cobranca> lista = cobrancas.lista(
                "todas".equals(origemEscolhida) ? null : origemEscolhida,
                "todas".equals(situacaoEscolhida) ? null : situacaoEscolhida,
                null, null, null);
        Cobrancas.Resumo resumo = cobrancas.resumo(lista);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("pacotes e serviços", "Cobranças",
                "O que os pacotes e os atendimentos geraram. Aprovar vira título.",
                filtro("Origem", origemEscolhida,
                        java.util.List.of("todas", "PACOTE", "SERVICO"),
                        escolhido -> {
                            origemEscolhida = escolhido;
                            janela.atualizar();
                        }),
                filtro("Situação", situacaoEscolhida,
                        java.util.List.of("todas", "PENDENTE", "APROVADA", "ENVIADA", "PAGA",
                                "CANCELADA", "SEM_VALOR"),
                        escolhido -> {
                            situacaoEscolhida = escolhido;
                            janela.atualizar();
                        }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Cobrado", Pecas.dinheiro(resumo.cobrado()),
                        "somando tudo", true, false),
                Pecas.quadro("Recebido", Pecas.dinheiro(resumo.recebido()), "já entrou"),
                Pecas.quadro("Pendente", Pecas.dinheiro(resumo.pendente()),
                        "ainda não entrou", false, resumo.pendente().signum() > 0),
                Pecas.quadro("Gratuidades", Pecas.dinheiro(resumo.gratuito()),
                        "registrado, sem valor a receber"))));

        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Serviços", () -> janela.ir(TelaServicos.class));
        partes.put("Pacotes", () -> janela.ir(TelaPacotes.class));
        partes.put("Serviços realizados", () -> janela.ir(TelaRealizados.class));
        partes.put("Cobranças", () -> janela.ir(TelaCobrancas.class));
        partes.put("Fechamento", () -> janela.ir(TelaFechamento.class));
        tela.getChildren().add(Pecas.abas("Cobranças", partes));


        tela.getChildren().add(Pecas.secao("Cobranças"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Origem", Cobranca::getOrigemResumida, 1)
                .coluna("Cliente", c -> c.getPagador() == null ? "" : c.getPagador().getNome(), 2)
                .coluna("O que gerou", Cobranca::getDescricao, 2)
                .coluna("Período", Cobranca::getPeriodoResumido)
                .valor("Valor original", c -> Pecas.numero(c.getValorOriginal()))
                .valor("Desconto", c -> Pecas.numero(c.getDesconto()))
                .valor("Valor final", c -> Pecas.numero(c.getValorFinal()))
                .coluna("Vencimento", c -> Pecas.data(c.getVencimento()))
                .coluna("Cobrança", c -> c.getSituacao() == null ? ""
                        : c.getSituacao().name().toLowerCase())
                .comMarca(Cobranca::getSituacaoDoPagamento,
                        c -> c.estaPaga() ? "s-pago" : "")
                .aoClicar(c -> janela.ir(TelaCobranca.class, c.getId()))
                .quandoVazia("Nenhuma cobrança gerada ainda.")
                .montar());
        return tela;
    }
}
