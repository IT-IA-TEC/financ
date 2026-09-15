package br.com.itia.financeiro.dominio;

import java.util.List;

/**
 * Um modelo pronto de ligação.
 *
 * O conector não é uma trava: ele apenas preenche de uma vez o tipo, a forma
 * de autenticação, o endereço e as operações mais usadas daquele sistema. Tudo
 * continua editável depois, e quem quiser integrar com um sistema que não está
 * nesta lista usa PERSONALIZADA e monta do zero.
 *
 * As operações sugeridas são ponto de partida. Confira sempre o caminho na
 * documentação do provedor antes de ligar em produção.
 */
public enum Conector {

    PERSONALIZADA("Do zero",
            "Monte a ligação você mesmo: escolha o tipo, a autenticação e as operações.",
            null, null, null, List.of()),

    BANCO_API("Banco (conta corrente e cobrança)",
            "Conta bancária por API: extrato, saldo, emissão de cobrança e aviso de recebimento.",
            TipoIntegracao.API_REST, TipoAutenticacao.CERTIFICADO, null,
            List.of(new Sugestao("Consultar extrato", "GET", "/banking/v2/extrato",
                            "Traz o movimento da conta num período."),
                    new Sugestao("Consultar saldo", "GET", "/banking/v2/saldo",
                            "Saldo atual da conta."),
                    new Sugestao("Emitir cobrança", "POST", "/cobranca/v3/cobrancas",
                            "Cria boleto ou PIX para um título."),
                    new Sugestao("Consultar cobrança", "GET", "/cobranca/v3/cobrancas/{codigo}",
                            "Situação de uma cobrança emitida."))),

    SISTEMA_DE_COBRANCA("Sistema de cobrança",
            "Plataforma de cobrança: sincroniza título, emite boleto e devolve a baixa.",
            TipoIntegracao.API_REST, TipoAutenticacao.TOKEN_FIXO, null,
            List.of(new Sugestao("Listar títulos", "GET", "/titulos",
                            "Traz os títulos do período."),
                    new Sugestao("Enviar título", "POST", "/titulos",
                            "Manda um título daqui para lá."),
                    new Sugestao("Emitir boleto", "POST", "/boletos",
                            "Pede a emissão do boleto de um título."))),

    BANCO_DE_DADOS_EXTERNO("Banco de dados por API",
            "Base de fora acessada por API, como o Supabase: lê e grava tabela sem abrir o banco.",
            TipoIntegracao.API_REST, TipoAutenticacao.CHAVE_NO_CABECALHO, null,
            List.of(new Sugestao("Consultar tabela", "GET", "/rest/v1/{tabela}",
                            "Lê linhas de uma tabela."),
                    new Sugestao("Gravar linha", "POST", "/rest/v1/{tabela}",
                            "Insere uma linha numa tabela."),
                    new Sugestao("Atualizar linha", "PATCH", "/rest/v1/{tabela}",
                            "Altera linhas que atendem ao filtro."))),

    CONSULTA_DE_CNPJ("Consulta de CNPJ",
            "Traz os dados da empresa direto da Receita: razao social, situacao, CNAE e porte.",
            TipoIntegracao.API_REST, TipoAutenticacao.CHAVE_NO_CABECALHO, null,
            List.of(new Sugestao("Consultar CNPJ", "GET", "/office/{cnpj}",
                    "Traz os dados cadastrais de um CNPJ. O trecho {cnpj} e trocado na hora."))),

    SO_RECEBIMENTO("Só recebimento",
            "Um endereço para o outro sistema avisar o que aconteceu. Não sai nada daqui.",
            TipoIntegracao.WEBHOOK, TipoAutenticacao.ASSINATURA_HMAC, null,
            List.of()),

    WHATSAPP("WhatsApp (mensagem)",
            "Manda e recebe mensagem de cobrança pelo WhatsApp, e a conversa fica dentro do "
                    + "sistema. O endereço e o número saem da conta oficial da empresa.",
            TipoIntegracao.API_REST, TipoAutenticacao.TOKEN_FIXO, null,
            List.of(new Sugestao("Enviar mensagem", "POST", "/{numero}/messages",
                            "Manda um texto para um número. O trecho {numero} é o identificador "
                                    + "do telefone da empresa na conta oficial."),
                    new Sugestao("Enviar modelo aprovado", "POST", "/{numero}/messages",
                            "Manda um modelo já aprovado, para começar conversa fora da janela "
                                    + "de 24 horas."))),

    ARQUIVO_BANCARIO("Arquivo de retorno",
            "Processa arquivo de retorno bancário ou extrato para dar baixa em lote.",
            TipoIntegracao.ARQUIVO, TipoAutenticacao.NENHUMA, null,
            List.of());

    /**
     * Uma operação já sugerida pelo modelo.
     *
     * @param nome      o que essa chamada faz, em português
     * @param verbo     GET, POST, PUT, PATCH ou DELETE
     * @param caminho   o caminho depois do endereço base
     * @param paraQue   para que serve, na prática
     */
    public record Sugestao(String nome, String verbo, String caminho, String paraQue) {
    }

    private final String rotulo;
    private final String resumo;
    private final TipoIntegracao tipoSugerido;
    private final TipoAutenticacao autenticacaoSugerida;
    private final String enderecoSugerido;
    private final List<Sugestao> operacoesSugeridas;

    Conector(String rotulo, String resumo, TipoIntegracao tipoSugerido,
             TipoAutenticacao autenticacaoSugerida, String enderecoSugerido,
             List<Sugestao> operacoesSugeridas) {
        this.rotulo = rotulo;
        this.resumo = resumo;
        this.tipoSugerido = tipoSugerido;
        this.autenticacaoSugerida = autenticacaoSugerida;
        this.enderecoSugerido = enderecoSugerido;
        this.operacoesSugeridas = operacoesSugeridas;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getResumo() {
        return resumo;
    }

    public TipoIntegracao getTipoSugerido() {
        return tipoSugerido == null ? TipoIntegracao.API_REST : tipoSugerido;
    }

    public TipoAutenticacao getAutenticacaoSugerida() {
        return autenticacaoSugerida == null ? TipoAutenticacao.NENHUMA : autenticacaoSugerida;
    }

    public String getEnderecoSugerido() {
        return enderecoSugerido;
    }

    public List<Sugestao> getOperacoesSugeridas() {
        return operacoesSugeridas;
    }
}
