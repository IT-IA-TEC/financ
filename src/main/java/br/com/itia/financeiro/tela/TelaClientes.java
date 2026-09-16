package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ColunaCarteira;
import br.com.itia.financeiro.dominio.ArquivoRecebido;
import br.com.itia.financeiro.servico.CadastroDeUnidades;
import br.com.itia.financeiro.servico.CarteiraServico;
import br.com.itia.financeiro.servico.ColunasServico;
import br.com.itia.financeiro.servico.FiltroCarteira;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A carteira: quem são os clientes desta empresa e como cada um está.
 *
 * Uma pessoa pode ter várias unidades (lojas, filiais, CNPJs). A lista mostra
 * uma linha por unidade, com o nome da pessoa do lado, porque é assim que a
 * cobrança acontece: quem paga é a pessoa, o que se cobra é da unidade.
 */
@Component
public class TelaClientes implements Tela {

    private final CarteiraServico carteira;
    private final ColunasServico colunas;
    private final CadastroDeUnidades cadastro;
    private final Janela janela;

    /** O que a pessoa digitou na busca, para não se perder ao voltar. */
    private String busca = "";

    /** O que cada funil de coluna está peneirando agora. */
    private final Map<String, String> filtros = new HashMap<>();

    public TelaClientes(CarteiraServico carteira, ColunasServico colunas,
                        CadastroDeUnidades cadastro, @Lazy Janela janela) {
        this.carteira = carteira;
        this.colunas = colunas;
        this.cadastro = cadastro;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "clientes";
    }

    @Override
    public Node montar() {
        Map<String, String> parametros = new HashMap<>(filtros);
        if (busca != null && !busca.isBlank()) {
            parametros.put("f_" + ColunaCarteira.CLIENTE.name(), busca.trim());
        }
        FiltroCarteira filtro = FiltroCarteira.de(parametros);
        List<CarteiraServico.LinhaPessoa> pessoas = carteira.carteira(filtro);
        List<ColunaCarteira> escolhidas = colunas.escolhidas();

        // uma linha por unidade, com a pessoa junto: é como a cobrança enxerga
        List<Linha> linhas = new ArrayList<>();
        BigDecimal emAberto = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        for (CarteiraServico.LinhaPessoa pessoa : pessoas) {
            for (CarteiraServico.LinhaUnidade unidade : pessoa.unidades()) {
                linhas.add(new Linha(pessoa, unidade));
                emAberto = emAberto.add(unidade.getEmAberto());
                vencido = vencido.add(unidade.getVencido());
            }
        }

        TextField procurar = new TextField(busca);
        procurar.setPromptText("procurar pelo nome do cliente");
        procurar.setPrefWidth(320);
        procurar.setOnAction(acao -> {
            busca = procurar.getText();
            janela.atualizar();
        });

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("carteira", "Clientes",
                "Quem paga é a pessoa. O que se cobra é de cada unidade dela.",
                procurar,
                Pecas.botaoVazado("Colunas", this::abrirColunas),
                Pecas.botaoVazado("Importar base", this::abrirImportar),
                Pecas.botao("Cadastrar cliente",
                        () -> janela.ir(TelaClienteNovo.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Clientes", String.valueOf(pessoas.size()),
                        linhas.size() + " unidades", true, false),
                Pecas.quadro("Em aberto", Pecas.dinheiro(emAberto), "somando as unidades"),
                Pecas.quadro("Vencido", Pecas.dinheiro(vencido), "passou do vencimento",
                        false, vencido.signum() > 0))));


        Tabela<Linha> tabela = Tabela.de(linhas);
        for (ColunaCarteira coluna : escolhidas) {
            double peso = coluna == ColunaCarteira.CLIENTE || coluna == ColunaCarteira.UNIDADE
                    ? 1.6 : 1;
            if (coluna.alinhaADireita()) {
                tabela.valor(coluna.getRotulo(), linha -> linha.valor(coluna));
            } else {
                tabela.coluna(coluna.getRotulo(), linha -> linha.valor(coluna), peso);
            }
            tabela.funil(coluna.getRotulo(), filtro.temFiltro(coluna), () -> filtrar(coluna));
        }
        tabela.comMarca(linha -> linha.unidade().getSituacao(),
                        linha -> corDaSituacao(linha.unidade().getSituacao()))
                .aoClicar(linha -> janela.ir(TelaCliente.class, linha.unidade().getId()))
                .quandoVazia(busca == null || busca.isBlank()
                        ? "Nenhum cliente cadastrado nesta empresa ainda."
                        : "Nenhum cliente encontrado com esse nome.");

        Label comoAbrir = new Label("Clique numa linha para abrir a unidade. "
                + "Para ver a pessoa inteira, use a ficha da pessoa dentro da unidade.");
        comoAbrir.getStyleClass().add("dica");

