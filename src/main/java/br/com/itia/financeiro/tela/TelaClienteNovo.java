package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.servico.CarteiraServico;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cadastrar cliente.
 *
 * A pessoa vem primeiro, e as unidades dela vêm em seguida. Se o CPF já existir
 * na carteira, o sistema não cria um cadastro repetido: ele pergunta se as
 * unidades novas são da mesma pessoa.
 */
@Component
public class TelaClienteNovo implements Tela {

    private final CarteiraServico carteira;
    private final TelaClientes lista;
    private final Janela janela;

    private final List<HBox> linhasDeUnidade = new ArrayList<>();

    public TelaClienteNovo(CarteiraServico carteira, TelaClientes lista, @Lazy Janela janela) {
        this.carteira = carteira;
        this.lista = lista;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "clientes";
    }

    @Override
    public Node montar() {
        linhasDeUnidade.clear();

        TextField nome = new TextField();
        nome.setPromptText("nome de quem paga");
        TextField cpf = new TextField();
        cpf.setPromptText("somente números");
        TextField whatsapp = new TextField();
        whatsapp.setPromptText("com DDD");
        TextField telefone = new TextField();
        TextField email = new TextField();

        VBox unidades = new VBox(10);
        unidades.getChildren().add(linhaDeUnidade());

        VBox tela = new VBox(16, Pecas.cabecalho("nova pessoa", "Cadastrar cliente",
                "Quem paga é a pessoa. As unidades são as empresas ou lojas dela."));

        tela.getChildren().add(Pecas.caixa(
                linha(Pecas.campo("Nome", nome), Pecas.campo("CPF", cpf)),
                linha(Pecas.campo("WhatsApp", whatsapp), Pecas.campo("Telefone", telefone),
                        Pecas.campo("E-mail", email))));

        tela.getChildren().add(Pecas.secao("Unidades desta pessoa"));
        Label explicacao = new Label("Deixe o nome da unidade em branco para usar o nome da "
                + "própria pessoa. É o caso de quem tem uma só.");
        explicacao.getStyleClass().add("dica");

        HBox maisUma = new HBox(Pecas.botaoVazado("+ Adicionar outro CNPJ",
                () -> unidades.getChildren().add(linhaDeUnidade())));

        tela.getChildren().add(Pecas.caixa(explicacao, unidades, maisUma));

        tela.getChildren().add(new HBox(12,
                Pecas.botao("Cadastrar", () -> cadastrar(nome.getText(), cpf.getText(),
                        whatsapp.getText(), telefone.getText(), email.getText())),
                Pecas.botaoVazado("Cancelar", () -> janela.ir(TelaClientes.class))));
        return tela;
    }

    private HBox linhaDeUnidade() {
        TextField nome = new TextField();
        nome.setPromptText("nome da unidade");
        TextField documento = new TextField();
        documento.setPromptText("CNPJ ou CPF");
        TextField codigo = new TextField();
        codigo.setPromptText("código de origem, se houver");

        HBox linha = linha(Pecas.campo("Nome da unidade", nome),
                Pecas.campo("CNPJ da unidade", documento),
                Pecas.campo("Código de origem", codigo));
        linhasDeUnidade.add(linha);
        return linha;
    }

    private HBox linha(Node... campos) {
        HBox linha = new HBox(16, campos);
        for (Node campo : campos) {
            HBox.setHgrow(campo, Priority.ALWAYS);
        }
        return linha;
    }

    private void cadastrar(String nome, String cpf, String whatsapp, String telefone,
                           String email) {
        if (nome == null || nome.isBlank()) {
            janela.reclamar("Escreva o nome de quem paga.");
            return;
        }
        List<CarteiraServico.UnidadeInformada> unidades = unidadesDigitadas();

        // CPF já cadastrado: em vez de duplicar a pessoa, soma as unidades nela
        Optional<Pagador> jaExiste = carteira.pessoaComEsseCpf(cpf);
        if (jaExiste.isPresent()) {
            perguntarSeSoma(jaExiste.get(), unidades);
            return;
        } else {
            carteira.cadastrar(nome.trim(), vazioViraNulo(cpf), vazioViraNulo(whatsapp),
                    vazioViraNulo(telefone), vazioViraNulo(email), unidades);
            janela.avisar("Cliente cadastrado.");
        }
        lista.limparBusca();
        janela.ir(TelaClientes.class);
    }

    /**
     * CPF já cadastrado: em vez de duplicar a pessoa, o sistema pergunta se as
     * unidades novas são da mesma pessoa. Nenhum cadastro novo é criado sem a
     * resposta.
     */
    private void perguntarSeSoma(Pagador dono, List<CarteiraServico.UnidadeInformada> unidades) {
        Label quem = new Label("Já existe um cliente com esse CPF nesta empresa: "
                + dono.getNome() + (dono.getCpf() == null ? "" : "  ·  " + dono.getCpf()) + ".");
        quem.setWrapText(true);

        Label pergunta = new Label("Quer somar as unidades informadas a esse cliente? Nenhum "
                + "cadastro novo de pessoa é criado, e o histórico do cliente continua inteiro.");
        pergunta.getStyleClass().add("dica");
        pergunta.setWrapText(true);

        JanelaFlutuante caixa = JanelaFlutuante.estreita(janela.palco(),
                "Esse CPF já tem cliente", "CPF já cadastrado.");
        caixa.com(quem, pergunta, Tabela.de(unidades)
                .coluna("Unidade", u -> u.nome() == null ? "(sem nome)" : u.nome(), 2)
                .coluna("CNPJ", CarteiraServico.UnidadeInformada::documento)
                .coluna("Código", CarteiraServico.UnidadeInformada::codigo)
                .quandoVazia("Nenhuma unidade informada.")
                .montar());
        caixa.acao("Somar ao cliente existente", () -> {
            carteira.somarUnidades(dono.getId(), unidades);
            janela.avisar("As unidades foram somadas a " + dono.getNome()
                    + ", sem criar cadastro repetido.");
            lista.limparBusca();
            janela.ir(TelaClientes.class);
            return true;
        });
        caixa.abrir();
    }

    private List<CarteiraServico.UnidadeInformada> unidadesDigitadas() {
        List<CarteiraServico.UnidadeInformada> unidades = new ArrayList<>();
        for (HBox linha : linhasDeUnidade) {
            String nome = textoDoCampo(linha, 0);
            String documento = textoDoCampo(linha, 1);
            String codigo = textoDoCampo(linha, 2);
            if (nome != null || documento != null || codigo != null) {
                unidades.add(new CarteiraServico.UnidadeInformada(nome, documento, codigo));
            }
        }
        return unidades;
    }

    /** Cada campo da linha é um rótulo em cima e o campo embaixo. */
    private String textoDoCampo(HBox linha, int posicao) {
        VBox caixa = (VBox) linha.getChildren().get(posicao);
        TextField campo = (TextField) caixa.getChildren().get(1);
        return vazioViraNulo(campo.getText());
    }

    private String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
