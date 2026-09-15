package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.FonteDaFicha;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.LinhaDaFicha;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.AcordoRepositorio;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.ComprovanteRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.LinhaDaFichaRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A ficha do caso: os números que importam antes de falar de dinheiro.
 *
 * Cada empresa escolhe quais linhas quer ver e em que ordem, porque uma
 * contabilidade que cobra mensalidade não olha as mesmas coisas que uma
 * empresa que vende por pedido. O cálculo de cada linha é sempre do que está
 * gravado: nenhuma linha aqui é digitada por alguém.
 */
@Service
public class FichaDoCaso {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final LinhaDaFichaRepositorio linhas;
    private final TituloRepositorio titulos;
    private final ClienteRepositorio clientes;
    private final MensagemRepositorio mensagens;
    private final InteracaoRepositorio interacoes;
    private final AcordoRepositorio acordos;
    private final CasoDeCobrancaRepositorio casos;
    private final ComprovanteRepositorio comprovantes;
    private final ContextoEmpresa contexto;

    public FichaDoCaso(LinhaDaFichaRepositorio linhas, TituloRepositorio titulos,
                       ClienteRepositorio clientes, MensagemRepositorio mensagens,
                       InteracaoRepositorio interacoes, AcordoRepositorio acordos,
                       CasoDeCobrancaRepositorio casos, ComprovanteRepositorio comprovantes,
                       ContextoEmpresa contexto) {
        this.linhas = linhas;
        this.titulos = titulos;
        this.clientes = clientes;
        this.mensagens = mensagens;
        this.interacoes = interacoes;
        this.acordos = acordos;
        this.casos = casos;
        this.comprovantes = comprovantes;
        this.contexto = contexto;
    }

    /** Uma linha pronta para a tela. */
    public record Linha(String rotulo, String valor, String detalhe, boolean atencao) {
    }

    // ----------------------------------------------------------- configuracao

    public List<LinhaDaFicha> configuracao() {
        UUID empresaId = contexto.exigirEmpresaId();
        List<LinhaDaFicha> guardadas = linhas.findByEmpresaIdOrderByOrdem(empresaId);
        return guardadas.isEmpty() ? prepararEmpresa() : guardadas;
    }

    /** A ficha que a empresa recebe antes de mexer em qualquer coisa. */
    @Transactional
    public List<LinhaDaFicha> prepararEmpresa() {
        UUID empresaId = contexto.exigirEmpresaId();
        if (!linhas.findByEmpresaIdOrderByOrdem(empresaId).isEmpty()) {
            return linhas.findByEmpresaIdOrderByOrdem(empresaId);
        }
        int ordem = 1;
        for (FonteDaFicha fonte : FonteDaFicha.daFabrica()) {
            linhas.save(new LinhaDaFicha(empresaId, ordem++, fonte, fonte.getRotuloPadrao()));
        }
        return linhas.findByEmpresaIdOrderByOrdem(empresaId);
    }

