package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.ExecucaoDoFluxo;
import br.com.itia.financeiro.dominio.Fluxo;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.dominio.PassoDoFluxo;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.EmpresaRepositorio;
import br.com.itia.financeiro.repositorio.ExecucaoDoFluxoRepositorio;
import br.com.itia.financeiro.repositorio.FluxoRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.ModeloDeMensagemRepositorio;
import br.com.itia.financeiro.repositorio.PassoDoFluxoRepositorio;
import br.com.itia.financeiro.repositorio.RegraDaEmpresaRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O estúdio de fluxos: automação montada pelo usuário, sem programar.
 *
 * O que este serviço protege:
 *   1. Todo fluxo é a mesma frase: quando, se, faça. Quem lê a tela sabe o
 *      que vai acontecer antes de ligar.
 *   2. Toda rodada grava o que fez e em quem, inclusive quando não fez nada e
 *      por quê. Automação sem registro é caixa preta.
 *   3. Mandar mensagem por fluxo passa pelas mesmas travas da cobrança: quem
 *      pediu para não ser cobrado continua sem receber, e ninguém leva duas
 *      mensagens do mesmo documento no mesmo dia.
 *   4. Fluxo desligado não roda, e desligar não apaga o histórico.
 */
@Service
public class Fluxos {

    private static final Logger LOG = LoggerFactory.getLogger(Fluxos.class);

    private final FluxoRepositorio fluxos;
    private final PassoDoFluxoRepositorio passos;
    private final ExecucaoDoFluxoRepositorio execucoes;
    private final ModeloDeMensagemRepositorio modelos;
    private final TituloRepositorio titulos;
    private final CasoDeCobrancaRepositorio casos;
    private final MensagemRepositorio mensagens;
    private final InteracaoRepositorio interacoes;
    private final RegraDaEmpresaRepositorio regras;
    private final EmpresaRepositorio empresas;
    private final MontadorDeCobranca montador;
    private final ContextoEmpresa contexto;

    public Fluxos(FluxoRepositorio fluxos, PassoDoFluxoRepositorio passos,
                  ExecucaoDoFluxoRepositorio execucoes, ModeloDeMensagemRepositorio modelos,
                  TituloRepositorio titulos, CasoDeCobrancaRepositorio casos,
                  MensagemRepositorio mensagens, InteracaoRepositorio interacoes,
                  RegraDaEmpresaRepositorio regras, EmpresaRepositorio empresas,
                  MontadorDeCobranca montador, ContextoEmpresa contexto) {
        this.fluxos = fluxos;
        this.passos = passos;
        this.execucoes = execucoes;
        this.modelos = modelos;
        this.titulos = titulos;
        this.casos = casos;
        this.mensagens = mensagens;
        this.interacoes = interacoes;
        this.regras = regras;
        this.empresas = empresas;
        this.montador = montador;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ montar

    public List<Fluxo> todos() {
        return fluxos.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public Fluxo fluxo(UUID id) {
        return fluxos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Fluxo não encontrado."));
    }

    public List<PassoDoFluxo> passosDe(UUID fluxoId) {
        return passos.findByFluxoIdOrderByOrdem(fluxoId);
    }

    public List<ExecucaoDoFluxo> ultimasExecucoes() {
        return execucoes.findTop50ByEmpresaIdOrderByOcorridoEmDesc(contexto.exigirEmpresaId());
    }

    public List<ExecucaoDoFluxo> execucoesDe(UUID fluxoId) {
        return execucoes.findTop30ByFluxoIdOrderByOcorridoEmDesc(fluxoId);
    }

    @Transactional
    public Fluxo salvar(UUID id, String nome, String descricao, String gatilho, int dias,
                        boolean ativo) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        Fluxo fluxo;
        if (id == null) {
            fluxo = new Fluxo(contexto.exigirEmpresaId(), nome, descricao, gatilho, dias,
                    contexto.autor());
        } else {
            fluxo = fluxo(id);
            fluxo.ajustar(nome, descricao, gatilho, dias, ativo);
        }
        return fluxos.save(fluxo);
    }

    @Transactional
    public void desativar(UUID id) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        Fluxo fluxo = fluxo(id);
        fluxo.desativar();
        fluxos.save(fluxo);
    }

    @Transactional
    public void acrescentarCondicao(UUID fluxoId, String campo, String operador, String valor) {
        Fluxo fluxo = fluxo(fluxoId);
        passos.save(PassoDoFluxo.condicao(fluxo.getId(), proximaOrdem(fluxoId), campo,
                operador, valor));
    }

