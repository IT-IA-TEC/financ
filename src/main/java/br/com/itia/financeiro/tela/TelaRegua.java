package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.dominio.PassoDaRegua;
import br.com.itia.financeiro.servico.Mensagens;
import br.com.itia.financeiro.servico.ReguaDeCadencia;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A régua: o que o sistema fala sozinho, e quando.
 *
 * Cada passo tem um gatilho (antes de vencer, no vencimento, depois de vencer),
 * um modelo de mensagem e uma ordem. O mesmo cliente não recebe duas vezes no
 * mesmo dia.
 */
@Component
public class TelaRegua implements Tela {

    private final ReguaDeCadencia regua;
    private final Mensagens mensagens;
    private final Janela janela;

    public TelaRegua(ReguaDeCadencia regua, Mensagens mensagens, @Lazy Janela janela) {
        this.regua = regua;
        this.mensagens = mensagens;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        mensagens.prepararEmpresa();
        regua.prepararEmpresa();
        List<PassoDaRegua> passos = regua.passos();
        List<ModeloDeMensagem> modelos = mensagens.modelosAtivos();
        Map<UUID, String> nomeDoModelo = modelos.stream()
                .collect(Collectors.toMap(ModeloDeMensagem::getId, ModeloDeMensagem::getNome,
                        (a, b) -> a));

        long ativos = passos.stream().filter(PassoDaRegua::isAtivo).count();
        long antes = passos.stream()
                .filter(p -> p.getGatilho() != null && p.getGatilho().startsWith("ANTES")).count();
        long depois = passos.size() - antes;

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("falar na hora certa", "Régua de cobrança",
                "O que o sistema fala sozinho e em que dia. Ninguém recebe duas vezes no "
                        + "mesmo dia.",
                Pecas.botao("+ Novo passo", this::abrirNovoPasso),
                Pecas.botaoVazado("Desligar um passo", this::abrirDesligar),
                Pecas.botaoVazado("Rodar a régua de hoje", this::rodar),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Passos ativos", String.valueOf(ativos), "falam sozinhos todo dia",
                        true, false),
                Pecas.quadro("Antes de vencer", String.valueOf(antes), "aviso amigável"),
                Pecas.quadro("Depois de vencer", String.valueOf(depois), "cobrança em atraso"),
                Pecas.quadro("Rodada", "08h05", "todo dia, empresa por empresa"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Régua"));

        tela.getChildren().add(Pecas.secao("Passos da régua"));
        tela.getChildren().add(Tabela.de(passos)
                .coluna("Ordem", p -> String.valueOf(p.getOrdem()), 0.4)
                .coluna("Nome", PassoDaRegua::getNome, 1.6)
                .coluna("Quando fala", PassoDaRegua::getQuandoLegivel, 1.4)
                .coluna("Modelo", p -> nomeDoModelo.getOrDefault(p.getModeloId(), ""), 1.4)
                .coluna("Canal", PassoDaRegua::getCanal, 0.7)
                .coluna("Antes de sair", p -> p.isExigeConfirmacao() ? "alguém confere"
                        : "sai sozinho", 1)
                .coluna("Última rodada", p -> Pecas.data(p.getUltimaRodada()), 0.9)
                .comMarca(p -> p.isAtivo() ? "ativo" : "desligado",
                        p -> p.isAtivo() ? "" : "s-cancelado")
                .quandoVazia("Nenhum passo na régua. Cadastre o primeiro abaixo.")
                .montar());

        return tela;
    }

    /** O passo novo abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovoPasso() {
        List<ModeloDeMensagem> modelos = mensagens.modelosAtivos();
        if (modelos.isEmpty()) {
            janela.reclamar("Nenhum modelo de mensagem cadastrado. Crie um em Modelos.");
            return;
        }

        TextField nome = new TextField();
        nome.setPromptText("aviso três dias antes");

        ComboBox<String> gatilho = new ComboBox<>();
        gatilho.getItems().addAll("ANTES_DE_VENCER", "NO_VENCIMENTO", "DEPOIS_DE_VENCER");
        gatilho.getSelectionModel().selectFirst();
        gatilho.setMaxWidth(Double.MAX_VALUE);

        TextField dias = new TextField("3");
        TextField ordem = new TextField("1");

        ComboBox<ModeloDeMensagem> modelo = escolherModelo(modelos);

        ComboBox<String> canal = new ComboBox<>();
        canal.getItems().addAll("WHATSAPP", "EMAIL");
        canal.getSelectionModel().selectFirst();
        canal.setMaxWidth(Double.MAX_VALUE);
        CheckBox confirmar = new CheckBox("olhar antes de mandar");

        HBox campos = new HBox(16,
                Pecas.campo("Nome do passo", nome),
                Pecas.campo("Quando", gatilho),
                Pecas.campo("Dias", dias),
                Pecas.campo("Texto que sai", modelo),
                Pecas.campo("Canal", canal),
                Pecas.campo("Ordem", ordem));
        campos.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Novo passo da régua",
                        "O que o sistema fala sozinho e em que dia.")
                .com(campos, confirmar)
                .acao("Guardar o passo", () -> {
            if (nome.getText() == null || nome.getText().isBlank() || modelo.getValue() == null) {
                janela.reclamar("Escreva o nome do passo e escolha o modelo.");
                return false;
            }
            regua.salvar(null, nome.getText().trim(), gatilho.getValue(),
                    inteiro(dias.getText(), 0), modelo.getValue().getId(),
                    canal.getValue(), inteiro(ordem.getText(), 1),
                    confirmar.isSelected(), true);
            janela.avisar("Passo guardado na régua.");
            janela.ir(TelaRegua.class);
            return true;
                })
                .abrir();
    }

    /** O desligamento também abre por cima, em vez de ficar solto na página. */
    private void abrirDesligar() {
        List<PassoDaRegua> passos = regua.passos();
        ComboBox<PassoDaRegua> qual = new ComboBox<>();
        qual.getItems().addAll(passos.stream().filter(PassoDaRegua::isAtivo).toList());
        qual.setConverter(converter(PassoDaRegua::getNome));
        qual.getSelectionModel().selectFirst();
        qual.setMaxWidth(Double.MAX_VALUE);

        Label aviso = new Label("Passo desligado não fala mais com ninguém, mas continua "
                + "registrado com tudo o que já fez.");
        aviso.getStyleClass().add("dica");

        HBox linha = new HBox(16, Pecas.campo("Passo", qual));
        linha.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Desligar um passo",
                        "Ele para de falar, mas continua registrado.")
                .com(aviso, linha)
                .acao("Desligar", () -> {
                    if (qual.getValue() == null) {
                        janela.reclamar("Escolha o passo que vai ser desligado.");
                        return false;
                    }
                    regua.desativar(qual.getValue().getId());
                    janela.avisar("Passo desativado. Ele não fala mais com ninguém.");
                    janela.ir(TelaRegua.class);
                    return true;
                })
                .abrir();
    }

    private void rodar() {
        LoteDeMensagem lote = regua.rodarHoje();
        if (lote == null) {
            janela.reclamar("Nenhum passo ativo na régua. Cadastre pelo menos um.");
            janela.atualizar();
            return;
        }
        janela.avisar("A régua rodou: " + lote.getQuantidade() + " mensagens montadas e "
                + lote.getFora() + " ficaram de fora. Confira antes de confirmar.");
        janela.ir(TelaDisparo.class, lote.getId());
    }

    private ComboBox<ModeloDeMensagem> escolherModelo(List<ModeloDeMensagem> modelos) {
        ComboBox<ModeloDeMensagem> modelo = new ComboBox<>();
        modelo.getItems().addAll(modelos);
        modelo.setConverter(converter(ModeloDeMensagem::getNome));
        modelo.getSelectionModel().selectFirst();
        modelo.setMaxWidth(Double.MAX_VALUE);
        return modelo;
    }

    private <T> StringConverter<T> converter(Function<T, String> nome) {
        return new StringConverter<>() {
            @Override
            public String toString(T qual) {
                return qual == null ? "" : nome.apply(qual);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        };
    }

    private int inteiro(String texto, int padrao) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return padrao;
        }
    }
}
