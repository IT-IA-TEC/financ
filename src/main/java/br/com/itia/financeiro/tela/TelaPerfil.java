package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Quem está trabalhando agora, e a saída do sistema.
 */
@Component
public class TelaPerfil implements Tela {

    private final ContextoEmpresa contexto;
    private final FinanceiroServico financeiro;
    private final Janela janela;

    public TelaPerfil(ContextoEmpresa contexto, FinanceiroServico financeiro,
                      @Lazy Janela janela) {
        this.contexto = contexto;
        this.financeiro = financeiro;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "";
    }

    @Override
    public Node montar() {
        VBox tela = new VBox(16, Pecas.cabecalho("meu perfil", contexto.getNome(),
                contexto.getEmail() == null ? "sem e-mail informado" : contexto.getEmail()));

        String papel = contexto.getPapel() == null ? "sem alçada nesta empresa"
                : contexto.getPapel().name().toLowerCase();
        String empresa = contexto.temEmpresaEscolhida()
                ? contexto.exigirEmpresa().getNome() : "nenhuma escolhida";

        java.util.List<Empresa> empresas = financeiro.empresasAtivas();
        tela.getChildren().add(Pecas.quadros(
                Pecas.quadro("Empresas que você acessa", String.valueOf(empresas.size()),
                        "onde você pode trabalhar", true, false),
                Pecas.quadro("Empresa aberta agora", contexto.temEmpresaEscolhida()
                        ? contexto.exigirEmpresa().getApelido() : "nenhuma", empresa),
                Pecas.quadro("Acesso", papel, "sua alçada nesta empresa")));

        tela.getChildren().add(Pecas.secao("Suas empresas"));
        tela.getChildren().add(Tabela.de(empresas)
                .coluna("Empresa", Empresa::getNome, 2)
                .coluna("Apelido", Empresa::getApelido, 0.8)
                .coluna("CNPJ", Empresa::getCnpj, 1)
                .aoClicar(qual -> {
                    contexto.escolher(qual.getId());
                    janela.ir(TelaPainel.class);
                })
                .quandoVazia("Nenhuma empresa cadastrada.")
                .montar());

        tela.getChildren().add(Pecas.caixa(
                Pecas.campo("Empresa em que você está", new javafx.scene.control.Label(empresa)),
                Pecas.campo("Sua alçada aqui", new javafx.scene.control.Label(papel)),
                new HBox(12,
                        Pecas.botaoVazado("Trocar de empresa",
                                () -> janela.ir(TelaEmpresas.class)),
                        Pecas.botaoVazado("Empresas", () -> janela.ir(
                                TelaEmpresasConfigurar.class)),
                        Pecas.botaoVazado("Regras desta empresa",
                                () -> janela.ir(TelaRegras.class)),
                        Pecas.botaoVazado("Quem cuida desta empresa",
                                () -> janela.ir(TelaPessoasDaEmpresa.class)),
                        Pecas.botaoPerigo("Sair do sistema", this::sair))));
        return tela;
    }

    private void sair() {
        contexto.sair();
        janela.limparRecados();
        janela.ir(TelaEntrar.class);
    }
}