    @Transactional
    public void acrescentarAcao(UUID fluxoId, String acao, UUID modeloId, String texto,
                                int dias) {
        Fluxo fluxo = fluxo(fluxoId);
        if (modeloId != null) {
            modelos.findByIdAndEmpresaId(modeloId, fluxo.getEmpresaId())
                    .orElseThrow(() -> new IllegalArgumentException("Texto não encontrado."));
        }
        passos.save(PassoDoFluxo.acao(fluxo.getId(), proximaOrdem(fluxoId), acao, modeloId,
                texto, dias));
    }

    @Transactional
    public void apagarPasso(UUID fluxoId, UUID passoId) {
        fluxo(fluxoId);
        PassoDoFluxo passo = passos.findByIdAndFluxoId(passoId, fluxoId)
                .orElseThrow(() -> new IllegalArgumentException("Passo não encontrado."));
        passos.delete(passo);
    }

    private int proximaOrdem(UUID fluxoId) {
        return passos.findByFluxoIdOrderByOrdem(fluxoId).size() + 1;
    }

    // ------------------------------------------------------------------ rodar

    /** Roda os fluxos de calendário desta empresa, para hoje. */
    @Transactional
    public int rodarHoje() {
        return rodarCalendario(contexto.exigirEmpresa(), LocalDate.now());
    }

