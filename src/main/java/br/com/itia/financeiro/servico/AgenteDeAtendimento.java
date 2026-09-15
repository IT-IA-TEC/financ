package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Agente;
import br.com.itia.financeiro.dominio.AtendimentoDoAgente;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.AgenteRepositorio;
import br.com.itia.financeiro.repositorio.AtendimentoDoAgenteRepositorio;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.EmpresaRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.RegraDaEmpresaRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * O agente de primeiro atendimento.
 *
 * O que este serviço protege:
 *   1. O agente só fala o que o sistema sabe: saldo, vencimento, chave PIX e o
 *      que já foi combinado. Ele não inventa valor.
 *   2. Assunto que envolve negociar dinheiro (parcelar, desconto, reclamação)
 *      sempre vai para uma pessoa, e o cliente é avisado de que foi.
 *   3. Se a empresa mandar, ele diz na primeira fala que é um atendimento
 *      automático. Isso é uma escolha da empresa, nas regras.
 *   4. Fora do horário combinado ele não responde: a mensagem fica esperando
 *      gente, e ninguém recebe resposta de máquina de madrugada.
 *   5. Toda pergunta e resposta fica registrada, com a intenção entendida.
 */
@Service
public class AgenteDeAtendimento {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern DATA_FALADA = Pattern.compile(
            "(\\d{1,2})[/ .-](\\d{1,2})(?:[/ .-](\\d{2,4}))?");

    private final AgenteRepositorio agentes;
    private final AtendimentoDoAgenteRepositorio atendimentos;
    private final MensagemRepositorio mensagens;
    private final TituloRepositorio titulos;
    private final ClienteRepositorio clientes;
    private final CasoDeCobrancaRepositorio casos;
    private final InteracaoRepositorio interacoes;
    private final RegraDaEmpresaRepositorio regras;
    private final EmpresaRepositorio empresas;
    private final ContextoEmpresa contexto;

    public AgenteDeAtendimento(AgenteRepositorio agentes,
                               AtendimentoDoAgenteRepositorio atendimentos,
                               MensagemRepositorio mensagens, TituloRepositorio titulos,
                               ClienteRepositorio clientes, CasoDeCobrancaRepositorio casos,
                               InteracaoRepositorio interacoes, RegraDaEmpresaRepositorio regras,
                               EmpresaRepositorio empresas, ContextoEmpresa contexto) {
        this.agentes = agentes;
        this.atendimentos = atendimentos;
        this.mensagens = mensagens;
        this.titulos = titulos;
        this.clientes = clientes;
        this.casos = casos;
        this.interacoes = interacoes;
        this.regras = regras;
        this.empresas = empresas;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------- configurar

    public Agente daEmpresa() {
        UUID empresaId = contexto.exigirEmpresaId();
        return agentes.findById(empresaId).orElseGet(() -> new Agente(empresaId));
    }

    @Transactional
    public void salvar(boolean ativo, String saudacao, String assinatura, int comecaAs,
                       int terminaAs, boolean respondeSabado) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        UUID empresaId = contexto.exigirEmpresaId();
        Agente agente = agentes.findById(empresaId).orElseGet(() -> new Agente(empresaId));
        agente.ajustar(ativo, saudacao, assinatura, comecaAs, terminaAs, respondeSabado,
                contexto.autor());
        agentes.save(agente);
    }

    public List<AtendimentoDoAgente> ultimos() {
        return atendimentos.findTop50ByEmpresaIdOrderByOcorridoEmDesc(
                contexto.exigirEmpresaId());
    }

    public long quantosEscalados() {
        return atendimentos.countByEmpresaIdAndEscaladoTrue(contexto.exigirEmpresaId());
    }

    public long quantosAtendidos() {
        return atendimentos.countByEmpresaId(contexto.exigirEmpresaId());
    }

    // --------------------------------------------------------------- entender

