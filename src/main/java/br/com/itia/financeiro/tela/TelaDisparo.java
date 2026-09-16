package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ForaDoLote;
import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.servico.Mensagens;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A prévia de um disparo: quem vai receber, o que cada um vai ler, e quem
 * ficou de fora com o motivo.
 *
 * Enquanto não é confirmado, nada sai.
 */
@Component
public class TelaDisparo implements TelaDeUmSo {

    private final Mensagens mensagens;
    private final Janela janela;

    private UUID qual;

    public TelaDisparo(Mensagens mensagens, @Lazy Janela janela) {
        this.mensagens = mensagens;
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
        LoteDeMensagem lote = mensagens.lote(qual);
        List<Mensagem> lista = mensagens.mensagensDo(qual);
        List<ForaDoLote> foras = mensagens.forasDo(qual);
        Map<UUID, String> nomes = mensagens.nomesDosTitulos();

        Label situacao = new Label(lote.getSituacaoLegivel());
        situacao.getStyleClass().addAll("marca-situacao",
                "CONFIRMADO".equals(lote.getSituacao()) ? "s-pago"
                        : "CANCELADO".equals(lote.getSituacao()) ? "s-cancelado" : "");

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("disparo", lote.getNome(),
                lote.getQuantidade() + " vão receber · " + lote.getFora() + " ficaram de fora",
                situacao, Pecas.botaoVazado("Voltar", () -> janela.ir(TelaDisparos.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Vão receber", String.valueOf(lote.getQuantidade()),
                        "pelo canal " + lote.getCanal(), true, false),
                Pecas.quadro("Valor somado", Pecas.dinheiro(lote.getValorTotal()),
                        "o que está sendo cobrado"),
                Pecas.quadro("Ficaram de fora", String.valueOf(lote.getFora()),
                        "cada um com o motivo", false, lote.getFora() > 0))));


        if ("PREVIA".equals(lote.getSituacao())) {
            tela.getChildren().add(Pecas.secao("Confirmar"));
            Label aviso = new Label("Esta é a prévia. Nenhuma mensagem saiu ainda. Confira a "
                    + "lista e confirme para colocar tudo na fila.");
            aviso.getStyleClass().add("dica");
            aviso.setWrapText(true);

            javafx.scene.control.DatePicker dia =
                    new javafx.scene.control.DatePicker(java.time.LocalDate.now());
            javafx.scene.control.TextField hora = new javafx.scene.control.TextField("09:00");
            hora.setPrefWidth(90);

            HBox quando = new HBox(12, new Label("Sair em"), dia, hora,
                    Pecas.botao("Confirmar o disparo", () -> {
                        mensagens.confirmar(qual, dia.getValue(), horaDe(hora.getText()));
                        janela.avisar("Disparo confirmado. As mensagens estão na fila.");
                        janela.ir(TelaDisparo.class, qual);
                    }),
                    Pecas.botaoPerigo("Cancelar este disparo", () -> {
                        mensagens.cancelarLote(qual);
                        janela.avisar("Disparo cancelado. Nada saiu.");
                        janela.ir(TelaDisparo.class, qual);
                    }));
            quando.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tela.getChildren().add(Pecas.caixa(aviso, quando));
        }

        tela.getChildren().add(Pecas.secao("Quem vai receber"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Cliente", m -> nomes.getOrDefault(m.getTituloId(), "cliente"), 1.2)
                .coluna("Destino", Mensagem::getDestino, 1.2)
                .coluna("Mensagem", Mensagem::getCorpo, 3)
                .comMarca(Mensagem::getSituacaoLegivel,
                        m -> m.saiu() ? "s-pago" : "FALHOU".equals(m.getSituacao())
                                ? "s-vencido" : "")
                .aoClicar(m -> oQueFazer(lote, m))
                .quandoVazia("Ninguém entrou neste disparo.")
                .montar());

        Label comoMexer = new Label("Clique numa mensagem para marcar como enviada, dizer que "
                + "não foi entregue ou tirar do disparo.");
        comoMexer.getStyleClass().add("dica");
        tela.getChildren().add(comoMexer);

        tela.getChildren().add(Pecas.secao("Quem ficou de fora"));
        tela.getChildren().add(Tabela.de(foras)
                .coluna("Quem", ForaDoLote::getQuem, 1.5)
                .coluna("Por quê", ForaDoLote::getMotivo, 3)
                .quandoVazia("Ninguém ficou de fora.")
                .montar());
        return tela;
    }

    /** O que dá para fazer com uma mensagem do disparo. */
    private void oQueFazer(LoteDeMensagem lote, Mensagem mensagem) {
        if (!mensagem.naFila()) {
            janela.reclamar("Esta mensagem não está mais na fila.");
            return;
        }
        javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
        motivo.setPromptText("o que deu errado");

        Label corpo = new Label(mensagem.getCorpo());
        corpo.setWrapText(true);

        JanelaFlutuante caixa = JanelaFlutuante.estreita(janela.palco(), "O que fazer",
                mensagem.getDestino());
        caixa.com(corpo, Pecas.campo("Se não foi entregue, por quê", motivo));
        caixa.outraAcao("Não entregue", () -> {
            mensagens.marcarFalha(mensagem.getId(), motivo.getText());
            janela.avisar("Marcada como não entregue.");
            janela.ir(TelaDisparo.class, qual);
        });
        if ("PREVIA".equals(lote.getSituacao())) {
            caixa.outraAcao("Tirar do disparo", () -> {
                mensagens.tirarDoLote(mensagem.getId());
                janela.avisar("Mensagem tirada do disparo.");
                janela.ir(TelaDisparo.class, qual);
            });
        }
        caixa.acao("Marcar como enviada", () -> {
            mensagens.marcarEnviada(mensagem.getId(), null);
            janela.avisar("Marcada como enviada.");
            janela.ir(TelaDisparo.class, qual);
            return true;
        });
        caixa.abrir();
    }

    private java.time.LocalTime horaDe(String texto) {
        try {
            return java.time.LocalTime.parse(texto.trim());
        } catch (RuntimeException naoEhHora) {
            return java.time.LocalTime.of(9, 0);
        }
    }
}