    /**
     * Roda os fluxos de data de uma empresa.
     *
     * Recebe a empresa de fora porque quem chama de manhã é o relógio, e aí
     * não existe ninguém logado.
     */
    @Transactional
    public int rodarCalendario(Empresa empresa, LocalDate hoje) {
        UUID empresaId = empresa.getId();
        RegraDaEmpresa regra = regras.findById(empresaId)
                .orElseGet(() -> new RegraDaEmpresa(empresaId));
        List<Titulo> emAberto = titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL));

        int quantasAcoes = 0;
        for (Fluxo fluxo : fluxos.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId)) {
            if (!fluxo.deCalendario()) {
                continue;
            }
            List<PassoDoFluxo> receita = passos.findByFluxoIdOrderByOrdem(fluxo.getId());
            for (Titulo titulo : emAberto) {
                if (!fluxo.valeHoje(titulo.getVencimento(), hoje)) {
                    continue;
                }
                quantasAcoes += aplicar(empresa, fluxo, receita, titulo, regra, hoje);
            }
            fluxo.anotarRodada(hoje);
            fluxos.save(fluxo);
        }
        return quantasAcoes;
    }

    /**
     * Roda um fluxo num documento: confere as condições e faz o que manda.
     *
     * Devolve quantas ações aconteceram de verdade.
     */
    private int aplicar(Empresa empresa, Fluxo fluxo, List<PassoDoFluxo> receita, Titulo titulo,
                        RegraDaEmpresa regra, LocalDate hoje) {
        UUID empresaId = empresa.getId();
        ClienteEspelho cliente = titulo.getCliente();
        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, cliente.getId())
                .orElse(null);

        for (PassoDoFluxo passo : receita) {
            if (!passo.ehCondicao()) {
                continue;
            }
            if (!condicaoVale(passo, titulo, cliente, caso, hoje)) {
                execucoes.save(new ExecucaoDoFluxo(empresaId, fluxo.getId(), cliente.getId(),
                        titulo.getId(), false, "não fez: " + passo.getResumo()));
                return 0;
            }
        }

        int feitas = 0;
        for (PassoDoFluxo passo : receita) {
            if (passo.ehCondicao()) {
                continue;
            }
            String oQueFez = executar(empresa, fluxo, passo, titulo, cliente, regra, hoje);
            execucoes.save(new ExecucaoDoFluxo(empresaId, fluxo.getId(), cliente.getId(),
                    titulo.getId(), oQueFez != null,
                    oQueFez == null ? "não fez: " + passo.getResumo() : oQueFez));
            if (oQueFez != null) {
                feitas++;
            }
        }
        return feitas;
    }

    private boolean condicaoVale(PassoDoFluxo passo, Titulo titulo, ClienteEspelho cliente,
                                 CasoDeCobranca caso, LocalDate hoje) {
        return switch (passo.getCampo()) {
            case "VALOR_EM_ABERTO" -> passo.aceita(titulo.getSaldo());
            case "DIAS_ATRASO" -> passo.aceita(BigDecimal.valueOf(titulo.diasDeAtraso(hoje)));
            case "SITUACAO_DO_CASO" -> passo.aceita(caso == null ? "EM_COBRANCA"
                    : caso.getSituacao());
            default -> passo.aceita(cliente.getTomDeCobranca());
        };
    }

    /** Faz a ação e devolve o que foi feito, ou nulo quando não deu. */
    private String executar(Empresa empresa, Fluxo fluxo, PassoDoFluxo passo, Titulo titulo,
                            ClienteEspelho cliente, RegraDaEmpresa regra, LocalDate hoje) {
        UUID empresaId = empresa.getId();
        switch (passo.getAcao()) {
            case "MANDAR_MENSAGEM" -> {
                CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId,
                        cliente.getId()).orElse(null);
                if (caso != null && !caso.aceitaCobrancaAutomatica()) {
                    return null;
                }
                ModeloDeMensagem modelo = modelos.findByIdAndEmpresaId(passo.getModeloId(),
                        empresaId).orElse(null);
                if (modelo == null) {
                    return null;
                }
                MontadorDeCobranca.Pronta pronta = montador.montar(empresa, titulo, modelo,
                        modelo.getCanal(), regra, "fluxo: " + fluxo.getNome(), hoje);
                if (!pronta.entrou()) {
                    return null;
                }
                Mensagem mensagem = pronta.mensagem();
                mensagens.save(mensagem);
                return "mensagem na fila para " + mensagem.getDestino();
            }
            case "MARCAR_CASO" -> {
                CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId,
                                cliente.getId())
                        .orElseGet(() -> new CasoDeCobranca(empresaId, cliente.getId()));
                caso.ajustar(passo.getTexto(), caso.getResponsavel(), caso.getProximaAcao(),
                        caso.getProximaData(), caso.getObservacao(), "fluxo " + fluxo.getNome());
                casos.save(caso);
                return "caso marcado como " + caso.getSituacaoLegivel();
            }
            case "ANOTAR_PROXIMA_ACAO" -> {
                CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId,
                                cliente.getId())
                        .orElseGet(() -> new CasoDeCobranca(empresaId, cliente.getId()));
                caso.ajustar(caso.getSituacao(), caso.getResponsavel(), passo.getTexto(),
                        hoje.plusDays(passo.getDias()), caso.getObservacao(),
                        "fluxo " + fluxo.getNome());
                casos.save(caso);
                return "próxima ação anotada: " + passo.getTexto();
            }
            default -> {
                interacoes.save(new Interacao(empresaId, cliente.getPagador() == null ? null
                        : cliente.getPagador().getId(), cliente.getId(), "OBSERVACAO",
                        passo.getTexto(), null, null, "SISTEMA", "fluxo " + fluxo.getNome()));
                return "observação registrada";
            }
        }
    }

    /**
     * Roda os fluxos de um acontecimento, como o cliente responder.
     *
     * Aqui não existe documento: as condições que dependem de documento não
     * valem, e por isso só rodam as ações que falam do cliente.
     */
    @Transactional
    public void aconteceu(String gatilho, UUID empresaId, UUID unidadeId) {
        for (Fluxo fluxo : fluxos.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId)) {
            if (!gatilho.equals(fluxo.getGatilho())) {
                continue;
            }
            CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                    .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
            for (PassoDoFluxo passo : passos.findByFluxoIdOrderByOrdem(fluxo.getId())) {
                if (passo.ehCondicao() || "MANDAR_MENSAGEM".equals(passo.getAcao())) {
                    continue;
                }
                if ("MARCAR_CASO".equals(passo.getAcao())) {
                    caso.ajustar(passo.getTexto(), caso.getResponsavel(), caso.getProximaAcao(),
                            caso.getProximaData(), caso.getObservacao(),
                            "fluxo " + fluxo.getNome());
                } else if ("ANOTAR_PROXIMA_ACAO".equals(passo.getAcao())) {
                    caso.ajustar(caso.getSituacao(), caso.getResponsavel(), passo.getTexto(),
                            LocalDate.now().plusDays(passo.getDias()), caso.getObservacao(),
                            "fluxo " + fluxo.getNome());
                } else {
                    interacoes.save(new Interacao(empresaId, null, unidadeId, "OBSERVACAO",
                            passo.getTexto(), null, null, "SISTEMA",
                            "fluxo " + fluxo.getNome()));
                }
                casos.save(caso);
                execucoes.save(new ExecucaoDoFluxo(empresaId, fluxo.getId(), unidadeId, null,
                        true, passo.getResumo()));
            }
        }
    }

    /** O relógio que roda os fluxos de data, depois da régua. */
    @Scheduled(cron = "0 20 8 * * *")
    public void rodarTodoDia() {
        LocalDate hoje = LocalDate.now();
        for (Empresa empresa : empresas.findByAtivaTrueOrderByNome()) {
            try {
                rodarCalendario(empresa, hoje);
            } catch (RuntimeException erro) {
                LOG.warn("fluxos da empresa {} falharam: {}", empresa.getApelido(),
                        erro.getMessage());
            }
        }
    }
}
