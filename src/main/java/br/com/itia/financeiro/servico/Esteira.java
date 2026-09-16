package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A esteira de inadimplência: por onde cada devedor está passando.
 *
 * O que este serviço protege:
 *   1. A faixa de atraso é calculada, nunca arrastada na mão. Ninguém "esquece"
 *      um caso numa coluna antiga.
 *   2. Cada cliente aparece uma vez só, com o total vencido somado. A conversa
 *      de cobrança é com a pessoa, não com o boleto.
 *   3. Caso em acordo, contestado ou no jurídico fica marcado como fora da
 *      cobrança automática, e a régua respeita isso.
 *   4. O que não é do sistema (o combinado, o responsável, a próxima ação)
 *      fica escrito, com quem mexeu e quando.
 */
@Service
public class Esteira {

    /** As faixas por onde o atraso caminha. */
    public static final List<Faixa> FAIXAS = List.of(
            new Faixa("a_vencer", "A vencer", -1, -1),
            new Faixa("1_7", "1 a 7 dias", 1, 7),
            new Faixa("8_15", "8 a 15 dias", 8, 15),
            new Faixa("16_30", "16 a 30 dias", 16, 30),
            new Faixa("31_60", "31 a 60 dias", 31, 60),
            new Faixa("61_mais", "mais de 60 dias", 61, 100000));

    public record Faixa(String chave, String nome, int de, int ate) {
        public boolean pega(long atraso) {
            return de < 0 ? atraso <= 0 : atraso >= de && atraso <= ate;
        }
    }

    /**
     * Um cliente na esteira.
     *
     * @param maiorAtraso  o pior atraso que ele tem hoje
     * @param vencido      quanto está vencido
     * @param emAberto     tudo que ele deve, vencido ou não
     * @param documentos   quantos documentos em aberto
     * @param ultimaFala   quando falamos com ele pela última vez
     */
    public record NaEsteira(UUID unidadeId, String quem, long maiorAtraso, BigDecimal vencido,
                            BigDecimal emAberto, int documentos, OffsetDateTime ultimaFala,
                            CasoDeCobranca caso) {

        public boolean semFalarHaMuito(LocalDate hoje) {
            return ultimaFala == null
                    || ultimaFala.toLocalDate().isBefore(hoje.minusDays(7));
        }

        public String situacao() {
            return caso == null ? "em cobrança" : caso.getSituacaoLegivel();
        }
    }

    private final TituloRepositorio titulos;
    private final ClienteRepositorio clientes;
    private final MensagemRepositorio mensagens;
    private final CasoDeCobrancaRepositorio casos;
    private final ContextoEmpresa contexto;

    public Esteira(TituloRepositorio titulos, ClienteRepositorio clientes,
                   MensagemRepositorio mensagens, CasoDeCobrancaRepositorio casos,
                   ContextoEmpresa contexto) {
        this.titulos = titulos;
        this.clientes = clientes;
        this.mensagens = mensagens;
        this.casos = casos;
        this.contexto = contexto;
    }

