package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Evento;
import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.EmpresaRepositorio;
import br.com.itia.financeiro.repositorio.EventoRepositorio;
import br.com.itia.financeiro.repositorio.PagamentoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import br.com.itia.financeiro.config.Avisos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * As regras do dinheiro. Tudo que mexe em titulo ou pagamento passa por aqui,
 * e tudo que passa por aqui grava um evento de auditoria.
 */
@Service
public class FinanceiroServico {

    private final EmpresaRepositorio empresas;
    private final ClienteRepositorio clientes;
    private final TituloRepositorio titulos;
    private final PagamentoRepositorio pagamentos;
    private final EventoRepositorio eventos;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;
    private final Fluxos fluxos;

    public FinanceiroServico(EmpresaRepositorio empresas,
                             ClienteRepositorio clientes,
                             TituloRepositorio titulos,
                             PagamentoRepositorio pagamentos,
                             EventoRepositorio eventos,
                             ContextoEmpresa contexto,
                             Avisos avisos,
                             Fluxos fluxos) {
        this.fluxos = fluxos;
        this.empresas = empresas;
        this.clientes = clientes;
        this.titulos = titulos;
        this.pagamentos = pagamentos;
        this.eventos = eventos;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ---------------------------------------------------------------- empresa

    @Transactional
    public Empresa cadastrarEmpresa(String apelido, String nome, String cnpj, String chavePix) {
        empresas.findByApelido(apelido).ifPresent(existente -> {
            throw new IllegalArgumentException("Ja existe uma empresa com o apelido " + apelido + ".");
        });
        return empresas.save(new Empresa(apelido, nome, cnpj, chavePix));
    }

    public List<Empresa> empresasAtivas() {
        return empresas.findByAtivaTrueOrderByNome();
    }

    public List<Empresa> todasAsEmpresas() {
        return empresas.findAll(org.springframework.data.domain.Sort.by("nome"));
    }

    public Empresa empresa(UUID empresaId) {
        return empresas.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada."));
    }

    @Transactional
    public Empresa atualizarEmpresa(UUID empresaId, String apelido, String nome,
                                    String cnpj, String chavePix) {
        Empresa empresa = empresa(empresaId);
        empresas.findByApelido(apelido).ifPresent(outra -> {
            if (!outra.getId().equals(empresaId)) {
                throw new IllegalArgumentException(
                        "O apelido " + apelido + " ja e de outra empresa.");
            }
        });
        empresa.atualizar(apelido, nome, cnpj, chavePix);
        return empresa;
    }

    /**
     * Exclui a empresa de vez.
     *
     * So acontece se ela estiver vazia. Empresa que ja tem cliente ou titulo
     * guarda historico de dinheiro, e historico de dinheiro nao se apaga:
     * nesse caso o caminho e desativar.
     */
    @Transactional
    public void excluirEmpresa(UUID empresaId) {
        contexto.exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel.DIRETOR);
        Empresa empresa = empresa(empresaId);
        long qtdClientes = clientes.countByEmpresaId(empresaId);
        long qtdTitulos = titulos.countByEmpresaId(empresaId);
        if (qtdClientes > 0 || qtdTitulos > 0) {
            throw new IllegalStateException("A empresa " + empresa.getNome() + " tem "
                    + qtdClientes + " cliente(s) e " + qtdTitulos + " titulo(s). "
                    + "Empresa com movimento nao e excluida, e desativada.");
        }
        empresas.delete(empresa);
    }

    @Transactional
    public void desativarEmpresa(UUID empresaId) {
        empresa(empresaId).desativar();
    }

    @Transactional
    public void reativarEmpresa(UUID empresaId) {
        empresa(empresaId).reativar();
    }

    /** Quantos clientes e titulos cada empresa tem, para a tela de configuracao. */
    public java.util.Map<UUID, long[]> movimentoPorEmpresa() {
        java.util.Map<UUID, long[]> mapa = new java.util.HashMap<>();
        for (Empresa e : todasAsEmpresas()) {
            mapa.put(e.getId(), new long[]{
                    clientes.countByEmpresaId(e.getId()),
                    titulos.countByEmpresaId(e.getId())});
        }
        return mapa;
    }

    // ---------------------------------------------------------------- cliente

