package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.ForaDoLote;
import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.dominio.PassoDaRegua;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.EmpresaRepositorio;
import br.com.itia.financeiro.repositorio.ForaDoLoteRepositorio;
import br.com.itia.financeiro.repositorio.LoteDeMensagemRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.ModeloDeMensagemRepositorio;
import br.com.itia.financeiro.repositorio.PassoDaReguaRepositorio;
import br.com.itia.financeiro.repositorio.RegraDaEmpresaRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A régua de cobrança: falar na hora certa, sem ninguém marcar agenda.
 *
 * O que este serviço protege:
 *   1. O gatilho é sempre relativo ao vencimento do documento. A régua vale
 *      para qualquer cliente, e nada precisa ser agendado na mão.
 *   2. Cada rodada gera um lote do dia, com quem entrou e quem ficou de fora.
 *      Passo que exige confirmação deixa o lote na prévia.
 *   3. O mesmo documento não recebe duas mensagens no mesmo dia, nem quando
 *      dois passos caem juntos.
 *   4. Rodar duas vezes no mesmo dia não duplica nada: o segundo lote nasce
 *      vazio, porque a trava de repetição é por documento e por dia.
 */
@Service
public class ReguaDeCadencia {

    private static final Logger LOG = LoggerFactory.getLogger(ReguaDeCadencia.class);
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PassoDaReguaRepositorio passos;
    private final ModeloDeMensagemRepositorio modelos;
    private final LoteDeMensagemRepositorio lotes;
    private final MensagemRepositorio mensagens;
    private final ForaDoLoteRepositorio foras;
    private final TituloRepositorio titulos;
    private final RegraDaEmpresaRepositorio regras;
    private final EmpresaRepositorio empresas;
    private final CasoDeCobrancaRepositorio casos;
    private final MontadorDeCobranca montador;
    private final ContextoEmpresa contexto;

    public ReguaDeCadencia(PassoDaReguaRepositorio passos, ModeloDeMensagemRepositorio modelos,
                           LoteDeMensagemRepositorio lotes, MensagemRepositorio mensagens,
                           ForaDoLoteRepositorio foras, TituloRepositorio titulos,
                           RegraDaEmpresaRepositorio regras, EmpresaRepositorio empresas,
                           CasoDeCobrancaRepositorio casos, MontadorDeCobranca montador,
                           ContextoEmpresa contexto) {
        this.passos = passos;
        this.modelos = modelos;
        this.lotes = lotes;
        this.mensagens = mensagens;
        this.foras = foras;
        this.titulos = titulos;
        this.regras = regras;
        this.empresas = empresas;
        this.casos = casos;
        this.montador = montador;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ passos

    public List<PassoDaRegua> passos() {
        return passos.findByEmpresaIdOrderByOrdem(contexto.exigirEmpresaId());
    }

    public PassoDaRegua passo(UUID id) {
        return passos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Passo não encontrado."));
    }

    @Transactional
    public PassoDaRegua salvar(UUID id, String nome, String gatilho, int dias, UUID modeloId,
                               String canal, int ordem, boolean exigeConfirmacao, boolean ativo) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        UUID empresaId = contexto.exigirEmpresaId();
        modelos.findByIdAndEmpresaId(modeloId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Texto não encontrado."));
        PassoDaRegua passo;
        if (id == null) {
            passo = new PassoDaRegua(empresaId, nome, gatilho, dias, modeloId, canal, ordem,
                    exigeConfirmacao, contexto.autor());
        } else {
            passo = passo(id);
            passo.ajustar(nome, gatilho, dias, modeloId, canal, ordem, exigeConfirmacao, ativo);
        }
        return passos.save(passo);
    }

    @Transactional
    public void desativar(UUID id) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        PassoDaRegua passo = passo(id);
        passo.desativar();
        passos.save(passo);
    }

    /** Uma régua de três passos, para a empresa não começar do zero. */
    @Transactional
    public void prepararEmpresa() {
        UUID empresaId = contexto.exigirEmpresaId();
        if (!passos.findByEmpresaIdOrderByOrdem(empresaId).isEmpty()) {
            return;
        }
        List<ModeloDeMensagem> prontos = modelos.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        if (prontos.isEmpty()) {
            return;
        }
        String quem = contexto.autor();
        escolher(prontos, "ANTES_VENCER").ifPresent(modelo -> passos.save(new PassoDaRegua(
                empresaId, "Aviso três dias antes", "ANTES_DE_VENCER", 3, modelo.getId(),
                "WHATSAPP", 1, true, quem)));
        escolher(prontos, "NO_DIA").ifPresent(modelo -> passos.save(new PassoDaRegua(
                empresaId, "Lembrete no dia", "NO_VENCIMENTO", 0, modelo.getId(),
                "WHATSAPP", 2, true, quem)));
        escolher(prontos, "APOS_VENCER").ifPresent(modelo -> passos.save(new PassoDaRegua(
                empresaId, "Cobrança três dias depois", "DEPOIS_DE_VENCER", 3, modelo.getId(),
                "WHATSAPP", 3, true, quem)));
    }

