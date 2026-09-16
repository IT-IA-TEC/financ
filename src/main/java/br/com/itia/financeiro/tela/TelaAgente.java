package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Agente;
import br.com.itia.financeiro.dominio.AtendimentoDoAgente;
import br.com.itia.financeiro.servico.AgenteDeAtendimento;
import br.com.itia.financeiro.servico.Regras;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * O agente que responde sozinho, dentro do horário combinado.
 *
 * Ele só responde o que dá para responder com dado do próprio sistema. Quando
 * o assunto é negociação, ele passa para uma pessoa em vez de inventar.
 */
@Component
public class TelaAgente implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final AgenteDeAtendimento agente;
    private final Regras regras;
    private final Janela janela;

    public TelaAgente(AgenteDeAtendimento agente, Regras regras, @Lazy Janela janela) {
        this.agente = agente;
        this.regras = regras;
        this.janela = janela;
    }

    /** Uma linha da lista do que o agente sabe responder. */
    private record Entendimento(String pergunta, String resposta) {
    }

    private static final java.util.List<Entendimento> SABE_RESPONDER = java.util.List.of(
            new Entendimento("quanto eu devo, qual o saldo, tem algo em aberto",
                    "lista o que está em aberto, com valor e vencimento, e soma o total"),
            new Entendimento("como eu pago, manda o PIX, segunda via",
                    "manda a chave PIX da empresa e o código do documento"),
            new Entendimento("já paguei, segue o comprovante",
                    "pede o comprovante e marca o caso como esperando conferência"),
            new Entendimento("pago sexta, pago dia 20",
                    "registra a promessa com a data e marca o caso para conferir"),
            new Entendimento("quero parcelar, consigo desconto",
                    "passa para uma pessoa e avisa o cliente de que passou"),
            new Entendimento("essa cobrança está errada, quero cancelar",
                    "passa para uma pessoa e marca o caso como contestado"));

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        Agente configuracao = agente.daEmpresa();
        List<AtendimentoDoAgente> ultimos = agente.ultimos();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("cobrança", "Agente de atendimento",
                "Responde o que dá para responder com dado do sistema. O resto vai para uma "
                        + "pessoa.",
                Pecas.quadrosDoTopo(
                Pecas.quadro("Situação", configuracao.isAtivo() ? "ligado" : "desligado",
                        configuracao.isAtivo() ? "respondendo no horário" : "ninguém responde",
                        true, false),
                Pecas.quadro("Atendidos", String.valueOf(agente.quantosAtendidos()),
                        "respondidos sozinho"),
                Pecas.quadro("Passou adiante", String.valueOf(agente.quantosEscalados()),
                        "quando não dava para responder"),
                Pecas.quadro("Diz que é robô",
                        regras.daEmpresa().isAgenteSeIdentifica() ? "sim" : "não",
                        "muda nas regras da empresa"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Agente"));


        tela.getChildren().add(Pecas.secao("O que ele sabe responder"));
        tela.getChildren().add(Tabela.de(SABE_RESPONDER)
                .coluna("Quando o cliente diz", Entendimento::pergunta, 1.6)
                .coluna("O que o agente faz", Entendimento::resposta, 2.4)
                .montar());

        CheckBox ativo = new CheckBox("Agente ligado");
        ativo.setSelected(configuracao.isAtivo());

        TextField saudacao = new TextField(configuracao.getSaudacao());
        TextField assinatura = new TextField(configuracao.getAssinatura());
        TextField comeca = new TextField(String.valueOf(configuracao.getComecaAs()));
        TextField termina = new TextField(String.valueOf(configuracao.getTerminaAs()));
        CheckBox sabado = new CheckBox("responde aos sábados");
        sabado.setSelected(configuracao.isRespondeSabado());

        HBox linha1 = new HBox(16, Pecas.campo("Saudação", saudacao),
                Pecas.campo("Assinatura", assinatura));
        HBox linha2 = new HBox(16, Pecas.campo("Começa às", comeca),
                Pecas.campo("Termina às", termina));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Como ele trabalha"));
        tela.getChildren().add(Pecas.caixa(ativo, linha1, linha2, sabado,
                new HBox(Pecas.botao("Guardar", () -> {
                    agente.salvar(ativo.isSelected(), saudacao.getText(), assinatura.getText(),
                            hora(comeca.getText(), 8), hora(termina.getText(), 18),
                            sabado.isSelected());
                    janela.avisar(ativo.isSelected()
                            ? "Agente ligado. Ele responde dentro do horário combinado."
                            : "Agente desligado. As mensagens ficam esperando gente.");
                    janela.ir(TelaAgente.class);
                }))));

        TextField frase = new TextField();
        frase.setPromptText("qual o valor do meu boleto?");
        tela.getChildren().add(Pecas.secao("Testar sem mandar nada"));
        tela.getChildren().add(Pecas.caixa(
                new HBox(16, Pecas.campo("Testar o entendimento", frase)),
                new HBox(Pecas.botao("Ver o que ele entenderia", () -> {
                    if (frase.getText() == null || frase.getText().isBlank()) {
                        janela.reclamar("Escreva uma frase para testar.");
                        return;
                    }
                    String intencao = agente.entender(frase.getText());
                    janela.avisar("Nessa frase o agente entenderia: "
                            + intencao.toLowerCase().replace('_', ' ') + ".");
                    janela.atualizar();
                }))));

        tela.getChildren().add(Pecas.secao("Últimos atendimentos"));
        tela.getChildren().add(Tabela.de(ultimos)
                .coluna("Quando", a -> a.getOcorridoEm() == null ? ""
                        : a.getOcorridoEm().format(QUANDO))
                .coluna("O cliente disse", AtendimentoDoAgente::getPergunta, 2)
                .coluna("Entendeu como", AtendimentoDoAgente::getIntencaoLegivel)
                .coluna("Observação", a -> a.getMotivo() == null ? "" : a.getMotivo(), 2)
                .comMarca(a -> a.isEscalado() ? "passou para gente" : "respondido",
                        a -> a.isEscalado() ? "s-vencido" : "s-pago")
                .quandoVazia("Nenhum atendimento ainda.")
                .montar());
        return tela;
    }

    private int hora(String texto, int padrao) {
        try {
            int hora = Integer.parseInt(texto.trim());
            return hora < 0 || hora > 23 ? padrao : hora;
        } catch (RuntimeException naoEhNumero) {
            return padrao;
        }
    }
}
