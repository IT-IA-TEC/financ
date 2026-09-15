package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.config.Avisos;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.EventoIntegracao;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.Conector;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.dominio.TipoAutenticacao;
import br.com.itia.financeiro.dominio.TipoIntegracao;
import br.com.itia.financeiro.repositorio.EventoIntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.IntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.OperacaoIntegracaoRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O modulo de integracoes.
 *
 * Cuida de tres coisas: guardar como cada ligacao esta configurada, receber o
 * que os outros sistemas mandam, e registrar tudo que passa. Quem interpreta o
 * conteudo de cada aviso e o tradutor daquele provedor, que entra depois; aqui
 * o aviso e recebido, guardado bruto e fica esperando.
 */
@Service
public class IntegracaoServico {

    private static final Logger LOG = LoggerFactory.getLogger(IntegracaoServico.class);

    private final IntegracaoRepositorio integracoes;
    private final OperacaoIntegracaoRepositorio operacoes;
    private final EventoIntegracaoRepositorio eventos;
    private final BaixaAutomatica baixa;
    private final EntradaDeWhatsApp entradaDeWhatsApp;
    private final EntradaDeTarefas entradaDeTarefas;
    private final ContextoEmpresa contexto;
    private final Cofre cofre;
    private final Avisos avisos;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public IntegracaoServico(IntegracaoRepositorio integracoes,
                             OperacaoIntegracaoRepositorio operacoes,
                             EventoIntegracaoRepositorio eventos, BaixaAutomatica baixa,
                             EntradaDeWhatsApp entradaDeWhatsApp,
                             EntradaDeTarefas entradaDeTarefas,
                             ContextoEmpresa contexto, Cofre cofre, Avisos avisos) {
        this.integracoes = integracoes;
        this.operacoes = operacoes;
        this.eventos = eventos;
        this.baixa = baixa;
        this.entradaDeWhatsApp = entradaDeWhatsApp;
        this.entradaDeTarefas = entradaDeTarefas;
        this.contexto = contexto;
        this.cofre = cofre;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------- configuracao

    public List<Integracao> daEmpresa() {
        return integracoes.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public Integracao buscar(UUID id) {
        return integracoes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Integração não encontrada nesta empresa."));
    }

    @Transactional
    public Integracao criar(Conector conector, TipoIntegracao tipo,
                            TipoAutenticacao autenticacao, String nome) {
        Empresa empresa = contexto.exigirEmpresa();
        Integracao nova = integracoes.save(
                new Integracao(empresa, conector, tipo, autenticacao, nome, contexto.autor()));

        // O modelo ja entra com as operacoes mais usadas daquele tipo de sistema.
        // Sao sugestao: da para mudar, apagar e acrescentar depois.
        for (Conector.Sugestao s : conector.getOperacoesSugeridas()) {
            operacoes.save(new OperacaoIntegracao(nova.getId(), s.nome(), s.verbo(),
                    s.caminho(), s.paraQue()));
        }
        return nova;
    }

    // --------------------------------------------------------------- operacoes

    public List<OperacaoIntegracao> operacoesDe(UUID integracaoId) {
        return operacoes.findByIntegracaoIdOrderByNome(integracaoId);
    }

    @Transactional
    public void criarOperacao(UUID integracaoId, String nome, String verbo,
                              String caminho, String paraQue) {
        buscar(integracaoId);
        operacoes.save(new OperacaoIntegracao(integracaoId, nome, verbo, caminho, paraQue));
    }

    @Transactional
    public void apagarOperacao(UUID integracaoId, UUID operacaoId) {
        buscar(integracaoId);
        operacoes.findByIdAndIntegracaoId(operacaoId, integracaoId).ifPresent(operacoes::delete);
    }

    /**
     * Roda uma operacao de verdade contra a outra ponta.
     *
     * Aplica a autenticacao configurada, respeita o tempo limite, tenta de novo
     * quando a falha e passageira, e grava o que aconteceu. O conteudo da
     * resposta fica guardado; a credencial, nunca.
     */
    @Transactional
    public String executar(UUID integracaoId, UUID operacaoId, String corpo) {
        Integracao integracao = buscar(integracaoId);
        OperacaoIntegracao operacao = operacoes.findByIdAndIntegracaoId(operacaoId, integracaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operacao nao encontrada."));

        if (!integracao.isAtiva()) {
            throw new IllegalStateException("Integracao desligada. Ligue antes de executar.");
        }
        if (!integracao.getAutenticacao().executaHoje()) {
            throw new IllegalStateException("A execucao com "
                    + integracao.getAutenticacao().getRotulo()
                    + " ainda nao esta pronta. A configuracao ja fica guardada.");
        }

        String endereco = (integracao.getBaseUrl() == null ? "" : integracao.getBaseUrl().trim())
                + operacao.getCaminho();
        Map<String, String> acesso = credenciais(integracao);

        RuntimeException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= integracao.getTentativas(); tentativa++) {
            try {
                HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(endereco))
                        .timeout(Duration.ofSeconds(integracao.getTempoLimiteSegundos()))
                        .header("Accept", "application/json");

                aplicarAcesso(integracao, acesso, pedido);

                String conteudo = corpo == null ? "" : corpo;
                if ("GET".equals(operacao.getVerbo()) || "DELETE".equals(operacao.getVerbo())) {
                    pedido.method(operacao.getVerbo(), HttpRequest.BodyPublishers.noBody());
                } else {
                    pedido.header("Content-Type", "application/json")
                          .method(operacao.getVerbo(), HttpRequest.BodyPublishers.ofString(conteudo));
                }

                HttpResponse<String> resposta = http.send(pedido.build(),
                        HttpResponse.BodyHandlers.ofString());

                EventoIntegracao registro = new EventoIntegracao(integracao.getId(),
                        integracao.getEmpresa().getId(), "SAIDA", operacao.getNome(),
                        String.valueOf(resposta.statusCode()), recortar(resposta.body()));
                if (resposta.statusCode() >= 400) {
                    registro.marcarErro("A outra ponta respondeu " + resposta.statusCode());
                } else {
                    registro.marcarProcessado();
                }
                eventos.save(registro);
                avisos.avisar(integracao.getEmpresa().getId(), "INTEGRACAO_EXECUTOU");
                return resposta.body();

            } catch (Exception erro) {
                ultimaFalha = new IllegalStateException(erro.getMessage(), erro);
                LOG.debug("tentativa {} falhou: {}", tentativa, erro.getMessage());
                esperar(integracao.getEsperaEntreTentativasMs());
            }
        }

        EventoIntegracao registro = new EventoIntegracao(integracao.getId(),
                integracao.getEmpresa().getId(), "SAIDA", operacao.getNome(), null, null);
        registro.marcarErro(ultimaFalha == null ? "falhou" : ultimaFalha.getMessage());
        eventos.save(registro);
        throw new IllegalStateException("Nao consegui completar a chamada depois de "
                + integracao.getTentativas() + " tentativas.");
    }

    private void aplicarAcesso(Integracao integracao, Map<String, String> acesso,
                               HttpRequest.Builder pedido) {
        switch (integracao.getAutenticacao()) {
            case CHAVE_NO_CABECALHO -> {
                String cabecalho = acesso.getOrDefault("cabecalho", "Authorization");
                pedido.header(cabecalho, acesso.getOrDefault("chave", ""));
            }
            case TOKEN_FIXO -> pedido.header("Authorization",
                    "Bearer " + acesso.getOrDefault("token", ""));
            case BASICA -> {
                String par = acesso.getOrDefault("usuario", "") + ":"
                        + acesso.getOrDefault("senha", "");
                pedido.header("Authorization", "Basic "
                        + java.util.Base64.getEncoder().encodeToString(
                                par.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            case OAUTH2 -> pedido.header("Authorization", "Bearer " + tokenOauth(integracao, acesso));
            default -> {
                // NENHUMA e ASSINATURA_HMAC nao mandam nada na saida
            }
        }
    }

    /** Pega um token novo a cada uso. Guardar token vencido custa mais caro que pedir outro. */
    private String tokenOauth(Integracao integracao, Map<String, String> acesso) {
        try {
            String corpo = "grant_type=client_credentials"
                    + "&client_id=" + java.net.URLEncoder.encode(
                            acesso.getOrDefault("clientId", ""),
                            java.nio.charset.StandardCharsets.UTF_8)
                    + "&client_secret=" + java.net.URLEncoder.encode(
                            acesso.getOrDefault("clientSecret", ""),
                            java.nio.charset.StandardCharsets.UTF_8);
            String escopo = acesso.get("escopo");
            if (escopo != null && !escopo.isBlank()) {
                corpo = corpo + "&scope=" + java.net.URLEncoder.encode(escopo,
                        java.nio.charset.StandardCharsets.UTF_8);
            }
            HttpRequest pedido = HttpRequest.newBuilder(
                            URI.create(acesso.getOrDefault("urlDoToken", "")))
                    .timeout(Duration.ofSeconds(integracao.getTempoLimiteSegundos()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo))
                    .build();
            HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            return json.readTree(resposta.body()).path("access_token").asText("");
        } catch (Exception erro) {
            throw new IllegalStateException("Nao consegui pegar o token de acesso: "
                    + erro.getMessage(), erro);
        }
    }

    private void esperar(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
        }
    }

    private String recortar(String corpo) {
        if (corpo == null) {
            return null;
        }
        return corpo.length() <= 4000 ? corpo : corpo.substring(0, 4000) + "...";
    }

    @Transactional
    public void salvar(UUID id, String nome, String ambiente, String baseUrl,
                       String observacao, TipoIntegracao tipo, TipoAutenticacao autenticacao,
                       Integer tempoLimite, Integer tentativas, Integer espera,
                       boolean verificarAssinatura, Map<String, String> camposInformados) {
        Integracao integracao = buscar(id);
        integracao.ajustar(nome, ambiente, baseUrl, observacao, tipo, autenticacao,
                tempoLimite, tentativas, espera, verificarAssinatura);

        // Campo de segredo em branco quer dizer "mantem o que ja estava".
        Map<String, String> atuais = credenciais(integracao);
        Map<String, String> novos = new LinkedHashMap<>(atuais);
        for (TipoAutenticacao.Campo campo : integracao.getCamposDeAcesso()) {
            String valor = camposInformados.get(campo.chave());
            if (valor != null && !valor.isBlank()) {
                novos.put(campo.chave(), valor.trim());
            }
        }
        integracao.guardarCredenciais(cifrar(novos));
    }

    @Transactional
    public void ligar(UUID id) {
        Integracao integracao = buscar(id);
        List<String> faltando = faltando(integracao);
        if (!faltando.isEmpty()) {
            throw new IllegalStateException("Falta preencher: " + String.join(", ", faltando));
        }
        integracao.ligar();
    }

    @Transactional
    public void desligar(UUID id) {
        buscar(id).desligar();
    }

    @Transactional
    public void trocarSegredo(UUID id) {
        buscar(id).trocarSegredo();
    }

    /** Campos exigidos que ainda estao em branco. */
    public List<String> faltando(Integracao integracao) {
        Map<String, String> guardadas = credenciais(integracao);
        return integracao.getCamposDeAcesso().stream()
                .filter(TipoAutenticacao.Campo::exigido)
                .filter(c -> {
                    String valor = guardadas.get(c.chave());
                    return valor == null || valor.isBlank();
                })
                .map(TipoAutenticacao.Campo::rotulo)
                .toList();
    }

    /** O que aparece na tela: segredo so mostra os ultimos digitos. */
    public Map<String, String> paraMostrar(Integracao integracao) {
        Map<String, String> guardadas = credenciais(integracao);
        Map<String, String> visivel = new LinkedHashMap<>();
        for (TipoAutenticacao.Campo campo : integracao.getCamposDeAcesso()) {
            String valor = guardadas.get(campo.chave());
            visivel.put(campo.chave(), campo.segredo() ? Cofre.mascarar(valor)
                    : (valor == null ? "" : valor));
        }
        return visivel;
    }

    // ------------------------------------------------------------------- teste

    /**
     * Bate na outra ponta so para ver se responde.
     *
     * Nao manda dado nenhum e nao muda nada la: e uma chamada de leitura no
     * endereco base. Serve para a pessoa saber se o endereco e a credencial
     * estao de pe antes de ligar a integracao.
     */
    @Transactional
    public boolean testar(UUID id) {
        Integracao integracao = buscar(id);
        if (!integracao.getTipo().isChamaParaFora()) {
            integracao.anotarChecagem(true, null);
            return true;
        }
        if (integracao.getBaseUrl() == null || integracao.getBaseUrl().isBlank()) {
            integracao.anotarChecagem(false, "Sem endereço para consultar.");
            return false;
        }
        try {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(integracao.getBaseUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            boolean respondeu = resposta.statusCode() < 500;
            integracao.anotarChecagem(respondeu,
                    respondeu ? null : "A outra ponta respondeu " + resposta.statusCode());
            registrar(integracao, "SAIDA", "TESTE_DE_CONEXAO",
                    String.valueOf(resposta.statusCode()), null);
            return respondeu;
        } catch (Exception erro) {
            LOG.debug("teste de conexao falhou: {}", erro.getMessage());
            integracao.anotarChecagem(false, erro.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------- recebimento

    /**
     * Guarda o que chegou pelo webhook. Nao interpreta nada ainda: guarda bruto,
     * avisa as telas e deixa o evento esperando o tradutor daquele provedor.
     */
    @Transactional
    public void receber(Integracao integracao, String tipo, String corpo) {
        EventoIntegracao evento = new EventoIntegracao(integracao.getId(),
                integracao.getEmpresa().getId(), "ENTRADA", tipo, referenciaDe(corpo), corpo);
        eventos.save(evento);
        avisos.avisar(integracao.getEmpresa().getId(), "INTEGRACAO_RECEBEU");

        // Guardado o aviso bruto, o tradutor certo assume. Mensagem de
        // WhatsApp vira conversa; o resto tenta virar baixa. Se nao conseguir,
        // o aviso fica na fila de investigacao com o motivo escrito.
        if (integracao.getProvedor() == Conector.WHATSAPP) {
            entradaDeWhatsApp.processar(evento, integracao.getEmpresa().getId());
            eventos.save(evento);
        } else if (integracao.getProvedor() == Conector.TAREFAS_DE_OUTRO_SISTEMA) {
            entradaDeTarefas.receber(evento, integracao.getEmpresa().getId());
            eventos.save(evento);
        } else {
            baixa.processar(evento, integracao.getEmpresa().getId());
        }
    }

    public Optional<Integracao> porWebhook(UUID id, String segredo) {
        return integracoes.findById(id)
                .filter(i -> i.getWebhookSegredo().equals(segredo))
                .filter(Integracao::isAtiva);
    }

    public List<EventoIntegracao> eventosDe(UUID integracaoId) {
        return eventos.findTop50ByIntegracaoIdOrderByOcorridoEmDesc(integracaoId);
    }

    public List<EventoIntegracao> ultimosEventos() {
        return eventos.findTop30ByEmpresaIdOrderByOcorridoEmDesc(contexto.exigirEmpresaId());
    }

    // ------------------------------------------------------------------- apoio

    private void registrar(Integracao integracao, String direcao, String tipo,
                           String referencia, String carga) {
        eventos.save(new EventoIntegracao(integracao.getId(), integracao.getEmpresa().getId(),
                direcao, tipo, referencia, carga));
    }

    private Map<String, String> credenciais(Integracao integracao) {
        String aberto = cofre.decifrar(integracao.getCredenciaisCifradas());
        if (aberto == null || aberto.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return json.readValue(aberto, new com.fasterxml.jackson.core.type.TypeReference<>() { });
        } catch (Exception erro) {
            LOG.warn("credenciais guardadas ilegiveis: {}", erro.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String cifrar(Map<String, String> valores) {
        try {
            return cofre.cifrar(json.writeValueAsString(valores));
        } catch (Exception erro) {
            throw new IllegalStateException("Nao consegui guardar as credenciais.", erro);
        }
    }

    /** Tenta achar um identificador dentro do aviso, so para aparecer na lista. */
    private String referenciaDe(String corpo) {
        if (corpo == null) {
            return null;
        }
        for (String chave : List.of("id", "codigo", "txid", "nossoNumero", "seuNumero")) {
            int posicao = corpo.indexOf("\"" + chave + "\"");
            if (posicao >= 0) {
                String pedaco = corpo.substring(posicao, Math.min(corpo.length(), posicao + 80));
                return pedaco.replaceAll("[\"{}]", "").trim();
            }
        }
        return null;
    }
}
