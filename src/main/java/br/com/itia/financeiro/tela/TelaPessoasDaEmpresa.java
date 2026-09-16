package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Usuario;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.servico.Acessos;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem cuida desta empresa, e até onde cada um pode ir.
 *
 * A alçada é por empresa: a mesma pessoa pode ser diretora em uma e operadora
 * em outra. Ninguém tira a própria alçada.
 */
@Component
public class TelaPessoasDaEmpresa implements Tela {

    private final Acessos acessos;
    private final ContextoEmpresa contexto;
    private final Janela janela;

    public TelaPessoasDaEmpresa(Acessos acessos, ContextoEmpresa contexto, @Lazy Janela janela) {
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
        UUID empresaId = contexto.exigirEmpresaId();
        List<UsuarioEmpresa> crachas = acessos.daEmpresa(empresaId);
        Map<UUID, Usuario> pessoas = acessos.pessoas(crachas);

        VBox tela = new VBox(16, Pecas.cabecalho("configurações",
                "Quem cuida desta empresa",
                "A alçada é por empresa. A mesma pessoa pode ter alçadas diferentes em cada uma.",
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaPainel.class))));

        tela.getChildren().add(Pecas.secao("Pessoas com acesso"));
        tela.getChildren().add(Tabela.de(crachas)
                .coluna("Nome", c -> nome(pessoas, c), 2)
                .coluna("E-mail", c -> email(pessoas, c), 2)
                .comMarca(c -> c.getPapel().name().toLowerCase(),
                        c -> c.getPapel() == UsuarioEmpresa.Papel.DIRETOR ? "s-pago" : "")
                .quandoVazia("Ninguém tem acesso a esta empresa ainda.")
                .montar());

        if (!crachas.isEmpty()) {
            ComboBox<UsuarioEmpresa> quem = new ComboBox<>();
            quem.getItems().addAll(crachas);
            quem.setConverter(new StringConverter<>() {
                @Override
                public String toString(UsuarioEmpresa cracha) {
                    return cracha == null ? "" : nome(pessoas, cracha);
                }

                @Override
                public UsuarioEmpresa fromString(String texto) {
                    return null;
                }
            });
            quem.getSelectionModel().selectFirst();
            quem.setMaxWidth(Double.MAX_VALUE);

            ComboBox<UsuarioEmpresa.Papel> papel = new ComboBox<>();
            papel.getItems().addAll(UsuarioEmpresa.Papel.values());
            papel.getSelectionModel().selectFirst();
            papel.setMaxWidth(Double.MAX_VALUE);

            Label aviso = new Label("Operador lança e recebe. Gestor também cancela e dá baixa "
                    + "na mão. Diretor mexe nas regras e nos acessos.");
            aviso.getStyleClass().add("dica");
            aviso.setWrapText(true);

            HBox campos = new HBox(16, Pecas.campo("Pessoa", quem),
                    Pecas.campo("Alçada", papel));
            campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

            tela.getChildren().add(Pecas.secao("Mudar a alçada de alguém"));
            tela.getChildren().add(Pecas.caixa(aviso, campos,
                    new HBox(Pecas.botao("Guardar alçada", () -> {
                        if (quem.getValue() == null) {
                            janela.reclamar("Escolha a pessoa.");
                            return;
                        }
                        acessos.mudarPapel(quem.getValue().getUsuarioId(), empresaId,
                                papel.getValue(), contexto.getUsuarioId());
                        janela.avisar("Alçada trocada.");
                        janela.ir(TelaPessoasDaEmpresa.class);
                    }))));
        }
        return tela;
    }

    private String nome(Map<UUID, Usuario> pessoas, UsuarioEmpresa cracha) {
        Usuario pessoa = pessoas.get(cracha.getUsuarioId());
        return pessoa == null ? "" : pessoa.getNome();
    }

    private String email(Map<UUID, Usuario> pessoas, UsuarioEmpresa cracha) {
        Usuario pessoa = pessoas.get(cracha.getUsuarioId());
        return pessoa == null || pessoa.getEmail() == null ? "" : pessoa.getEmail();
    }
}
