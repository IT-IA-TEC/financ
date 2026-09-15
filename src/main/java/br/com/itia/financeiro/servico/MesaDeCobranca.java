package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.AcordoRepositorio;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A mesa de quem cobra: a fila de um lado, o caso no meio, o cliente do outro.
 *
 * A ideia é que a pessoa abra uma tela só e saiba, sem procurar, com quem
 * falar agora, o que já foi dito, quanto o cliente deve e o que costuma
 * resolver. O que este serviço protege:
 *   1. A fila é ordenada por quem precisa mais, não por quem chegou por
 *      último. Cliente que respondeu e está esperando vem antes de todo mundo.
 *   2. O resumo do caso é montado do que está gravado, nunca de palpite.
 *   3. A sugestão diz em quantos casos ela se apoia. Sem base, ela não sugere
 *      nada e fala isso com todas as letras.
 *   4. Os atalhos escrevem o texto com os números reais do cliente, para
 *      ninguém digitar valor errado na pressa.
 */
@Service
public class MesaDeCobranca {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MensagemRepositorio mensagens;
    private final ClienteRepositorio clientes;
    private final TituloRepositorio titulos;
    private final CasoDeCobrancaRepositorio casos;
    private final InteracaoRepositorio interacoes;
    private final AcordoRepositorio acordos;
    private final Regras regras;
    private final ContextoEmpresa contexto;

    public MesaDeCobranca(MensagemRepositorio mensagens, ClienteRepositorio clientes,
                          TituloRepositorio titulos, CasoDeCobrancaRepositorio casos,
                          InteracaoRepositorio interacoes, AcordoRepositorio acordos,
                          Regras regras, ContextoEmpresa contexto) {
        this.mensagens = mensagens;
        this.clientes = clientes;
        this.titulos = titulos;
        this.casos = casos;
        this.interacoes = interacoes;
        this.acordos = acordos;
        this.regras = regras;
        this.contexto = contexto;
    }

    // -------------------------------------------------------------------- fila

    /**
     * Uma linha da fila.
     *
     * @param etiqueta  o que está acontecendo com este cliente, em duas palavras
     * @param urgencia  espera, promessa, acordo, atraso ou calmo
     * @param resumo    a linha de baixo, com o que aconteceu por último
     * @param peso      quanto menor, mais para cima na fila
     */
    public record NaFila(UUID unidadeId, String quem, String etiqueta, String urgencia,
                        String resumo, OffsetDateTime quando, BigDecimal vencido,
                        BigDecimal emAberto, long maiorAtraso, int unidades, boolean minha,
                        boolean semResposta, boolean promessaHoje, boolean acordoQuebrado,
                        boolean bloqueioPedido, boolean comprovanteEsperando, boolean quitado) {
    }

    /** Os recortes da fila, com a conta de cada um. */
    public record Recorte(String chave, String nome, long quantos) {
    }

    public List<Recorte> recortes(List<NaFila> fila) {
        return List.of(
                new Recorte("dia", "Fila do dia", fila.stream()
                        .filter(f -> !"calmo".equals(f.urgencia())).count()),
                new Recorte("sem_resposta", "Sem resposta", fila.stream()
                        .filter(NaFila::semResposta).count()),
                new Recorte("promessa", "Promessa hoje", fila.stream()
                        .filter(NaFila::promessaHoje).count()),
                new Recorte("comprovante", "Comprovante", fila.stream()
                        .filter(NaFila::comprovanteEsperando).count()),
                new Recorte("acordo_quebrado", "Acordo quebrado", fila.stream()
                        .filter(NaFila::acordoQuebrado).count()),
                new Recorte("bloqueio", "Bloqueio pedido", fila.stream()
                        .filter(NaFila::bloqueioPedido).count()),
                new Recorte("meus", "Meus", fila.stream().filter(NaFila::minha).count()),
                new Recorte("tudo", "Todos", fila.size()));
    }