    /** O que o cliente quis dizer, pelo que ele escreveu. */
    public String entender(String texto) {
        String fala = texto == null ? "" : texto.toLowerCase();
        if (contem(fala, "parcel", "dividir", "em vezes", "em x", "acordo", "desconto",
                "abatimento")) {
            return "QUERO_PARCELAR";
        }
        if (contem(fala, "já paguei", "ja paguei", "paguei", "comprovante", "transferi",
                "pagamento feito")) {
            return "JA_PAGUEI";
        }
        if (contem(fala, "vou pagar", "pago dia", "pago sexta", "pago segunda", "pago amanha",
                "pago amanhã", "consigo pagar", "só consigo", "so consigo")) {
            return "VOU_PAGAR";
        }
        if (contem(fala, "quanto", "saldo", "devo", "em aberto", "valor", "boleto de quanto")) {
            return "QUANTO_DEVO";
        }
        if (contem(fala, "pix", "chave", "como pago", "como faço", "como faco", "segunda via",
                "boleto", "codigo", "código")) {
            return "COMO_PAGO";
        }
        if (contem(fala, "absurdo", "cancelar", "reclama", "processo", "advogado", "errado",
                "não contratei", "nao contratei", "cobrança indevida", "cobranca indevida")) {
            return "RECLAMACAO";
        }
        return "NAO_ENTENDI";
    }