    /** O quadro inteiro: cada faixa com os clientes que estão nela. */
    public Map<Faixa, List<NaEsteira>> quadro(LocalDate hoje) {
        UUID empresaId = contexto.exigirEmpresaId();
        Map<UUID, CasoDeCobranca> porUnidade = new LinkedHashMap<>();
        for (CasoDeCobranca caso : casos.findByEmpresaId(empresaId)) {
            porUnidade.put(caso.getUnidadeId(), caso);
        }
        Map<UUID, OffsetDateTime> ultimaFala = new LinkedHashMap<>();
        for (Mensagem mensagem : mensagens.findByEmpresaIdOrderByCriadoEmDesc(empresaId)) {
            if (mensagem.getUnidadeId() != null && !mensagem.deEntrada() && mensagem.saiu()) {
                ultimaFala.putIfAbsent(mensagem.getUnidadeId(), mensagem.getCriadoEm());
            }
        }

        Map<UUID, List<Titulo>> porCliente = new LinkedHashMap<>();
        for (Titulo titulo : titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL))) {
            porCliente.computeIfAbsent(titulo.getCliente().getId(), id -> new ArrayList<>())
                    .add(titulo);
        }

        Map<Faixa, List<NaEsteira>> quadro = new LinkedHashMap<>();
        for (Faixa faixa : FAIXAS) {
            quadro.put(faixa, new ArrayList<>());
        }

        for (Map.Entry<UUID, List<Titulo>> linha : porCliente.entrySet()) {
            List<Titulo> documentos = linha.getValue();
            ClienteEspelho cliente = documentos.get(0).getCliente();
            long maior = 0;
            BigDecimal vencido = BigDecimal.ZERO;
            BigDecimal emAberto = BigDecimal.ZERO;
            for (Titulo titulo : documentos) {
                long atraso = titulo.diasDeAtraso(hoje);
                emAberto = emAberto.add(titulo.getSaldo());
                if (atraso > 0) {
                    maior = Math.max(maior, atraso);
                    vencido = vencido.add(titulo.getSaldo());
                }
            }
            NaEsteira naEsteira = new NaEsteira(cliente.getId(), cliente.getRazaoSocial(), maior,
                    vencido, emAberto, documentos.size(), ultimaFala.get(cliente.getId()),
                    porUnidade.get(cliente.getId()));
            for (Faixa faixa : FAIXAS) {
                if (faixa.pega(maior)) {
                    quadro.get(faixa).add(naEsteira);
                    break;
                }
            }
        }

        for (List<NaEsteira> coluna : quadro.values()) {
            coluna.sort(Comparator.comparing(NaEsteira::vencido).reversed());
        }
        return quadro;
    }

    /**
     * Quantos clientes têm pelo menos um título vencido hoje.
     *
     * É a conta que aparece no aviso vermelho da coluna lateral: um cliente
     * conta uma vez, tendo ele um título atrasado ou dez.
     */
    public int quantosInadimplentes(LocalDate hoje) {
        UUID empresaId = contexto.exigirEmpresaId();
        java.util.Set<UUID> quem = new java.util.HashSet<>();
        for (Titulo titulo : titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL))) {
            if (titulo.diasDeAtraso(hoje) > 0) {
                quem.add(titulo.getCliente().getId());
            }
        }
        return quem.size();
    }

    public BigDecimal totalDe(List<NaEsteira> coluna) {
        return coluna.stream().map(NaEsteira::vencido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CasoDeCobranca caso(UUID unidadeId) {
        UUID empresaId = contexto.exigirEmpresaId();
        return casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
    }

    @Transactional
    public void anotar(UUID unidadeId, String situacao, String responsavel, String proximaAcao,
                       LocalDate proximaData, String observacao) {
        clientes.findByIdAndEmpresaId(unidadeId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        CasoDeCobranca caso = caso(unidadeId);
        caso.ajustar(situacao, responsavel, proximaAcao, proximaData, observacao,
                contexto.autor());
        casos.save(caso);
    }

    /** Os casos com próxima ação marcada para hoje ou já atrasada. */
    public List<CasoDeCobranca> paraHoje(LocalDate hoje) {
        return casos.findByEmpresaId(contexto.exigirEmpresaId()).stream()
                .filter(c -> c.getProximaData() != null && !c.getProximaData().isAfter(hoje))
                .sorted(Comparator.comparing(CasoDeCobranca::getProximaData))
                .toList();
    }

    public Map<UUID, String> nomes() {
        Map<UUID, String> nomes = new LinkedHashMap<>();
        for (ClienteEspelho cliente : clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(
                contexto.exigirEmpresaId())) {
            nomes.put(cliente.getId(), cliente.getRazaoSocial());
        }
        return nomes;
    }
}
