package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.ColunaCarteira;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.PagadorRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A carteira: as pessoas que pagam e as unidades de cada uma, com os numeros
 * de cada linha calculados na hora.
 *
 * O desenho da tela segue a base de clientes do sistema do grupo: uma linha por
 * pessoa, com as unidades dela empilhadas ao lado.
 */
@Service
public class CarteiraServico {

    private final PagadorRepositorio pagadores;
    private final ClienteRepositorio unidades;
    private final TituloRepositorio titulos;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;

    public CarteiraServico(PagadorRepositorio pagadores, ClienteRepositorio unidades,
                           TituloRepositorio titulos, ContextoEmpresa contexto,
                           Avisos avisos) {
        this.pagadores = pagadores;
        this.unidades = unidades;
        this.titulos = titulos;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ----------------------------------------------------------------- cadastro

    /**
     * Uma unidade informada na tela de cadastro.
     *
     * @param nome      nome da unidade
     * @param documento CNPJ, quando houver
     * @param codigo    codigo no sistema de origem
     */
    public record UnidadeInformada(String nome, String documento, String codigo) {

        public boolean vazia() {
            return (nome == null || nome.isBlank())
                    && (documento == null || documento.isBlank())
                    && (codigo == null || codigo.isBlank());
        }
    }

    /**
     * Procura uma pessoa pelo CPF, ignorando ponto e traco.
     *
     * E o que impede cadastro repetido: o mesmo CPF digitado de dois jeitos
     * diferentes continua sendo a mesma pessoa.
     */
    public java.util.Optional<Pagador> pessoaComEsseCpf(String cpf) {
        String procurado = somenteDigitos(cpf);
        if (procurado.isEmpty()) {
            return java.util.Optional.empty();
        }
        return pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId())
                .stream()
                .filter(p -> somenteDigitos(p.getCpf()).equals(procurado))
                .findFirst();
    }

    private static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    @Transactional
    public Pagador cadastrar(String nome, String cpf, String whatsapp, String telefone,
                             String email, List<UnidadeInformada> unidadesInformadas) {
        Empresa empresa = contexto.exigirEmpresa();
        Pagador pagador = new Pagador(empresa, nome, cpf, contexto.autor());
        pagador.atualizar(nome, cpf, whatsapp, telefone, email, null);
        pagadores.save(pagador);

        // Toda pessoa nasce com pelo menos uma unidade. Se nao informarem
        // nenhuma, a unidade leva o nome da propria pessoa.
        List<UnidadeInformada> validas = validas(unidadesInformadas);
        if (validas.isEmpty()) {
            adicionarUnidade(pagador, nome, null, null, telefone, email);
        } else {
            for (UnidadeInformada u : validas) {
                adicionarUnidade(pagador, nomeDaUnidade(u, nome), u.documento(), u.codigo(),
                        telefone, email);
            }
        }
        avisos.avisar(empresa.getId(), "CLIENTE_CADASTRADO");
        return pagador;
    }

    /** Soma unidades novas a uma pessoa que ja existe, sem criar cadastro repetido. */
    @Transactional
    public Pagador somarUnidades(UUID pagadorId, List<UnidadeInformada> unidadesInformadas) {
        Pagador dono = pagador(pagadorId);
        for (UnidadeInformada u : validas(unidadesInformadas)) {
            adicionarUnidade(dono, nomeDaUnidade(u, dono.getNome()), u.documento(), u.codigo(),
                    dono.getTelefone(), dono.getEmail());
        }
        return dono;
    }

    private List<UnidadeInformada> validas(List<UnidadeInformada> informadas) {
        if (informadas == null) {
            return List.of();
        }
        return informadas.stream().filter(u -> u != null && !u.vazia()).toList();
    }

    private String nomeDaUnidade(UnidadeInformada u, String nomeDaPessoa) {
        if (u.nome() != null && !u.nome().isBlank()) {
            return u.nome().trim();
        }
        // Sem nome, mas com CNPJ: a unidade fica com o documento como nome, para
        // ninguem ficar com uma linha em branco na tabela.
        if (u.documento() != null && !u.documento().isBlank()) {
            return u.documento().trim();
        }
        return nomeDaPessoa;
    }

