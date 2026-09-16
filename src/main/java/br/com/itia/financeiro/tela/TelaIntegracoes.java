package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Conector;
import br.com.itia.financeiro.dominio.EventoIntegracao;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.TipoAutenticacao;
import br.com.itia.financeiro.dominio.TipoIntegracao;
import br.com.itia.financeiro.servico.BaixaAutomatica;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.IntegracaoServico;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Integrações: as ligações do sistema com o mundo de fora.
 *
 * Cada ligação diz o que falta para funcionar, em vez de falhar calada na hora
 * do uso.
 */
@Component
public class TelaIntegracoes implements Tela {

    private static final java.time.format.DateTimeFormatter QUANDO =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
    private static final java.time.format.DateTimeFormatter CHECAGEM =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final IntegracaoServico integracoes;
    private final BaixaAutomatica baixa;
    private final ContextoEmpresa contexto;
    private final Janela janela;

    public TelaIntegracoes(IntegracaoServico integracoes, BaixaAutomatica baixa,
                           ContextoEmpresa contexto, @Lazy Janela janela) {
        this.integracoes = integracoes;
        this.baixa = baixa;
        this.contexto = contexto;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "integracoes";
    }

    @Override
    public Node montar() {
        List<Integracao> lista = integracoes.daEmpresa();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("ligações com outros sistemas", "Integrações",
                "As ligações do sistema com o mundo de fora, e o que falta em cada uma.",
                Pecas.botaoVazado("Tipos de integração",
                        () -> janela.ir(TelaIntegracoesAjuda.class)),
                Pecas.botao("+ Nova integração", this::abrirNovaLigacao)));

        tela.getChildren().add(Pecas.secao("Ligações"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Integração", i -> i.getNome() + (i.getProvedor() == null ? ""
                        : "  ·  " + i.getProvedor().getRotulo()), 1.8)
                .coluna("Como funciona", i -> i.getTipo() == null ? ""
                        : i.getTipo().getRotulo(), 1.2)
                .coluna("Acesso", i -> i.getAutenticacao() == null ? ""
                        : i.getAutenticacao().getRotulo(), 1)
                .coluna("Ambiente", Integracao::getAmbiente, 0.8)
                .coluna("Última checagem", i -> i.getUltimaChecagemEm() == null ? "nunca testada"
                        : i.getUltimaChecagemEm().format(CHECAGEM)
                                + (Boolean.TRUE.equals(i.getUltimaChecagemOk()) ? " ok"
                                        : " falhou"), 1.1)
                .coluna("Falta", i -> String.join(", ", integracoes.faltando(i)), 1.6)
                .comMarca(i -> i.isAtiva() ? "ligada" : "desligada",
                        i -> i.isAtiva() ? "s-pago" : "s-cancelado")
                .aoClicar(i -> janela.ir(TelaIntegracao.class, i.getId()))
                .quandoVazia("Nenhuma integração configurada nesta empresa. Veja os tipos "
                        + "possíveis antes de criar a primeira.")
                .montar());

        tela.getChildren().add(Pecas.cabecalho("entradas e saídas",
                "Último movimento das integrações",
                "Tudo o que entrou e tudo o que saiu, com o que voltou de cada chamada.",
                Pecas.botaoVazado("Reprocessar avisos parados", () -> {
                    int quantos = baixa.reprocessarParados(contexto.exigirEmpresaId());
                    janela.avisar(quantos == 0 ? "Nenhum aviso parado para reprocessar."
                            : quantos + " aviso(s) reprocessados.");
                    janela.atualizar();
                })));
        tela.getChildren().add(Tabela.de(integracoes.ultimosEventos())
                .coluna("Quando", e -> e.getOcorridoEm() == null ? ""
                        : e.getOcorridoEm().format(QUANDO), 0.9)
                .coluna("Direção", EventoIntegracao::getDirecao, 0.7)
                .coluna("Operação", EventoIntegracao::getTipo, 1.2)
                .coluna("Referência", EventoIntegracao::getReferencia, 1.2)
                .coluna("Conteúdo", e -> e.getErro() != null ? e.getErro()
                        : e.getResumoDaCarga(), 2.4)
                .comMarca(EventoIntegracao::getStatus,
                        e -> "ERRO".equals(e.getStatus()) ? "s-vencido"
                                : "PROCESSADO".equals(e.getStatus()) ? "s-pago" : "")
                .quandoVazia("Nenhum movimento registrado ainda.")
                .montar());
        return tela;
    }

    /** A criação de ligação abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovaLigacao() {
        ComboBox<Conector> provedor = new ComboBox<>();
        provedor.getItems().addAll(Conector.values());
        provedor.getSelectionModel().selectFirst();
        provedor.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TipoIntegracao> tipo = new ComboBox<>();
        tipo.getItems().addAll(TipoIntegracao.values());
        tipo.getSelectionModel().selectFirst();
        tipo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TipoAutenticacao> autenticacao = new ComboBox<>();
        autenticacao.getItems().addAll(TipoAutenticacao.values());
        autenticacao.getSelectionModel().selectFirst();
        autenticacao.setMaxWidth(Double.MAX_VALUE);

        TextField nome = new TextField();
        nome.setPromptText("WhatsApp da empresa");

        Label dica = new Label("Depois de criar, abra a ligação para preencher endereço, chave "
                + "e segredo. Nada disso aparece na tela depois de guardado.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        HBox campos = new HBox(16, Pecas.campo("Nome desta ligação", nome),
                Pecas.campo("Modelo", provedor), Pecas.campo("Tipo de ligação", tipo),
                Pecas.campo("Forma de acesso", autenticacao));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Como esta integração vai funcionar",
                        "Uma ligação nova do sistema com um serviço de fora.")
                .com(campos, dica)
                .acao("Criar ligação", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome da ligação.");
                        return false;
                    }
                    Integracao nova = integracoes.criar(provedor.getValue(), tipo.getValue(),
                            autenticacao.getValue(), nome.getText().trim());
                    janela.avisar("Ligação criada. Agora preencha o que falta nela.");
                    janela.ir(TelaIntegracao.class, nova.getId());
                    return true;
                })
                .abrir();
    }
}
