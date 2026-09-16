package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.servico.Acessos;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A porta de entrada de verdade: em qual empresa a pessoa vai trabalhar.
 *
 * Cada empresa tem clientes, títulos e números próprios. Nada se mistura entre
 * elas, e é aqui que a separação começa.
 */
@Component
public class TelaEmpresas implements Tela {

    private final FinanceiroServico financeiro;
    private final Acessos acessos;
    private final ContextoEmpresa contexto;
    private final Janela janela;

    public TelaEmpresas(FinanceiroServico financeiro, Acessos acessos, ContextoEmpresa contexto,
                        @Lazy Janela janela) {
        this.financeiro = financeiro;
        this.acessos = acessos;
        this.contexto = contexto;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "";
    }

    @Override
    public Node montar() {
        List<Empresa> empresas = financeiro.empresasAtivas();
        Map<UUID, long[]> movimento = financeiro.movimentoPorEmpresa();

        VBox tela = new VBox(16, Pecas.cabecalho("porta de entrada",
                "Em qual empresa você vai trabalhar?",
                "Cada empresa tem clientes, títulos e números próprios. "
                        + "Nada se mistura entre elas."));

        if (empresas.isEmpty()) {
            tela.getChildren().add(Pecas.vazio(
                    "Nenhuma empresa cadastrada ainda. Cadastre a primeira em configurações."));
            return tela;
        }

        FlowPane cartoes = new FlowPane(14, 14);
        for (Empresa empresa : empresas) {
            long[] numeros = movimento.getOrDefault(empresa.getId(), new long[]{0, 0});
            cartoes.getChildren().add(cartao(empresa, numeros));
        }
        tela.getChildren().add(cartoes);
        return tela;
    }

    /**
     * O cartão de empresa: 282 de largura por 168 de altura mínima, nas listras
     * da marca com borda vermelha. O pé é o único pedaço branco do cartão.
     */
    private VBox cartao(Empresa empresa, long[] numeros) {
        Label apelido = new Label(empresa.getApelido() == null ? ""
                : empresa.getApelido().toUpperCase(new java.util.Locale("pt", "BR")));
        apelido.getStyleClass().add("apelido-empresa");

        Label nome = new Label(empresa.getNome());
        nome.getStyleClass().add("nome-empresa");
        nome.setWrapText(true);

        VBox alto = new VBox(6, apelido, nome);
        alto.setPadding(new javafx.geometry.Insets(12, 16, 12, 16));
        VBox.setVgrow(alto, javafx.scene.layout.Priority.ALWAYS);

        Label movimento = new Label(numeros[0] + " clientes · " + numeros[1] + " títulos");
        movimento.getStyleClass().add("movimento-empresa");

        Label entrar = new Label("Entrar →");
        entrar.getStyleClass().add("entrar-empresa");

        HBox pe = new HBox(10, movimento, Pecas.empurrar(), entrar);
        pe.getStyleClass().add("pe-empresa");
        pe.setAlignment(Pos.CENTER_LEFT);
        pe.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));

        VBox cartao = new VBox(alto, pe);
        cartao.getStyleClass().add("cartao-empresa");
        cartao.setPrefSize(282, 168);
        cartao.setMinSize(282, 168);
        cartao.setMaxSize(282, 168);
        cartao.setOnMouseClicked(clique -> entrarNa(empresa.getId()));
        return cartao;
    }

    private void entrarNa(UUID empresaId) {
        contexto.escolher(empresaId);
        // a alçada é por empresa: ao trocar de empresa, o crachá muda junto
        if (contexto.getUsuarioId() != null) {
            contexto.assumirPapel(acessos.crachaDe(contexto.getUsuarioId(), empresaId).getPapel());
        }
        janela.limparRecados();
        janela.ir(TelaPainel.class);
    }
}
