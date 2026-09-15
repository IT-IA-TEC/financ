package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.AgenteIa;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.dominio.PermissaoDaIa;
import br.com.itia.financeiro.dominio.PermissaoLigada;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsoDaIa;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.AcordoRepositorio;
import br.com.itia.financeiro.repositorio.AgenteIaRepositorio;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.IntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.OperacaoIntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.PermissaoLigadaRepositorio;
import br.com.itia.financeiro.repositorio.RegraDaEmpresaRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import br.com.itia.financeiro.repositorio.UsoDaIaRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O modelo de inteligência, com coleira curta.
 *
 * Duas conferências sustentam tudo o que este serviço faz:
 *
 *   1. Na LEITURA. O texto que vai para o modelo é montado pedaço por pedaço,
 *      e cada pedaço só entra se a chave daquele assunto estiver ligada. O que
 *      não está liberado não chega lá, então o modelo não tem como falar sobre
 *      isso nem por engano.
 *
 *   2. Na AÇÃO. Antes de executar qualquer coisa, a chave é conferida de novo,
 *      junto com o teto de valor e o teto de mensagens do dia. Sem chave, a
 *      ação é recusada e a recusa fica gravada com o motivo.
 *
 * Fora disso existe uma lista de coisas que não acontecem nunca, com chave
 * nenhuma: dar baixa, dar desconto, cancelar cobrança, fechar acordo sozinho e
 * mexer em contas a pagar. Isso não é configuração, é desenho.
 */
@Service
public class Inteligencia {

    private static final Logger LOG = LoggerFactory.getLogger(Inteligencia.class);
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AgenteIaRepositorio agentes;
    private final PermissaoLigadaRepositorio permissoes;
    private final UsoDaIaRepositorio usos;
    private final IntegracaoRepositorio integracoes;
    private final OperacaoIntegracaoRepositorio operacoes;
    private final IntegracaoServico integracaoServico;
    private final ClienteRepositorio clientes;
    private final TituloRepositorio titulos;
    private final MensagemRepositorio mensagens;
    private final InteracaoRepositorio interacoes;
    private final AcordoRepositorio acordos;
    private final CasoDeCobrancaRepositorio casos;
    private final RegraDaEmpresaRepositorio regras;
    private final ContextoEmpresa contexto;

    public Inteligencia(AgenteIaRepositorio agentes, PermissaoLigadaRepositorio permissoes,
                        UsoDaIaRepositorio usos, IntegracaoRepositorio integracoes,
                        OperacaoIntegracaoRepositorio operacoes,
                        IntegracaoServico integracaoServico, ClienteRepositorio clientes,
                        TituloRepositorio titulos, MensagemRepositorio mensagens,
                        InteracaoRepositorio interacoes, AcordoRepositorio acordos,
                        CasoDeCobrancaRepositorio casos, RegraDaEmpresaRepositorio regras,
                        ContextoEmpresa contexto) {
        this.agentes = agentes;
        this.permissoes = permissoes;
        this.usos = usos;
        this.integracoes = integracoes;
        this.operacoes = operacoes;
        this.integracaoServico = integracaoServico;
        this.clientes = clientes;
        this.titulos = titulos;
        this.mensagens = mensagens;
        this.interacoes = interacoes;
        this.acordos = acordos;
        this.casos = casos;
        this.regras = regras;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------ configuracao

    public AgenteIa daEmpresa() {
        UUID empresaId = contexto.exigirEmpresaId();
        return agentes.findById(empresaId).orElseGet(() -> new AgenteIa(empresaId));
    }

    @Transactional
    public void salvar(UUID integracaoId, String modelo, boolean ativo, String modo,
                       String instrucao, BigDecimal tetoValor, int tetoMensagensDia) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.DIRETOR);
        UUID empresaId = contexto.exigirEmpresaId();
        if (integracaoId != null) {
            integracoes.findByIdAndEmpresaId(integracaoId, empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Integração não encontrada."));
        }
        AgenteIa agente = agentes.findById(empresaId).orElseGet(() -> new AgenteIa(empresaId));
        agente.ajustar(integracaoId, modelo, ativo, modo, instrucao, tetoValor,
                tetoMensagensDia, contexto.autor());
        agentes.save(agente);
    }