    private boolean contem(String fala, String... pedacos) {
        for (String pedaco : pedacos) {
            if (fala.contains(pedaco)) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------- atender

    /**
     * Atende uma mensagem que chegou do cliente.
     *
     * Devolve a resposta gravada, ou nulo quando o agente decidiu não falar
     * (desligado, fora do horário, ou assunto de gente).
     */
    @Transactional
    public Mensagem atender(UUID empresaId, Mensagem entrada, LocalDateTime agora) {
        if (entrada == null || !entrada.deEntrada() || entrada.getUnidadeId() == null) {
            return null;
        }
        Agente agente = agentes.findById(empresaId).orElse(null);
        if (agente == null || !agente.isAtivo()) {
            return null;
        }
        Empresa empresa = empresas.findById(empresaId).orElse(null);
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(entrada.getUnidadeId(), empresaId)
                .orElse(null);
        if (empresa == null || cliente == null) {
            return null;
        }

        String intencao = entender(entrada.getCorpo());
        AtendimentoDoAgente registro = new AtendimentoDoAgente(empresaId, cliente.getId(),
                entrada.getId(), entrada.getCorpo(), intencao);

        if (!agente.podeFalar(agora)) {
            registro.escalou("chegou fora do horário do agente");
            atendimentos.save(registro);
            return null;
        }

        String resposta = montarResposta(agente, empresa, cliente, intencao,
                entrada.getCorpo(), agora.toLocalDate(), registro);
        if (resposta == null) {
            atendimentos.save(registro);
            return null;
        }

        Mensagem saida = new Mensagem(empresaId, "WHATSAPP", "SAIDA", entrada.getDestino(),
                resposta, "agente");
        saida.ligarAoCliente(entrada.getPagadorId(), cliente.getId(), null);
        mensagens.save(saida);

        entrada.marcarRespondida();
        mensagens.save(entrada);

        registro.respondeu(saida.getId());
        atendimentos.save(registro);
        return saida;
    }

    /** O texto que o agente responde, ou nulo quando o assunto é de gente. */
    private String montarResposta(Agente agente, Empresa empresa, ClienteEspelho cliente,
                                  String intencao, String pergunta, LocalDate hoje,
                                  AtendimentoDoAgente registro) {
        RegraDaEmpresa regra = regras.findById(empresa.getId())
                .orElseGet(() -> new RegraDaEmpresa(empresa.getId()));
        List<Titulo> emAberto = titulos.findByClienteIdOrderByVencimentoDesc(cliente.getId())
                .stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();

        StringBuilder texto = new StringBuilder();
        if (regra.isAgenteSeIdentifica()) {
            texto.append("Oi! Aqui é o atendimento automático da ")
                    .append(empresa.getNome()).append(".\n\n");
        } else if (agente.getSaudacao() != null && !agente.getSaudacao().isBlank()) {
            texto.append(agente.getSaudacao()).append("\n\n");
        }

        switch (intencao) {
            case "QUANTO_DEVO" -> {
                if (emAberto.isEmpty()) {
                    texto.append("Está tudo em dia por aqui: não encontrei nada em aberto"
                            + " no seu nome.");
                } else {
                    BigDecimal total = BigDecimal.ZERO;
                    texto.append("Hoje consta em aberto:\n");
                    for (Titulo titulo : emAberto) {
                        texto.append("· ").append(titulo.getDescricao()).append(", ")
                                .append(MontadorDeCobranca.dinheiro(titulo.getSaldo()))
                                .append(", vencimento ")
                                .append(titulo.getVencimento().format(BR)).append("\n");
                        total = total.add(titulo.getSaldo());
                    }
                    texto.append("\nTotal: ").append(MontadorDeCobranca.dinheiro(total))
                            .append(".");
                }
            }
            case "COMO_PAGO" -> {
                if (empresa.getChavePix() == null || empresa.getChavePix().isBlank()) {
                    registro.escalou("a empresa não tem chave PIX cadastrada");
                    return null;
                }
                texto.append("O pagamento é por PIX.\nChave: ").append(empresa.getChavePix());
                if (regra.isPixIdentificador() && !emAberto.isEmpty()) {
                    texto.append("\n\nSe der, coloque na descrição o código ")
                            .append(emAberto.get(0).getIdentificadorPix())
                            .append(": assim a baixa acontece sozinha e ninguém precisa"
                                    + " conferir comprovante.");
                }
            }
            case "JA_PAGUEI" -> {
                texto.append("Perfeito. Me manda o comprovante por aqui, por favor?\n"
                        + "Assim que chegar, a gente confere e dá baixa.");
                anotarCaso(empresa.getId(), cliente.getId(), "aguardando o comprovante", hoje);
            }
            case "VOU_PAGAR" -> {
                LocalDate prometida = lerData(pergunta, hoje);
                texto.append("Combinado");
                if (prometida != null) {
                    texto.append(", anotei para ").append(prometida.format(BR));
                    interacoes.save(new Interacao(empresa.getId(),
                            cliente.getPagador() == null ? null : cliente.getPagador().getId(),
                            cliente.getId(), "PROMESSA",
                            "Promessa registrada pelo atendimento automático",
                            emAberto.isEmpty() ? null : emAberto.get(0).getSaldo(),
                            prometida, "WHATSAPP", "agente"));
                    anotarCaso(empresa.getId(), cliente.getId(),
                            "conferir o pagamento prometido", prometida);
                }
                texto.append(". Qualquer coisa me chama por aqui.");
            }
            case "QUERO_PARCELAR" -> {
                registro.escalou("cliente pediu para negociar valor");
                anotarCaso(empresa.getId(), cliente.getId(),
                        "cliente pediu parcelamento, retornar", hoje);
                texto.append("Entendi. Negociação eu não resolvo sozinho: já passei para a"
                        + " equipe e alguém te responde por aqui.");
            }
            case "RECLAMACAO" -> {
                registro.escalou("reclamação ou contestação");
                anotarCaso(empresa.getId(), cliente.getId(),
                        "cliente contestou a cobrança, retornar", hoje);
                texto.append("Obrigado por avisar. Esse assunto eu passo direto para uma"
                        + " pessoa da equipe, que vai olhar o seu caso e te responder.");
            }
            default -> {
                registro.escalou("não entendi o que o cliente pediu");
                anotarCaso(empresa.getId(), cliente.getId(),
                        "mensagem que o agente não entendeu, responder", hoje);
                texto.append("Desculpa, não entendi direito. Já chamei alguém da equipe"
                        + " para te responder por aqui.");
            }
        }

        if (agente.getAssinatura() != null && !agente.getAssinatura().isBlank()) {
            texto.append("\n\n").append(agente.getAssinatura());
        }
        return texto.toString();
    }

    private void anotarCaso(UUID empresaId, UUID unidadeId, String proximaAcao, LocalDate data) {
        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
        String situacao = proximaAcao.contains("prometido") ? "PROMESSA"
                : (proximaAcao.contains("contestou") ? "CONTESTADO" : caso.getSituacao());
        caso.ajustar(situacao, caso.getResponsavel(), proximaAcao, data, caso.getObservacao(),
                "agente");
        casos.save(caso);
    }

    /** A data que o cliente falou, quando ele falou uma. */
    private LocalDate lerData(String texto, LocalDate hoje) {
        if (texto == null) {
            return null;
        }
        String fala = texto.toLowerCase();
        if (fala.contains("amanhã") || fala.contains("amanha")) {
            return hoje.plusDays(1);
        }
        if (fala.contains("hoje")) {
            return hoje;
        }
        Matcher achou = DATA_FALADA.matcher(texto);
        if (achou.find()) {
            try {
                int dia = Integer.parseInt(achou.group(1));
                int mes = Integer.parseInt(achou.group(2));
                int ano = achou.group(3) == null ? hoje.getYear()
                        : Integer.parseInt(achou.group(3));
                if (ano < 100) {
                    ano = 2000 + ano;
                }
                LocalDate lida = LocalDate.of(ano, mes, dia);
                // Data já passada normalmente quer dizer o mês que vem.
                return lida.isBefore(hoje) && achou.group(3) == null
                        ? lida.plusMonths(1) : lida;
            } catch (RuntimeException erro) {
                return null;
            }
        }
        return null;
    }
}