    private java.util.Optional<ModeloDeMensagem> escolher(List<ModeloDeMensagem> prontos,
                                                          String momento) {
        return prontos.stream().filter(m -> momento.equals(m.getMomento())).findFirst();
    }

    // ------------------------------------------------------------------ rodada

    /** Roda a régua desta empresa, para hoje. */
    @Transactional
    public LoteDeMensagem rodarHoje() {
        return rodar(contexto.exigirEmpresa(), LocalDate.now(), contexto.autor());
    }

    /**
     * Roda a régua de uma empresa num dia.
     *
     * Recebe a empresa de fora de propósito: quem chama de madrugada é o
     * relógio, e aí não existe ninguém logado.
     */
    @Transactional
    public LoteDeMensagem rodar(Empresa empresa, LocalDate hoje, String autor) {
        UUID empresaId = empresa.getId();
        List<PassoDaRegua> ativos = passos.findByEmpresaIdAndAtivoTrueOrderByOrdem(empresaId);
        if (ativos.isEmpty()) {
            return null;
        }
        RegraDaEmpresa regra = regras.findById(empresaId)
                .orElseGet(() -> new RegraDaEmpresa(empresaId));
        List<Titulo> emAberto = titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL));

        boolean algumExigeConfirmacao = ativos.stream()
                .anyMatch(PassoDaRegua::isExigeConfirmacao);

        LoteDeMensagem lote = new LoteDeMensagem(empresaId, "Régua · " + hoje.format(BR), null,
                "WHATSAPP", "passos da régua que caem hoje", autor);
        lotes.save(lote);

        int dentro = 0;
        int fora = 0;
        BigDecimal total = BigDecimal.ZERO;
        List<UUID> jaEntraram = new ArrayList<>();

        for (PassoDaRegua passo : ativos) {
            ModeloDeMensagem modelo = modelos.findByIdAndEmpresaId(passo.getModeloId(), empresaId)
                    .orElse(null);
            if (modelo == null) {
                continue;
            }
            for (Titulo titulo : emAberto) {
                if (!passo.valeHoje(titulo.getVencimento(), hoje)
                        || jaEntraram.contains(titulo.getId())) {
                    continue;
                }
                ClienteEspelho cliente = titulo.getCliente();

                // Quem ja combinou pagamento ou contestou a conta nao leva
                // mensagem automatica. Cobrar de novo e o jeito mais rapido
                // de perder um cliente que estava resolvendo.
                CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId,
                        cliente.getId()).orElse(null);
                if (caso != null && !caso.aceitaCobrancaAutomatica()) {
                    foras.save(new ForaDoLote(empresaId, lote.getId(), cliente.getId(),
                            titulo.getId(), cliente.getRazaoSocial(),
                            "caso " + caso.getSituacaoLegivel() + ", fora da cobrança automática"));
                    fora++;
                    jaEntraram.add(titulo.getId());
                    continue;
                }

                MontadorDeCobranca.Pronta pronta = montador.montar(empresa, titulo, modelo,
                        passo.getCanal(), regra, "régua: " + passo.getNome(), hoje);
                if (!pronta.entrou()) {
                    foras.save(new ForaDoLote(empresaId, lote.getId(), cliente.getId(),
                            titulo.getId(), cliente.getRazaoSocial(), pronta.motivoDeFora()));
                    fora++;
                    continue;
                }
                pronta.mensagem().ligarAoLote(lote.getId(), modelo.getId());
                mensagens.save(pronta.mensagem());
                jaEntraram.add(titulo.getId());
                dentro++;
                total = total.add(titulo.getSaldo());
            }
            passo.anotarRodada(hoje);
            passos.save(passo);
        }

        lote.contar(dentro, fora, total);
        if (!algumExigeConfirmacao && dentro > 0) {
            lote.confirmar(autor, null);
        }
        return lotes.save(lote);
    }

    /**
     * O relógio que roda a régua todo dia de manhã, empresa por empresa.
     *
     * Falha de uma empresa não derruba as outras: o erro fica no registro e a
     * volta continua.
     */
    @Scheduled(cron = "0 5 8 * * *")
    public void rodarTodoDia() {
        LocalDate hoje = LocalDate.now();
        for (Empresa empresa : empresas.findByAtivaTrueOrderByNome()) {
            try {
                rodar(empresa, hoje, "régua automática");
            } catch (RuntimeException erro) {
                LOG.warn("régua da empresa {} falhou: {}", empresa.getApelido(),
                        erro.getMessage());
            }
        }
    }
}
