package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.AnaliseDeCredito;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Consentimento;
import br.com.itia.financeiro.dominio.Contato;
import br.com.itia.financeiro.dominio.Contrato;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.Restricao;
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
 * A ficha completa de uma pessoa que paga.
 *
 * Sete partes, como sempre foi: o resumo da posição, o cadastro com os contatos,
 * os contratos, o crédito e o risco, o histórico de conversa, os documentos e a
 * conformidade (com que base se pode falar com a pessoa).
 */
@Component
public class TelaPessoa implements TelaDeUmSo {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PerfilServico perfil;
    private final Janela janela;

    private UUID qual;
    private String aba = "resumo";

    public TelaPessoa(PerfilServico perfil, @Lazy Janela janela) {
        this.perfil = perfil;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "clientes";
    }

    @Override
    public void escolher(UUID id) {
        if (!id.equals(qual)) {
            aba = "resumo";
        }
        this.qual = id;
    }

    @Override
    public Node montar() {
        Pagador pessoa = perfil.pessoa(qual);
        PerfilServico.Posicao posicao = perfil.posicaoDa(qual);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("cliente", pessoa.getNome(),
                (pessoa.getCpf() == null ? "" : pessoa.getCpf() + " · ")
                        + (pessoa.getSituacao() == null ? "" : pessoa.getSituacao()),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaClientes.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Em aberto", Pecas.dinheiro(posicao.getEmAberto()),
                        posicao.getQuantidadeDeTitulos() + " títulos", true, false),
                Pecas.quadro("Vencido", Pecas.dinheiro(posicao.getVencido()),
                        posicao.getMaiorAtraso() > 0
                                ? "maior atraso de " + posicao.getMaiorAtraso() + " dias"
                                : "nada vencido",
                        false, posicao.getVencido().signum() > 0),
                Pecas.quadro("A vencer", Pecas.dinheiro(posicao.getAVencer()),
                        posicao.getProximoVencimento() == null ? "sem vencimento à frente"
                                : "próximo em " + Pecas.data(posicao.getProximoVencimento())),
                Pecas.quadro("Comportamento", posicao.getVencido().signum() > 0
                                ? "em atraso" : "paga em dia",
                        "ticket médio de " + Pecas.dinheiro(posicao.getTicketMedio())))));


        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Resumo", () -> trocar("resumo"));
        partes.put("Cadastro", () -> trocar("cadastro"));
        partes.put("Contratos", () -> trocar("contratos"));
        partes.put("Crédito e risco", () -> trocar("credito"));
        partes.put("Histórico", () -> trocar("historico"));
        partes.put("Documentos", () -> trocar("documentos"));
        partes.put("Conformidade", () -> trocar("conformidade"));
        tela.getChildren().add(Pecas.abas(switch (aba) {
            case "cadastro" -> "Cadastro";
            case "contratos" -> "Contratos";
            case "credito" -> "Crédito e risco";
            case "historico" -> "Histórico";
            case "documentos" -> "Documentos";
            case "conformidade" -> "Conformidade";
            default -> "Resumo";
        }, partes));

        switch (aba) {
            case "cadastro" -> cadastro(tela, pessoa);
            case "contratos" -> contratos(tela);
            case "credito" -> credito(tela);
            case "historico" -> historico(tela);
            case "documentos" -> documentos(tela);
            case "conformidade" -> conformidade(tela);
            default -> resumo(tela, posicao);
        }
        return tela;
    }

    private void trocar(String qualAba) {
        this.aba = qualAba;
        janela.atualizar();
    }

    // ----------------------------------------------------------------- resumo

    private void resumo(VBox tela, PerfilServico.Posicao posicao) {
        tela.getChildren().add(Pecas.secao("Aging do que está vencido"));
        tela.getChildren().add(Pecas.quadros(
                Pecas.quadro("Até 30 dias", Pecas.dinheiro(posicao.getAte30()), ""),
                Pecas.quadro("31 a 60", Pecas.dinheiro(posicao.getDe31a60()), ""),
                Pecas.quadro("61 a 90", Pecas.dinheiro(posicao.getDe61a90()), ""),
                Pecas.quadro("Mais de 90", Pecas.dinheiro(posicao.getAcimaDe90()), "",
                        false, posicao.getAcimaDe90().signum() > 0)));

        tela.getChildren().add(Pecas.secao("Unidades desta pessoa"));
        tela.getChildren().add(Tabela.de(perfil.unidadesDe(qual))
                .coluna("Unidade", ClienteEspelho::getRazaoSocial, 2.4)
                .coluna("CNPJ", ClienteEspelho::getCnpjCpf, 1.2)
                .aoClicar(u -> janela.ir(TelaCliente.class, u.getId()))
                .quandoVazia("Nenhuma unidade cadastrada para esta pessoa.")
                .montar());

        Label dica = new Label("Clique numa unidade para ver os títulos dela.");
        dica.getStyleClass().add("dica");
        tela.getChildren().add(dica);

        tela.getChildren().add(Pecas.secao("Ações rápidas"));
        tela.getChildren().add(Pecas.caixa(new HBox(12,
                Pecas.botaoVazado("Registrar promessa", () -> janelaDaInteracao("PROMESSA")),
                Pecas.botaoVazado("Registrar ligação", () -> janelaDaInteracao("LIGACAO")),
                Pecas.botao("Lançar título", () -> janela.ir(TelaTituloNovo.class)))));
    }

    // ---------------------------------------------------------------- cadastro

    private void cadastro(VBox tela, Pagador pessoa) {
        TextField nome = new TextField(pessoa.getNome());
        TextField nomeSocial = new TextField(pessoa.getNomeSocial());
        TextField cpf = new TextField(pessoa.getCpf());
        DatePicker nascimento = new DatePicker(pessoa.getDataNascimento());
        DatePicker desde = new DatePicker(pessoa.getClienteDesde());
        nascimento.setMaxWidth(Double.MAX_VALUE);
        desde.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> situacao = new ComboBox<>();
        situacao.getItems().addAll("ativo", "inativo", "em disputa", "encerrado");
        situacao.getSelectionModel().select(pessoa.getSituacao() == null ? "ativo"
                : pessoa.getSituacao());
        situacao.setMaxWidth(Double.MAX_VALUE);

        TextField whatsapp = new TextField(pessoa.getWhatsapp());
        TextField telefone = new TextField(pessoa.getTelefone());
        TextField email = new TextField(pessoa.getEmail());

        TextArea observacao = new TextArea(pessoa.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Nome completo", nome),
                Pecas.campo("Nome social ou apelido", nomeSocial),
                Pecas.campo("CPF", cpf));
        HBox linha2 = new HBox(16, Pecas.campo("Data de nascimento", nascimento),
                Pecas.campo("Cliente desde", desde), Pecas.campo("Situação", situacao));
        HBox linha3 = new HBox(16, Pecas.campo("WhatsApp", whatsapp),
                Pecas.campo("Telefone", telefone), Pecas.campo("E-mail", email));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha3.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Identificação"));
        tela.getChildren().add(Pecas.caixa(linha1, linha2, linha3,
                Pecas.campo("Observação", observacao),
                new HBox(Pecas.botao("Salvar identificação", () -> {
                    perfil.salvarIdentificacao(qual, nome.getText(), nomeSocial.getText(),
                            cpf.getText(), nascimento.getValue(), desde.getValue(),
                            situacao.getValue(), whatsapp.getText(), telefone.getText(),
                            email.getText(), observacao.getText());
                    janela.avisar("Identificação salva.");
                    janela.atualizar();
                }))));

        tela.getChildren().add(Pecas.cabecalho("contatos", "Contatos e papéis",
                "Quem se procura nesta pessoa, em que ordem e por qual canal.",
                Pecas.botao("Adicionar contato", () -> janelaDoContato(null))));
        tela.getChildren().add(Tabela.de(perfil.contatosDe(qual))
                .coluna("Contato", Contato::getNome, 1.8)
                .coluna("Telefone", Contato::getTelefone, 1)
                .coluna("WhatsApp", Contato::getWhatsapp, 1)
                .coluna("E-mail", Contato::getEmail, 1.6)
                .coluna("Ordem", c -> String.valueOf(c.getPrioridade()), 0.5)
                .comMarca(c -> c.isAceitaCobranca() ? "aceita cobrança" : "não cobrar",
                        c -> c.isAceitaCobranca() ? "s-pago" : "s-cancelado")
                .aoClicar(this::janelaDoContato)
                .quandoVazia("Nenhum contato cadastrado.")
                .montar());
    }

    private void janelaDoContato(Contato contato) {
        boolean novo = contato == null;

        TextField nome = new TextField(novo ? "" : contato.getNome());
        TextField telefone = new TextField(novo ? "" : contato.getTelefone());
        TextField whatsapp = new TextField(novo ? "" : contato.getWhatsapp());
        TextField email = new TextField(novo ? "" : contato.getEmail());
        TextField prioridade = new TextField(novo ? "1"
                : String.valueOf(contato.getPrioridade()));
        TextField horario = new TextField(novo ? "" : contato.getMelhorHorario());
        horario.setPromptText("de manhã, depois das 14h");

        CheckBox aceita = new CheckBox("Aceita receber cobrança");
        aceita.setSelected(novo || contato.isAceitaCobranca());
        CheckBox ativo = new CheckBox("Contato ativo");
        ativo.setSelected(novo || contato.isAtivo());

        TextArea observacao = new TextArea(novo ? "" : contato.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Nome", nome),
                Pecas.campo("Telefone", telefone), Pecas.campo("WhatsApp", whatsapp));
        HBox linha2 = new HBox(16, Pecas.campo("E-mail", email),
                Pecas.campo("Ordem de procura", prioridade),
                Pecas.campo("Melhor horário", horario));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), novo ? "Novo contato" : "Contato",
                        "Quem falar nesta pessoa, e por qual canal.")
                .com(linha1, linha2, Pecas.campo("Observação", observacao), aceita, ativo)
                .acao(novo ? "Adicionar contato" : "Salvar contato", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome do contato.");
                        return false;
                    }
                    perfil.salvarContato(novo ? null : contato.getId(), qual, null,
                            nome.getText(), telefone.getText(), whatsapp.getText(),
                            email.getText(), inteiro(prioridade.getText(), 1),
                            aceita.isSelected(), horario.getText(), observacao.getText(),
                            ativo.isSelected(), List.of());
                    janela.avisar("Contato salvo.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // --------------------------------------------------------------- contratos

    private void contratos(VBox tela) {
        tela.getChildren().add(Pecas.cabecalho("contratos", "Contratos desta pessoa",
                "O contrato fica na pessoa e pode cobrir uma unidade ou todas.",
                Pecas.botao("Cadastrar contrato", () -> janelaDoContrato(null))));

        tela.getChildren().add(Tabela.de(perfil.contratosDe(qual))
                .coluna("Número", Contrato::getNumero, 1)
                .coluna("Descrição", Contrato::getDescricao, 2)
                .coluna("Unidade", Contrato::getNomeDaUnidade, 1.6)
                .valor("Valor", c -> Pecas.numero(c.getValor()))
                .coluna("Vigência", c -> Pecas.data(c.getInicio())
                        + (c.getFim() == null ? " · sem fim" : " a " + Pecas.data(c.getFim())),
                        1.4)
                .comMarca(Contrato::getSituacao,
                        c -> "ATIVO".equalsIgnoreCase(c.getSituacao()) ? "s-pago" : "s-cancelado")
                .aoClicar(this::janelaDoContrato)
                .quandoVazia("Nenhum contrato cadastrado.")
                .montar());
    }

    private void janelaDoContrato(Contrato contrato) {
        boolean novo = contrato == null;

        TextField numero = new TextField(novo ? "" : contrato.getNumero());
        TextField descricao = new TextField(novo ? "" : contrato.getDescricao());
        TextField valor = new TextField(novo ? "0,00" : Pecas.numero(contrato.getValor()));

        ComboBox<ClienteEspelho> unidade = new ComboBox<>();
        unidade.getItems().addAll(perfil.unidadesDe(qual));
        unidade.setConverter(nome(ClienteEspelho::getRazaoSocial));
        if (!novo && contrato.getUnidadeId() != null) {
            unidade.getItems().stream().filter(u -> u.getId().equals(contrato.getUnidadeId()))
                    .findFirst().ifPresent(unidade.getSelectionModel()::select);
        }
        unidade.setMaxWidth(Double.MAX_VALUE);

        DatePicker inicio = new DatePicker(novo ? LocalDate.now() : contrato.getInicio());
        DatePicker fim = new DatePicker(novo ? null : contrato.getFim());
        inicio.setMaxWidth(Double.MAX_VALUE);
        fim.setMaxWidth(Double.MAX_VALUE);

        TextField indice = new TextField(novo ? "" : contrato.getIndiceReajuste());
        indice.setPromptText("IPCA, IGPM");
        TextField mesReajuste = new TextField(novo || contrato.getMesReajuste() == null ? ""
                : String.valueOf(contrato.getMesReajuste()));
        TextField diaVencimento = new TextField(novo || contrato.getDiaVencimento() == null ? ""
                : String.valueOf(contrato.getDiaVencimento()));
        TextField periodicidade = new TextField(novo ? "" : contrato.getPeriodicidade());
        TextField responsavel = new TextField(novo ? "" : contrato.getResponsavelComercial());

        ComboBox<String> situacao = new ComboBox<>();
        situacao.getItems().addAll("ATIVO", "SUSPENSO", "ENCERRADO");
        situacao.getSelectionModel().select(novo ? "ATIVO" : contrato.getSituacao());
        situacao.setMaxWidth(Double.MAX_VALUE);

        TextArea observacao = new TextArea(novo ? "" : contrato.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Número", numero),
                Pecas.campo("Cobre qual unidade", unidade), Pecas.campo("Valor", valor));
        HBox linha2 = new HBox(16, Pecas.campo("Início", inicio), Pecas.campo("Fim", fim),
                Pecas.campo("Periodicidade", periodicidade));
        HBox linha3 = new HBox(16, Pecas.campo("Índice de reajuste", indice),
                Pecas.campo("Mês do reajuste", mesReajuste),
                Pecas.campo("Dia de vencimento", diaVencimento));
        HBox linha4 = new HBox(16, Pecas.campo("Responsável comercial", responsavel),
                Pecas.campo("Situação", situacao));
        for (HBox linha : List.of(linha1, linha2, linha3, linha4)) {
            linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        }

        JanelaFlutuante.nova(janela.palco(), novo ? "Novo contrato" : "Contrato",
                        "O contrato descreve o combinado e de onde sai a cobrança.")
                .com(Pecas.campo("Descrição", descricao), linha1, linha2, linha3, linha4,
                        Pecas.campo("Observação", observacao))
                .acao(novo ? "Cadastrar contrato" : "Salvar contrato", () -> {
                    perfil.salvarContrato(novo ? null : contrato.getId(), qual,
                            unidade.getValue() == null ? null : unidade.getValue().getId(),
                            numero.getText(), descricao.getText(), inicio.getValue(),
                            fim.getValue(), numero(valor.getText()), indice.getText(),
                            inteiroOuNulo(mesReajuste.getText()),
                            inteiroOuNulo(diaVencimento.getText()), periodicidade.getText(),
                            responsavel.getText(), situacao.getValue(), observacao.getText());
                    janela.avisar("Contrato salvo.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // ----------------------------------------------------------------- crédito

    private void credito(VBox tela) {
        AnaliseDeCredito analise = perfil.creditoDe(qual);

        ComboBox<String> classificacao = new ComboBox<>();
        classificacao.getItems().addAll("sem classificação", "A", "B", "C", "D");
        classificacao.getSelectionModel().select(analise.getClassificacao() == null
                ? "sem classificação" : analise.getClassificacao());
        classificacao.setMaxWidth(Double.MAX_VALUE);

        TextField limite = new TextField(Pecas.numero(analise.getLimiteCredito()));
        CheckBox bloqueado = new CheckBox("Cliente bloqueado");
        bloqueado.setSelected(analise.isBloqueado());
        TextField motivo = new TextField(analise.getMotivoBloqueio());
        motivo.setPromptText("por que está bloqueado");

        TextArea observacao = new TextArea(analise.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha = new HBox(16, Pecas.campo("Classificação", classificacao),
                Pecas.campo("Limite de crédito", limite),
                Pecas.campo("Motivo do bloqueio", motivo));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Análise de crédito"));
        tela.getChildren().add(Pecas.caixa(linha, bloqueado,
                Pecas.campo("Observação da análise", observacao),
                new Label("Limite ainda disponível: "
                        + Pecas.dinheiro(perfil.limiteDisponivel(qual))),
                new HBox(Pecas.botao("Salvar análise", () -> {
                    perfil.salvarCredito(qual, classificacao.getValue(),
                            numero(limite.getText()), bloqueado.isSelected(), motivo.getText(),
                            observacao.getText());
                    janela.avisar("Análise de crédito salva.");
                    janela.atualizar();
                }))));

        tela.getChildren().add(Pecas.cabecalho("risco", "Restrições",
                "O que pesa contra esta pessoa, de dentro ou de fora.",
                Pecas.botao("Registrar restrição", this::janelaDaRestricao)));
        tela.getChildren().add(Tabela.de(perfil.restricoesDe(qual))
                .coluna("Tipo", Restricao::getTipo, 1.2)
                .coluna("Origem", Restricao::getOrigem, 1.6)
                .valor("Valor", r -> Pecas.numero(r.getValor()))
                .coluna("Data", r -> Pecas.data(r.getData()), 0.9)
                .comMarca(Restricao::getSituacao, r -> "ATIVA".equalsIgnoreCase(r.getSituacao())
                        ? "s-vencido" : "s-pago")
                .quandoVazia("Nenhuma restrição registrada.")
                .montar());
    }

    private void janelaDaRestricao() {
        ComboBox<String> tipo = new ComboBox<>();
        tipo.getItems().addAll("PROTESTO", "NEGATIVACAO", "ACAO_JUDICIAL", "INTERNA");
        tipo.getSelectionModel().selectFirst();
        tipo.setMaxWidth(Double.MAX_VALUE);

        TextField origem = new TextField();
        origem.setPromptText("de onde veio a informação");
        TextField valor = new TextField("0,00");
        DatePicker data = new DatePicker(LocalDate.now());
        data.setMaxWidth(Double.MAX_VALUE);
        TextArea observacao = new TextArea();
        observacao.setPrefRowCount(2);

        HBox linha = new HBox(16, Pecas.campo("Tipo", tipo), Pecas.campo("Origem", origem),
                Pecas.campo("Valor", valor), Pecas.campo("Data", data));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Registrar restrição",
                        "Fica no histórico da pessoa e pesa na análise de crédito.")
                .com(linha, Pecas.campo("Observação", observacao))
                .acao("Registrar", () -> {
                    perfil.registrarRestricao(qual, tipo.getValue(), origem.getText(),
                            numero(valor.getText()), data.getValue(), observacao.getText());
                    janela.avisar("Restrição registrada.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // --------------------------------------------------------------- histórico

    private void historico(VBox tela) {
        List<Interacao> conversas = perfil.historicoDe(qual);

        tela.getChildren().add(Pecas.cabecalho("histórico", "O que já aconteceu",
                "Promessa, ligação, visita: tudo o que foi falado com esta pessoa.",
                Pecas.botao("Registrar", () -> janelaDaInteracao("LIGACAO"))));

        tela.getChildren().add(Tabela.de(conversas)
                .coluna("Quando", i -> i.getOcorridoEm() == null ? ""
                        : i.getOcorridoEm().format(QUANDO), 1.1)
                .coluna("Tipo", Interacao::getTipoLegivel, 1.2)
                .coluna("O que aconteceu", Interacao::getDescricao, 2.6)
                .valor("Valor", i -> Pecas.numero(i.getValor()))
                .coluna("Data prometida", i -> Pecas.data(i.getDataPrometida()), 1)
                .coluna("Canal", Interacao::getCanal, 0.8)
                .comMarca(Interacao::getSituacao, i -> "CUMPRIDA".equalsIgnoreCase(i.getSituacao())
                        ? "s-pago" : "QUEBRADA".equalsIgnoreCase(i.getSituacao())
                        ? "s-vencido" : "")
                .aoClicar(this::janelaDaPromessa)
                .quandoVazia("Nada registrado no histórico ainda.")
                .montar());

        Label dica = new Label("Clique numa promessa para dizer se ela foi cumprida.");
        dica.getStyleClass().add("dica");
        tela.getChildren().add(dica);
    }

    private void janelaDaInteracao(String tipoInicial) {
        ComboBox<String> tipo = new ComboBox<>();
        tipo.getItems().addAll("PROMESSA", "LIGACAO", "VISITA", "NOTA");
        tipo.getSelectionModel().select(tipoInicial);
        tipo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ClienteEspelho> unidade = new ComboBox<>();
        unidade.getItems().addAll(perfil.unidadesDe(qual));
        unidade.setConverter(nome(ClienteEspelho::getRazaoSocial));
        unidade.setMaxWidth(Double.MAX_VALUE);

        TextArea descricao = new TextArea();
        descricao.setPromptText("o que foi falado");
        descricao.setPrefRowCount(3);
        descricao.setWrapText(true);

        TextField valor = new TextField("0,00");
        DatePicker prometida = new DatePicker();
        prometida.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> canal = new ComboBox<>();
        canal.getItems().addAll("WhatsApp", "telefone", "e-mail", "presencial");
        canal.getSelectionModel().selectFirst();
        canal.setMaxWidth(Double.MAX_VALUE);

        HBox linha = new HBox(16, Pecas.campo("Tipo", tipo),
                Pecas.campo("Unidade", unidade), Pecas.campo("Canal", canal));
        HBox linha2 = new HBox(16, Pecas.campo("Valor", valor),
                Pecas.campo("Data prometida", prometida));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Registrar no histórico",
                        "Fica guardado com a data, o autor e o canal.")
                .com(linha, Pecas.campo("O que aconteceu", descricao), linha2)
                .acao("Registrar", () -> {
                    if (descricao.getText() == null || descricao.getText().isBlank()) {
                        janela.reclamar("Escreva o que aconteceu.");
                        return false;
                    }
                    perfil.registrarInteracao(qual,
                            unidade.getValue() == null ? null : unidade.getValue().getId(),
                            tipo.getValue(), descricao.getText(), numero(valor.getText()),
                            prometida.getValue(), canal.getValue());
                    janela.avisar("Registrado no histórico.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void janelaDaPromessa(Interacao interacao) {
        if (!"PROMESSA".equalsIgnoreCase(interacao.getTipo())) {
            return;
        }
        JanelaFlutuante.estreita(janela.palco(), "Promessa de pagamento",
                        interacao.getDescricao())
                .acao("Cumpriu", () -> {
                    perfil.baixarPromessa(interacao.getId(), true);
                    janela.avisar("Promessa marcada como cumprida.");
                    janela.atualizar();
                    return true;
                })
                .outraAcao("Quebrou", () -> {
                    perfil.baixarPromessa(interacao.getId(), false);
                    janela.avisar("Promessa marcada como quebrada.");
                    janela.atualizar();
                })
                .abrir();
    }

    // -------------------------------------------------------------- documentos

    private void documentos(VBox tela) {
        tela.getChildren().add(Pecas.secao("Documentos desta pessoa"));
        tela.getChildren().add(Tabela.de(perfil.documentosDe(qual))
                .coluna("Arquivo", Documento::getNomeArquivo, 2.4)
                .coluna("Tipo", Documento::getTipo, 1.2)
                .coluna("Tamanho", Documento::getTamanhoLegivel, 0.8)
                .coluna("Validade", d -> Pecas.data(d.getValidade()), 0.9)
                .coluna("Anexado em", d -> d.getAnexadoEm() == null ? ""
                        : d.getAnexadoEm().format(QUANDO), 1.1)
                .quandoVazia("Nenhum documento anexado.")
                .montar());
    }

    // ------------------------------------------------------------ conformidade

    private void conformidade(VBox tela) {
        tela.getChildren().add(Pecas.cabecalho("conformidade", "Com que base se pode falar",
                "Guarda a autorização de cada canal, e também a recusa.",
                Pecas.botao("Registrar consentimento", this::janelaDoConsentimento)));

        tela.getChildren().add(Tabela.de(perfil.consentimentosDe(qual))
                .coluna("Canal", Consentimento::getCanal, 1)
                .coluna("Base legal", Consentimento::getBaseLegal, 1.6)
                .coluna("Onde", Consentimento::getOrigem, 1.6)
                .coluna("Quando", c -> c.getRegistradoEm() == null ? ""
                        : c.getRegistradoEm().format(QUANDO), 1.1)
                .coluna("Quem", Consentimento::getRegistradoPor, 1.2)
                .comMarca(Consentimento::getSituacao,
                        c -> "ACEITO".equalsIgnoreCase(c.getSituacao()) ? "s-pago" : "s-cancelado")
                .quandoVazia("Nenhum consentimento registrado.")
                .montar());
    }

    private void janelaDoConsentimento() {
        ComboBox<String> canal = new ComboBox<>();
        canal.getItems().addAll("WHATSAPP", "EMAIL", "SMS", "TELEFONE");
        canal.getSelectionModel().selectFirst();
        canal.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> base = new ComboBox<>();
        base.getItems().addAll("consentimento", "execução de contrato", "legítimo interesse",
                "obrigação legal");
        base.getSelectionModel().selectFirst();
        base.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> situacao = new ComboBox<>();
        situacao.getItems().addAll("ACEITO", "RECUSADO", "REVOGADO");
        situacao.getSelectionModel().selectFirst();
        situacao.setMaxWidth(Double.MAX_VALUE);

        TextField origem = new TextField();
        origem.setPromptText("onde a pessoa disse isso");
        TextArea observacao = new TextArea();
        observacao.setPrefRowCount(2);

        HBox linha = new HBox(16, Pecas.campo("Canal", canal), Pecas.campo("Base legal", base),
                Pecas.campo("Situação", situacao));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Registrar consentimento",
                        "O registro de que se pode falar com a pessoa por aquele canal.")
                .com(linha, Pecas.campo("Onde", origem), Pecas.campo("Observação", observacao))
                .acao("Registrar", () -> {
                    perfil.registrarConsentimento(qual, null, canal.getValue(), base.getValue(),
                            situacao.getValue(), origem.getText(), observacao.getText());
                    janela.avisar("Consentimento registrado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // ------------------------------------------------------------------ apoio

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

    private BigDecimal numero(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private int inteiro(String texto, int seNaoDer) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return seNaoDer;
        }
    }

    private Integer inteiroOuNulo(String texto) {
        try {
            return Integer.valueOf(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return null;
        }
    }
}