    /** Aplica o recorte e a busca por nome, telefone ou valor. */
    public List<NaFila> peneirar(List<NaFila> fila, String recorte, String busca) {
        List<NaFila> filtrada = new ArrayList<>(fila);
        if (recorte != null) {
            filtrada = switch (recorte) {
                case "sem_resposta" -> filtrada.stream().filter(NaFila::semResposta).toList();
                case "promessa" -> filtrada.stream().filter(NaFila::promessaHoje).toList();
                case "comprovante" -> filtrada.stream()
                        .filter(NaFila::comprovanteEsperando).toList();
                case "acordo_quebrado" -> filtrada.stream()
                        .filter(NaFila::acordoQuebrado).toList();
                case "bloqueio" -> filtrada.stream().filter(NaFila::bloqueioPedido).toList();
                case "meus" -> filtrada.stream().filter(NaFila::minha).toList();
                case "tudo" -> filtrada;
                default -> filtrada.stream()
                        .filter(f -> !"calmo".equals(f.urgencia())).toList();
            };
        }
        if (busca != null && !busca.isBlank()) {
            String procurado = busca.trim().toLowerCase();
            String soNumeros = procurado.replaceAll("\\D", "");
            filtrada = filtrada.stream().filter(f -> {
                if (f.quem() != null && f.quem().toLowerCase().contains(procurado)) {
                    return true;
                }
                if (!soNumeros.isEmpty()) {
                    String valor = f.emAberto() == null ? "" : f.emAberto().toString();
                    return valor.replaceAll("\\D", "").contains(soNumeros);
                }
                return false;
            }).toList();
        }
        return filtrada;
    }

