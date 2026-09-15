package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.config.Avisos;
import br.com.itia.financeiro.dominio.Departamento;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.FormaDePreco;
import br.com.itia.financeiro.dominio.ItemDeServico;
import br.com.itia.financeiro.dominio.MudancaDePreco;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.TratamentoDePreco;
import br.com.itia.financeiro.dominio.UnidadeDeCobranca;
import br.com.itia.financeiro.repositorio.DepartamentoRepositorio;
import br.com.itia.financeiro.repositorio.ItemDeServicoRepositorio;
import br.com.itia.financeiro.repositorio.MudancaDePrecoRepositorio;
import br.com.itia.financeiro.repositorio.ServicoRepositorio;
import br.com.itia.financeiro.repositorio.UnidadeDeCobrancaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O catálogo de serviços.
 *
 * As regras que este serviço protege, e que valem para o módulo inteiro:
 *   1. Preço não definido é diferente de preço zero.
 *   2. Toda troca de preço escreve uma linha no histórico, com autor e data.
 *   3. Item incluído no preço do serviço não é somado de novo.
 *   4. Serviço e item que já foram usados são inativados, nunca apagados.
 *   5. Desconto num atendimento específico não mexe no preço do catálogo.
 */
@Service
public class CatalogoServico {

    private final ServicoRepositorio servicos;
    private final ItemDeServicoRepositorio itens;
    private final DepartamentoRepositorio departamentos;
    private final UnidadeDeCobrancaRepositorio unidades;
    private final MudancaDePrecoRepositorio historico;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;

