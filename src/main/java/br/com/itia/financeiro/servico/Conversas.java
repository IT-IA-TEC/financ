package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.IntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.OperacaoIntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A conversa de cobrança, dentro da plataforma.
 *
 * O que este serviço protege:
 *   1. A conversa toda mora aqui. Nada de "olha no meu celular": ida e volta
 *      ficam gravadas na mesma linha do tempo, com autor e hora.
 *   2. Mensagem que chega de um número desconhecido não é jogada fora nem
 *      colada no cliente errado: fica separada, esperando alguém dizer de quem é.
 *   3. Se o canal não estiver ligado, a mensagem fica na fila com o texto
 *      pronto. Ninguém perde o que ia dizer.
 *   4. Toda resposta do cliente marca a cobrança como respondida, e é isso que
 *      alimenta a esteira de inadimplência depois.
 */
@Service
public class Conversas {

    private static final Logger LOG = LoggerFactory.getLogger(Conversas.class);

    private final MensagemRepositorio mensagens;
    private final ClienteRepositorio clientes;
    private final TituloRepositorio titulos;
    private final InteracaoRepositorio interacoes;
    private final IntegracaoRepositorio integracoes;
    private final OperacaoIntegracaoRepositorio operacoes;
    private final IntegracaoServico integracaoServico;
    private final MontadorDeCobranca montador;
    private final ContextoEmpresa contexto;

    public Conversas(MensagemRepositorio mensagens, ClienteRepositorio clientes,
                     TituloRepositorio titulos,
                     InteracaoRepositorio interacoes, IntegracaoRepositorio integracoes,
                     OperacaoIntegracaoRepositorio operacoes,
                     IntegracaoServico integracaoServico, MontadorDeCobranca montador,
                     ContextoEmpresa contexto) {
        this.mensagens = mensagens;
        this.clientes = clientes;
        this.titulos = titulos;
        this.interacoes = interacoes;
        this.integracoes = integracoes;
        this.operacoes = operacoes;
        this.integracaoServico = integracaoServico;
        this.montador = montador;
        this.contexto = contexto;
    }

    /**
     * Uma linha da lista de conversas.
     *
     * @param unidadeId   de quem é a conversa, quando o sistema sabe
     * @param quem        o nome que aparece na lista
     * @param destino     o telefone ou e-mail usado
     * @param ultima      o texto da última mensagem
     * @param quando      quando ela foi
     * @param esperando   quantas respostas do cliente ainda não foram respondidas
     * @param emAberto    quanto esse cliente deve agora
     */
    public record Linha(UUID unidadeId, String quem, String destino, String ultima,
                        OffsetDateTime quando, int esperando, BigDecimal emAberto) {
    }

