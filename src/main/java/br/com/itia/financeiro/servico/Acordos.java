package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.DocumentoDoAcordo;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.ParcelaDoAcordo;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.AcordoRepositorio;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.DocumentoDoAcordoRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.ParcelaDoAcordoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acordos e parcelamento de dívida.
 *
 * O que este serviço protege:
 *   1. O acordo não apaga o que era devido. Os documentos originais ficam
 *      guardados, marcados como em acordo, e voltam se o acordo quebrar.
 *   2. A soma das parcelas fecha com o valor combinado, até o último centavo.
 *   3. Desconto sem motivo escrito não passa, e quem autorizou fica no papel.
 *   4. Documento que já está num acordo ativo não entra em outro.
 *   5. Fechar o acordo marca o caso do cliente como em acordo, e a régua para
 *      de cobrar quem já combinou pagamento.
 */
@Service
public class Acordos {

    private final AcordoRepositorio acordos;
    private final DocumentoDoAcordoRepositorio documentos;
    private final ParcelaDoAcordoRepositorio parcelas;
    private final TituloRepositorio titulos;
    private final ClienteRepositorio clientes;
    private final CasoDeCobrancaRepositorio casos;
    private final InteracaoRepositorio interacoes;
    private final Regras regras;
    private final Fluxos fluxos;
    private final ContextoEmpresa contexto;

    public Acordos(AcordoRepositorio acordos, DocumentoDoAcordoRepositorio documentos,
                   ParcelaDoAcordoRepositorio parcelas, TituloRepositorio titulos,
                   ClienteRepositorio clientes, CasoDeCobrancaRepositorio casos,
                   InteracaoRepositorio interacoes, Regras regras, Fluxos fluxos,
                   ContextoEmpresa contexto) {
        this.fluxos = fluxos;
        this.acordos = acordos;
        this.documentos = documentos;
        this.parcelas = parcelas;
        this.titulos = titulos;
        this.clientes = clientes;
        this.casos = casos;
        this.interacoes = interacoes;
        this.regras = regras;
        this.contexto = contexto;
    }

