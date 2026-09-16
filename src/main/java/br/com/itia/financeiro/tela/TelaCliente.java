package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Contato;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Endereco;
import br.com.itia.financeiro.dominio.HistoricoDaUnidade;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.PreferenciaDeCobranca;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.CadastroDeUnidades;
import br.com.itia.financeiro.servico.CarteiraServico;
import br.com.itia.financeiro.servico.PerfilServico;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * A ficha de uma unidade do cliente: o que ela deve, o que já pagou e todos os
 * títulos dela.
 */
@Component
public class TelaCliente implements TelaDeUmSo {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CarteiraServico carteira;
    private final PerfilServico perfil;
    private final CadastroDeUnidades unidades;
    private final Janela janela;

    private UUID qual;
    private String aba = "titulos";

    public TelaCliente(CarteiraServico carteira, PerfilServico perfil,
                       CadastroDeUnidades unidades, @Lazy Janela janela) {
        this.carteira = carteira;
        this.perfil = perfil;
        this.unidades = unidades;
        this.janela = janela;
    }

    @Override
    public void escolher(UUID id) {
        if (!id.equals(qual)) {
            aba = "titulos";
        }
        this.qual = id;
    }

    @Override
    public String secao() {
        return "clientes";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        CarteiraServico.LinhaUnidade unidade = carteira.unidade(qual);

        Label situacao = new Label(unidade.getSituacao());
        situacao.getStyleClass().addAll("marca-situacao",
                unidade.getVencido().signum() > 0 ? "s-vencido" : "");

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("cliente", unidade.getNome(),
                apoio(unidade), situacao,
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaClientes.class)),
                Pecas.botaoVazado("Ficha da pessoa", () -> {
                    ClienteEspelho dona = perfil.unidade(qual);
                    if (dona.getPagador() == null) {
                        janela.reclamar("Esta unidade nao tem pessoa vinculada.");
                        return;
                    }
                    janela.ir(TelaPessoa.class, dona.getPagador().getId());
                }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Em aberto", Pecas.dinheiro(unidade.getEmAberto()),
                        unidade.getQuantidadeTitulos() + " títulos no total", true, false),
                Pecas.quadro("Vencido", Pecas.dinheiro(unidade.getVencido()),
                        unidade.getMaiorAtraso() > 0
                                ? "maior atraso de " + unidade.getMaiorAtraso() + " dias"
                                : "nada vencido",
                        false, unidade.getVencido().signum() > 0),
                Pecas.quadro("Próximo vencimento",
                        unidade.getProximoVencimento() == null ? "nenhum"
                                : Pecas.data(unidade.getProximoVencimento()),
                        "o que vence primeiro"),
                Pecas.quadro("Último recebimento",
                        unidade.getUltimoRecebimento() == null ? "nunca"
                                : Pecas.data(unidade.getUltimoRecebimento()),
                        "quando pagou pela última vez"))));


        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Títulos", () -> trocar("titulos"));
        partes.put("Dados da unidade", () -> trocar("dados"));
        partes.put("Endereços", () -> trocar("enderecos"));
        partes.put("Contatos", () -> trocar("contatos"));
        partes.put("Cobrança", () -> trocar("cobranca"));
        partes.put("Documentos", () -> trocar("documentos"));
        partes.put("Histórico", () -> trocar("historico"));
        tela.getChildren().add(Pecas.abas(switch (aba) {
            case "dados" -> "Dados da unidade";
            case "enderecos" -> "Endereços";
            case "contatos" -> "Contatos";
            case "cobranca" -> "Cobrança";
            case "documentos" -> "Documentos";
            case "historico" -> "Histórico";
            default -> "Títulos";
        }, partes));

        switch (aba) {
            case "dados" -> dadosDaUnidade(tela);
            case "enderecos" -> enderecos(tela);
            case "contatos" -> contatos(tela);
            case "cobranca" -> cobranca(tela);
            case "documentos" -> documentos(tela);
            case "historico" -> historico(tela);
            default -> titulos(tela, unidade, hoje);
        }
        return tela;
    }

    private void trocar(String qualAba) {
        this.aba = qualAba;
        janela.atualizar();
    }

    // --------------------------------------------------------------- títulos

    private void titulos(VBox tela, CarteiraServico.LinhaUnidade unidade, LocalDate hoje) {
        tela.getChildren().add(Pecas.secao("Títulos desta unidade"));
        tela.getChildren().add(Tabela.de(unidade.getTitulos())
                .coluna("Nº", t -> String.valueOf(t.getNumero()), 0.4)
                .coluna("Descrição", Titulo::getDescricao, 2)
                .coluna("Vencimento", t -> Pecas.data(t.getVencimento()))
                .valor("Valor", t -> Pecas.numero(t.getValor()))
                .valor("Pago", t -> Pecas.numero(t.getTotalPago()))
                .valor("Saldo", t -> Pecas.numero(t.getSaldo()))
                .comMarca(t -> t.estaVencido(hoje)
                                ? t.diasDeAtraso(hoje) + " dias"
                                : t.getSituacao().name().toLowerCase(),
                        t -> t.estaVencido(hoje) ? "s-vencido"
                                : "s-" + t.getSituacao().name().toLowerCase())
                .aoClicar(t -> janela.ir(TelaTitulo.class, t.getId()))
                .quandoVazia("Nenhum título lançado para esta unidade.")
                .montar());
    }

    // ------------------------------------------------------- dados da unidade

    private void dadosDaUnidade(VBox tela) {
        ClienteEspelho ficha = perfil.unidade(qual);

        TextField percentual = new TextField(Pecas.numero(ficha.getPercentual()));
        TextField plataforma = new TextField(ficha.getPlataforma());
        plataforma.setPromptText("onde a cobranca e feita");

        ComboBox<String> tom = new ComboBox<>();
        tom.getItems().addAll("formal", "cordial", "direto");
        tom.getSelectionModel().select(ficha.getTomDeCobranca() == null ? "cordial"
                : ficha.getTomDeCobranca());
        tom.setMaxWidth(Double.MAX_VALUE);

        CheckBox parcelamento = new CheckBox("Aceita parcelamento");
        parcelamento.setSelected(ficha.isAceitaParcelamento());

        DatePicker inicio = new DatePicker(ficha.getInicioNaCasa());
        inicio.setMaxWidth(Double.MAX_VALUE);

        TextField motivo = new TextField();
        motivo.setPromptText("motivo, se mudou o percentual");

        HBox linha = new HBox(16, Pecas.campo("Percentual cobrado (%)", percentual),
                Pecas.campo("Plataforma", plataforma),
                Pecas.campo("Tom de cobranca", tom),
                Pecas.campo("Inicio na casa", inicio));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Identificacao da empresa"));
        tela.getChildren().add(Pecas.caixa(
                Pecas.campo("Razao social", new Label(texto(ficha.getRazaoSocial()))),
                Pecas.campo("Nome fantasia", new Label(texto(ficha.getNomeFantasia()))),
                Pecas.campo("CNPJ", new Label(texto(ficha.getCnpjCpf()))),
                Pecas.campo("Regime tributario", new Label(texto(ficha.getRegimeTributario()))),
                Pecas.campo("Situacao na Receita",
                        new Label(texto(ficha.getSituacaoCadastral())))));

        tela.getChildren().add(Pecas.secao("Acordo comercial desta unidade"));
        tela.getChildren().add(Pecas.caixa(linha, parcelamento,
                Pecas.campo("Motivo, se mudou o percentual", motivo),
                new HBox(12,
                        Pecas.botao("Salvar acordo comercial", () -> {
                            unidades.salvarComercial(qual, numero(percentual.getText()),
                                    plataforma.getText(), tom.getValue(),
                                    parcelamento.isSelected(), inicio.getValue(),
                                    motivo.getText());
                            janela.avisar("Acordo comercial salvo.");
                            janela.atualizar();
                        }),
                        Pecas.botaoVazado("Passar para outra pessoa", this::janelaDeTrocarDono))));
    }

    private void janelaDeTrocarDono() {
        ComboBox<Pagador> pessoa = new ComboBox<>();
        pessoa.getItems().addAll(unidades.pessoas());
        pessoa.setConverter(nome(Pagador::getNome));
        pessoa.getSelectionModel().selectFirst();
        pessoa.setMaxWidth(Double.MAX_VALUE);

        TextField motivo = new TextField();
        motivo.setPromptText("por que a unidade muda de dono");

        JanelaFlutuante.estreita(janela.palco(), "Passar esta unidade para outra pessoa",
                        "Os titulos continuam onde estao. Muda quem responde por ela daqui em "
                                + "diante.")
                .com(Pecas.campo("Nova pessoa responsavel", pessoa),
                        Pecas.campo("Motivo", motivo))
                .acao("Passar", () -> {
                    if (pessoa.getValue() == null || motivo.getText().isBlank()) {
                        janela.reclamar("Escolha a pessoa e escreva o motivo.");
                        return false;
                    }
                    unidades.trocarDono(qual, pessoa.getValue().getId(), motivo.getText().trim());
                    janela.avisar("Unidade passada para " + pessoa.getValue().getNome() + ".");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // -------------------------------------------------------------- enderecos

    private void enderecos(VBox tela) {
        tela.getChildren().add(Pecas.cabecalho("enderecos", "Enderecos desta unidade",
                "Onde a unidade fica e para onde o documento e enviado.",
                Pecas.botao("Adicionar endereco", () -> janelaDoEndereco(null))));
        tela.getChildren().add(Tabela.de(perfil.enderecosDe(qual))
                .coluna("Tipo", Endereco::getTipo, 0.8)
                .coluna("Endereco", Endereco::getLinha, 3.4)
                .coluna("Cidade", Endereco::getCidade, 1.2)
                .coluna("UF", Endereco::getUf, 0.4)
                .coluna("CEP", Endereco::getCep, 0.8)
                .aoClicar(this::janelaDoEndereco)
                .quandoVazia("Nenhum endereco cadastrado.")
                .montar());
    }

    private void janelaDoEndereco(Endereco endereco) {
        boolean novo = endereco == null;

        ComboBox<String> tipo = new ComboBox<>();
        tipo.getItems().addAll("SEDE", "COBRANCA", "ENTREGA", "OUTRO");
        tipo.getSelectionModel().select(novo ? "SEDE" : endereco.getTipo());
        tipo.setMaxWidth(Double.MAX_VALUE);

        TextField cep = new TextField(novo ? "" : endereco.getCep());
        TextField logradouro = new TextField(novo ? "" : endereco.getLogradouro());
        TextField numero = new TextField(novo ? "" : endereco.getNumero());
        TextField complemento = new TextField(novo ? "" : endereco.getComplemento());
        TextField bairro = new TextField(novo ? "" : endereco.getBairro());
        TextField cidade = new TextField(novo ? "" : endereco.getCidade());
        TextField uf = new TextField(novo ? "" : endereco.getUf());

        HBox linha1 = new HBox(16, Pecas.campo("Tipo", tipo), Pecas.campo("CEP", cep),
                Pecas.campo("Logradouro", logradouro), Pecas.campo("Numero", numero));
        HBox linha2 = new HBox(16, Pecas.campo("Complemento", complemento),
                Pecas.campo("Bairro", bairro), Pecas.campo("Cidade", cidade),
                Pecas.campo("UF", uf));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), novo ? "Novo endereco" : "Endereco",
                        "O endereco fica na unidade, nao na pessoa.")
                .com(linha1, linha2)
                .acao(novo ? "Adicionar endereco" : "Salvar endereco", () -> {
                    perfil.salvarEndereco(novo ? null : endereco.getId(), qual, tipo.getValue(),
                            cep.getText(), logradouro.getText(), numero.getText(),
                            complemento.getText(), bairro.getText(), cidade.getText(),
                            uf.getText(), "BR", br.com.itia.financeiro.dominio.Fonte.MANUAL);
                    janela.avisar("Endereco salvo.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // --------------------------------------------------------------- contatos

    private void contatos(VBox tela) {
        tela.getChildren().add(Pecas.secao("Contatos desta unidade"));
        tela.getChildren().add(Tabela.de(perfil.contatosDaUnidade(qual))
                .coluna("Contato", Contato::getNome, 1.8)
                .coluna("Telefone", Contato::getTelefone, 1)
                .coluna("WhatsApp", Contato::getWhatsapp, 1)
                .coluna("E-mail", Contato::getEmail, 1.6)
                .comMarca(c -> c.isAceitaCobranca() ? "aceita cobranca" : "nao cobrar",
                        c -> c.isAceitaCobranca() ? "s-pago" : "s-cancelado")
                .quandoVazia("Nenhum contato desta unidade. Os contatos da pessoa valem aqui.")
                .montar());
    }

    // --------------------------------------------------------------- cobranca

    private void cobranca(VBox tela) {
        PreferenciaDeCobranca como = perfil.cobrancaDe(qual);

        ComboBox<String> forma = new ComboBox<>();
        forma.getItems().addAll("PIX", "BOLETO", "TRANSFERENCIA", "CARTAO");
        forma.getSelectionModel().select(como.getFormaPreferida() == null ? "PIX"
                : como.getFormaPreferida());
        forma.setMaxWidth(Double.MAX_VALUE);

        TextField chave = new TextField(como.getChavePix());
        TextField email = new TextField(como.getEmailCobranca());
        TextField dia = new TextField(como.getDiaVencimento() == null ? ""
                : String.valueOf(como.getDiaVencimento()));
        TextField periodicidade = new TextField(como.getPeriodicidade());
        TextField juros = new TextField(Pecas.numero(como.getJurosAoMes()));
        TextField multa = new TextField(Pecas.numero(como.getMultaPercentual()));
        TextField carencia = new TextField(como.getDiasCarencia() == null ? ""
                : String.valueOf(como.getDiasCarencia()));

        CheckBox recorrente = new CheckBox("Aceita debito recorrente");
        recorrente.setSelected(como.isAceitaDebitoRecorrente());

        TextArea instrucoes = new TextArea(como.getInstrucoes());
        instrucoes.setPrefRowCount(2);
        instrucoes.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Forma preferida", forma),
                Pecas.campo("Chave PIX do cliente", chave),
                Pecas.campo("E-mail que recebe a cobranca", email));
        HBox linha2 = new HBox(16, Pecas.campo("Dia de vencimento", dia),
                Pecas.campo("Periodicidade", periodicidade),
                Pecas.campo("Juros ao mes (%)", juros),
                Pecas.campo("Multa (%)", multa),
                Pecas.campo("Carencia (dias)", carencia));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Como esta unidade recebe a cobranca"));
        tela.getChildren().add(Pecas.caixa(linha1, linha2, recorrente,
                Pecas.campo("Instrucoes", instrucoes),
                new HBox(Pecas.botao("Salvar cobranca", () -> {
                    perfil.salvarCobranca(qual, forma.getValue(), chave.getText(), null, null,
                            null, null, inteiroOuNulo(dia.getText()), periodicidade.getText(),
                            email.getText(), recorrente.isSelected(), numero(juros.getText()),
                            numero(multa.getText()), BigDecimal.ZERO,
                            inteiroOuNulo(carencia.getText()), null, instrucoes.getText(),
                            br.com.itia.financeiro.dominio.Fonte.MANUAL);
                    janela.avisar("Preferencia de cobranca salva.");
                    janela.atualizar();
                }))));
    }

    // ------------------------------------------------------------- documentos

    private void documentos(VBox tela) {
        tela.getChildren().add(Pecas.secao("Documentos desta unidade"));
        tela.getChildren().add(Tabela.de(perfil.documentosDaUnidade(qual))
                .coluna("Arquivo", Documento::getNomeArquivo, 2.4)
                .coluna("Tipo", Documento::getTipo, 1.2)
                .coluna("Tamanho", Documento::getTamanhoLegivel, 0.8)
                .coluna("Anexado em", d -> d.getAnexadoEm() == null ? ""
                        : d.getAnexadoEm().format(QUANDO), 1.1)
                .quandoVazia("Nenhum documento desta unidade.")
                .montar());
    }

    // -------------------------------------------------------------- historico

    private void historico(VBox tela) {
        tela.getChildren().add(Pecas.secao("O que ja aconteceu com esta unidade"));
        tela.getChildren().add(Tabela.de(perfil.historicoDaUnidade(qual))
                .coluna("Quando", i -> i.getOcorridoEm() == null ? ""
                        : i.getOcorridoEm().format(QUANDO), 1.1)
                .coluna("Tipo", Interacao::getTipoLegivel, 1.2)
                .coluna("O que aconteceu", Interacao::getDescricao, 3)
                .coluna("Canal", Interacao::getCanal, 0.8)
                .quandoVazia("Nada registrado para esta unidade.")
                .montar());

        tela.getChildren().add(Pecas.secao("Trocas de dono e de telefone"));
        tela.getChildren().add(Tabela.de(unidades.trocasDe(qual))
                .coluna("Quando", h -> h.getQuando() == null ? ""
                        : h.getQuando().format(QUANDO), 1.1)
                .coluna("O que mudou", HistoricoDaUnidade::getTipo, 1.6)
                .coluna("De", HistoricoDaUnidade::getDe, 1.6)
                .coluna("Para", HistoricoDaUnidade::getPara, 1.6)
                .coluna("Motivo", HistoricoDaUnidade::getMotivo, 2)
                .quandoVazia("Nenhuma troca registrada.")
                .montar());
    }

    // ------------------------------------------------------------------ apoio

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "nao informado" : valor;
    }

    private <T> StringConverter<T> nome(java.util.function.Function<T, String> comoChamar) {
        return new StringConverter<>() {
            @Override
            public String toString(T qualItem) {
                return qualItem == null ? "" : comoChamar.apply(qualItem);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        };
    }

    private BigDecimal numero(String valor) {
        BigDecimal quanto = TelaTituloNovo.dinheiro(valor);
        return quanto == null ? BigDecimal.ZERO : quanto;
    }

    private Integer inteiroOuNulo(String valor) {
        try {
            return Integer.valueOf(valor.trim());
        } catch (RuntimeException naoEhNumero) {
            return null;
        }
    }

    private String apoio(CarteiraServico.LinhaUnidade unidade) {
        StringBuilder texto = new StringBuilder();
        if (unidade.getDocumento() != null && !unidade.getDocumento().isBlank()) {
            texto.append(unidade.getDocumento());
        }
        if (unidade.getCodigo() != null && !unidade.getCodigo().isBlank()) {
            if (texto.length() > 0) {
                texto.append(" · ");
            }
            texto.append("código ").append(unidade.getCodigo());
        }
        return texto.toString();
    }
}