        tela.getChildren().add(Pecas.secao("Carteira"));
        if (!filtros.isEmpty()) {
            HBox aplicado = new HBox(12,
                    new Label("Filtro aplicado nesta tabela: " + filtros.size() + " coluna(s)."),
                    Pecas.botaoVazado("Limpar filtros", () -> {
                        filtros.clear();
                        janela.atualizar();
                    }));
            aplicado.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            aplicado.getChildren().get(0).getStyleClass().add("dica");
            tela.getChildren().add(aplicado);
        }
        tela.getChildren().add(comoAbrir);
        tela.getChildren().add(tabela.montar());
        return tela;
    }

    /** A janelinha do funil daquela coluna, pelo tipo de conteúdo dela. */
    private void filtrar(ColunaCarteira coluna) {
        switch (coluna.getFormato()) {
            case SITUACAO -> filtrarSituacao(coluna);
            case DINHEIRO, NUMERO -> filtrarFaixa(coluna, false);
            case DATA -> filtrarFaixa(coluna, true);
            default -> filtrarTexto(coluna);
        }
    }

    private void filtrarTexto(ColunaCarteira coluna) {
        TextField procurado = new TextField(filtros.getOrDefault("f_" + coluna.name(), ""));
        procurado.setPromptText("parte do texto");
        aplicar(coluna, Pecas.campo("Contém", procurado),
                () -> guardar("f_" + coluna.name(), procurado.getText()));
    }

    private void filtrarFaixa(ColunaCarteira coluna, boolean ehData) {
        if (ehData) {
            javafx.scene.control.DatePicker de = new javafx.scene.control.DatePicker(
                    data("f_" + coluna.name() + "_de"));
            javafx.scene.control.DatePicker ate = new javafx.scene.control.DatePicker(
                    data("f_" + coluna.name() + "_ate"));
            HBox campos = new HBox(16, Pecas.campo("De", de), Pecas.campo("Até", ate));
            campos.getChildren().forEach(c ->
                    HBox.setHgrow(c, javafx.scene.layout.Priority.ALWAYS));
            aplicar(coluna, campos, () -> {
                guardar("f_" + coluna.name() + "_de",
                        de.getValue() == null ? "" : de.getValue().toString());
                guardar("f_" + coluna.name() + "_ate",
                        ate.getValue() == null ? "" : ate.getValue().toString());
            });
            return;
        }
        TextField de = new TextField(filtros.getOrDefault("f_" + coluna.name() + "_de", ""));
        TextField ate = new TextField(filtros.getOrDefault("f_" + coluna.name() + "_ate", ""));
        HBox campos = new HBox(16, Pecas.campo("De", de), Pecas.campo("Até", ate));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, javafx.scene.layout.Priority.ALWAYS));
        aplicar(coluna, campos, () -> {
            guardar("f_" + coluna.name() + "_de", de.getText());
            guardar("f_" + coluna.name() + "_ate", ate.getText());
        });
    }

    private void filtrarSituacao(ColunaCarteira coluna) {
        String escolhidas = filtros.getOrDefault("f_" + coluna.name(), "");
        List<String> marcadas = escolhidas.isBlank() ? List.of()
                : List.of(escolhidas.split("\\|"));
        List<CheckBox> marcas = new ArrayList<>();
        VBox lista = new VBox(8);
        for (String situacao : ColunaCarteira.situacoes()) {
            CheckBox marca = new CheckBox(situacao);
            marca.setSelected(marcadas.contains(situacao));
            marcas.add(marca);
            lista.getChildren().add(marca);
        }
        aplicar(coluna, lista, () -> {
            List<String> ligadas = new ArrayList<>();
            for (CheckBox marca : marcas) {
                if (marca.isSelected()) {
                    ligadas.add(marca.getText());
                }
            }
            guardar("f_" + coluna.name(), String.join("|", ligadas));
        });
    }

    private void aplicar(ColunaCarteira coluna, Node campos, Runnable oQueGuardar) {
        JanelaFlutuante.estreita(janela.palco(), "Filtrar por "
                        + coluna.getRotulo().toLowerCase(), null)
                .com(campos)
                .acao("Aplicar", () -> {
                    oQueGuardar.run();
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void guardar(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            filtros.remove(campo);
        } else {
            filtros.put(campo, valor.trim());
        }
    }

    private java.time.LocalDate data(String campo) {
        String bruto = filtros.get(campo);
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(bruto);
        } catch (RuntimeException naoEhData) {
            return null;
        }
    }

    /**
     * Escolher o que aparece na tabela. Só aparecem aqui campos que o sistema
     * tem para mostrar, e as colunas fixas ninguém desliga.
     */
    private void abrirColunas() {
        List<ColunaCarteira> escolhidas = colunas.escolhidas();
        List<CheckBox> marcas = new ArrayList<>();

        VBox daPessoa = new VBox(8, Pecas.secao("Da pessoa"));
        for (ColunaCarteira coluna : ColunaCarteira.daPessoa()) {
            daPessoa.getChildren().add(marca(coluna, escolhidas, marcas));
        }
        VBox daUnidade = new VBox(8, Pecas.secao("Da unidade"));
        for (ColunaCarteira coluna : ColunaCarteira.daUnidade()) {
            daUnidade.getChildren().add(marca(coluna, escolhidas, marcas));
        }

        Label dica = new Label("Coluna sempre visível não desliga: ela é o que identifica a "
                + "linha.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Colunas da tela",
                        "O que aparece na carteira.")
                .com(dica, daPessoa, daUnidade)
                .acao("Salvar colunas", () -> {
                    List<String> ligadas = new ArrayList<>();
                    for (CheckBox marca : marcas) {
                        if (marca.isSelected()) {
                            ligadas.add((String) marca.getUserData());
                        }
                    }
                    colunas.salvar(ligadas);
                    janela.avisar("Colunas atualizadas.");
                    janela.ir(TelaClientes.class);
                    return true;
                })
                .abrir();
    }

    private CheckBox marca(ColunaCarteira coluna, List<ColunaCarteira> escolhidas,
                           List<CheckBox> marcas) {
        CheckBox marca = new CheckBox(coluna.getRotulo()
                + (coluna.isFixa() ? "  ·  sempre visível" : ""));
        marca.setUserData(coluna.name());
        marca.setSelected(escolhidas.contains(coluna) || coluna.isFixa());
        marca.setDisable(coluna.isFixa());
        marcas.add(marca);
        return marca;
    }

    /** A base de clientes de fora, num arquivo de texto. */
    private void abrirImportar() {
        Label comoE = new Label("Arquivo de texto com cinco colunas separadas por ponto e "
                + "vírgula: nome da pessoa, CPF, razão social da unidade, CNPJ e WhatsApp.");
        comoE.getStyleClass().add("dica");
        comoE.setWrapText(true);

        Label exemplo = new Label("Maria Souza;123.456.789-00;Loja da Maria;"
                + "12.345.678/0001-99;(11) 99999-0000");
        exemplo.getStyleClass().add("texto");
        exemplo.setWrapText(true);

        Label reaproveita = new Label("Pessoa e unidade são reconhecidas pelo documento. Quem já "
                + "existe é reaproveitado, e nada vira cadastro repetido.");
        reaproveita.getStyleClass().add("dica");
        reaproveita.setWrapText(true);

        Label escolhido = new Label("nenhum arquivo escolhido");
        escolhido.getStyleClass().add("dica");
        java.io.File[] arquivo = new java.io.File[1];

        JanelaFlutuante.estreita(janela.palco(), "Importar a base de clientes", "Carteira.")
                .com(comoE, Pecas.caixa(exemplo), reaproveita,
                        new javafx.scene.layout.HBox(12,
                                Pecas.botaoVazado("Escolher arquivo", () -> {
                                    javafx.stage.FileChooser escolher =
                                            new javafx.stage.FileChooser();
                                    escolher.setTitle("Base de clientes");
                                    escolher.getExtensionFilters().add(
                                            new javafx.stage.FileChooser.ExtensionFilter(
                                                    "Texto", "*.csv", "*.txt"));
                                    java.io.File qual = escolher.showOpenDialog(janela.palco());
                                    if (qual != null) {
                                        arquivo[0] = qual;
                                        escolhido.setText(qual.getName());
                                    }
                                }), escolhido))
                .acao("Importar", () -> {
                    if (arquivo[0] == null) {
                        janela.reclamar("Escolha o arquivo da base.");
                        return false;
                    }
                    try {
                        byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo[0].toPath());
                        CadastroDeUnidades.Importacao feito = cadastro.importarBase(
                                new ArquivoRecebido(arquivo[0].getName(), conteudo));
                        janela.avisar(feito.pessoasNovas() + " pessoa(s) e "
                                + feito.unidadesNovas() + " unidade(s) entraram. "
                                + feito.jaExistiam() + " já existiam"
                                + (feito.recusadas().isEmpty() ? "."
                                        : ", e " + feito.recusadas().size()
                                                + " linha(s) ficaram de fora."));
                        janela.ir(TelaClientes.class);
                        return true;
                    } catch (java.io.IOException naoLeu) {
                        janela.reclamar("Não deu para ler o arquivo: " + naoLeu.getMessage());
                        return false;
                    }
                })
                .abrir();
    }

    private String corDaSituacao(String situacao) {
        return switch (situacao) {
            case "crítico", "em atraso" -> "s-vencido";
            case "sem débito" -> "s-cancelado";
            default -> "";
        };
    }

    /** Uma linha da lista: a unidade e a pessoa dona dela. */
    private record Linha(CarteiraServico.LinhaPessoa pessoa,
                         CarteiraServico.LinhaUnidade unidade) {

        String valor(ColunaCarteira coluna) {
            return coluna.ehDaPessoa() ? pessoa.valor(coluna) : unidade.valor(coluna);
        }
    }

    /** Usado pela tela de cadastro para voltar limpando a busca. */
    void limparBusca() {
        this.busca = "";
    }
}
