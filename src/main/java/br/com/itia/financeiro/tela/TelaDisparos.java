package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Mensagens;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Disparo em lote: falar com muita gente de uma vez.
 *
 * Nada sai antes de alguém conferir. O sistema monta a prévia, mostra quem vai
 * receber e quem ficou de fora, e só então o disparo pode ser confirmado.
 */
@Component
public class TelaDisparos implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Mensagens mensagens;
    private final ContextoEmpresa contexto;
    private final Janela janela;

    public TelaDisparos(Mensagens mensagens, ContextoEmpresa contexto, @Lazy Janela janela) {
        this.mensagens = mensagens;
        this.contexto = contexto;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        mensagens.prepararEmpresa();
        List<LoteDeMensagem> lotes = mensagens.lotes();
        List<ModeloDeMensagem> modelos = mensagens.modelosAtivos();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("cobrança em lote", "Disparos",
                "Monte a prévia, confira quem entra e quem fica de fora, e só então confirme.",
                Pecas.botao("+ Novo disparo", () -> abrirNovoDisparo(modelos)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Na fila", String.valueOf(mensagens.quantasNaFila()),
                        "esperando sair", true, false),
                Pecas.quadro("Disparos", String.valueOf(lotes.size()), "já montados"),
                Pecas.quadro("Modelos", String.valueOf(modelos.size()), "prontos para usar"),
                Pecas.quadro("Chave PIX",
                        contexto.exigirEmpresa().getChavePix() == null ? "falta" : "ok",
                        contexto.exigirEmpresa().getChavePix() == null
                                ? "cadastre para entrar no texto" : "entra sozinha no texto"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Disparos"));


        tela.getChildren().add(Pecas.secao("Disparos montados"));
        tela.getChildren().add(Tabela.de(lotes)
                .coluna("Quando", l -> l.getCriadoEm() == null ? ""
                        : l.getCriadoEm().format(QUANDO), 0.8)
                .coluna("Disparo", LoteDeMensagem::getNome, 1.6)
                .coluna("Recorte", LoteDeMensagem::getFiltro, 1.4)
                .coluna("Canal", LoteDeMensagem::getCanal)
                .valor("Vão receber", l -> String.valueOf(l.getQuantidade()))
                .valor("Ficaram de fora", l -> String.valueOf(l.getFora()))
                .valor("Valor", l -> Pecas.numero(l.getValorTotal()))
                .coluna("Confirmado", l -> l.getConfirmadoEm() == null ? ""
                        : l.getConfirmadoEm().format(QUANDO))
                .comMarca(LoteDeMensagem::getSituacaoLegivel,
                        l -> "CONFIRMADO".equals(l.getSituacao()) ? "s-pago"
                                : "CANCELADO".equals(l.getSituacao()) ? "s-cancelado" : "")
                .aoClicar(l -> janela.ir(TelaDisparo.class, l.getId()))
                .quandoVazia("Nenhum disparo montado ainda.")
                .montar());
        return tela;
    }

    /** O disparo novo abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovoDisparo(List<ModeloDeMensagem> modelos) {
        if (modelos.isEmpty()) {
            janela.reclamar("Nenhum modelo de mensagem cadastrado. Crie um em Modelos.");
            return;
        }

        TextField nome = new TextField();
        nome.setPromptText("cobrança de setembro");

        ComboBox<ModeloDeMensagem> modelo = new ComboBox<>();
        modelo.getItems().addAll(modelos);
        modelo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModeloDeMensagem qual) {
                return qual == null ? "" : qual.getNome();
            }

            @Override
            public ModeloDeMensagem fromString(String texto) {
                return null;
            }
        });
        modelo.getSelectionModel().selectFirst();
        modelo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> visao = new ComboBox<>();
        visao.getItems().addAll("vencidos", "a_vencer", "todos");
        visao.getSelectionModel().selectFirst();
        visao.setMaxWidth(Double.MAX_VALUE);

        TextField de = new TextField();
        de.setPromptText("de quantos dias");
        TextField ate = new TextField();
        ate.setPromptText("até quantos dias");

        ComboBox<String> canal = new ComboBox<>();
        canal.getItems().addAll("WHATSAPP", "EMAIL");
        canal.getSelectionModel().selectFirst();
        canal.setMaxWidth(Double.MAX_VALUE);

        HBox campos = new HBox(16,
                Pecas.campo("Nome deste disparo", nome),
                Pecas.campo("Texto", modelo),
                Pecas.campo("Canal", canal),
                Pecas.campo("Quem entra", visao));
        HBox faixa = new HBox(16, Pecas.campo("De (dias)", de), Pecas.campo("Até (dias)", ate));
        campos.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));
        faixa.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Novo disparo", "Cobrança em lote.")
                .com(campos, faixa)
                .acao("Montar a prévia", () -> {
                    montarPrevia(nome.getText(), modelo.getValue(), visao.getValue(),
                            de.getText(), ate.getText());
                    return true;
                })
                .abrir();
    }

    private void montarPrevia(String nome, ModeloDeMensagem modelo, String visao, String de,
                              String ate) {
        if (modelo == null) {
            janela.reclamar("Escolha o modelo da mensagem.");
            return;
        }
        LoteDeMensagem lote = mensagens.montarPrevia(
                nome == null || nome.isBlank() ? null : nome.trim(),
                modelo.getId(), modelo.getCanal(), visao, inteiro(de), inteiro(ate));
        janela.avisar("Prévia pronta: " + lote.getQuantidade() + " vão receber e "
                + lote.getFora() + " ficaram de fora. Confira antes de confirmar.");
        janela.ir(TelaDisparo.class, lote.getId());
    }

    private Integer inteiro(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException naoEhNumero) {
            return null;
        }
    }
}