    public List<Acordo> todos() {
        return acordos.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId());
    }

    public Acordo acordo(UUID id) {
        return acordos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Acordo não encontrado."));
    }

    public List<Titulo> documentosDo(UUID acordoId) {
        List<Titulo> lista = new ArrayList<>();
        for (DocumentoDoAcordo ligacao : documentos.findByAcordoId(acordoId)) {
            titulos.findByIdAndEmpresaId(ligacao.getTituloId(), contexto.exigirEmpresaId())
                    .ifPresent(lista::add);
        }
        return lista;
    }

    public List<Titulo> parcelasDo(UUID acordoId) {
        List<Titulo> lista = new ArrayList<>();
        for (ParcelaDoAcordo ligacao : parcelas.findByAcordoIdOrderByOrdem(acordoId)) {
            titulos.findByIdAndEmpresaId(ligacao.getTituloId(), contexto.exigirEmpresaId())
                    .ifPresent(lista::add);
        }
        return lista;
    }

    /** O que um cliente tem em aberto e pode entrar num acordo. */
    public List<Titulo> podeEntrar(UUID unidadeId) {
        return titulos.findByClienteIdOrderByVencimentoDesc(unidadeId).stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .toList();
    }

    public Map<UUID, String> clientesComDivida() {
        Map<UUID, String> nomes = new LinkedHashMap<>();
        for (Titulo titulo : titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                contexto.exigirEmpresaId(),
                List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL))) {
            nomes.putIfAbsent(titulo.getCliente().getId(),
                    titulo.getCliente().getRazaoSocial());
        }
        return nomes;
    }

    /** O acréscimo de juros e multa que a regra da empresa manda cobrar hoje. */
    public BigDecimal acrescimoDe(List<Titulo> escolhidos, LocalDate hoje) {
        RegraDaEmpresa regra = regras.daEmpresa();
        BigDecimal soma = BigDecimal.ZERO;
        for (Titulo titulo : escolhidos) {
            soma = soma.add(regra.acrescimoDe(titulo.getSaldo(), titulo.getVencimento(), hoje));
        }
        return soma;
    }

    // ------------------------------------------------------------------ fechar

    /**
     * Fecha o acordo: tira os documentos da cobrança e cria as parcelas.
     *
     * As parcelas são títulos de verdade, e por isso caem no contas a receber,
     * na conciliação e na cobrança como qualquer outro documento.
     */
    @Transactional
    public Acordo fechar(UUID unidadeId, List<UUID> tituloIds, BigDecimal acrescimo,
                         BigDecimal desconto, BigDecimal entrada, int quantasParcelas,
                         LocalDate primeiroVencimento, String motivoDesconto,
                         String observacao) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        if (tituloIds == null || tituloIds.isEmpty()) {
            throw new IllegalArgumentException("Escolha os documentos que entram no acordo.");
        }

        List<Titulo> escolhidos = new ArrayList<>();
        BigDecimal original = BigDecimal.ZERO;
        for (UUID tituloId : tituloIds) {
            Titulo titulo = titulos.findByIdAndEmpresaId(tituloId, empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
            if (!titulo.getCliente().getId().equals(unidadeId)) {
                throw new IllegalArgumentException(
                        "Um dos documentos é de outro cliente. O acordo é de um cliente só.");
            }
            if (titulo.getSituacao() == SituacaoTitulo.EM_ACORDO) {
                throw new IllegalStateException("O documento " + titulo.getIdentificadorPix()
                        + " já está dentro de outro acordo.");
            }
            if (titulo.getSaldo().signum() <= 0) {
                throw new IllegalStateException("O documento " + titulo.getIdentificadorPix()
                        + " não tem saldo em aberto.");
            }
            escolhidos.add(titulo);
            original = original.add(titulo.getSaldo());
        }

        Acordo acordo = new Acordo(empresaId, unidadeId, acordos.proximoNumero(empresaId),
                original, acrescimo, desconto, entrada, quantasParcelas, primeiroVencimento,
                motivoDesconto, contexto.autor(), observacao, contexto.autor());
        acordos.save(acordo);

        for (Titulo titulo : escolhidos) {
            documentos.save(new DocumentoDoAcordo(acordo.getId(), titulo.getId(),
                    titulo.getSaldo()));
            titulo.entrarEmAcordo();
            titulos.save(titulo);
        }

        List<BigDecimal> valores = acordo.valoresDasParcelas();
        for (int i = 0; i < valores.size(); i++) {
            LocalDate vencimento = primeiroVencimento.plusMonths(i);
            String descricao = "Acordo " + acordo.getNumero() + " · parcela "
                    + (i + 1) + " de " + valores.size();
            Titulo parcela = new Titulo(cliente, titulos.proximoNumero(empresaId),
                    vencimento.withDayOfMonth(1), descricao, valores.get(i), vencimento,
                    "ACORDO", contexto.autor());
            titulos.save(parcela);
            parcelas.save(new ParcelaDoAcordo(acordo.getId(), parcela.getId(), i + 1));
        }

        CasoDeCobranca caso = casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
        caso.ajustar("EM_ACORDO", caso.getResponsavel(),
                "acompanhar as parcelas do acordo " + acordo.getNumero(),
                primeiroVencimento, caso.getObservacao(), contexto.autor());
        casos.save(caso);

        interacoes.save(new Interacao(empresaId, cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), unidadeId, "ACORDO",
                "Acordo " + acordo.getNumero() + " fechado: "
                        + MontadorDeCobranca.dinheiro(acordo.getValorCombinado())
                        + " em " + quantasParcelas + " parcela(s)"
                        + (acordo.getDesconto().signum() > 0
                        ? ", com desconto de " + MontadorDeCobranca.dinheiro(acordo.getDesconto())
                        : ""),
                acordo.getValorCombinado(), primeiroVencimento, "SISTEMA", contexto.autor()));

        return acordo;
    }

    // ------------------------------------------------------------ acompanhar

    /**
     * Quebra o acordo: as parcelas ainda não pagas são canceladas e os
     * documentos originais voltam para a cobrança.
     */
    @Transactional
    public void quebrar(UUID acordoId, String motivo) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        UUID empresaId = contexto.exigirEmpresaId();
        Acordo acordo = acordo(acordoId);
        acordo.quebrar(motivo);

        for (Titulo parcela : parcelasDo(acordoId)) {
            if (parcela.getSituacao() == SituacaoTitulo.ABERTO) {
                parcela.cancelar(contexto.autor(),
                        "acordo " + acordo.getNumero() + " quebrado");
                titulos.save(parcela);
            }
        }
        for (Titulo original : documentosDo(acordoId)) {
            original.sairDoAcordo();
            titulos.save(original);
        }

        casos.findByEmpresaIdAndUnidadeId(empresaId, acordo.getUnidadeId()).ifPresent(caso -> {
            caso.ajustar("EM_COBRANCA", caso.getResponsavel(),
                    "acordo quebrado, retomar a cobrança", LocalDate.now(),
                    caso.getObservacao(), contexto.autor());
            casos.save(caso);
        });

        interacoes.save(new Interacao(empresaId, null, acordo.getUnidadeId(), "ACORDO",
                "Acordo " + acordo.getNumero() + " quebrado: " + acordo.getMotivoQuebra(),
                null, null, "SISTEMA", contexto.autor()));
        acordos.save(acordo);
        // Quem montou um fluxo para "quando um acordo for quebrado" e avisado agora.
        fluxos.aconteceu("ACORDO_QUEBRADO", empresaId, acordo.getUnidadeId());
    }

    /**
     * Confere se o acordo já foi cumprido.
     *
     * Cumprido é quando toda parcela está paga. Não é um botão: é uma conta.
     */
    @Transactional
    public boolean conferirCumprimento(UUID acordoId) {
        Acordo acordo = acordo(acordoId);
        if (!acordo.estaAtivo()) {
            return false;
        }
        List<Titulo> asParcelas = parcelasDo(acordoId);
        if (asParcelas.isEmpty()) {
            return false;
        }
        boolean todasPagas = asParcelas.stream()
                .allMatch(t -> t.getSituacao() == SituacaoTitulo.PAGO);
        if (!todasPagas) {
            return false;
        }
        acordo.marcarCumprido();
        acordos.save(acordo);
        casos.findByEmpresaIdAndUnidadeId(acordo.getEmpresaId(), acordo.getUnidadeId())
                .ifPresent(caso -> {
                    caso.ajustar("EM_COBRANCA", caso.getResponsavel(), null, null,
                            caso.getObservacao(), contexto.autor());
                    casos.save(caso);
                });
        return true;
    }

    /** Quanto já entrou de um acordo, somando as parcelas pagas. */
    public BigDecimal jaPagoDe(UUID acordoId) {
        return parcelasDo(acordoId).stream()
                .map(Titulo::getTotalPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
