package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Cobranca;
import br.com.itia.financeiro.dominio.ItemDaCobranca;
import br.com.itia.financeiro.dominio.SituacaoDaCobranca;
import br.com.itia.financeiro.servico.Cobrancas;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * A ficha de uma cobrança.
 *
 * Mostra de onde ela veio, o que entrou no valor, e o caminho dela até virar
 * título: ajustar o valor, aprovar e mandar para o sistema de cobrança.
 */
@Component
public class TelaCobranca implements TelaDeUmSo {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Cobrancas cobrancas;
    private final Janela janela;

    private UUID qual;

    public TelaCobranca(Cobrancas cobrancas, @Lazy Janela janela) {
        this.cobrancas = cobrancas;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    @Override
    public void escolher(UUID id) {
        this.qual = id;
    }

    @Override
    public Node montar() {
        Cobranca cobranca = cobrancas.cobranca(qual);
        boolean aberta = cobranca.getSituacao() == SituacaoDaCobranca.PENDENTE;

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("cobrança", cobranca.getDescricao(),
                (cobranca.getPagador() == null ? "" : cobranca.getPagador().getNome() + " · ")
                        + cobranca.getOrigemResumida(),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaCobrancas.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Valor final", Pecas.dinheiro(cobranca.getValorFinal()),
                        cobranca.temDesconto()
                                ? "com " + Pecas.dinheiro(cobranca.getDesconto()) + " de desconto"
                                : "sem desconto", true, false),
                Pecas.quadro("Situação da cobrança", cobranca.getSituacao() == null ? ""
                        : cobranca.getSituacao().name().toLowerCase(),
                        cobranca.getAprovadaPor() == null ? "ainda não aprovada"
                                : "aprovada por " + cobranca.getAprovadaPor()),
                Pecas.quadro("Pagamento", cobranca.getSituacaoDoPagamento(),
                        cobranca.getTitulo() == null ? "ainda não virou título"
                                : "título " + cobranca.getTitulo().getNumero()),
                Pecas.quadro("Período de referência", cobranca.getPeriodoResumido(),
                        "vence em " + Pecas.data(cobranca.getVencimento())))));


        tela.getChildren().add(Pecas.secao("Valor, desconto e vencimento"));
        tela.getChildren().add(ajuste(cobranca, aberta));

        tela.getChildren().add(Pecas.secao("Itens e quantidades"));
        tela.getChildren().add(Tabela.de(cobranca.getItens())
                .coluna("Item", ItemDaCobranca::getDescricao, 2.6)
                .valor("Quantidade", i -> Pecas.numero(i.getQuantidade()))
                .valor("Valor unitário", i -> Pecas.numero(i.getValorUnitario()))
                .valor("Total", i -> Pecas.numero(i.getTotal()))
                .quandoVazia("Esta cobrança não foi detalhada em itens.")
                .montar());

        tela.getChildren().add(Pecas.secao("Envio ao sistema de cobrança"));
        tela.getChildren().add(envio(cobranca));
        return tela;
    }

    /** O que dá para mexer enquanto a cobrança não virou título. */
    private VBox ajuste(Cobranca cobranca, boolean aberta) {
        if (!aberta) {
            return Pecas.vazio("Esta cobrança já foi aprovada ou cancelada. O valor não muda "
                    + "mais por aqui.");
        }

        TextField valor = new TextField(Pecas.numero(cobranca.getValorOriginal()));
        TextField desconto = new TextField(Pecas.numero(cobranca.getDesconto()));
        DatePicker vencimento = new DatePicker(cobranca.getVencimento());
        vencimento.setMaxWidth(Double.MAX_VALUE);
        TextField justificativa = new TextField(cobranca.getJustificativa());
        justificativa.setPromptText("por que houve desconto");

        HBox linha = new HBox(16, Pecas.campo("Valor original (R$)", valor),
                Pecas.campo("Desconto (R$)", desconto),
                Pecas.campo("Vencimento", vencimento));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        return Pecas.caixa(linha, Pecas.campo("Justificativa do desconto", justificativa),
                new HBox(12,
                        Pecas.botaoVazado("Salvar valores", () -> {
                            cobrancas.ajustarValor(qual, numero(valor.getText()),
                                    numero(desconto.getText()), justificativa.getText(),
                                    vencimento.getValue());
                            janela.avisar("Valores salvos.");
                            janela.atualizar();
                        }),
                        Pecas.botao("Aprovar e lançar título", () -> {
                            cobrancas.aprovar(qual);
                            janela.avisar("Cobrança aprovada. O título já está no contas a "
                                    + "receber.");
                            janela.atualizar();
                        }),
                        Pecas.botaoPerigo("Cancelar cobrança", () -> {
                            cobrancas.cancelar(qual);
                            janela.avisar("Cobrança cancelada.");
                            janela.atualizar();
                        })));
    }

    /** A parte que fala com o sistema de cobrança de fora. */
    private VBox envio(Cobranca cobranca) {
        Label identificacao = new Label(cobranca.getReferenciaExterna() == null
                ? "ainda não enviada" : cobranca.getReferenciaExterna());
        Label quando = new Label(cobranca.getEnviadaEm() == null ? "nunca"
                : cobranca.getEnviadaEm().format(QUANDO));
        Label noFinanceiro = new Label(cobranca.getTitulo() == null ? "ainda não virou título"
                : "título " + cobranca.getTitulo().getNumero());

        HBox linha = new HBox(16,
                Pecas.campo("Identificação no sistema de cobrança", identificacao),
                Pecas.campo("Enviada em", quando),
                Pecas.campo("Identificação no financeiro", noFinanceiro));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        if (cobrancas.sistemaDeCobranca().isEmpty()) {
            return Pecas.caixa(linha, new Label("Nenhum sistema de cobrança ligado. "
                    + "Configure em Integrações para poder enviar."));
        }

        if (cobranca.getUltimoErro() != null) {
            Label erro = new Label("Último erro: " + cobranca.getUltimoErro());
            erro.getStyleClass().add("recado");
            erro.setWrapText(true);
            return Pecas.caixa(linha, erro, new HBox(botaoDeEnviar()));
        }
        return Pecas.caixa(linha, new HBox(botaoDeEnviar()));
    }

    private javafx.scene.control.Button botaoDeEnviar() {
        return Pecas.botao("Enviar ao sistema de cobrança", () -> {
            cobrancas.enviar(qual);
            janela.avisar("Cobrança enviada. O que voltou fica no movimento das integrações.");
            janela.atualizar();
        });
    }

    private BigDecimal numero(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
