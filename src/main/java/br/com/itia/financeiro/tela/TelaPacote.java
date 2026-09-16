package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.AbrangenciaDoPacote;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.ContratacaoDePacote;
import br.com.itia.financeiro.dominio.ItemDeServico;
import br.com.itia.financeiro.dominio.ItemDoPacote;
import br.com.itia.financeiro.dominio.Pacote;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.dominio.PeriodoDoLimite;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.SituacaoDaContratacao;
import br.com.itia.financeiro.dominio.TratamentoDoExcedente;
import br.com.itia.financeiro.servico.PacoteServico;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * A ficha de um pacote.
 *
 * Três partes, como sempre foi: os dados do pacote, a composição (o que está
 * incluído e até onde) e quem tem este pacote contratado.
 */
@Component
public class TelaPacote implements TelaDeUmSo {

    private final PacoteServico pacotes;
    private final Janela janela;

    private UUID qual;
    private String aba = "dados";

    public TelaPacote(PacoteServico pacotes, @Lazy Janela janela) {
        this.pacotes = pacotes;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    @Override
    public void escolher(UUID id) {
        if (!id.equals(qual)) {
            aba = "dados";
        }
        this.qual = id;
    }

    @Override
    public Node montar() {
        Pacote pacote = pacotes.pacote(qual);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("catálogo", pacote.getNome(),
                pacote.getCodigo() + " · " + pacote.getValorResumido() + " · "
                        + pacote.getPeriodicidadeResumida(),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaPacotes.class)),
                pacote.isAtivo()
                        ? Pecas.botaoPerigo("Inativar pacote", () -> {
                            pacotes.inativar(qual);
                            janela.avisar("Pacote inativado. Ninguém contrata por ele agora.");
                            janela.atualizar();
                        })
                        : Pecas.botao("Reativar pacote", () -> {
                            pacotes.reativar(qual);
                            janela.avisar("Pacote reativado.");
                            janela.atualizar();
                        }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Valor do pacote", pacote.getValorResumido(),
                        pacote.getPeriodicidadeResumida(), true, false),
                Pecas.quadro("Serviços incluídos", String.valueOf(pacote.getComposicao().size()),
                        "no que está contratado"),
                Pecas.quadro("Contratações",
                        String.valueOf(pacotes.quantasContratacoes(qual)), "clientes com ele"),
                Pecas.quadro("Situação", pacote.isAtivo() ? "ativo" : "inativo",
                        pacote.isAtivo() ? "aceita contratação nova" : "não aceita contratação"))));


        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Dados do pacote", () -> trocar("dados"));
        partes.put("Composição", () -> trocar("composicao"));
        partes.put("Contratações", () -> trocar("contratacoes"));
        tela.getChildren().add(Pecas.abas(switch (aba) {
            case "composicao" -> "Composição";
            case "contratacoes" -> "Contratações";
            default -> "Dados do pacote";
        }, partes));

        switch (aba) {
            case "composicao" -> composicao(tela, pacote);
            case "contratacoes" -> contratacoes(tela);
            default -> dados(tela, pacote);
        }
        return tela;
    }

    private void trocar(String qualAba) {
        this.aba = qualAba;
        janela.atualizar();
    }

    // -------------------------------------------------------------- dados

    private void dados(VBox tela, Pacote pacote) {
        TextField nome = new TextField(pacote.getNome());
        TextField valor = new TextField(Pecas.numero(pacote.getValor()));

        ComboBox<Periodicidade> periodicidade = new ComboBox<>();
        periodicidade.getItems().addAll(Periodicidade.values());
        periodicidade.setConverter(nome(Periodicidade::getRotulo));
        periodicidade.getSelectionModel().select(pacote.getPeriodicidade());
        periodicidade.setMaxWidth(Double.MAX_VALUE);

        TextField outra = new TextField(pacote.getPeriodicidadeOutra());
        outra.setPromptText("se for outra, qual");

        CheckBox ativo = new CheckBox("Pacote ativo");
        ativo.setSelected(pacote.isAtivo());

        TextArea descricao = new TextArea(pacote.getDescricao());
        descricao.setPrefRowCount(3);
        descricao.setWrapText(true);

        HBox linha = new HBox(16, Pecas.campo("Nome", nome),
                Pecas.campo("Valor do pacote (R$)", valor),
                Pecas.campo("Periodicidade", periodicidade),
                Pecas.campo("Se for outra, qual", outra));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Dados do pacote"));
        tela.getChildren().add(Pecas.caixa(linha, Pecas.campo("Descrição", descricao), ativo,
                new HBox(Pecas.botao("Salvar dados", () -> {
                    pacotes.salvarDados(qual, nome.getText(), descricao.getText(),
                            numero(valor.getText()), periodicidade.getValue(), outra.getText(),
                            ativo.isSelected());
                    janela.avisar("Dados do pacote salvos.");
                    janela.atualizar();
                }))));
    }

    // --------------------------------------------------------- composição

    private void composicao(VBox tela, Pacote pacote) {
        tela.getChildren().add(Pecas.cabecalho("composição", "O que está incluído",
                "Cada linha diz qual serviço entra, quanto está incluído e o que acontece se "
                        + "passar do limite.",
                Pecas.botao("Incluir serviço", () -> janelaDaLinha(null))));

        tela.getChildren().add(Tabela.de(pacote.getComposicao())
                .coluna("O que entra", ItemDoPacote::getResumoDoQueEntra, 2.4)
                .coluna("Abrangência", l -> l.getAbrangencia() == null ? ""
                        : l.getAbrangencia().getRotulo(), 1.4)
                .coluna("Incluído", l -> l.isIlimitado() ? "ilimitado"
                        : Pecas.numero(l.getQuantidadeIncluida()), 1)
                .coluna("Período do limite", l -> l.getPeriodoLimite() == null ? ""
                        : l.getPeriodoLimite().getRotulo(), 1.2)
                .coluna("Excedente", l -> l.getTratamentoExcedente() == null ? ""
                        : l.getTratamentoExcedente().getRotulo(), 1.4)
                .aoClicar(this::janelaDaLinha)
                .quandoVazia("Nada incluído ainda. Use Incluir serviço.")
                .montar());

        Label dica = new Label("Clique numa linha para editar ou tirar do pacote.");
        dica.getStyleClass().add("dica");
        tela.getChildren().add(dica);
    }

    private void janelaDaLinha(ItemDoPacote linha) {
        boolean nova = linha == null;

        ComboBox<Servico> servico = new ComboBox<>();
        servico.getItems().addAll(pacotes.servicosDisponiveis());
        servico.setConverter(nome(Servico::getNome));
        if (!nova && linha.getServico() != null) {
            servico.getItems().stream()
                    .filter(s -> s.getId().equals(linha.getServico().getId()))
                    .findFirst().ifPresent(servico.getSelectionModel()::select);
        } else {
            servico.getSelectionModel().selectFirst();
        }
        servico.setMaxWidth(Double.MAX_VALUE);

        ComboBox<AbrangenciaDoPacote> abrangencia = new ComboBox<>();
        abrangencia.getItems().addAll(AbrangenciaDoPacote.values());
        abrangencia.setConverter(nome(AbrangenciaDoPacote::getRotulo));
        abrangencia.getSelectionModel().select(nova ? AbrangenciaDoPacote.SERVICO_COMPLETO
                : linha.getAbrangencia());
        abrangencia.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ItemDeServico> item = new ComboBox<>();
        if (servico.getValue() != null) {
            item.getItems().addAll(servico.getValue().getItensAtivos());
        }
        item.setConverter(nome(ItemDeServico::getNome));
        if (!nova && linha.getItem() != null) {
            item.getItems().stream().filter(i -> i.getId().equals(linha.getItem().getId()))
                    .findFirst().ifPresent(item.getSelectionModel()::select);
        }
        item.setMaxWidth(Double.MAX_VALUE);
        servico.setOnAction(acao -> {
            item.getItems().clear();
            if (servico.getValue() != null) {
                item.getItems().addAll(servico.getValue().getItensAtivos());
            }
        });

        CheckBox ilimitado = new CheckBox("Utilização ilimitada");
        ilimitado.setSelected(nova || linha.isIlimitado());

        TextField quantidade = new TextField(nova ? "1"
                : Pecas.numero(linha.getQuantidadeIncluida()));

        ComboBox<PeriodoDoLimite> periodo = new ComboBox<>();
        periodo.getItems().addAll(PeriodoDoLimite.values());
        periodo.setConverter(nome(PeriodoDoLimite::getRotulo));
        periodo.getSelectionModel().select(nova ? PeriodoDoLimite.POR_PERIODO_DO_PACOTE : linha.getPeriodoLimite());
        periodo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TratamentoDoExcedente> excedente = new ComboBox<>();
        excedente.getItems().addAll(TratamentoDoExcedente.values());
        excedente.setConverter(nome(TratamentoDoExcedente::getRotulo));
        excedente.getSelectionModel().select(nova ? TratamentoDoExcedente.PRECO_DO_CATALOGO
                : linha.getTratamentoExcedente());
        excedente.setMaxWidth(Double.MAX_VALUE);

        TextField ordem = new TextField(nova ? "" : String.valueOf(linha.getOrdem()));

        TextArea observacao = new TextArea(nova ? "" : linha.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Serviço do catálogo", servico),
                Pecas.campo("O que entra", abrangencia),
                Pecas.campo("Item, quando for só um", item));
        HBox linha2 = new HBox(16, Pecas.campo("Quantidade incluída", quantidade),
                Pecas.campo("Período do limite", periodo),
                Pecas.campo("Se passar do limite", excedente));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante caixa = JanelaFlutuante.nova(janela.palco(),
                nova ? "Incluir serviço no pacote" : "Serviço do pacote",
                "O que entra no valor do pacote e até onde vai o que está incluído.");
        caixa.com(linha1, ilimitado, linha2, Pecas.campo("Observação", observacao));
        if (!nova) {
            caixa.com(Pecas.campo("Ordem na lista", ordem));
        }
        caixa.acao(nova ? "Incluir no pacote" : "Salvar linha", () -> {
            if (servico.getValue() == null) {
                janela.reclamar("Escolha o serviço do catálogo.");
                return false;
            }
            UUID itemId = item.getValue() == null ? null : item.getValue().getId();
            if (nova) {
                pacotes.adicionarLinha(qual, servico.getValue().getId(), abrangencia.getValue(),
                        itemId, ilimitado.isSelected(), numero(quantidade.getText()),
                        periodo.getValue(), excedente.getValue(), observacao.getText());
                janela.avisar("Serviço incluído no pacote.");
            } else {
                pacotes.salvarLinha(qual, linha.getId(), servico.getValue().getId(),
                        abrangencia.getValue(), itemId, ilimitado.isSelected(),
                        numero(quantidade.getText()), periodo.getValue(), excedente.getValue(),
                        observacao.getText(), inteiro(ordem.getText(), linha.getOrdem()));
                janela.avisar("Linha do pacote salva.");
            }
            janela.atualizar();
            return true;
        });
        if (!nova) {
            caixa.outraAcao("Tirar do pacote", () -> {
                pacotes.removerLinha(qual, linha.getId());
                janela.avisar("Serviço tirado do pacote.");
                caixa.fechar();
                janela.atualizar();
            });
        }
        caixa.abrir();
    }

    // ------------------------------------------------------- contratações

    private void contratacoes(VBox tela) {
        List<ContratacaoDePacote> lista = pacotes.contratacoesDo(qual);

        tela.getChildren().add(Pecas.cabecalho("clientes", "Quem tem este pacote",
                "Cada contratação pode ter um valor acordado diferente do valor de tabela.",
                Pecas.botao("Contratar para um cliente", () -> janelaDaContratacao(null))));

        tela.getChildren().add(Tabela.de(lista)
                .coluna("Cliente", c -> c.getPagador() == null ? "" : c.getPagador().getNome(), 2)
                .coluna("Unidade", c -> c.getUnidade() == null ? "todas"
                        : c.getUnidade().getRazaoSocial(), 1.8)
                .valor("Valor acordado", ContratacaoDePacote::getValorResumido)
                .coluna("Vigência", ContratacaoDePacote::getVigenciaResumida, 1.4)
                .comMarca(c -> c.getSituacao() == null ? ""
                                : c.getSituacao().name().toLowerCase(),
                        c -> c.getSituacao() == SituacaoDaContratacao.ATIVA ? "s-pago"
                                : "s-cancelado")
                .aoClicar(this::janelaDaContratacao)
                .quandoVazia("Ninguém contratou este pacote ainda.")
                .montar());

        Label dica = new Label("Clique numa contratação para editar ou encerrar.");
        dica.getStyleClass().add("dica");
        tela.getChildren().add(dica);
    }

    private void janelaDaContratacao(ContratacaoDePacote contratacao) {
        boolean nova = contratacao == null;

        ComboBox<Pagador> cliente = new ComboBox<>();
        cliente.getItems().addAll(pacotes.clientes());
        cliente.setConverter(nome(Pagador::getNome));
        if (!nova && contratacao.getPagador() != null) {
            cliente.getItems().stream()
                    .filter(c -> c.getId().equals(contratacao.getPagador().getId()))
                    .findFirst().ifPresent(cliente.getSelectionModel()::select);
        } else {
            cliente.getSelectionModel().selectFirst();
        }
        cliente.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ClienteEspelho> unidade = new ComboBox<>();
        unidade.getItems().addAll(pacotes.unidadesDaEmpresa());
        unidade.setConverter(nome(ClienteEspelho::getRazaoSocial));
        if (!nova && contratacao.getUnidade() != null) {
            unidade.getItems().stream()
                    .filter(u -> u.getId().equals(contratacao.getUnidade().getId()))
                    .findFirst().ifPresent(unidade.getSelectionModel()::select);
        }
        unidade.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField(nova ? ""
                : Pecas.numero(contratacao.getValorAcordado()));
        valor.setPromptText("em branco usa o valor do pacote");

        DatePicker inicio = new DatePicker(nova ? LocalDate.now() : contratacao.getInicio());
        DatePicker fim = new DatePicker(nova ? null : contratacao.getFim());
        inicio.setMaxWidth(Double.MAX_VALUE);
        fim.setMaxWidth(Double.MAX_VALUE);

        ComboBox<SituacaoDaContratacao> situacao = new ComboBox<>();
        situacao.getItems().addAll(SituacaoDaContratacao.values());
        situacao.getSelectionModel().select(nova ? SituacaoDaContratacao.ATIVA
                : contratacao.getSituacao());
        situacao.setMaxWidth(Double.MAX_VALUE);

        TextArea observacao = new TextArea(nova ? "" : contratacao.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Cliente", cliente),
                Pecas.campo("Unidade", unidade),
                Pecas.campo("Valor acordado (R$)", valor));
        HBox linha2 = new HBox(16, Pecas.campo("Início da vigência", inicio),
                Pecas.campo("Fim da vigência", fim));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante caixa = JanelaFlutuante.nova(janela.palco(),
                nova ? "Contratar este pacote" : "Contratação",
                "A contratação é o que faz o pacote virar cobrança todo período.");
        caixa.com(linha1, linha2, Pecas.campo("Observação", observacao));
        if (!nova) {
            caixa.com(Pecas.campo("Situação", situacao));
        }
        caixa.acao(nova ? "Registrar contratação" : "Salvar contratação", () -> {
            if (cliente.getValue() == null) {
                janela.reclamar("Escolha o cliente.");
                return false;
            }
            UUID unidadeId = unidade.getValue() == null ? null : unidade.getValue().getId();
            BigDecimal quanto = TelaTituloNovo.dinheiro(valor.getText());
            if (nova) {
                pacotes.contratar(qual, cliente.getValue().getId(), unidadeId, quanto,
                        inicio.getValue(), fim.getValue(), observacao.getText());
                janela.avisar("Contratação registrada.");
            } else {
                pacotes.salvarContratacao(contratacao.getId(), qual, cliente.getValue().getId(),
                        unidadeId, quanto, inicio.getValue(), fim.getValue(),
                        situacao.getValue(), observacao.getText());
                janela.avisar("Contratação salva.");
            }
            janela.atualizar();
            return true;
        });
        if (!nova) {
            caixa.outraAcao("Encerrar contratação hoje", () -> {
                pacotes.encerrarContratacao(contratacao.getId(), LocalDate.now());
                janela.avisar("Contratação encerrada hoje.");
                caixa.fechar();
                janela.atualizar();
            });
        }
        caixa.abrir();
    }

    // ------------------------------------------------------------- apoio

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
}