    @Transactional
    public void acrescentar(FonteDaFicha fonte, String rotulo) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        UUID empresaId = contexto.exigirEmpresaId();
        int ordem = linhas.findByEmpresaIdOrderByOrdem(empresaId).size() + 1;
        linhas.save(new LinhaDaFicha(empresaId, ordem, fonte, rotulo));
    }

    @Transactional
    public void ajustar(UUID id, int ordem, String rotulo, boolean ativa) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        LinhaDaFicha linha = linhas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Linha não encontrada."));
        linha.ajustar(ordem, rotulo, ativa);
        linhas.save(linha);
    }

    @Transactional
    public void tirar(UUID id) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        LinhaDaFicha linha = linhas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Linha não encontrada."));
        linhas.delete(linha);
    }

    // ------------------------------------------------------------- montagem

    /** A ficha de um cliente, do jeito que esta empresa configurou. */
    public List<Linha> de(UUID unidadeId, LocalDate hoje) {
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        List<Titulo> todos = titulos.findByClienteIdOrderByVencimentoDesc(unidadeId);
        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElse(null);

        List<Linha> prontas = new ArrayList<>();
        for (LinhaDaFicha configurada : configuracao()) {
            if (!configurada.isAtiva()) {
                continue;
            }
            Linha linha = calcular(configurada, cliente, todos, caso, hoje);
            if (linha != null) {
                prontas.add(linha);
            }
        }
        return prontas;
    }

    private Linha calcular(LinhaDaFicha configurada, ClienteEspelho cliente, List<Titulo> todos,
                           CasoDeCobranca caso, LocalDate hoje) {
        String rotulo = configurada.getRotulo();
        List<Titulo> emAberto = todos.stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();

        return switch (configurada.daFonte()) {
            case EM_ABERTO -> {
                BigDecimal total = somar(emAberto);
                yield new Linha(rotulo, MontadorDeCobranca.dinheiro(total),
                        emAberto.size() + " documento(s)", false);
            }
            case VENCIDO -> {
                List<Titulo> vencidos = emAberto.stream()
                        .filter(t -> t.diasDeAtraso(hoje) > 0).toList();
                yield new Linha(rotulo, MontadorDeCobranca.dinheiro(somar(vencidos)),
                        vencidos.size() + " documento(s) vencido(s)", !vencidos.isEmpty());
            }
            case MAIOR_ATRASO -> {
                long maior = emAberto.stream().mapToLong(t -> t.diasDeAtraso(hoje)).max()
                        .orElse(0);
                yield new Linha(rotulo, maior == 0 ? "em dia" : maior + " dias",
                        maior == 0 ? "nada vencido" : "do documento mais antigo", maior > 30);
            }
            case DOCUMENTO_MAIS_ANTIGO -> {
                Titulo antigo = emAberto.stream()
                        .min(Comparator.comparing(Titulo::getVencimento)).orElse(null);
                yield antigo == null ? new Linha(rotulo, "nada em aberto", null, false)
                        : new Linha(rotulo, antigo.getCompetencia() == null
                        ? antigo.getVencimento().format(MES)
                        : antigo.getCompetencia().format(MES),
                        antigo.getIdentificadorPix() + " · "
                                + MontadorDeCobranca.dinheiro(antigo.getSaldo()),
                        antigo.diasDeAtraso(hoje) > 60);
            }
            case PROXIMO_VENCIMENTO -> {
                Titulo proximo = emAberto.stream()
                        .filter(t -> !t.getVencimento().isBefore(hoje))
                        .min(Comparator.comparing(Titulo::getVencimento)).orElse(null);
                yield proximo == null ? new Linha(rotulo, "nada a vencer", null, false)
                        : new Linha(rotulo, proximo.getVencimento().format(BR),
                        MontadorDeCobranca.dinheiro(proximo.getSaldo()), false);
            }
            case COMO_PAGA, ATRASO_MEDIO -> {
                List<Long> atrasos = atrasosPagos(todos);
                if (atrasos.isEmpty()) {
                    yield new Linha(rotulo, "sem histórico", "ainda não pagou nada aqui", false);
                }
                long media = Math.round(atrasos.stream().mapToLong(Long::longValue)
                        .average().orElse(0));
                yield new Linha(rotulo, media <= 0 ? "em dia" : media + " dias",
                        "média de " + atrasos.size() + " pagamento(s)", media > 15);
            }
            case JA_PAGOU -> {
                BigDecimal pago = todos.stream().map(Titulo::getTotalPago)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield new Linha(rotulo, MontadorDeCobranca.dinheiro(pago),
                        "desde o começo", false);
            }
            case COMPROVANTES -> {
                long quantos = comprovantes.findByEmpresaIdOrderByCriadoEmDesc(
                                cliente.getEmpresa().getId()).stream()
                        .filter(c -> cliente.getId().equals(c.getUnidadeId()))
                        .count();
                yield new Linha(rotulo, String.valueOf(quantos), "provas de pagamento", false);
            }
            case PROMESSAS_CUMPRIDAS -> {
                List<Interacao> promessas = interacoes
                        .findByUnidadeIdOrderByOcorridoEmDesc(cliente.getId()).stream()
                        .filter(i -> "PROMESSA".equals(i.getTipo())).toList();
                long cumpridas = promessas.stream()
                        .filter(i -> "CUMPRIDA".equals(i.getSituacao())).count();
                yield promessas.isEmpty()
                        ? new Linha(rotulo, "nenhuma", "ainda não prometeu nada", false)
                        : new Linha(rotulo, cumpridas + " de " + promessas.size(),
                        "promessas de pagamento", cumpridas * 2 < promessas.size());
            }
            case ULTIMA_COBRANCA -> {
                Mensagem ultima = mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(
                                cliente.getEmpresa().getId(), cliente.getId()).stream()
                        .filter(m -> !m.deEntrada())
                        .reduce((a, b) -> b).orElse(null);
                yield ultima == null
                        ? new Linha(rotulo, "nunca", "ninguém falou com ele ainda", true)
                        : new Linha(rotulo, ultima.getCriadoEm().toLocalDate().format(BR),
                        ChronoUnit.DAYS.between(ultima.getCriadoEm().toLocalDate(), hoje)
                                + " dias atrás",
                        ChronoUnit.DAYS.between(ultima.getCriadoEm().toLocalDate(), hoje) > 7);
            }
            case ULTIMA_FALA -> {
                Mensagem dele = mensagens.findByEmpresaIdAndUnidadeIdOrderByCriadoEm(
                                cliente.getEmpresa().getId(), cliente.getId()).stream()
                        .filter(Mensagem::deEntrada)
                        .reduce((a, b) -> b).orElse(null);
                yield dele == null ? new Linha(rotulo, "nunca escreveu", null, false)
                        : new Linha(rotulo, recortar(dele.getCorpo(), 90),
                        dele.getCriadoEm().toLocalDate().format(BR), false);
            }
            case PROMESSA -> {
                Interacao promessa = interacoes
                        .findByUnidadeIdOrderByOcorridoEmDesc(cliente.getId()).stream()
                        .filter(i -> "PROMESSA".equals(i.getTipo())
                                && "EM_ABERTO".equals(i.getSituacao()))
                        .findFirst().orElse(null);
                yield promessa == null ? new Linha(rotulo, "nenhuma em aberto", null, false)
                        : new Linha(rotulo, promessa.getDataPrometida().format(BR),
                        promessa.promessaVencida(hoje) ? "passou da data" : "prometido",
                        promessa.promessaVencida(hoje));
            }
            case ACORDO -> {
                List<Acordo> dele = acordos.findByEmpresaIdAndUnidadeIdOrderByCriadoEmDesc(
                        cliente.getEmpresa().getId(), cliente.getId());
                Acordo ativo = dele.stream().filter(Acordo::estaAtivo).findFirst().orElse(null);
                if (ativo != null) {
                    yield new Linha(rotulo, "acordo " + ativo.getNumero(),
                            ativo.getParcelas() + " parcelas de "
                                    + MontadorDeCobranca.dinheiro(
                                    ativo.valoresDasParcelas().get(0)), false);
                }
                Acordo quebrado = dele.stream()
                        .filter(a -> "QUEBRADO".equals(a.getSituacao())).findFirst().orElse(null);
                yield quebrado != null
                        ? new Linha(rotulo, "quebrou o acordo " + quebrado.getNumero(),
                        quebrado.getMotivoQuebra(), true)
                        : new Linha(rotulo, dele.isEmpty() ? "nenhum" : "cumprido",
                        dele.isEmpty() ? "nunca parcelou" : "já pagou um acordo até o fim",
                        false);
            }
            case SITUACAO_DO_CASO -> new Linha(rotulo,
                    caso == null ? "em cobrança" : caso.getSituacaoLegivel(), null,
                    caso != null && ("CONTESTADO".equals(caso.getSituacao())
                            || "JURIDICO".equals(caso.getSituacao())));
            case RESPONSAVEL -> new Linha(rotulo,
                    caso == null || caso.getResponsavel() == null ? "ninguém ainda"
                            : caso.getResponsavel(), null, false);
            case PROXIMA_ACAO -> caso == null || caso.getProximaAcao() == null
                    ? new Linha(rotulo, "nada combinado", null, false)
                    : new Linha(rotulo, caso.getProximaAcao(),
                    caso.getProximaData() == null ? null
                            : caso.getProximaData().format(BR),
                    caso.atrasado(hoje));
            case TOM_DE_COBRANCA -> new Linha(rotulo,
                    cliente.getTomDeCobranca() == null ? "padrão"
                            : cliente.getTomDeCobranca().toLowerCase(), null, false);
            case ACEITA_PARCELAMENTO -> new Linha(rotulo,
                    cliente.isAceitaParcelamento() ? "aceita" : "não aceita", null, false);
            case TEMPO_DE_CASA -> cliente.getInicioNaCasa() == null
                    ? new Linha(rotulo, "não informado", null, false)
                    : new Linha(rotulo,
                    ChronoUnit.MONTHS.between(cliente.getInicioNaCasa(), hoje) + " meses",
                    "desde " + cliente.getInicioNaCasa().format(BR), false);
            case UNIDADES -> {
                List<ClienteEspelho> irmas = irmasDe(cliente);
                if (irmas.isEmpty()) {
                    yield new Linha(rotulo, "só esta", null, false);
                }
                StringBuilder detalhe = new StringBuilder();
                for (ClienteEspelho irma : irmas) {
                    detalhe.append(irma.getRazaoSocial()).append(": ")
                            .append(MontadorDeCobranca.dinheiro(
                                    somar(emAbertoDe(irma.getId())))).append(" · ");
                }
                yield new Linha(rotulo, (irmas.size() + 1) + " unidades",
                        detalhe.toString(), false);
            }
            case RESUMO_DA_IA -> null;
        };
    }

    private List<ClienteEspelho> irmasDe(ClienteEspelho cliente) {
        if (cliente.getPagador() == null) {
            return List.of();
        }
        return clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(
                        cliente.getEmpresa().getId()).stream()
                .filter(c -> c.getPagador() != null
                        && c.getPagador().getId().equals(cliente.getPagador().getId()))
                .filter(c -> !c.getId().equals(cliente.getId()))
                .toList();
    }

    private List<Titulo> emAbertoDe(UUID unidadeId) {
        return titulos.findByClienteIdOrderByVencimentoDesc(unidadeId).stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();
    }

    private BigDecimal somar(List<Titulo> lista) {
        return lista.stream().map(Titulo::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Quantos dias de atraso teve cada pagamento que já entrou. */
    private List<Long> atrasosPagos(List<Titulo> todos) {
        List<Long> atrasos = new ArrayList<>();
        for (Titulo titulo : todos) {
            for (Pagamento pagamento : titulo.getPagamentos()) {
                if (pagamento.getPagoEm() != null) {
                    atrasos.add(Math.max(0, ChronoUnit.DAYS.between(titulo.getVencimento(),
                            pagamento.getPagoEm())));
                }
            }
        }
        return atrasos;
    }

    private String recortar(String texto, int quanto) {
        String limpo = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        return limpo.length() <= quanto ? limpo : limpo.substring(0, quanto) + "...";
    }

    /** Média mensal do que este cliente costuma dever, para a tela de ajuste. */
    public BigDecimal mediaMensal(UUID unidadeId) {
        List<Titulo> todos = titulos.findByClienteIdOrderByVencimentoDesc(unidadeId);
        if (todos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal soma = todos.stream().map(Titulo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(Math.max(1, todos.size())), 2,
                RoundingMode.HALF_UP);
    }
}