    public List<Linha> lista() {
        UUID empresaId = contexto.exigirEmpresaId();
        Map<String, List<Mensagem>> porConversa = new LinkedHashMap<>();
        for (Mensagem mensagem : mensagens.findByEmpresaIdOrderByCriadoEmDesc(empresaId)) {
            porConversa.computeIfAbsent(chaveDe(mensagem), chave -> new ArrayList<>())
                    .add(mensagem);
        }

        Map<UUID, String> nomes = new LinkedHashMap<>();
        Map<UUID, BigDecimal> devendo = new LinkedHashMap<>();
        for (Titulo titulo : titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL))) {
            UUID unidadeId = titulo.getCliente().getId();
            devendo.merge(unidadeId, titulo.getSaldo(), BigDecimal::add);
        }
        for (ClienteEspelho cliente : clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(empresaId)) {
            nomes.put(cliente.getId(), cliente.getRazaoSocial());
        }

        List<Linha> linhas = new ArrayList<>();
        for (List<Mensagem> conversa : porConversa.values()) {
            Mensagem ultima = conversa.get(0);
            UUID unidadeId = ultima.getUnidadeId();
            int esperando = 0;
            for (Mensagem mensagem : conversa) {
                if (mensagem.deEntrada() && !"RESPONDIDA".equals(mensagem.getSituacao())) {
                    esperando++;
                }
            }
            String quem = unidadeId == null ? "número não reconhecido"
                    : nomes.getOrDefault(unidadeId, "cliente");
            linhas.add(new Linha(unidadeId, quem, ultima.getDestino(), ultima.getCorpo(),
                    ultima.getCriadoEm(), esperando,
                    unidadeId == null ? BigDecimal.ZERO
                            : devendo.getOrDefault(unidadeId, BigDecimal.ZERO)));
        }
        linhas.sort(Comparator.comparing(Linha::quando).reversed());
        return linhas;
    }

    private String chaveDe(Mensagem mensagem) {
        return mensagem.getUnidadeId() != null
                ? "u:" + mensagem.getUnidadeId()
                : "d:" + soNumeros(mensagem.getDestino());
    }

    public List<Mensagem> conversa(UUID unidadeId) {
        return mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(
                contexto.exigirEmpresaId(), unidadeId);
    }

    /** As mensagens que chegaram de número que o sistema não reconheceu. */
    public List<Mensagem> semDono() {
        return mensagens.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId()).stream()
                .filter(m -> m.getUnidadeId() == null)
                .toList();
    }

    public ClienteEspelho cliente(UUID unidadeId) {
        return clientes.findByIdAndEmpresaId(unidadeId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    public List<Titulo> emAbertoDe(UUID unidadeId) {
        return titulos.findByClienteIdOrderByVencimentoDesc(unidadeId).stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();
    }

    // ---------------------------------------------------------------- escrever

    /**
     * Manda uma mensagem para o cliente.
     *
     * Se o canal do WhatsApp estiver ligado, sai na hora. Se não estiver, fica
     * na fila com o texto pronto, e a equipe manda pelo aparelho e marca aqui.
     */
    @Transactional
    public Mensagem responder(UUID unidadeId, String texto, String destinoEscolhido) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escreva a mensagem antes de mandar.");
        }
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = cliente(unidadeId);
        String destino = destinoEscolhido != null && !destinoEscolhido.isBlank()
                ? destinoEscolhido.trim() : destinoDe(cliente);
        if (destino == null) {
            throw new IllegalStateException(
                    "Este cliente não tem WhatsApp cadastrado. Cadastre o contato primeiro.");
        }

        Mensagem mensagem = new Mensagem(empresaId, "WHATSAPP", "SAIDA", destino,
                texto.trim(), contexto.autor());
        mensagem.ligarAoCliente(cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), cliente.getId(), null);
        mensagens.save(mensagem);

        marcarEntradasRespondidas(unidadeId);
        tentarEnviar(mensagem);
        return mensagens.save(mensagem);
    }

    /**
     * Para quem a mensagem vai.
     *
     * Usa exatamente a mesma escolha do disparo em lote: contato da unidade,
     * contato do pagador e, por último, o telefone da ficha. Duas telas
     * escolhendo diferente seria a receita para mandar mensagem para o número
     * errado.
     */
    private String destinoDe(ClienteEspelho cliente) {
        MontadorDeCobranca.Escolhido escolhido = montador.escolherDestino(cliente, "WHATSAPP");
        return escolhido.motivo() == null ? escolhido.destino() : null;
    }

    /**
     * Tenta entregar pela integração ligada.
     *
     * Falha de rede não derruba a tela: a mensagem fica na fila com o motivo,
     * e alguém decide o que fazer.
     */
    public void tentarEnviar(Mensagem mensagem) {
        Optional<Integracao> canal = canalDeWhatsApp();
        if (canal.isEmpty()) {
            return;
        }
        Optional<OperacaoIntegracao> enviar = operacoes
                .findByIntegracaoIdOrderByNome(canal.get().getId()).stream()
                .filter(o -> o.getNome().toLowerCase().startsWith("enviar mensagem"))
                .findFirst();
        if (enviar.isEmpty()) {
            return;
        }
        try {
            String corpo = """
                    {"messaging_product":"whatsapp","recipient_type":"individual",
                     "to":"%s","type":"text","text":{"preview_url":false,"body":"%s"}}"""
                    .formatted(soNumeros(mensagem.getDestino()), escapar(mensagem.getCorpo()));
            String resposta = integracaoServico.executar(canal.get().getId(),
                    enviar.get().getId(), corpo);
            mensagem.marcarEnviada(idDaResposta(resposta));
            registrarNaHistoria(mensagem);
        } catch (RuntimeException erro) {
            LOG.debug("envio de whatsapp falhou: {}", erro.getMessage());
            mensagem.marcarFalha("o canal respondeu: " + erro.getMessage());
        }
    }

    public Optional<Integracao> canalDeWhatsApp() {
        return integracoes.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId()).stream()
                .filter(Integracao::isAtiva)
                .filter(i -> i.getProvedor() == br.com.itia.financeiro.dominio.Conector.WHATSAPP)
                .findFirst();
    }

    private String idDaResposta(String resposta) {
        if (resposta == null) {
            return null;
        }
        java.util.regex.Matcher achou = java.util.regex.Pattern
                .compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(resposta);
        return achou.find() ? achou.group(1) : null;
    }

    private String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private void registrarNaHistoria(Mensagem mensagem) {
        interacoes.save(new Interacao(mensagem.getEmpresaId(), mensagem.getPagadorId(),
                mensagem.getUnidadeId(), "COBRANCA_ENVIADA",
                "Mensagem enviada por whatsapp para " + mensagem.getDestino(),
                null, null, "WHATSAPP", contexto.autor()));
    }

    // ----------------------------------------------------------------- receber

    /** Registra na mão uma resposta que chegou por fora. */
    @Transactional
    public Mensagem anotarResposta(UUID unidadeId, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escreva o que o cliente respondeu.");
        }
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = cliente(unidadeId);
        Mensagem entrada = new Mensagem(empresaId, "WHATSAPP", "ENTRADA",
                destinoDe(cliente), texto.trim(), contexto.autor());
        entrada.ligarAoCliente(cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), cliente.getId(), null);
        return mensagens.save(entrada);
    }

    /**
     * Junta uma mensagem de número desconhecido ao cliente certo.
     *
     * Quem decide é a pessoa, nunca o palpite do sistema.
     */
    @Transactional
    public void darDono(UUID mensagemId, UUID unidadeId) {
        UUID empresaId = contexto.exigirEmpresaId();
        Mensagem mensagem = mensagens.findByIdAndEmpresaId(mensagemId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));
        ClienteEspelho cliente = cliente(unidadeId);
        mensagem.ligarAoCliente(cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), cliente.getId(), null);
        mensagens.save(mensagem);
    }

    @Transactional
    public void marcarEntradasRespondidas(UUID unidadeId) {
        for (Mensagem mensagem : conversa(unidadeId)) {
            if (mensagem.deEntrada() && !"RESPONDIDA".equals(mensagem.getSituacao())) {
                mensagem.marcarRespondida();
                mensagens.save(mensagem);
            }
        }
    }

    private String soNumeros(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