    public CatalogoServico(ServicoRepositorio servicos, ItemDeServicoRepositorio itens,
                           DepartamentoRepositorio departamentos,
                           UnidadeDeCobrancaRepositorio unidades,
                           MudancaDePrecoRepositorio historico,
                           ContextoEmpresa contexto, Avisos avisos) {
        this.servicos = servicos;
        this.itens = itens;
        this.departamentos = departamentos;
        this.unidades = unidades;
        this.historico = historico;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------------ lista

    public List<Servico> catalogo() {
        return servicos.findByEmpresaIdOrderByCodigo(contexto.exigirEmpresaId());
    }

    /** A lista já filtrada pela busca e pelos filtros da tela. */
    public List<Servico> catalogo(String busca, UUID departamentoId, String situacao) {
        String procurado = busca == null ? "" : busca.trim().toLowerCase();
        return catalogo().stream()
                .filter(s -> procurado.isEmpty()
                        || s.getNome().toLowerCase().contains(procurado)
                        || s.getCodigo().toLowerCase().contains(procurado))
                .filter(s -> departamentoId == null
                        || (s.getDepartamento() != null
                            && s.getDepartamento().getId().equals(departamentoId)))
                .filter(s -> situacao == null || situacao.isBlank()
                        || ("ativo".equals(situacao) && s.isAtivo())
                        || ("inativo".equals(situacao) && !s.isAtivo()))
                .toList();
    }

    public Servico servico(UUID id) {
        return servicos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Serviço não encontrado nesta empresa."));
    }

    // --------------------------------------------------------------- cadastro

    @Transactional
    public Servico cadastrar(String nome, String descricao, UUID departamentoId,
                             String responsavelPadrao) {
        Empresa empresa = contexto.exigirEmpresa();
        Servico novo = new Servico(empresa, proximoCodigo(), nome, contexto.autor());
        novo.ajustarDadosGerais(nome, descricao, departamento(departamentoId),
                responsavelPadrao, true);
        servicos.save(novo);
        avisos.avisar(empresa.getId(), "SERVICO_CADASTRADO");
        return novo;
    }

    @Transactional
    public void salvarDadosGerais(UUID id, String nome, String descricao,
                                  UUID departamentoId, String responsavelPadrao, boolean ativo) {
        servico(id).ajustarDadosGerais(nome, descricao, departamento(departamentoId),
                responsavelPadrao, ativo);
    }

    /** Copia um serviço para cadastrar outro parecido, com os itens junto. */
    @Transactional
    public Servico duplicar(UUID id) {
        Servico original = servico(id);
        Servico copia = new Servico(original.getEmpresa(), proximoCodigo(),
                original.getNome() + " (cópia)", contexto.autor());
        copia.ajustarDadosGerais(copia.getNome(), original.getDescricao(),
                original.getDepartamento(), original.getResponsavelPadrao(), false);
        copia.ajustarPreco(original.getFormaPreco(), original.getValor(),
                original.getUnidade(), LocalDate.now());
        servicos.save(copia);

        for (ItemDeServico item : original.getItensAtivos()) {
            ItemDeServico novo = new ItemDeServico(copia, item.getNome(), item.getOrdem());
            novo.ajustar(item.getNome(), item.getDescricao(), item.isObrigatorio(),
                    item.getQuantidadePadrao(), item.getTratamentoPreco(), item.getValor(),
                    item.getUnidade(), item.getOrdem(), true);
            // O item entra pelo servico, que se encarrega de gravar.
            copia.receber(novo);
        }
        // A copia nasce inativa de proposito: alguem precisa conferir o preco
        // antes de ela aparecer como disponivel.
        return copia;
    }

    @Transactional
    public void inativar(UUID id) {
        servico(id).inativar();
    }

    @Transactional
    public void reativar(UUID id) {
        servico(id).reativar();
    }

    // ------------------------------------------------------------------ itens

    @Transactional
    public void adicionarItem(UUID servicoId, String nome, String descricao, boolean obrigatorio,
                              BigDecimal quantidadePadrao, TratamentoDePreco tratamento,
                              BigDecimal valor, UUID unidadeId) {
        Servico servico = servico(servicoId);
        int ordem = (int) itens.countByServicoId(servicoId) + 1;
        ItemDeServico item = new ItemDeServico(servico, nome, ordem);
        item.ajustar(nome, descricao, obrigatorio, quantidadePadrao, tratamento, valor,
                unidade(unidadeId), ordem, true);
        // Gravar aqui e no servico faria a mesma linha ser salva duas vezes.
        servico.receber(item);
        // O item precisa existir no banco antes do historico apontar para ele.
        servicos.saveAndFlush(servico);

        if (tratamento.cobraSeparado()) {
            anotar(servico, item, "Item com cobrança própria criado", null, valor, null);
        }
    }

    @Transactional
    public void salvarItem(UUID servicoId, UUID itemId, String nome, String descricao,
                           boolean obrigatorio, BigDecimal quantidadePadrao,
                           TratamentoDePreco tratamento, BigDecimal valor, UUID unidadeId,
                           int ordem, boolean ativo, String justificativa) {
        Servico servico = servico(servicoId);
        ItemDeServico item = itens.findByIdAndServicoId(itemId, servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado."));

        BigDecimal antes = item.getValor();
        item.ajustar(nome, descricao, obrigatorio, quantidadePadrao, tratamento, valor,
                unidade(unidadeId), ordem, ativo);

        if (!Objects.equals(antes, item.getValor())) {
            anotar(servico, item, "Preço do item alterado", antes, item.getValor(), justificativa);
        }
    }

    @Transactional
    public void inativarItem(UUID servicoId, UUID itemId) {
        servico(servicoId);
        itens.findByIdAndServicoId(itemId, servicoId).ifPresent(ItemDeServico::inativar);
    }

    // ----------------------------------------------------------------- precos

    @Transactional
    public void salvarPreco(UUID servicoId, FormaDePreco forma, BigDecimal valor,
                            UUID unidadeId, LocalDate vigenciaInicio, String justificativa) {
        Servico servico = servico(servicoId);
        BigDecimal antes = servico.getValor();
        FormaDePreco formaAntes = servico.getFormaPreco();

        servico.ajustarPreco(forma, valor, unidade(unidadeId),
                vigenciaInicio == null ? LocalDate.now() : vigenciaInicio);

        boolean mudouValor = !Objects.equals(antes, servico.getValor());
        boolean mudouForma = formaAntes != forma;
        if (mudouValor || mudouForma) {
            anotar(servico, null,
                    mudouForma ? "Forma de preço e valor do serviço" : "Preço do serviço alterado",
                    antes, servico.getValor(), justificativa);
        }
    }

    public List<MudancaDePreco> historicoDe(UUID servicoId) {
        servico(servicoId);
        return historico.findByServicoIdOrderByQuandoDesc(servicoId);
    }

    private void anotar(Servico servico, ItemDeServico item, String oQueMudou,
                        BigDecimal antes, BigDecimal depois, String justificativa) {
        historico.save(new MudancaDePreco(
                servico.getEmpresa().getId(), servico.getId(),
                item == null ? null : item.getId(), oQueMudou,
                servico.getFormaPreco().name(), antes, depois,
                servico.getUnidade() == null ? null : servico.getUnidade().getNome(),
                servico.getVigenciaInicio(), justificativa, contexto.autor()));
    }

    // ------------------------------------------------------- cadastros de apoio

    public List<Departamento> departamentos() {
        return departamentos.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public List<Departamento> departamentosAtivos() {
        return departamentos.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    @Transactional
    public void cadastrarDepartamento(String nome, String descricao) {
        departamentos.save(new Departamento(contexto.exigirEmpresa(), nome, descricao));
    }

    @Transactional
    public void salvarDepartamento(UUID id, String nome, String descricao, boolean ativo) {
        departamentos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado."))
                .ajustar(nome, descricao, ativo);
    }

    public List<UnidadeDeCobranca> unidades() {
        return unidades.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public List<UnidadeDeCobranca> unidadesAtivas() {
        return unidades.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    @Transactional
    public void cadastrarUnidade(String nome) {
        unidades.save(new UnidadeDeCobranca(contexto.exigirEmpresa(), nome));
    }

    @Transactional
    public void salvarUnidade(UUID id, String nome, boolean ativo) {
        unidades.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada."))
                .ajustar(nome, ativo);
    }

    // ------------------------------------------------------------------ apoio

    private Departamento departamento(UUID id) {
        if (id == null) {
            return null;
        }
        return departamentos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado."));
    }

    private UnidadeDeCobranca unidade(UUID id) {
        if (id == null) {
            return null;
        }
        return unidades.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada."));
    }

    /** Código sequencial por empresa: S0001, S0002. */
    private String proximoCodigo() {
        String ultimo = servicos.ultimoCodigo(contexto.exigirEmpresaId());
        int numero = 0;
        if (ultimo != null && ultimo.length() > 1) {
            try {
                numero = Integer.parseInt(ultimo.substring(1));
            } catch (NumberFormatException codigoAntigoForaDoPadrao) {
                numero = 0;
            }
        }
        return "S" + String.format("%04d", numero + 1);
    }
}
