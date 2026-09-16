package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.servico.Regras;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * As regras desta empresa: o que ela liga e o que ela desliga.
 *
 * O que for mudado aqui vale daqui em diante. Nada mexe no que já foi lançado.
 */
@Component
public class TelaRegras implements Tela {

    private final Regras regras;
    private final Janela janela;

    public TelaRegras(Regras regras, @Lazy Janela janela) {
        this.regras = regras;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "";
    }

    @Override
    public Node montar() {
        RegraDaEmpresa regra = regras.daEmpresa();

        CheckBox pixIdentificador = new CheckBox("Identificador próprio em cada cobrança");
        pixIdentificador.setSelected(regra.isPixIdentificador());

        CheckBox pixAutomatico = new CheckBox("Oferecer Pix Automático");
        pixAutomatico.setSelected(regra.isPixAutomatico());

        CheckBox agenteSeIdentifica = new CheckBox("O agente se identifica como robô");
        agenteSeIdentifica.setSelected(regra.isAgenteSeIdentifica());

        CheckBox cobrarJuros = new CheckBox("Cobrar do atraso");
        cobrarJuros.setSelected(regra.isCobrarJuros());

        TextField juros = new TextField(Pecas.numero(regra.getJurosAoMes()));
        TextField multa = new TextField(Pecas.numero(regra.getMultaPorAtraso()));
        TextField carencia = new TextField(String.valueOf(regra.getCarenciaDias()));

        Label aviso = new Label("O acréscimo é calculado na hora, até a data de hoje. O valor "
                + "original do título nunca muda.");
        aviso.getStyleClass().add("dica");
        aviso.setWrapText(true);

        HBox campos = new HBox(16, Pecas.campo("Juros ao mês (%)", juros),
                Pecas.campo("Multa por atraso (%)", multa),
                Pecas.campo("Carência (dias)", carencia));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        VBox tela = new VBox(16, Pecas.cabecalho("configuração", "Regras desta empresa",
                "O que for mudado aqui vale daqui em diante.",
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaPainel.class))));

        Label sobrePix = new Label("Com o identificador ligado, cada cobrança sai com um código "
                + "próprio e o aviso do banco encontra o título sozinho, sem ninguém ler "
                + "comprovante.");
        sobrePix.getStyleClass().add("dica");
        sobrePix.setWrapText(true);

        Label sobreAgente = new Label("Ligado, o agente responde que é um atendimento automático "
                + "quando o cliente pergunta. Desligado, ele segue a conversa sem tocar no "
                + "assunto.");
        sobreAgente.getStyleClass().add("dica");
        sobreAgente.setWrapText(true);

        tela.getChildren().add(Pecas.caixa(Pecas.secao("Cobrança por PIX"),
                pixIdentificador, pixAutomatico, sobrePix,
                Pecas.secao("Atendimento automático"), agenteSeIdentifica, sobreAgente,
                Pecas.secao("Juros e multa por atraso"), cobrarJuros, campos, aviso,
                new HBox(Pecas.botao("Salvar regras", () -> {
                    regras.salvar(pixIdentificador.isSelected(), pixAutomatico.isSelected(),
                            agenteSeIdentifica.isSelected(), cobrarJuros.isSelected(),
                            valor(juros.getText()), valor(multa.getText()),
                            inteiro(carencia.getText()));
                    janela.avisar("Regras salvas. Elas valem daqui em diante, sem mexer no que "
                            + "já foi lançado.");
                    janela.ir(TelaRegras.class);
                }))));
        return tela;
    }

    private BigDecimal valor(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private int inteiro(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return 0;
        }
    }
}
