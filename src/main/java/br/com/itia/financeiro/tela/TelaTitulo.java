package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.FinanceiroServico;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.servico.Regras;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A ficha de um título: o que foi lançado, o que já foi pago e o que falta.
 *
 * O valor original nunca muda. O acréscimo do atraso é calculado na hora, até
 * hoje, e só quando a empresa liga essa regra.
 */
@Component
public class TelaTitulo implements TelaDeUmSo {

    private static final DateTimeFormatter COMPETENCIA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final FinanceiroServico financeiro;
    private final Regras regras;
    private final Janela janela;

    private UUID qual;

    public TelaTitulo(FinanceiroServico financeiro, Regras regras, @Lazy Janela janela) {
        this.financeiro = financeiro;
        this.regras = regras;
        this.janela = janela;
    }

    @Override
    public void escolher(UUID id) {
        this.qual = id;
    }

    @Override
    public String secao() {
        return "titulos";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        Titulo titulo = financeiro.titulo(qual);
        RegraDaEmpresa regra = regras.daEmpresa();
        BigDecimal acrescimo = regra.acrescimoDe(titulo.getSaldo(), titulo.getVencimento(), hoje);

        Label situacao = new Label(titulo.estaVencido(hoje)
                ? "vencido há " + titulo.diasDeAtraso(hoje) + " dias"
                : titulo.getSituacao().name().toLowerCase());
        situacao.getStyleClass().addAll("marca-situacao",
                titulo.estaVencido(hoje) ? "s-vencido"
                        : "s-" + titulo.getSituacao().name().toLowerCase());

        List<Node> quadros = new ArrayList<>(List.of(
                Pecas.quadro("Valor", Pecas.dinheiro(titulo.getValor()),
                        "vence em " + Pecas.data(titulo.getVencimento()), true, false),
                Pecas.quadro("Já pago", Pecas.dinheiro(titulo.getTotalPago()),
                        titulo.getPagamentos().size() + " pagamento(s)"),
                Pecas.quadro("Saldo", Pecas.dinheiro(titulo.getSaldo()),
                        "o que ainda falta", false, true)));
        if (acrescimo.signum() > 0) {
            quadros.add(Pecas.quadro("Atualizado hoje",
                    Pecas.dinheiro(titulo.getSaldo().add(acrescimo)),
                    "com " + Pecas.dinheiro(acrescimo) + " de multa e juros", false, true));
        }
        quadros.add(Pecas.quadro("Código do PIX", titulo.getIdentificadorPix(),
                "é ele que faz a baixa automática"));

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("título nº " + titulo.getNumero(),
                titulo.getCliente().getRazaoSocial(),
                titulo.getDescricao() + " · competência "
                        + titulo.getCompetencia().format(COMPETENCIA),
                situacao, Pecas.botaoVazado("Voltar", () -> janela.ir(TelaTitulos.class)),
                Pecas.quadrosDoTopo(quadros.toArray(new Node[0]))));

        tela.getChildren().add(Pecas.secao("Pagamentos"));
        tela.getChildren().add(Tabela.de(titulo.getPagamentos())
                .coluna("Pago em", p -> Pecas.data(p.getPagoEm()))
                .valor("Valor", p -> Pecas.numero(p.getValor()))
                .coluna("Forma", Pagamento::getForma)
                .coluna("Transação", p -> p.getTransacaoId() == null ? "" : p.getTransacaoId())
                .coluna("Conferido por", p -> p.getConferidoPor() == null ? ""
                        : p.getConferidoPor())
                .quandoVazia("Nenhum pagamento recebido neste título.")
                .montar());

        String situacaoAgora = titulo.getSituacao().name();
        if (!"CANCELADO".equals(situacaoAgora) && !"PAGO".equals(situacaoAgora)) {
            tela.getChildren().add(Pecas.secao("Lançar pagamento"));
            tela.getChildren().add(formularioDePagamento(titulo, hoje));
        }
        if (titulo.getTotalPago().signum() == 0 && !"CANCELADO".equals(situacaoAgora)) {
            tela.getChildren().add(Pecas.secao("Cancelar título"));
            tela.getChildren().add(formularioDeCancelamento(titulo));
        }
        return tela;
    }

    private VBox formularioDePagamento(Titulo titulo, LocalDate hoje) {
        TextField valor = new TextField(Pecas.numero(titulo.getSaldo()));
        DatePicker pagoEm = new DatePicker(hoje);
        pagoEm.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> forma = new ComboBox<>();
        forma.getItems().addAll("PIX", "TRANSFERENCIA", "BOLETO", "DINHEIRO");
        forma.getSelectionModel().selectFirst();
        forma.setMaxWidth(Double.MAX_VALUE);

        TextField transacao = new TextField();
        transacao.setPromptText("opcional, barra pagamento repetido");

        HBox campos = new HBox(16,
                Pecas.campo("Valor recebido (R$)", valor),
                Pecas.campo("Pago em", pagoEm),
                Pecas.campo("Forma", forma),
                Pecas.campo("Identificador da transação", transacao));
        campos.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        return Pecas.caixa(campos, new HBox(Pecas.botao("Dar baixa",
                () -> receber(titulo.getId(), valor.getText(), pagoEm.getValue(),
                        forma.getValue(), transacao.getText()))));
    }

    private VBox formularioDeCancelamento(Titulo titulo) {
        TextField motivo = new TextField();
        motivo.setPromptText("lançado em duplicidade");

        HBox campos = new HBox(16, Pecas.campo("Motivo do cancelamento", motivo));
        campos.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        return Pecas.caixa(campos, new HBox(Pecas.botaoPerigo("Cancelar título",
                () -> cancelar(titulo.getId(), motivo.getText()))));
    }

    private void receber(UUID id, String valor, LocalDate pagoEm, String forma,
                         String transacao) {
        BigDecimal quanto = TelaTituloNovo.dinheiro(valor);
        if (quanto == null || quanto.signum() <= 0) {
            janela.reclamar("Escreva o valor recebido, maior que zero.");
            return;
        }
        financeiro.receberPagamento(id, quanto, pagoEm, forma,
                transacao == null || transacao.isBlank() ? null : transacao.trim());
        janela.avisar("Pagamento lançado.");
        janela.ir(TelaTitulo.class, id);
    }

    private void cancelar(UUID id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            janela.reclamar("Escreva o motivo do cancelamento.");
            return;
        }
        financeiro.cancelarTitulo(id, motivo.trim());
        janela.avisar("Título cancelado.");
        janela.ir(TelaTitulo.class, id);
    }
}
