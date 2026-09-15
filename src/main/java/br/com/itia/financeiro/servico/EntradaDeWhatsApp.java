package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Contato;
import br.com.itia.financeiro.dominio.EventoIntegracao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.ContatoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * O tradutor da mensagem que chega pelo WhatsApp.
 *
 * O que este serviço protege:
 *   1. Roda sem ninguém logado: quem chama é o provedor, e a empresa vem do
 *      endereço do webhook, nunca da sessão.
 *   2. A mesma mensagem não entra duas vezes, mesmo que o provedor repita o
 *      aviso.
 *   3. Número desconhecido não é colado no cliente errado. A mensagem entra
 *      sem dono, e a pessoa decide de quem é.
 *   4. Nunca deixa a exceção subir: o provedor não pode receber erro nosso por
 *      causa de um aviso que não soubemos ler.
 */
@Service
public class EntradaDeWhatsApp {

    private static final Logger LOG = LoggerFactory.getLogger(EntradaDeWhatsApp.class);

    /** O formato da conta oficial: quem mandou, o texto e o identificador. */
    private static final Pattern DE = Pattern.compile(
            "\"(from|telefone|phone|wa_id|remetente)\"\\s*:\\s*\"([+0-9 ()-]{8,25})\"");
    private static final Pattern TEXTO = Pattern.compile(
            "\"(body|text|mensagem|message)\"\\s*:\\s*\"([^\"]{1,3500})\"");
    private static final Pattern IDENTIFICADOR = Pattern.compile(
            "\"(id|message_id|wamid)\"\\s*:\\s*\"(wamid[^\"]+|[A-Za-z0-9._-]{10,120})\"");

    private final MensagemRepositorio mensagens;
    private final ClienteRepositorio clientes;
    private final ContatoRepositorio contatos;
    private final Fluxos fluxos;
    private final AgenteDeAtendimento agente;

    public EntradaDeWhatsApp(MensagemRepositorio mensagens, ClienteRepositorio clientes,
                             ContatoRepositorio contatos, Fluxos fluxos,
                             AgenteDeAtendimento agente) {
        this.agente = agente;
        this.fluxos = fluxos;
        this.mensagens = mensagens;
        this.contatos = contatos;
        this.clientes = clientes;
    }

    /** Lê o aviso bruto do webhook e transforma em mensagem da conversa. */
    @Transactional
    public void processar(EventoIntegracao evento, UUID empresaId) {
        try {
            String corpo = evento.getCarga();
            if (corpo == null || corpo.isBlank()) {
                return;
            }
            String telefone = achar(DE, corpo);
            String texto = achar(TEXTO, corpo);
            if (telefone == null || texto == null) {
                return;
            }
            guardar(empresaId, telefone, texto, achar(IDENTIFICADOR, corpo));
            evento.marcarProcessado();
        } catch (RuntimeException erro) {
            LOG.debug("não consegui ler a mensagem do whatsapp: {}", erro.getMessage());
        }
    }

    /** Grava a mensagem recebida, sem repetir o que já entrou. */
    @Transactional
    public Mensagem guardar(UUID empresaId, String telefone, String texto, String idExterno) {
        if (idExterno != null && !idExterno.isBlank()
                && mensagens.findByEmpresaIdAndIdExterno(empresaId, idExterno).isPresent()) {
            return null;
        }
        Mensagem entrada = new Mensagem(empresaId, "WHATSAPP", "ENTRADA", telefone,
                texto == null || texto.isBlank() ? "(mensagem sem texto)" : texto, "whatsapp");
        if (idExterno != null && !idExterno.isBlank()) {
            entrada.marcarEntregue();
            entrada.guardarIdExterno(idExterno);
        }
        acharDono(empresaId, telefone).ifPresent(cliente -> {
            entrada.ligarAoCliente(cliente.getPagador() == null ? null
                    : cliente.getPagador().getId(), cliente.getId(), null);
            // Quem montou um fluxo para "quando o cliente responder" e avisado agora.
            fluxos.aconteceu("CLIENTE_RESPONDEU", empresaId, cliente.getId());
        });
        Mensagem guardada = mensagens.save(entrada);
        // O agente de primeiro atendimento so fala se a empresa ligou e se
        // estamos dentro do horario dela. Fora disso, a mensagem espera gente.
        agente.atender(empresaId, guardada, java.time.LocalDateTime.now());
        return guardada;
    }

    /** Procura de quem é o número, pelo contato ou pela ficha do cliente. */
    public Optional<ClienteEspelho> acharDono(UUID empresaId, String telefone) {
        String procurado = soNumeros(telefone);
        if (procurado.length() < 8) {
            return Optional.empty();
        }
        String fim = procurado.substring(procurado.length() - 8);
        for (ClienteEspelho cliente
                : clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(empresaId)) {
            // Olha os contatos da unidade E os do pagador: em muitos clientes
            // quem atende o telefone esta cadastrado no pagador, nao na loja.
            java.util.List<Contato> lista =
                    new java.util.ArrayList<>(contatos.findByUnidadeIdOrderByPrioridade(
                            cliente.getId()));
            if (cliente.getPagador() != null) {
                lista.addAll(contatos.findByPagadorIdOrderByPrioridade(
                        cliente.getPagador().getId()));
            }
            for (Contato contato : lista) {
                String doContato = soNumeros(contato.getWhatsapp());
                if (doContato.length() >= 8 && doContato.endsWith(fim)) {
                    return Optional.of(cliente);
                }
                String fixo = soNumeros(contato.getTelefone());
                if (fixo.length() >= 8 && fixo.endsWith(fim)) {
                    return Optional.of(cliente);
                }
            }
            String daFicha = soNumeros(cliente.getTelefone());
            if (daFicha.length() >= 8 && daFicha.endsWith(fim)) {
                return Optional.of(cliente);
            }
        }
        return Optional.empty();
    }

    private String achar(Pattern padrao, String corpo) {
        Matcher achou = padrao.matcher(corpo);
        return achou.find() ? achou.group(2).trim() : null;
    }

    private String soNumeros(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
