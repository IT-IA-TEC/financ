package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * As empresas do grupo: cadastrar e ver o movimento de cada uma.
 *
 * Empresa não se apaga por engano: só sai quem não tem nenhum movimento.
 */
@Component
public class TelaEmpresasConfigurar implements Tela {

    private final FinanceiroServico financeiro;
    private final Janela janela;

    public TelaEmpresasConfigurar(FinanceiroServico financeiro, @Lazy Janela janela) {
        this.financeiro = financeiro;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "";
    }

    @Override
    public Node montar() {
        List<Empresa> empresas = financeiro.todasAsEmpresas();
        Map<UUID, long[]> movimento = financeiro.movimentoPorEmpresa();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("configurações", "Configurar empresas",
                "Cada empresa tem clientes, títulos e números próprios.",
                Pecas.botao("+ Cadastrar nova empresa", this::abrirNovaEmpresa),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaEmpresas.class))));

        tela.getChildren().add(Pecas.secao("Empresas cadastradas"));
        tela.getChildren().add(Tabela.de(empresas)
                .coluna("Apelido", Empresa::getApelido, 0.8)
                .coluna("Nome", Empresa::getNome, 2)
                .coluna("CNPJ", Empresa::getCnpj, 1.2)
                .coluna("Chave PIX", Empresa::getChavePix, 1.4)
                .valor("Clientes", e -> String.valueOf(
                        movimento.getOrDefault(e.getId(), new long[]{0, 0})[0]))
                .valor("Títulos", e -> String.valueOf(
                        movimento.getOrDefault(e.getId(), new long[]{0, 0})[1]))
                .comMarca(e -> e.isAtiva() ? "ativa" : "desativada",
                        e -> e.isAtiva() ? "" : "s-cancelado")
                .aoClicar(this::abrirEmpresa)
                .quandoVazia("Nenhuma empresa cadastrada ainda.")
                .montar());

        Label comoAbrir = new Label("Clique numa empresa para mudar os dados dela.");
        comoAbrir.getStyleClass().add("dica");
        tela.getChildren().add(comoAbrir);

        return tela;
    }


    /** A empresa nova abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovaEmpresa() {
        TextField apelido = new TextField();
        apelido.setPromptText("apelido curto, sem espaço");
        TextField nome = new TextField();
        nome.setPromptText("nome completo da empresa");
        TextField cnpj = new TextField();
        TextField pix = new TextField();
        pix.setPromptText("chave que recebe os pagamentos");

        HBox campos = new HBox(16, Pecas.campo("Apelido", apelido), Pecas.campo("Nome", nome),
                Pecas.campo("CNPJ", cnpj), Pecas.campo("Chave PIX que recebe", pix));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Cadastrar nova empresa",
                        "Uma empresa nova, com clientes e números próprios.")
                .com(campos)
                .acao("Cadastrar empresa", () -> {
                    if (apelido.getText() == null || apelido.getText().isBlank()
                            || nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o apelido e o nome da empresa.");
                        return false;
                    }
                    financeiro.cadastrarEmpresa(apelido.getText().trim().toLowerCase(),
                            nome.getText().trim(), vazioViraNulo(cnpj.getText()),
                            vazioViraNulo(pix.getText()));
                    janela.avisar("Empresa cadastrada.");
                    janela.ir(TelaEmpresasConfigurar.class);
                    return true;
                })
                .abrir();
    }

    /** Os dados de uma empresa, com desativar, reativar e excluir. */
    private void abrirEmpresa(Empresa empresa) {
        TextField apelido = new TextField(empresa.getApelido());
        TextField nome = new TextField(empresa.getNome());
        TextField cnpj = new TextField(empresa.getCnpj());
        TextField pix = new TextField(empresa.getChavePix());

        HBox campos = new HBox(16, Pecas.campo("Apelido", apelido), Pecas.campo("Nome", nome),
                Pecas.campo("CNPJ", cnpj), Pecas.campo("Chave PIX que recebe", pix));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        Label aviso = new Label("Excluir só acontece com a empresa vazia. Empresa que já tem "
                + "cliente ou título guarda histórico de dinheiro, e isso não se apaga: nesse "
                + "caso o caminho é desativar.");
        aviso.getStyleClass().add("dica");
        aviso.setWrapText(true);

        JanelaFlutuante caixa = JanelaFlutuante.estreita(janela.palco(), empresa.getNome(),
                "Os dados desta empresa.");
        caixa.com(campos, aviso);
        if (empresa.isAtiva()) {
            caixa.outraAcao("Desativar", () -> {
                financeiro.desativarEmpresa(empresa.getId());
                janela.avisar("Empresa desativada.");
                janela.ir(TelaEmpresasConfigurar.class);
            });
        } else {
            caixa.outraAcao("Reativar", () -> {
                financeiro.reativarEmpresa(empresa.getId());
                janela.avisar("Empresa reativada.");
                janela.ir(TelaEmpresasConfigurar.class);
            });
        }
        caixa.outraAcao("Excluir", () -> {
            financeiro.excluirEmpresa(empresa.getId());
            janela.avisar("Empresa excluída.");
            janela.ir(TelaEmpresasConfigurar.class);
        });
        caixa.acao("Salvar alterações", () -> {
            if (apelido.getText() == null || apelido.getText().isBlank()
                    || nome.getText() == null || nome.getText().isBlank()) {
                janela.reclamar("Escreva o apelido e o nome da empresa.");
                return false;
            }
            financeiro.atualizarEmpresa(empresa.getId(),
                    apelido.getText().trim().toLowerCase(), nome.getText().trim(),
                    vazioViraNulo(cnpj.getText()), vazioViraNulo(pix.getText()));
            janela.avisar("Empresa atualizada.");
            janela.ir(TelaEmpresasConfigurar.class);
            return true;
        });
        caixa.abrir();
    }

    private String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