    @Transactional
    public ClienteEspelho cadastrarCliente(String razaoSocial, String cnpjCpf,
                                           String responsavel, String telefone, String email) {
        Empresa empresa = contexto.exigirEmpresa();
        ClienteEspelho cliente = new ClienteEspelho(empresa, null, razaoSocial);
        cliente.atualizarCom(razaoSocial, cnpjCpf, responsavel, telefone, email, true);
        return clientes.save(cliente);
    }

    public List<ClienteEspelho> clientesDaEmpresa() {
        return clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(contexto.exigirEmpresaId());
    }

    public ClienteEspelho cliente(UUID clienteId) {
        return clientes.findByIdAndEmpresaId(clienteId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente nao encontrado nesta empresa."));
    }

    // ----------------------------------------------------------------- titulo

    /**
     * Lanca uma conta para o cliente pagar.
     * O numero e o identificador do PIX saem daqui, nunca digitados.
     */
    @Transactional
    public Titulo lancarTitulo(UUID clienteId, LocalDate competencia, String descricao,
                               BigDecimal valor, LocalDate vencimento) {
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = cliente(clienteId);

        Long numero = titulos.proximoNumero(empresaId);

        Titulo titulo = new Titulo(cliente, numero, competencia, descricao,
                valor, vencimento, "MANUAL", contexto.autor());
        titulos.save(titulo);

        registrar(empresaId, "titulo", titulo.getId(), "LANCADO",
                "{\"valor\": " + valor + ", \"cliente\": \"" + cliente.getRazaoSocial() + "\"}");
        return titulo;
    }

    public List<Titulo> titulosDaEmpresa() {
        return titulos.findByEmpresaIdOrderByVencimentoDesc(contexto.exigirEmpresaId());
    }

    public List<Titulo> titulosEmAberto() {
        return titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                contexto.exigirEmpresaId(),
                List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL));
    }

    public Titulo titulo(UUID tituloId) {
        return titulos.findByIdAndEmpresaId(tituloId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Titulo nao encontrado nesta empresa."));
    }

    @Transactional
    public void cancelarTitulo(UUID tituloId, String motivo) {
        contexto.exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel.GESTOR);
        Titulo titulo = titulo(tituloId);
        titulo.cancelar(contexto.autor(), motivo);
        registrar(titulo.getEmpresa().getId(), "titulo", titulo.getId(), "CANCELADO",
                "{\"motivo\": \"" + motivo + "\"}");
    }

    // -------------------------------------------------------------- pagamento

    /**
     * Da baixa. Se o identificador da transacao ja tiver entrado, recusa:
     * o mesmo PIX nunca paga duas vezes.
     */
    @Transactional
    public Pagamento receberPagamento(UUID tituloId, BigDecimal valor, LocalDate pagoEm,
                                      String forma, String transacaoId) {
        UUID empresaId = contexto.exigirEmpresaId();
        if (transacaoId != null && !transacaoId.isBlank()
                && pagamentos.existsByEmpresaIdAndTransacaoId(empresaId, transacaoId)) {
            throw new IllegalArgumentException(
                    "Este pagamento ja foi lancado antes (transacao " + transacaoId + ").");
        }

        Titulo titulo = titulo(tituloId);
        Pagamento pagamento = new Pagamento(valor, pagoEm, forma, transacaoId, contexto.autor());
        // O pagamento entra pelo titulo, e o titulo se encarrega de gravar.
        // Gravar aqui de novo faria a mesma linha ser salva duas vezes.
        titulo.receber(pagamento);

        registrar(empresaId, "titulo", titulo.getId(), "PAGAMENTO_RECEBIDO",
                "{\"valor\": " + valor + ", \"situacao\": \"" + titulo.getSituacao() + "\"}");
        // Quem montou um fluxo para "quando o pagamento entrar" e avisado agora.
        fluxos.aconteceu("PAGAMENTO_ENTROU", empresaId, titulo.getCliente().getId());
        return pagamento;
    }

    // ------------------------------------------------------------------ apoio

    private void registrar(UUID empresaId, String entidade, UUID entidadeId,
                           String acao, String detalhe) {
        eventos.save(new Evento(empresaId, entidade, entidadeId, acao, contexto.autor(), detalhe));
        // As telas abertas desta empresa ficam sabendo na hora.
        avisos.avisar(empresaId, acao);
    }

    public List<Evento> ultimosEventos() {
        return eventos.findTop50ByEmpresaIdOrderByOcorridoEmDesc(contexto.exigirEmpresaId());
    }
}