    /** As chaves desta empresa, todas, ligadas e desligadas. */
    public Map<PermissaoDaIa, Boolean> chaves() {
        UUID empresaId = contexto.exigirEmpresaId();
        Map<String, Boolean> ligadas = new LinkedHashMap<>();
        for (PermissaoLigada linha : permissoes.findByEmpresaId(empresaId)) {
            ligadas.put(linha.getChave(), linha.isLigada());
        }
        Map<PermissaoDaIa, Boolean> tudo = new LinkedHashMap<>();
        for (PermissaoDaIa permissao : PermissaoDaIa.values()) {
            tudo.put(permissao, ligadas.getOrDefault(permissao.name(), false));
        }
        return tudo;
    }

    /** Liga ou desliga uma chave, guardando quem mexeu. */
    @Transactional
    public void mexerNaChave(PermissaoDaIa permissao, boolean ligar) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.DIRETOR);
        UUID empresaId = contexto.exigirEmpresaId();
        PermissaoLigada linha = permissoes.findByEmpresaIdAndChave(empresaId, permissao.name())
                .orElseGet(() -> new PermissaoLigada(empresaId, permissao));
        if (ligar) {
            linha.ligar(contexto.autor());
        } else {
            linha.desligar(contexto.autor());
        }
        permissoes.save(linha);
    }

    /** Troca todas as chaves de uma vez, do jeito que a tela mandou. */
    @Transactional
    public void guardarChaves(List<String> ligadas) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.DIRETOR);
        Set<String> escolhidas = ligadas == null ? Set.of() : Set.copyOf(ligadas);
        for (PermissaoDaIa permissao : PermissaoDaIa.values()) {
            mexerNaChave(permissao, escolhidas.contains(permissao.name()));
        }
    }

    public boolean pode(UUID empresaId, PermissaoDaIa permissao) {
        return permissoes.findByEmpresaIdAndChave(empresaId, permissao.name())
                .map(PermissaoLigada::isLigada).orElse(false);
    }

    public List<UsoDaIa> ultimosUsos() {
        return usos.findTop50ByEmpresaIdOrderByOcorridoEmDesc(contexto.exigirEmpresaId());
    }

    public long quantosUsos() {
        return usos.countByEmpresaId(contexto.exigirEmpresaId());
    }

    public long quantasAcoes() {
        return usos.countByEmpresaIdAndExecutadaTrue(contexto.exigirEmpresaId());
    }

    // ---------------------------------------------------------------- leitura

    /**
     * Monta o que o modelo pode ver deste cliente.
     *
     * Cada bloco olha a chave antes de entrar. O que sai daqui é exatamente o
     * que o modelo vai enxergar: nada além.
     */
    public String contextoDe(UUID unidadeId, LocalDate hoje, Set<PermissaoDaIa> usadas) {
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        StringBuilder texto = new StringBuilder();

        if (pode(empresaId, PermissaoDaIa.LER_CADASTRO)) {
            usadas.add(PermissaoDaIa.LER_CADASTRO);
            texto.append("Cliente: ").append(cliente.getRazaoSocial()).append("\n");
            if (cliente.getResponsavel() != null) {
                texto.append("Falamos com: ").append(cliente.getResponsavel()).append("\n");
            }
            texto.append("Tom combinado: ").append(cliente.getTomDeCobranca()).append("\n")
                    .append("Aceita parcelamento: ")
                    .append(cliente.isAceitaParcelamento() ? "sim" : "não").append("\n\n");
        }

        if (pode(empresaId, PermissaoDaIa.LER_SALDO)) {
            usadas.add(PermissaoDaIa.LER_SALDO);
            List<Titulo> emAberto = emAbertoDe(unidadeId);
            BigDecimal total = BigDecimal.ZERO;
            texto.append("Em aberto:\n");
            for (Titulo titulo : emAberto) {
                texto.append("- ").append(titulo.getDescricao()).append(", ")
                        .append(MontadorDeCobranca.dinheiro(titulo.getSaldo()))
                        .append(", vence em ").append(titulo.getVencimento().format(BR));
                long atraso = titulo.diasDeAtraso(hoje);
                if (atraso > 0) {
                    texto.append(", atrasado ").append(atraso).append(" dias");
                }
                texto.append("\n");
                total = total.add(titulo.getSaldo());
            }
            texto.append("Total em aberto: ").append(MontadorDeCobranca.dinheiro(total))
                    .append("\n\n");
        }

        if (pode(empresaId, PermissaoDaIa.LER_REGRAS)) {
            usadas.add(PermissaoDaIa.LER_REGRAS);
            RegraDaEmpresa regra = regras.findById(empresaId)
                    .orElseGet(() -> new RegraDaEmpresa(empresaId));
            Empresa empresa = contexto.exigirEmpresa();
            texto.append("Chave PIX da empresa: ")
                    .append(empresa.getChavePix() == null ? "não cadastrada"
                            : empresa.getChavePix()).append("\n");
            texto.append("Cobra juros e multa por atraso: ")
                    .append(regra.isCobrarJuros() ? "sim" : "não").append("\n\n");
        }

        if (pode(empresaId, PermissaoDaIa.LER_HISTORICO)) {
            usadas.add(PermissaoDaIa.LER_HISTORICO);
            texto.append("História recente:\n");
            for (Interacao linha : interacoes.findByUnidadeIdOrderByOcorridoEmDesc(unidadeId)
                    .stream().limit(8).toList()) {
                texto.append("- ").append(linha.getTipoLegivel()).append(": ")
                        .append(linha.getDescricao()).append("\n");
            }
            for (Acordo acordo : acordos.findByEmpresaIdAndUnidadeIdOrderByCriadoEmDesc(
                    empresaId, unidadeId)) {
                texto.append("- Acordo ").append(acordo.getNumero()).append(": ")
                        .append(acordo.getSituacaoLegivel()).append("\n");
            }
            texto.append("\n");
        }

        if (pode(empresaId, PermissaoDaIa.LER_PAGAMENTOS)) {
            usadas.add(PermissaoDaIa.LER_PAGAMENTOS);
            texto.append("Pagamentos:\n");
            for (Titulo titulo : titulos.findByClienteIdOrderByVencimentoDesc(unidadeId)) {
                if (titulo.getSituacao() == SituacaoTitulo.PAGO
                        && !titulo.getPagamentos().isEmpty()) {
                    texto.append("- ").append(titulo.getDescricao()).append(": pago em ")
                            .append(titulo.getPagamentos().get(0).getPagoEm().format(BR))
                            .append(", vencia em ")
                            .append(titulo.getVencimento().format(BR)).append("\n");
                }
            }
            texto.append("\n");
        }

        if (pode(empresaId, PermissaoDaIa.LER_CONVERSA)) {
            usadas.add(PermissaoDaIa.LER_CONVERSA);
            texto.append("Conversa:\n");
            for (Mensagem mensagem : mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(
                    empresaId, unidadeId).stream().skip(
                    Math.max(0, mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(
                            empresaId, unidadeId).size() - 12)).toList()) {
                texto.append(mensagem.deEntrada() ? "cliente: " : "nós: ")
                        .append(mensagem.getCorpo().replaceAll("\\s+", " ")).append("\n");
            }
        }

        if (texto.length() == 0) {
            return null;
        }
        return texto.toString();
    }

    private List<Titulo> emAbertoDe(UUID unidadeId) {
        return titulos.findByClienteIdOrderByVencimentoDesc(unidadeId).stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();
    }

    // ----------------------------------------------------------------- chamar

    /** O que voltou do modelo. */
    public record Resposta(String texto, String recusa) {
        public boolean deuCerto() {
            return texto != null && recusa == null;
        }
    }

    /**
     * Pede ao modelo uma leitura do caso ou uma sugestão de resposta.
     *
     * Nunca manda nada para o cliente: o que volta fica na tela, para a pessoa
     * ler. Falar com o cliente é outra ação, com outra chave.
     */
    public Resposta pedirLeitura(UUID unidadeId, String oQuePrecisa) {
        UUID empresaId = contexto.exigirEmpresaId();
        AgenteIa agente = daEmpresa();
        if (!agente.isAtivo()) {
            return new Resposta(null, "O modelo está desligado nesta empresa.");
        }
        Set<PermissaoDaIa> usadas = EnumSet.noneOf(PermissaoDaIa.class);
        String contextoDoCaso = contextoDe(unidadeId, LocalDate.now(), usadas);
        if (contextoDoCaso == null) {
            return new Resposta(null, "Nenhuma chave de leitura está ligada:"
                    + " o modelo não tem o que ler.");
        }

        String pergunta = montarPergunta(agente, contextoDoCaso, oQuePrecisa);
        UsoDaIa registro = new UsoDaIa(empresaId, unidadeId, "LEITURA_DO_CASO",
                usadas.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse(""),
                pergunta);

        // A chamada de fora fica FORA de transacao nossa de proposito: se a rede
        // falhar, o registro do uso ainda precisa ser gravado, e transacao
        // marcada para desfazer nao grava mais nada.
        try {
            String resposta = chamarModelo(agente, pergunta);
            registro.respondeu(resposta);
            guardarUso(registro);
            return new Resposta(resposta, null);
        } catch (RuntimeException erro) {
            LOG.debug("modelo nao respondeu: {}", erro.getMessage());
            registro.recusou("LEITURA", erro.getMessage());
            guardarUso(registro);
            return new Resposta(null, "O modelo não respondeu: " + erro.getMessage());
        }
    }

    /** Grava o registro numa transacao so dele. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void guardarUso(UsoDaIa registro) {
        usos.save(registro);
    }

    private String montarPergunta(AgenteIa agente, String contextoDoCaso, String oQuePrecisa) {
        StringBuilder pergunta = new StringBuilder();
        pergunta.append("Você ajuda a equipe de cobrança de uma empresa. Trabalhe apenas com")
                .append(" os dados abaixo: não invente valor, data nem combinado.\n")
                .append("Você não dá desconto, não fecha acordo e não dá baixa em nada.\n");
        if (agente.getTetoValor().signum() > 0) {
            pergunta.append("Se falar em parcelamento, nunca passe de ")
                    .append(MontadorDeCobranca.dinheiro(agente.getTetoValor()))
                    .append(" no total.\n");
        }
        if (agente.getInstrucao() != null && !agente.getInstrucao().isBlank()) {
            pergunta.append("Instruções desta empresa: ").append(agente.getInstrucao())
                    .append("\n");
        }
        pergunta.append("\nDados do caso:\n").append(contextoDoCaso).append("\n");
        pergunta.append("\nO que a equipe precisa: ")
                .append(oQuePrecisa == null || oQuePrecisa.isBlank()
                        ? "um resumo curto do caso e o próximo passo sugerido"
                        : oQuePrecisa);
        return pergunta.toString();
    }

    /** Chama a integração configurada e devolve o texto que veio. */
    private String chamarModelo(AgenteIa agente, String pergunta) {
        Integracao ligacao = integracoes.findByIdAndEmpresaId(agente.getIntegracaoId(),
                        contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalStateException(
                        "A integração do modelo não está configurada."));
        if (!ligacao.isAtiva()) {
            throw new IllegalStateException("A integração do modelo está desligada.");
        }
        Optional<OperacaoIntegracao> gerar = operacoes
                .findByIntegracaoIdOrderByNome(ligacao.getId()).stream()
                .filter(o -> o.getNome().toLowerCase().startsWith("gerar resposta"))
                .findFirst();
        if (gerar.isEmpty()) {
            throw new IllegalStateException(
                    "Falta a operação \"Gerar resposta\" nesta integração.");
        }

        String corpo = """
                {"model":"%s","max_tokens":700,
                 "messages":[{"role":"user","content":"%s"}]}"""
                .formatted(agente.getModelo() == null ? "" : agente.getModelo(),
                        escapar(pergunta));
        String resposta = integracaoServico.executar(ligacao.getId(), gerar.get().getId(), corpo);
        return textoDaResposta(resposta);
    }

    /** Tira o texto da resposta, sem depender de um provedor só. */
    private String textoDaResposta(String resposta) {
        if (resposta == null || resposta.isBlank()) {
            throw new IllegalStateException("a resposta veio vazia");
        }
        java.util.regex.Matcher achou = java.util.regex.Pattern
                .compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(resposta);
        if (achou.find()) {
            return desescapar(achou.group(1));
        }
        achou = java.util.regex.Pattern
                .compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(resposta);
        if (achou.find()) {
            return desescapar(achou.group(1));
        }
        throw new IllegalStateException("não encontrei o texto na resposta do modelo");
    }

    private String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private String desescapar(String texto) {
        return texto.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // ----------------------------------------------------------------- acoes

    /**
     * O porteiro: confere a chave, o teto e o modo antes de deixar acontecer.
     *
     * Devolve nulo quando pode, ou o motivo da recusa quando não pode. Quem
     * chama é obrigado a olhar o retorno: nenhuma ação do modelo roda sem
     * passar por aqui.
     */
    public String podeFazer(PermissaoDaIa acao, UUID unidadeId, BigDecimal valor) {
        UUID empresaId = contexto.exigirEmpresaId();
        AgenteIa agente = daEmpresa();
        if (!agente.isAtivo()) {
            return "o modelo está desligado";
        }
        if (acao.deLeitura()) {
            return "esta chave é de leitura, não de ação";
        }
        if (!pode(empresaId, acao)) {
            return "a chave \"" + acao.getRotulo() + "\" está desligada";
        }
        if (acao == PermissaoDaIa.RESPONDER_CLIENTE && !agente.falaSozinho()) {
            return "o modelo está no modo \"" + agente.getModoLegivel() + "\"";
        }
        if (acao == PermissaoDaIa.PROPOR_ACORDO && !agente.dentroDoTeto(valor)) {
            return "o valor passa do teto de "
                    + MontadorDeCobranca.dinheiro(agente.getTetoValor());
        }
        if (acao == PermissaoDaIa.RESPONDER_CLIENTE
                && quantasHoje(empresaId, unidadeId) >= agente.getTetoMensagensDia()) {
            return "este cliente já recebeu o limite de "
                    + agente.getTetoMensagensDia() + " mensagem(ns) do modelo hoje";
        }
        return null;
    }

    private long quantasHoje(UUID empresaId, UUID unidadeId) {
        OffsetDateTime inicioDoDia = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();
        return mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(empresaId, unidadeId)
                .stream()
                .filter(m -> !m.deEntrada() && "modelo".equals(m.getCriadoPor()))
                .filter(m -> m.getCriadoEm().isAfter(inicioDoDia))
                .count();
    }

    /** Grava o que o modelo fez, ou o que ele tentou fazer e foi barrado. */
    @Transactional
    public void anotarAcao(UUID unidadeId, PermissaoDaIa acao, String detalhe, String recusa) {
        UsoDaIa registro = new UsoDaIa(contexto.exigirEmpresaId(), unidadeId, "ACAO",
                acao.name(), detalhe);
        if (recusa == null) {
            registro.fez(acao.getRotulo(), contexto.autor());
        } else {
            registro.recusou(acao.getRotulo(), recusa);
        }
        usos.save(registro);
    }

    /** O que nunca acontece, com chave nenhuma. */
    public List<String> oQueNuncaFaz() {
        return PermissaoDaIa.NUNCA;
    }

    /** O resultado do porteiro para uma ação, sem executar nada. */
    public record Trava(PermissaoDaIa acao, boolean deixa, String motivo) {
    }

    /**
     * Passa cada ação pelo porteiro e mostra o que aconteceria.
     *
     * Não executa nada: serve para ver, antes de ligar o modelo de verdade, o
     * que ele conseguiria fazer e o que seria barrado, e por quê.
     */
    public List<Trava> testarTravas(UUID unidadeId, BigDecimal valor) {
        List<Trava> travas = new ArrayList<>();
        for (PermissaoDaIa acao : PermissaoDaIa.acoes()) {
            String recusa = podeFazer(acao, unidadeId, valor);
            travas.add(new Trava(acao, recusa == null, recusa));
        }
        return travas;
    }

    /** Um cliente qualquer da empresa, para o teste das travas ter em quem rodar. */
    public UUID alguemParaTestar() {
        return clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(contexto.exigirEmpresaId())
                .stream().findFirst().map(ClienteEspelho::getId).orElse(null);
    }

    /** As integrações que servem de modelo, para escolher na tela. */
    public List<Integracao> integracoesDisponiveis() {
        return integracoes.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId()).stream()
                .filter(i -> i.getProvedor() == br.com.itia.financeiro.dominio.Conector.MODELO_DE_IA)
                .toList();
    }

    /** Se existe caso em que o modelo pode agir agora, para a tela avisar. */
    public boolean prontoParaTrabalhar() {
        AgenteIa agente = daEmpresa();
        return agente.isAtivo() && agente.getIntegracaoId() != null
                && chaves().values().stream().anyMatch(Boolean::booleanValue);
    }

    /** O caso do cliente, para a ação de marcar. */
    public CasoDeCobranca caso(UUID unidadeId) {
        UUID empresaId = contexto.exigirEmpresaId();
        return casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
    }
}
