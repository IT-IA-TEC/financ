package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Conector;
import br.com.itia.financeiro.dominio.Fonte;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.repositorio.IntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.OperacaoIntegracaoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Traz os dados da empresa da Receita, pela CNPJá.
 *
 * Não tem endereço nem chave escritos aqui: a ligação é uma integração comum do
 * módulo de Integrações, do tipo chamada de API. Quem configura endereço,
 * credencial e operação é a tela de integrações, igual a qualquer outra.
 *
 * Assim, trocar de fornecedor de consulta não mexe em nada deste arquivo: basta
 * configurar outra integração com a mesma operação.
 */
@Service
public class ConsultaDeCnpj {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultaDeCnpj.class);

    /** O nome que a operação precisa ter na integração, para ser encontrada. */
    public static final String OPERACAO = "Consultar CNPJ";

    private final IntegracaoRepositorio integracoes;
    private final OperacaoIntegracaoRepositorio operacoes;
    private final IntegracaoServico integracaoServico;
    private final ContextoEmpresa contexto;
    private final ObjectMapper json = new ObjectMapper();

    public ConsultaDeCnpj(IntegracaoRepositorio integracoes,
                          OperacaoIntegracaoRepositorio operacoes,
                          IntegracaoServico integracaoServico, ContextoEmpresa contexto) {
        this.integracoes = integracoes;
        this.operacoes = operacoes;
        this.integracaoServico = integracaoServico;
        this.contexto = contexto;
    }

    /** Se existe uma integração de consulta ligada nesta empresa. */
    public boolean disponivel() {
        return acharIntegracao().isPresent();
    }

    private Optional<Integracao> acharIntegracao() {
        return integracoes.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId()).stream()
                .filter(Integracao::isAtiva)
                .filter(i -> i.getProvedor() == Conector.CONSULTA_DE_CNPJ)
                .findFirst();
    }

    /**
     * Consulta o CNPJ e preenche os dados fiscais da unidade.
     *
     * O que já estava preenchido à mão não é apagado quando a consulta não traz
     * aquele campo: só é trocado o que vier com conteúdo.
     */
    @Transactional
    public String preencher(ClienteEspelho unidade) {
        Integracao integracao = acharIntegracao().orElseThrow(() -> new IllegalStateException(
                "Nenhuma integração de consulta de CNPJ ligada nesta empresa. "
                        + "Configure em Integrações, com o modelo Consulta de CNPJ."));

        String cnpj = somenteDigitos(unidade.getCnpjCpf());
        if (cnpj.length() != 14) {
            throw new IllegalArgumentException("Esta unidade não tem um CNPJ válido cadastrado.");
        }

        List<OperacaoIntegracao> lista = operacoes.findByIntegracaoIdOrderByNome(integracao.getId());
        OperacaoIntegracao operacao = lista.stream()
                .filter(o -> OPERACAO.equalsIgnoreCase(o.getNome()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A integração não tem a operação " + OPERACAO + " cadastrada."));

        // O caminho da operacao traz {cnpj}, que e trocado aqui.
        String caminhoOriginal = operacao.getCaminho();
        operacao.ajustar(operacao.getNome(), operacao.getVerbo(),
                caminhoOriginal.replace("{cnpj}", cnpj), operacao.getParaQue(),
                operacao.getCorpoModelo());

        String resposta;
        try {
            resposta = integracaoServico.executar(integracao.getId(), operacao.getId(), null);
        } finally {
            // Devolve o caminho com o {cnpj}, para a proxima consulta funcionar.
            operacao.ajustar(operacao.getNome(), operacao.getVerbo(), caminhoOriginal,
                    operacao.getParaQue(), operacao.getCorpoModelo());
        }

        aplicar(unidade, resposta);
        return resposta;
    }

    /**
     * Lê o que voltou e preenche a unidade.
     *
     * Aceita os nomes de campo mais comuns entre os serviços de consulta, para
     * não quebrar se a resposta vier num formato um pouco diferente.
     */
    private void aplicar(ClienteEspelho unidade, String corpo) {
        try {
            JsonNode raiz = json.readTree(corpo);

            String razao = texto(raiz, "razao_social", "razaoSocial", "name", "nome");
            String fantasia = texto(raiz, "nome_fantasia", "nomeFantasia", "alias", "fantasia");
            String situacao = texto(raiz, "situacao_cadastral", "descricao_situacao_cadastral",
                    "status", "situacao");
            String porte = texto(raiz, "porte", "size", "codigo_porte");
            String abertura = texto(raiz, "data_inicio_atividade", "founded", "abertura",
                    "data_abertura");
            String cnae = texto(raiz, "cnae_fiscal", "cnaePrincipal", "cnae");
            String cnaeDescricao = texto(raiz, "cnae_fiscal_descricao", "atividade_principal",
                    "cnaeDescricao");
            String inscricaoEstadual = texto(raiz, "inscricao_estadual", "inscricaoEstadual");
            Boolean simples = booleano(raiz, "opcao_pelo_simples", "simples", "optanteSimples");

            unidade.atualizarDadosFiscais(
                    manter(fantasia, unidade.getNomeFantasia()),
                    manter(inscricaoEstadual, unidade.getInscricaoEstadual()),
                    unidade.getInscricaoMunicipal(),
                    unidade.getRegimeTributario(),
                    manter(porte, unidade.getPorte()),
                    manter(cnae, unidade.getCnaePrincipal()),
                    manter(cnaeDescricao, unidade.getCnaeDescricao()),
                    data(abertura) != null ? data(abertura) : unidade.getDataAbertura(),
                    manter(situacao, unidade.getSituacaoCadastral()),
                    simples != null ? simples : unidade.getOptanteSimples(),
                    unidade.getRetencoes(),
                    Fonte.CNPJA);

            if (razao != null && !razao.isBlank()) {
                unidade.atualizarCom(razao, unidade.getCnpjCpf(), unidade.getResponsavel(),
                        unidade.getTelefone(), unidade.getEmail(), unidade.isAtivo());
            }
        } catch (Exception erro) {
            LOG.warn("nao consegui ler a resposta da consulta: {}", erro.getMessage());
            throw new IllegalStateException(
                    "A consulta respondeu, mas não consegui entender o conteúdo.");
        }
    }

    private String manter(String novo, String atual) {
        return novo == null || novo.isBlank() ? atual : novo;
    }

    private String texto(JsonNode raiz, String... nomes) {
        for (String nome : nomes) {
            JsonNode achado = procurar(raiz, nome);
            if (achado != null && !achado.isNull() && !achado.asText().isBlank()) {
                return achado.asText();
            }
        }
        return null;
    }

    private Boolean booleano(JsonNode raiz, String... nomes) {
        for (String nome : nomes) {
            JsonNode achado = procurar(raiz, nome);
            if (achado != null && achado.isBoolean()) {
                return achado.asBoolean();
            }
        }
        return null;
    }

    /** Procura o campo em qualquer nível da resposta. */
    private JsonNode procurar(JsonNode raiz, String nome) {
        if (raiz.has(nome)) {
            return raiz.get(nome);
        }
        for (JsonNode filho : raiz) {
            if (filho.isObject()) {
                JsonNode achado = procurar(filho, nome);
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }

    private LocalDate data(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.substring(0, 10));
        } catch (RuntimeException formatoDesconhecido) {
            return null;
        }
    }

    private String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    public UUID empresaAtual() {
        return contexto.exigirEmpresaId();
    }
}
