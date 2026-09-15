package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.config.Avisos;
import br.com.itia.financeiro.dominio.AbrangenciaDoPacote;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.ContratacaoDePacote;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.ItemDeServico;
import br.com.itia.financeiro.dominio.ItemDoPacote;
import br.com.itia.financeiro.dominio.Pacote;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.PeriodoDoLimite;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.SituacaoDaContratacao;
import br.com.itia.financeiro.dominio.TratamentoDoExcedente;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.ContratacaoDePacoteRepositorio;
import br.com.itia.financeiro.repositorio.ItemDeServicoRepositorio;
import br.com.itia.financeiro.repositorio.ItemDoPacoteRepositorio;
import br.com.itia.financeiro.repositorio.PacoteRepositorio;
import br.com.itia.financeiro.repositorio.PagadorRepositorio;
import br.com.itia.financeiro.repositorio.ServicoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Os pacotes: a oferta que reúne serviços já cadastrados.
 *
 * As regras que este serviço protege:
 *   1. O pacote aponta para o catálogo, nunca cadastra serviço de novo.
 *   2. Quantidade vazia com a marcação de ilimitado quer dizer sem limite,
 *      e não zero.
 *   3. O valor acordado com o cliente fica na contratação, e mexer no preço do
 *      pacote não muda o que já foi combinado.
 *   4. Pacote que já foi contratado é inativado, nunca apagado.
 */
@Service
public class PacoteServico {

    private final PacoteRepositorio pacotes;
    private final ItemDoPacoteRepositorio composicoes;
    private final ContratacaoDePacoteRepositorio contratacoes;
    private final ServicoRepositorio servicos;
    private final ItemDeServicoRepositorio itensDeServico;
    private final PagadorRepositorio pagadores;
    private final ClienteRepositorio unidades;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;