    @Transactional
    public ClienteEspelho adicionarUnidade(Pagador pagador, String nome, String documento,
                                           String codigo, String telefone, String email) {
        ClienteEspelho unidade = new ClienteEspelho(pagador.getEmpresa(), null, nome);
        unidade.atualizarCom(nome, documento, pagador.getNome(), telefone, email, true);
        unidade.vincularA(pagador);
        unidade.setCodigoExterno(codigo);
        ClienteEspelho salva = unidades.save(unidade);
        avisos.avisar(pagador.getEmpresa().getId(), "UNIDADE_ADICIONADA");
        return salva;
    }

    public Pagador pagador(UUID pagadorId) {
        return pagadores.findByIdAndEmpresaId(pagadorId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente nao encontrado nesta empresa."));
    }

    // ------------------------------------------------------------------ leitura

    public List<LinhaPessoa> carteira() {
        return carteira(new FiltroCarteira(java.util.Map.of()));
    }

    /** A tabela da tela de clientes, ja montada e filtrada. */
    public List<LinhaPessoa> carteira(FiltroCarteira filtro) {
        UUID empresaId = contexto.exigirEmpresaId();
        LocalDate hoje = LocalDate.now();

        List<Titulo> todosOsTitulos = titulos.findByEmpresaIdOrderByVencimentoDesc(empresaId);
        Map<UUID, List<Titulo>> porUnidade = new LinkedHashMap<>();
        for (Titulo t : todosOsTitulos) {
            porUnidade.computeIfAbsent(t.getCliente().getId(), k -> new ArrayList<>()).add(t);
        }

        Map<UUID, LinhaPessoa> linhas = new LinkedHashMap<>();
        for (Pagador p : pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId)) {
            linhas.put(p.getId(), new LinhaPessoa(p));
        }

        for (ClienteEspelho u : unidades.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(empresaId)) {
            LinhaUnidade linha = new LinhaUnidade(u, porUnidade.getOrDefault(u.getId(), List.of()), hoje);
            if (u.getPagador() == null) {
                // Unidade sem pessoa dona ainda: aparece sozinha, para nao sumir.
                linhas.computeIfAbsent(u.getId(), k -> new LinhaPessoa(u)).unidades().add(linha);
            } else {
                LinhaPessoa dona = linhas.get(u.getPagador().getId());
                if (dona != null) {
                    dona.unidades().add(linha);
                }
            }
        }

        List<LinhaPessoa> resultado = new ArrayList<>(linhas.values());

        // Filtra: primeiro as unidades, depois a pessoa. Pessoa que ficou sem
        // nenhuma unidade visivel sai da tabela.
        for (LinhaPessoa linha : resultado) {
            linha.unidades().removeIf(u -> !aceita(filtro, u));
        }
        resultado.removeIf(l -> l.unidades().isEmpty() || !aceita(filtro, l));
        resultado.sort(Comparator.comparing(LinhaPessoa::nome, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    /** As unidades de uma pessoa, para a janela do perfil. */
    public List<LinhaUnidade> unidadesDe(UUID pagadorId) {
        LocalDate hoje = LocalDate.now();
        List<LinhaUnidade> lista = new ArrayList<>();
        for (ClienteEspelho u : unidades.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(
                contexto.exigirEmpresaId())) {
            if (u.getPagador() != null && u.getPagador().getId().equals(pagadorId)) {
                lista.add(new LinhaUnidade(u, titulos.findByClienteIdOrderByVencimentoDesc(u.getId()), hoje));
            }
        }
        return lista;
    }

    public LinhaUnidade unidade(UUID unidadeId) {
        ClienteEspelho u = unidades.findByIdAndEmpresaId(unidadeId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unidade nao encontrada nesta empresa."));
        return new LinhaUnidade(u, titulos.findByClienteIdOrderByVencimentoDesc(u.getId()),
                LocalDate.now());
    }

    // ------------------------------------------------------------------ filtro

    private boolean aceita(FiltroCarteira filtro, LinhaPessoa p) {
        return filtro.aceitaTexto(ColunaCarteira.CLIENTE, p.nome() + " " + texto(p.getDocumento()))
                && filtro.aceitaTexto(ColunaCarteira.PESSOA_WHATSAPP, p.valor(ColunaCarteira.PESSOA_WHATSAPP))
                && filtro.aceitaTexto(ColunaCarteira.PESSOA_TELEFONE, p.valor(ColunaCarteira.PESSOA_TELEFONE))
                && filtro.aceitaTexto(ColunaCarteira.PESSOA_EMAIL, p.valor(ColunaCarteira.PESSOA_EMAIL))
                && filtro.aceitaSituacao(ColunaCarteira.PESSOA_SITUACAO, p.getSituacao())
                && filtro.aceitaFaixa(ColunaCarteira.PESSOA_EM_ABERTO, p.getEmAberto())
                && filtro.aceitaFaixa(ColunaCarteira.PESSOA_VENCIDO, p.getVencido());
    }

    private boolean aceita(FiltroCarteira filtro, LinhaUnidade u) {
        return filtro.aceitaTexto(ColunaCarteira.UNIDADE, u.getNome())
                && filtro.aceitaTexto(ColunaCarteira.UNIDADE_DOCUMENTO, u.getDocumento())
                && filtro.aceitaTexto(ColunaCarteira.UNIDADE_CODIGO, u.getCodigo())
                && filtro.aceitaTexto(ColunaCarteira.UNIDADE_RESPONSAVEL, u.getUnidade().getResponsavel())
                && filtro.aceitaTexto(ColunaCarteira.UNIDADE_TELEFONE, u.getUnidade().getTelefone())
                && filtro.aceitaTexto(ColunaCarteira.UNIDADE_EMAIL, u.getUnidade().getEmail())
                && filtro.aceitaSituacao(ColunaCarteira.UNIDADE_SITUACAO, u.getSituacao())
                && filtro.aceitaFaixa(ColunaCarteira.UNIDADE_EM_ABERTO, u.getEmAberto())
                && filtro.aceitaFaixa(ColunaCarteira.UNIDADE_VENCIDO, u.getVencido())
                && filtro.aceitaFaixa(ColunaCarteira.UNIDADE_ATRASO, BigDecimal.valueOf(u.getMaiorAtraso()))
                && filtro.aceitaFaixa(ColunaCarteira.UNIDADE_TITULOS, BigDecimal.valueOf(u.getQuantidadeTitulos()))
                && filtro.aceitaPeriodo(ColunaCarteira.UNIDADE_PROXIMO_VENCIMENTO, u.getProximoVencimento())
                && filtro.aceitaPeriodo(ColunaCarteira.UNIDADE_ULTIMO_RECEBIMENTO, u.getUltimoRecebimento());
    }

    // ------------------------------------------------------------------- linhas

    /** Uma linha da tabela: a pessoa, com as unidades dela empilhadas. */
    public static final class LinhaPessoa {
        private final Pagador pagador;
        private final String nomeSolto;
        private final List<LinhaUnidade> unidades = new ArrayList<>();

        LinhaPessoa(Pagador pagador) {
            this.pagador = pagador;
            this.nomeSolto = null;
        }

        LinhaPessoa(ClienteEspelho unidadeSemDono) {
            this.pagador = null;
            this.nomeSolto = unidadeSemDono.getRazaoSocial();
        }

        public Pagador getPagador() {
            return pagador;
        }

        public String nome() {
            return pagador != null ? pagador.getNome() : nomeSolto;
        }

        public String getNome() {
            return nome();
        }

        public String getDocumento() {
            return pagador != null ? pagador.getCpf() : null;
        }

        public String getContato() {
            if (pagador == null) {
                return null;
            }
            return pagador.getWhatsapp() != null ? pagador.getWhatsapp() : pagador.getTelefone();
        }

        public List<LinhaUnidade> unidades() {
            return unidades;
        }

        public List<LinhaUnidade> getUnidades() {
            return unidades;
        }

        public BigDecimal getEmAberto() {
            return unidades.stream().map(LinhaUnidade::getEmAberto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal getVencido() {
            return unidades.stream().map(LinhaUnidade::getVencido)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public long getMaiorAtraso() {
            return unidades.stream().mapToLong(LinhaUnidade::getMaiorAtraso).max().orElse(0);
        }

        public String getSituacao() {
            if (getVencido().signum() > 0) {
                return getMaiorAtraso() > 30 ? "crítico" : "em atraso";
            }
            return getEmAberto().signum() > 0 ? "em dia" : "sem débito";
        }

        /** O que aparece na celula daquela coluna. */
        public String valor(ColunaCarteira coluna) {
            return switch (coluna) {
                case CLIENTE -> nome();
                case PESSOA_DOCUMENTOS -> unidades.size() + " unidade(s)";
                case PESSOA_WHATSAPP -> pagador == null ? "" : texto(pagador.getWhatsapp());
                case PESSOA_TELEFONE -> pagador == null ? "" : texto(pagador.getTelefone());
                case PESSOA_EMAIL -> pagador == null ? "" : texto(pagador.getEmail());
                case PESSOA_EM_ABERTO -> dinheiro(getEmAberto());
                case PESSOA_VENCIDO -> dinheiro(getVencido());
                case PESSOA_SITUACAO -> getSituacao();
                case PESSOA_CLIENTE_DESDE -> pagador == null ? ""
                        : pagador.getCriadoEm().toLocalDate().format(DIA);
                default -> "";
            };
        }
    }

    /** Uma unidade da pessoa, com os numeros dela. */
    public static final class LinhaUnidade {
        private final ClienteEspelho unidade;
        private final List<Titulo> titulos;
        private final LocalDate hoje;

        LinhaUnidade(ClienteEspelho unidade, List<Titulo> titulos, LocalDate hoje) {
            this.unidade = unidade;
            this.titulos = titulos;
            this.hoje = hoje;
        }

        public ClienteEspelho getUnidade() {
            return unidade;
        }

        public List<Titulo> getTitulos() {
            return titulos;
        }

        public UUID getId() {
            return unidade.getId();
        }

        public String getNome() {
            return unidade.getRazaoSocial();
        }

        public String getDocumento() {
            return unidade.getCnpjCpf();
        }

        public String getCodigo() {
            return unidade.getCodigoExterno();
        }

        private List<Titulo> abertos() {
            return titulos.stream()
                    .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                            || t.getSituacao() == SituacaoTitulo.PARCIAL)
                    .toList();
        }

        public BigDecimal getEmAberto() {
            return abertos().stream().map(Titulo::getSaldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal getVencido() {
            return abertos().stream().filter(t -> t.estaVencido(hoje)).map(Titulo::getSaldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public long getMaiorAtraso() {
            return abertos().stream().mapToLong(t -> t.diasDeAtraso(hoje)).max().orElse(0);
        }

        public LocalDate getProximoVencimento() {
            return abertos().stream().map(Titulo::getVencimento)
                    .filter(v -> !v.isBefore(hoje)).min(Comparator.naturalOrder()).orElse(null);
        }

        public LocalDate getUltimoRecebimento() {
            return titulos.stream().flatMap(t -> t.getPagamentos().stream())
                    .map(pg -> pg.getPagoEm()).max(Comparator.naturalOrder()).orElse(null);
        }

        public int getQuantidadeTitulos() {
            return titulos.size();
        }

        public String getSituacao() {
            if (getVencido().signum() > 0) {
                return getMaiorAtraso() > 30 ? "crítico" : "em atraso";
            }
            return getEmAberto().signum() > 0 ? "em dia" : "sem débito";
        }

        /** O que aparece na celula daquela coluna. */
        public String valor(ColunaCarteira coluna) {
            return switch (coluna) {
                case UNIDADE -> getNome();
                case UNIDADE_DOCUMENTO -> texto(getDocumento());
                case UNIDADE_CODIGO -> texto(getCodigo());
                case UNIDADE_SITUACAO -> getSituacao();
                case UNIDADE_EM_ABERTO -> dinheiro(getEmAberto());
                case UNIDADE_VENCIDO -> dinheiro(getVencido());
                case UNIDADE_ATRASO -> getMaiorAtraso() > 0 ? getMaiorAtraso() + " d" : "";
                case UNIDADE_PROXIMO_VENCIMENTO -> data(getProximoVencimento());
                case UNIDADE_ULTIMO_RECEBIMENTO -> data(getUltimoRecebimento());
                case UNIDADE_TITULOS -> String.valueOf(getQuantidadeTitulos());
                case UNIDADE_RESPONSAVEL -> texto(unidade.getResponsavel());
                case UNIDADE_TELEFONE -> texto(unidade.getTelefone());
                case UNIDADE_EMAIL -> texto(unidade.getEmail());
                default -> "";
            };
        }
    }

    // ------------------------------------------------------------- formatacao

    private static final java.time.format.DateTimeFormatter DIA =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private static String data(LocalDate valor) {
        return valor == null ? "" : valor.format(DIA);
    }

    private static String dinheiro(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        return String.format(java.util.Locale.of("pt", "BR"), "%,.2f", valor);
    }
}
