package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.Acordos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Um acordo: o que entrou, quanto ficou combinado e como está sendo pago.
 *
 * Quebrar o acordo cancela as parcelas ainda em aberto e devolve os documentos
 * originais para a cobrança, do jeito que estavam.
 */
@Component
public class TelaAcordo implements TelaDeUmSo {

    private final Acordos acordos;
    private final Janela janela;

    private UUID qual;

    public TelaAcordo(Acordos acordos, @Lazy Janela janela) {
        this.acordos = acordos;
        this.janela = janela;
    }

    @Override
    public void escolher(UUID id) {
        this.qual = id;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        Acordo acordo = acordos.acordo(qual);
        List<Titulo> documentos = acordos.documentosDo(qual);
        List<Titulo> parcelas = acordos.parcelasDo(qual);

        Label situacao = new Label(acordo.getSituacaoLegivel());
        situacao.getStyleClass().addAll("marca-situacao",
                acordo.estaAtivo() ? "s-pago" : "s-cancelado");

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("acordo de dívida · nº " + acordo.getNumero(),
                "Acordo " + acordo.getNumero(),
                acordo.getParcelas() + "x a partir de "
                        + Pecas.data(acordo.getPrimeiroVencimento()),
                situacao, Pecas.botaoVazado("Voltar", () -> janela.ir(TelaAcordos.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Valor original", Pecas.dinheiro(acordo.getValorOriginal()),
                        documentos.size() + " documentos", true, false),
                Pecas.quadro("Acréscimo", Pecas.dinheiro(acordo.getAcrescimo()),
                        "multa e juros do atraso"),
                Pecas.quadro("Desconto", Pecas.dinheiro(acordo.getDesconto()),
                        acordo.getMotivoDesconto() == null ? "sem desconto"
                                : acordo.getMotivoDesconto()),
                Pecas.quadro("Combinado", Pecas.dinheiro(acordo.getValorCombinado()),
                        "o que o cliente vai pagar"))));


        tela.getChildren().add(Pecas.secao("Parcelas do acordo"));
        tela.getChildren().add(Tabela.de(parcelas)
                .coluna("Nº", t -> String.valueOf(t.getNumero()), 0.4)
                .coluna("Descrição", Titulo::getDescricao, 2)
                .coluna("Vencimento", t -> Pecas.data(t.getVencimento()))
                .valor("Valor", t -> Pecas.numero(t.getValor()))
                .valor("Saldo", t -> Pecas.numero(t.getSaldo()))
                .comMarca(t -> t.estaVencido(hoje) ? t.diasDeAtraso(hoje) + " dias"
                                : t.getSituacao().name().toLowerCase(),
                        t -> t.estaVencido(hoje) ? "s-vencido"
                                : "s-" + t.getSituacao().name().toLowerCase())
                .aoClicar(t -> janela.ir(TelaTitulo.class, t.getId()))
                .quandoVazia("Nenhuma parcela gerada.")
                .montar());

        tela.getChildren().add(Pecas.secao("Documentos que entraram no acordo"));
        tela.getChildren().add(Tabela.de(documentos)
                .coluna("Nº", t -> String.valueOf(t.getNumero()), 0.4)
                .coluna("Descrição", Titulo::getDescricao, 2)
                .coluna("Vencimento", t -> Pecas.data(t.getVencimento()))
                .valor("Saldo que entrou", t -> Pecas.numero(t.getValor()))
                .comMarca(t -> t.getSituacao().name().toLowerCase(),
                        t -> "s-" + t.getSituacao().name().toLowerCase())
                .quandoVazia("Nenhum documento vinculado.")
                .montar());

        if (acordo.estaAtivo()) {
            tela.getChildren().add(Pecas.secao("Quebrar o acordo"));
            Label aviso = new Label("Quebrar cancela as parcelas ainda em aberto e devolve os "
                    + "documentos originais para a cobrança, do jeito que estavam.");
            aviso.getStyleClass().add("dica");
            aviso.setWrapText(true);
            tela.getChildren().add(Pecas.caixa(aviso, new HBox(
                    Pecas.botaoPerigo("Quebrar acordo", this::quebrar))));
        }
        return tela;
    }

    private void quebrar() {
        javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
        motivo.setPromptText("o que aconteceu");
        JanelaFlutuante.estreita(janela.palco(), "Quebrar o acordo",
                        "Os documentos originais voltam para a cobrança, do jeito que estavam.")
                .com(Pecas.campo("Motivo", motivo))
                .acao("Quebrar o acordo", () -> {
                    acordos.quebrar(qual, motivo.getText().isBlank() ? null
                            : motivo.getText().trim());
                    janela.avisar("Acordo quebrado. Os documentos originais voltaram para a "
                            + "cobrança.");
                    janela.ir(TelaAcordo.class, qual);
                    return true;
                })
                .abrir();
    }
}