    public PacoteServico(PacoteRepositorio pacotes, ItemDoPacoteRepositorio composicoes,
                         ContratacaoDePacoteRepositorio contratacoes, ServicoRepositorio servicos,
                         ItemDeServicoRepositorio itensDeServico, PagadorRepositorio pagadores,
                         ClienteRepositorio unidades, ContextoEmpresa contexto, Avisos avisos) {
        this.pacotes = pacotes;
        this.composicoes = composicoes;
        this.contratacoes = contratacoes;
        this.servicos = servicos;
        this.itensDeServico = itensDeServico;
        this.pagadores = pagadores;
        this.unidades = unidades;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------------ lista

    public List<Pacote> todos() {
        return pacotes.findByEmpresaIdOrderByCodigo(contexto.exigirEmpresaId());
    }

    public List<Pacote> ativos() {
        return pacotes.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    /** A lista já filtrada pela busca e pelos filtros da tela. */
    public List<Pacote> lista(String busca, String situacao) {
        String procurado = busca == null ? "" : busca.trim().toLowerCase();
        return todos().stream()
                .filter(p -> procurado.isEmpty()
                        || p.getNome().toLowerCase().contains(procurado)
                        || p.getCodigo().toLowerCase().contains(procurado))
                .filter(p -> situacao == null || situacao.isBlank()
                        || ("ativo".equals(situacao) && p.isAtivo())
                        || ("inativo".equals(situacao) && !p.isAtivo()))
                .toList();
    }

    public Pacote pacote(UUID id) {
        return pacotes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pacote não encontrado nesta empresa."));
    }

    public long quantasContratacoes(UUID pacoteId) {
        return contratacoes.countByPacoteId(pacoteId);
    }

    // --------------------------------------------------------------- cadastro

    @Transactional
    public Pacote cadastrar(String nome, String descricao, BigDecimal valor,
                            Periodicidade periodicidade, String periodicidadeOutra) {
        Empresa empresa = contexto.exigirEmpresa();
        Pacote novo = new Pacote(empresa, proximoCodigo(), nome, contexto.autor());
        novo.ajustar(nome, descricao, valor,
                periodicidade == null ? Periodicidade.MENSAL : periodicidade,
                periodicidadeOutra, true);
        pacotes.save(novo);
        avisos.avisar(empresa.getId(), "PACOTE_CADASTRADO");
        return novo;
    }

    @Transactional
    public void salvarDados(UUID id, String nome, String descricao, BigDecimal valor,
                            Periodicidade periodicidade, String periodicidadeOutra, boolean ativo) {
        pacote(id).ajustar(nome, descricao, valor, periodicidade, periodicidadeOutra, ativo);
    }

    @Transactional
    public void inativar(UUID id) {
        pacote(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        pacote(id).reativar();
    }

    // -------------------------------------------------------------- composicao

    @Transactional
    public void adicionarLinha(UUID pacoteId, UUID servicoId, AbrangenciaDoPacote abrangencia,
                               UUID itemId, boolean ilimitado, BigDecimal quantidadeIncluida,
                               PeriodoDoLimite periodoLimite, TratamentoDoExcedente excedente,
                               String observacao) {
        Pacote pacote = pacote(pacoteId);
        Servico servico = servico(servicoId);
        int ordem = (int) composicoes.countByPacoteId(pacoteId) + 1;

        ItemDoPacote linha = new ItemDoPacote(pacote, servico, ordem);
        linha.ajustar(servico, abrangencia, item(servicoId, itemId), ilimitado, quantidadeIncluida,
                periodoLimite, excedente, observacao, ordem);
        // Gravar aqui e no pacote faria a mesma linha ser salva duas vezes.
        pacote.receber(linha);
    }

    @Transactional
    public void salvarLinha(UUID pacoteId, UUID linhaId, UUID servicoId,
                            AbrangenciaDoPacote abrangencia, UUID itemId, boolean ilimitado,
                            BigDecimal quantidadeIncluida, PeriodoDoLimite periodoLimite,
                            TratamentoDoExcedente excedente, String observacao, int ordem) {
        pacote(pacoteId);
        ItemDoPacote linha = composicoes.findByIdAndPacoteId(linhaId, pacoteId)
                .orElseThrow(() -> new IllegalArgumentException("Linha não encontrada no pacote."));
        linha.ajustar(servico(servicoId), abrangencia, item(servicoId, itemId), ilimitado,
                quantidadeIncluida, periodoLimite, excedente, observacao, ordem);
    }

    /**
     * Tira uma linha da composição.
     *
     * Aqui a linha some mesmo. Ela não é histórico de nada: é só a descrição do
     * que o pacote inclui hoje. O que foi cobrado continua nos títulos.
     */
    @Transactional
    public void removerLinha(UUID pacoteId, UUID linhaId) {
        pacote(pacoteId);
        composicoes.findByIdAndPacoteId(linhaId, pacoteId).ifPresent(composicoes::delete);
    }

    // ------------------------------------------------------------ contratacoes

    public List<ContratacaoDePacote> contratacoesDo(UUID pacoteId) {
        pacote(pacoteId);
        return contratacoes.findByPacoteIdOrderByCriadoEmDesc(pacoteId);
    }

    public List<ContratacaoDePacote> contratacoesDaEmpresa() {
        return contratacoes.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId());
    }

    public List<ContratacaoDePacote> contratacoesDaPessoa(UUID pagadorId) {
        return contratacoes.findByPagadorIdOrderByCriadoEmDesc(pagadorId);
    }

    @Transactional
    public ContratacaoDePacote contratar(UUID pacoteId, UUID pagadorId, UUID unidadeId,
                                         BigDecimal valorAcordado, LocalDate inicio,
                                         LocalDate fim, String observacao) {
        Pacote pacote = pacote(pacoteId);
        if (!pacote.isAtivo()) {
            throw new IllegalStateException(
                    "Este pacote está inativo. Reative antes de contratar por ele.");
        }
        Pagador pagador = pagador(pagadorId);
        ContratacaoDePacote nova = new ContratacaoDePacote(
                contexto.exigirEmpresa(), pacote, pagador, contexto.autor());
        nova.ajustar(pacote, pagador, unidade(unidadeId), valorAcordado,
                inicio == null ? LocalDate.now() : inicio, fim,
                SituacaoDaContratacao.ATIVA, observacao);
        contratacoes.save(nova);
        avisos.avisar(pacote.getEmpresa().getId(), "PACOTE_CONTRATADO");
        return nova;
    }

    @Transactional
    public void salvarContratacao(UUID contratacaoId, UUID pacoteId, UUID pagadorId,
                                  UUID unidadeId, BigDecimal valorAcordado, LocalDate inicio,
                                  LocalDate fim, SituacaoDaContratacao situacao,
                                  String observacao) {
        ContratacaoDePacote contratacao = contratacao(contratacaoId);
        contratacao.ajustar(pacote(pacoteId), pagador(pagadorId), unidade(unidadeId),
                valorAcordado, inicio, fim, situacao, observacao);
    }

    @Transactional
    public void encerrarContratacao(UUID contratacaoId, LocalDate quando) {
        contratacao(contratacaoId).encerrar(quando == null ? LocalDate.now() : quando);
    }

    public ContratacaoDePacote contratacao(UUID id) {
        return contratacoes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contratação não encontrada nesta empresa."));
    }

    // ------------------------------------------------------------------ apoio

    /** Os serviços que podem entrar num pacote: só os ativos. */
    public List<Servico> servicosDisponiveis() {
        return servicos.findByEmpresaIdOrderByCodigo(contexto.exigirEmpresaId()).stream()
                .filter(Servico::isAtivo)
                .toList();
    }

    public List<Pagador> clientes() {
        return pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<ClienteEspelho> unidadesDaEmpresa() {
        return unidades.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(contexto.exigirEmpresaId());
    }

    private Servico servico(UUID id) {
        return servicos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));
    }

    private ItemDeServico item(UUID servicoId, UUID itemId) {
        if (itemId == null) {
            return null;
        }
        return itensDeServico.findByIdAndServicoId(itemId, servicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item não encontrado dentro do serviço escolhido."));
    }

    private Pagador pagador(UUID id) {
        return pagadores.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    private ClienteEspelho unidade(UUID id) {
        if (id == null) {
            return null;
        }
        return unidades.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada."));
    }

    /** Código sequencial por empresa: P0001, P0002. */
    private String proximoCodigo() {
        String ultimo = pacotes.ultimoCodigo(contexto.exigirEmpresaId());
        int numero = 0;
        if (ultimo != null && ultimo.length() > 1) {
            try {
                numero = Integer.parseInt(ultimo.substring(1));
            } catch (NumberFormatException codigoAntigoForaDoPadrao) {
                numero = 0;
            }
        }
        return "P" + String.format("%04d", numero + 1);
    }
}