    /**
     * Monta a fila do dia.
     *
     * Entra quem tem conversa aberta e quem deve alguma coisa. A ordem é por
     * quem precisa mais: primeiro quem respondeu e está esperando, depois
     * promessa quebrada e acordo quebrado, depois maior atraso.
     */
    public List<NaFila> fila(LocalDate hoje) {
        UUID empresaId = contexto.exigirEmpresaId();
        String eu = contexto.autor();

        Map<UUID, List<Titulo>> porCliente = new LinkedHashMap<>();
        for (Titulo titulo : titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL))) {
            porCliente.computeIfAbsent(titulo.getCliente().getId(), id -> new ArrayList<>())
                    .add(titulo);
        }

        Map<UUID, Mensagem> ultimaDeCada = new LinkedHashMap<>();
        Map<UUID, Integer> esperando = new LinkedHashMap<>();
        for (Mensagem mensagem : mensagens.findByEmpresaIdOrderByCriadoEmDesc(empresaId)) {
            if (mensagem.getUnidadeId() == null) {
                continue;
            }
            ultimaDeCada.putIfAbsent(mensagem.getUnidadeId(), mensagem);
            if (mensagem.deEntrada() && !"RESPONDIDA".equals(mensagem.getSituacao())) {
                esperando.merge(mensagem.getUnidadeId(), 1, Integer::sum);
            }
        }

        Map<UUID, CasoDeCobranca> casoDe = new LinkedHashMap<>();
        for (CasoDeCobranca caso : casos.findByEmpresaId(empresaId)) {
            casoDe.put(caso.getUnidadeId(), caso);
        }

        List<UUID> unidades = new ArrayList<>(ultimaDeCada.keySet());
        for (UUID unidadeId : porCliente.keySet()) {
            if (!unidades.contains(unidadeId)) {
                unidades.add(unidadeId);
            }
        }

        List<NaFila> fila = new ArrayList<>();
        for (UUID unidadeId : unidades) {
            ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                    .orElse(null);
            if (cliente == null) {
                continue;
            }
            List<Titulo> documentos = porCliente.getOrDefault(unidadeId, List.of());
            BigDecimal vencido = BigDecimal.ZERO;
            BigDecimal emAberto = BigDecimal.ZERO;
            long maiorAtraso = 0;
            for (Titulo titulo : documentos) {
                emAberto = emAberto.add(titulo.getSaldo());
                long atraso = titulo.diasDeAtraso(hoje);
                if (atraso > 0) {
                    vencido = vencido.add(titulo.getSaldo());
                    maiorAtraso = Math.max(maiorAtraso, atraso);
                }
            }

            CasoDeCobranca caso = casoDe.get(unidadeId);
            Mensagem ultima = ultimaDeCada.get(unidadeId);
            int esperandoAqui = esperando.getOrDefault(unidadeId, 0);
            Interacao promessa = promessaEmAberto(unidadeId);
            List<Interacao> historia = interacoes.findByUnidadeIdOrderByOcorridoEmDesc(unidadeId);

            boolean acordoQuebrado = acordos.findByEmpresaIdAndUnidadeIdOrderByCriadoEmDesc(
                            empresaId, unidadeId).stream()
                    .anyMatch(a -> "QUEBRADO".equals(a.getSituacao()));
            boolean bloqueioPedido = historia.stream()
                    .anyMatch(i -> "BLOQUEIO".equals(i.getTipo()));
            boolean comprovanteEsperando = caso != null && caso.getProximaAcao() != null
                    && caso.getProximaAcao().toLowerCase().contains("comprovante");
            boolean semResposta = ultima != null && !ultima.deEntrada()
                    && ChronoUnit.DAYS.between(ultima.getCriadoEm().toLocalDate(), hoje) >= 3;
            boolean promessaHoje = promessa != null && promessa.getDataPrometida() != null
                    && !promessa.getDataPrometida().isAfter(hoje);

            String etiqueta;
            String urgencia;
            if (esperandoAqui > 0) {
                etiqueta = "respondeu, esperando";
                urgencia = "espera";
            } else if (promessa != null && promessa.promessaVencida(hoje)) {
                etiqueta = "promessa quebrada";
                urgencia = "promessa";
            } else if (acordoQuebrado && maiorAtraso > 0) {
                etiqueta = "acordo quebrado";
                urgencia = "promessa";
            } else if (promessa != null) {
                etiqueta = "promete " + promessa.getDataPrometida().format(BR);
                urgencia = "calmo";
            } else if (caso != null && caso.pausada(hoje)) {
                etiqueta = "régua pausada";
                urgencia = "calmo";
            } else if (caso != null && "EM_ACORDO".equals(caso.getSituacao())) {
                etiqueta = "acordo em dia";
                urgencia = "acordo";
            } else if (caso != null && "CONTESTADO".equals(caso.getSituacao())) {
                etiqueta = "contestado";
                urgencia = "promessa";
            } else if (caso != null && caso.atrasado(hoje)) {
                etiqueta = "tarefa atrasada";
                urgencia = "promessa";
            } else if (maiorAtraso > 0) {
                etiqueta = maiorAtraso + " dias de atraso";
                urgencia = "atraso";
            } else if (documentos.isEmpty()) {
                etiqueta = "quitado";
                urgencia = "calmo";
            } else {
                etiqueta = "em dia";
                urgencia = "calmo";
            }

            int quantasUnidades = 1;
            if (cliente.getPagador() != null) {
                quantasUnidades = (int) clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(
                                empresaId).stream()
                        .filter(c -> c.getPagador() != null
                                && c.getPagador().getId().equals(cliente.getPagador().getId()))
                        .count();
            }

            fila.add(new NaFila(unidadeId, cliente.getRazaoSocial(), etiqueta, urgencia,
                    resumoDaLinha(ultima, caso, documentos, vencido),
                    ultima == null ? null : ultima.getCriadoEm(), vencido, emAberto,
                    maiorAtraso, quantasUnidades,
                    caso != null && eu.equals(caso.getResponsavel()),
                    semResposta, promessaHoje, acordoQuebrado, bloqueioPedido,
                    comprovanteEsperando, documentos.isEmpty()));
        }

        fila.sort(Comparator.comparingInt((NaFila linha) -> switch (linha.urgencia()) {
            case "espera" -> 0;
            case "promessa" -> 1;
            case "atraso" -> 2;
            case "acordo" -> 3;
            default -> 4;
        }).thenComparing(NaFila::vencido, Comparator.reverseOrder()));
        return fila;
    }

    private String resumoDaLinha(Mensagem ultima, CasoDeCobranca caso, List<Titulo> documentos,
                                 BigDecimal vencido) {
        StringBuilder resumo = new StringBuilder();
        if (ultima != null) {
            resumo.append(ultima.deEntrada() ? "ele disse: " : "mandamos: ")
                    .append(recortar(ultima.getCorpo(), 60));
        } else {
            resumo.append(documentos.size()).append(" documento(s) em aberto");
        }
        if (vencido.signum() > 0) {
            resumo.append(" · ").append(MontadorDeCobranca.dinheiro(vencido)).append(" vencido");
        }
        if (caso != null && caso.getResponsavel() != null) {
            resumo.append(" · ").append(caso.getResponsavel());
        }
        return resumo.toString();
    }

    private String recortar(String texto, int quanto) {
        String limpo = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        return limpo.length() <= quanto ? limpo : limpo.substring(0, quanto) + "...";
    }

    private Interacao promessaEmAberto(UUID unidadeId) {
        return interacoes.findByUnidadeIdOrderByOcorridoEmDesc(unidadeId).stream()
                .filter(i -> "PROMESSA".equals(i.getTipo()) && "EM_ABERTO".equals(i.getSituacao()))
                .findFirst().orElse(null);
    }

    // -------------------------------------------------------------------- caso

    /**
     * A ficha do caso: o que dá para saber sem abrir mais nada.
     *
     * @param pedido     a última coisa que o cliente falou
     * @param jaFoiDito  o que a empresa já mandou, em uma linha
     * @param historico  quantas conversas houve e desde quando
     * @param alerta     o que precisa de atenção agora, ou nulo
     */
    public record Ficha(String pedido, String jaFoiDito, String historico, String situacao,
                        String alerta, String responsavel, LocalDate proximaData,
                        String proximaAcao) {
    }

    public Ficha ficha(UUID unidadeId, LocalDate hoje) {
        List<Mensagem> conversa = mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(
                contexto.exigirEmpresaId(), unidadeId);
        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(contexto.exigirEmpresaId(),
                unidadeId).orElse(null);

        String pedido = "o cliente ainda não escreveu nada";
        String jaFoiDito = "nada foi mandado ainda";
        for (Mensagem mensagem : conversa) {
            if (mensagem.deEntrada()) {
                pedido = recortar(mensagem.getCorpo(), 180);
            } else {
                jaFoiDito = recortar(mensagem.getCorpo(), 120);
            }
        }

        long desteMes = conversa.stream()
                .filter(m -> m.getCriadoEm().toLocalDate().isAfter(hoje.minusDays(30)))
                .count();
        OffsetDateTime primeira = conversa.isEmpty() ? null : conversa.get(0).getCriadoEm();
        String historico = conversa.isEmpty() ? "primeira conversa com este cliente"
                : desteMes + " mensagem(ns) nos últimos 30 dias, desde "
                        + primeira.toLocalDate().format(BR);

        return new Ficha(pedido, jaFoiDito, historico,
                caso == null ? "em cobrança" : caso.getSituacaoLegivel(),
                alertaDe(unidadeId, caso, hoje),
                caso == null ? null : caso.getResponsavel(),
                caso == null ? null : caso.getProximaData(),
                caso == null ? null : caso.getProximaAcao());
    }

    /** O que está fora do lugar neste caso, se estiver. */
    private String alertaDe(UUID unidadeId, CasoDeCobranca caso, LocalDate hoje) {
        Interacao promessa = promessaEmAberto(unidadeId);
        if (promessa != null && promessa.promessaVencida(hoje)) {
            return "prometeu pagar em " + promessa.getDataPrometida().format(BR)
                    + " e não pagou";
        }
        for (Acordo acordo : acordos.findByEmpresaIdAndUnidadeIdOrderByCriadoEmDesc(
                contexto.exigirEmpresaId(), unidadeId)) {
            if ("QUEBRADO".equals(acordo.getSituacao())) {
                return "já quebrou o acordo " + acordo.getNumero()
                        + ": cuidado ao oferecer outro";
            }
            if (acordo.estaAtivo()) {
                return "tem o acordo " + acordo.getNumero() + " em andamento";
            }
        }
        if (caso != null && caso.atrasado(hoje)) {
            return "a tarefa marcada para " + caso.getProximaData().format(BR)
                    + " não foi feita";
        }
        if (caso != null && "CONTESTADO".equals(caso.getSituacao())) {
            return "o cliente contestou a cobrança: não cobre antes de resolver";
        }
        return null;
    }

    // ---------------------------------------------------------------- copiloto

    /**
     * O que costuma resolver, com base no que já aconteceu nesta empresa.
     *
     * @param texto  a leitura, em português
     * @param base   em quantos casos ela se apoia
     */
    public record Sugestao(String texto, int base, String atalhoSugerido) {
    }

    /**
     * Olha os clientes que estavam na mesma faixa de atraso e acabaram pagando,
     * e conta o que tinha acontecido antes do pagamento.
     *
     * Quando não há casos parecidos suficientes, não inventa: diz que ainda não
     * tem base.
     */
    public Sugestao copiloto(UUID unidadeId, LocalDate hoje) {
        UUID empresaId = contexto.exigirEmpresaId();
        long atrasoAqui = maiorAtrasoDe(unidadeId, hoje);
        int comPromessa = 0;
        int comAcordo = 0;
        int soCobranca = 0;

        for (Titulo titulo : titulos.findByEmpresaIdOrderByVencimentoDesc(empresaId)) {
            if (titulo.getSituacao() != SituacaoTitulo.PAGO) {
                continue;
            }
            long atrasoQueTeve = ChronoUnit.DAYS.between(titulo.getVencimento(),
                    titulo.getPagamentos().isEmpty() ? titulo.getVencimento()
                            : titulo.getPagamentos().get(0).getPagoEm());
            if (!mesmaFaixa(atrasoQueTeve, atrasoAqui)) {
                continue;
            }
            UUID quem = titulo.getCliente().getId();
            List<Interacao> historia = interacoes.findByUnidadeIdOrderByOcorridoEmDesc(quem);
            boolean teveAcordo = historia.stream().anyMatch(i -> "ACORDO".equals(i.getTipo()));
            boolean tevePromessa = historia.stream()
                    .anyMatch(i -> "PROMESSA".equals(i.getTipo()));
            if (teveAcordo) {
                comAcordo++;
            } else if (tevePromessa) {
                comPromessa++;
            } else {
                soCobranca++;
            }
        }

        int base = comPromessa + comAcordo + soCobranca;
        if (base < 3) {
            return new Sugestao("Ainda não há casos parecidos suficientes nesta empresa para"
                    + " sugerir um caminho. Conforme os pagamentos forem entrando, esta leitura"
                    + " começa a aparecer.", base, null);
        }
        if (comPromessa >= comAcordo && comPromessa >= soCobranca) {
            return new Sugestao("Em " + comPromessa + " de " + base + " casos parecidos"
                    + " (atraso semelhante), o cliente pagou depois de combinar uma data."
                    + " Combinar o dia costuma render mais do que repetir a cobrança.",
                    base, "prazo");
        }
        if (comAcordo >= soCobranca) {
            return new Sugestao("Em " + comAcordo + " de " + base + " casos parecidos, o que"
                    + " destravou foi parcelar. Vale propor um acordo antes de insistir na"
                    + " cobrança cheia.", base, null);
        }
        return new Sugestao("Em " + soCobranca + " de " + base + " casos parecidos, o cliente"
                + " pagou com a cobrança simples, sem precisar negociar. Mandar o valor e a"
                + " chave costuma bastar.", base, "pix");
    }

    private boolean mesmaFaixa(long umAtraso, long outroAtraso) {
        return faixa(umAtraso) == faixa(outroAtraso);
    }

    private int faixa(long atraso) {
        if (atraso <= 0) {
            return 0;
        }
        if (atraso <= 7) {
            return 1;
        }
        if (atraso <= 15) {
            return 2;
        }
        if (atraso <= 30) {
            return 3;
        }
        if (atraso <= 60) {
            return 4;
        }
        return 5;
    }

    private long maiorAtrasoDe(UUID unidadeId, LocalDate hoje) {
        long maior = 0;
        for (Titulo titulo : titulos.findByClienteIdOrderByVencimentoDesc(unidadeId)) {
            if (titulo.getSituacao() == SituacaoTitulo.ABERTO
                    || titulo.getSituacao() == SituacaoTitulo.PARCIAL) {
                maior = Math.max(maior, titulo.diasDeAtraso(hoje));
            }
        }
        return maior;
    }

    // ---------------------------------------------------------------- atalhos

    /**
     * Os textos prontos, já com os números deste cliente.
     *
     * Ninguém digita valor na pressa: o atalho escreve o que o sistema sabe.
     */
    public Map<String, String> atalhos(UUID unidadeId, LocalDate hoje) {
        Empresa empresa = contexto.exigirEmpresa();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId,
                contexto.exigirEmpresaId()).orElseThrow();
        List<Titulo> emAberto = titulos.findByClienteIdOrderByVencimentoDesc(unidadeId).stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();

        BigDecimal total = emAberto.stream().map(Titulo::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal atualizado = total;
        for (Titulo titulo : emAberto) {
            atualizado = atualizado.add(regras.daEmpresa().acrescimoDe(titulo.getSaldo(),
                    titulo.getVencimento(), hoje));
        }

        StringBuilder lista = new StringBuilder();
        for (Titulo titulo : emAberto) {
            lista.append("· ").append(titulo.getDescricao()).append(", ")
                    .append(MontadorDeCobranca.dinheiro(titulo.getSaldo()))
                    .append(", vence em ").append(titulo.getVencimento().format(BR))
                    .append("\n");
        }

        String chave = empresa.getChavePix() == null || empresa.getChavePix().isBlank()
                ? "(a empresa ainda não tem chave PIX cadastrada)" : empresa.getChavePix();
        String primeiroNome = cliente.getResponsavel() == null
                || cliente.getResponsavel().isBlank()
                ? cliente.getRazaoSocial() : cliente.getResponsavel().split("\\s+")[0];

        Map<String, String> atalhos = new LinkedHashMap<>();
        atalhos.put("saldo", "Oi " + primeiroNome + "! Hoje consta em aberto:\n"
                + (lista.length() == 0 ? "nada em aberto por aqui.\n" : lista)
                + "\nTotal: " + MontadorDeCobranca.dinheiro(total) + ".");
        atalhos.put("pix", "A chave PIX é " + chave + "."
                + (emAberto.isEmpty() ? ""
                : "\nSe puder, coloque na descrição o código "
                        + emAberto.get(0).getIdentificadorPix()
                        + ": assim a baixa cai sozinha."));
        atalhos.put("atualizado", "Com os encargos até hoje, o valor fica "
                + MontadorDeCobranca.dinheiro(atualizado) + ".");
        atalhos.put("prazo", "Consegue me dizer um dia para eu anotar aqui? Assim eu seguro a"
                + " cobrança até lá.");
        atalhos.put("comprovante", "Se já tiver pago, me manda o comprovante por aqui que eu"
                + " confiro e dou baixa na hora.");
        atalhos.put("acordo", "Dá para dividir esse valor em parcelas. Me diz em quantas vezes"
                + " ficaria bom que eu monto e te mando.");
        return atalhos;
    }

    // ----------------------------------------------------------------- acoes

    /** Assume o caso: a partir daqui ele aparece como seu na fila. */
    @Transactional
    public void assumir(UUID unidadeId) {
        UUID empresaId = contexto.exigirEmpresaId();
        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
        caso.ajustar(caso.getSituacao(), contexto.autor(), caso.getProximaAcao(),
                caso.getProximaData(), caso.getObservacao(), contexto.autor());
        casos.save(caso);
    }

    /**
     * Fecha o atendimento: guarda o que era, o que ficou combinado e quando
     * voltar a falar.
     */
    @Transactional
    public void finalizar(UUID unidadeId, String situacao, String combinado,
                          LocalDate proximaData, String proximaAcao) {
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
        caso.ajustar(situacao, contexto.autor(), proximaAcao, proximaData,
                caso.getObservacao(), contexto.autor());
        casos.save(caso);

        interacoes.save(new Interacao(empresaId, cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), unidadeId, "OBSERVACAO",
                combinado == null || combinado.isBlank()
                        ? "Atendimento fechado sem observação." : combinado,
                null, proximaData, "WHATSAPP", contexto.autor()));
    }

    /** Registra que o cliente prometeu pagar em tal dia. */
    @Transactional
    public void anotarPromessa(UUID unidadeId, LocalDate quando, BigDecimal valor) {
        if (quando == null) {
            throw new IllegalArgumentException("Diga o dia que o cliente prometeu.");
        }
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        interacoes.save(new Interacao(empresaId, cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), unidadeId, "PROMESSA",
                "Prometeu pagar em " + quando.format(BR), valor, quando, "WHATSAPP",
                contexto.autor()));

        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
        caso.ajustar("PROMESSA", contexto.autor(), "conferir o pagamento prometido", quando,
                caso.getObservacao(), contexto.autor());
        casos.save(caso);
    }

    /** A história recente deste cliente, para a coluna da direita. */
    public List<Interacao> historia(UUID unidadeId) {
        return interacoes.findByUnidadeIdOrderByOcorridoEmDesc(unidadeId).stream()
                .limit(6).toList();
    }
}
