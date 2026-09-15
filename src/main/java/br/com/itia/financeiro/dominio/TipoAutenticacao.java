package br.com.itia.financeiro.dominio;

import java.util.List;

/**
 * COMO o outro lado sabe que somos nós.
 *
 * Fica separado do tipo de ligação de propósito: a mesma forma de
 * autenticação serve para APIs diferentes, e uma API pode mudar de forma sem
 * mudar o resto da configuração.
 */
public enum TipoAutenticacao {

    NENHUMA("Sem autenticação",
            "O outro lado é aberto, ou a proteção é o próprio endereço secreto.",
            List.of()),

    CHAVE_NO_CABECALHO("Chave em cabeçalho",
            "Uma chave fixa enviada em um cabeçalho da chamada. O mais comum em APIs.",
            List.of(new Campo("cabecalho", "Nome do cabeçalho", false, true),
                    new Campo("chave", "Chave", true, true))),

    TOKEN_FIXO("Token fixo",
            "Um token enviado como Bearer, sem data para vencer.",
            List.of(new Campo("token", "Token", true, true))),

    BASICA("Usuário e senha",
            "Autenticação básica: usuário e senha na própria chamada.",
            List.of(new Campo("usuario", "Usuário", false, true),
                    new Campo("senha", "Senha", true, true))),

    OAUTH2("OAuth2 por credencial de aplicação",
            "O sistema pega um token que vence e renova sozinho antes de cada uso.",
            List.of(new Campo("clientId", "Client ID", false, true),
                    new Campo("clientSecret", "Client Secret", true, true),
                    new Campo("urlDoToken", "Endereço que emite o token", false, true),
                    new Campo("escopo", "Escopo", false, false))),

    CERTIFICADO("Certificado digital",
            "O acesso exige certificado do cliente, como em API de banco.",
            List.of(new Campo("certificado", "Certificado (conteúdo .crt)", true, true),
                    new Campo("chavePrivada", "Chave privada (conteúdo .key)", true, true),
                    new Campo("clientId", "Client ID", false, false),
                    new Campo("clientSecret", "Client Secret", true, false))),

    ASSINATURA_HMAC("Assinatura do aviso",
            "Cada aviso recebido vem assinado. O sistema confere a assinatura antes de aceitar.",
            List.of(new Campo("segredoAssinatura", "Segredo da assinatura", true, true),
                    new Campo("cabecalhoAssinatura", "Cabeçalho que traz a assinatura", false, true)));

    /**
     * Um campo de configuração.
     *
     * @param chave   nome interno
     * @param rotulo  o que aparece na tela
     * @param segredo se some da tela depois de salvo
     * @param exigido se sem ele a integração não liga
     */
    public record Campo(String chave, String rotulo, boolean segredo, boolean exigido) {
    }

    private final String rotulo;
    private final String resumo;
    private final List<Campo> campos;

    TipoAutenticacao(String rotulo, String resumo, List<Campo> campos) {
        this.rotulo = rotulo;
        this.resumo = resumo;
        this.campos = campos;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getResumo() {
        return resumo;
    }

    public List<Campo> getCampos() {
        return campos;
    }

    /** Se a execução desta forma já está pronta, ou só o guardar. */
    public boolean executaHoje() {
        return this != CERTIFICADO;
    }
}
