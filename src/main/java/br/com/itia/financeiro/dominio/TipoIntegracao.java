package br.com.itia.financeiro.dominio;

import java.util.List;

/**
 * COMO a ligação funciona. É a pergunta mais importante de uma integração, e
 * vem antes de escolher com quem se vai falar.
 *
 * Cada tipo traz, junto, a explicação do que dá para fazer com ele. É essa
 * explicação que a tela de ajuda mostra: o sistema documenta a si mesmo, em vez
 * de depender de manual que envelhece.
 */
public enum TipoIntegracao {

    API_REST("Chamada de API",
            "O sistema chama o outro lado quando precisa: consulta, cria, atualiza.",
            List.of("Consultar extrato, saldo e movimento de conta bancária",
                    "Emitir cobrança, boleto ou PIX em banco e em gateway de pagamento",
                    "Buscar e enviar cadastro de cliente para um ERP ou CRM",
                    "Ler e gravar em qualquer sistema que tenha API"),
            List.of("Informe o endereço base e a forma de autenticação",
                    "Cadastre as operações: o que cada chamada faz, o verbo e o caminho",
                    "Teste a conexão e ligue",
                    "A partir daí o sistema chama sozinho quando a regra manda"),
            List.of(TipoAutenticacao.NENHUMA, TipoAutenticacao.CHAVE_NO_CABECALHO,
                    TipoAutenticacao.TOKEN_FIXO, TipoAutenticacao.BASICA,
                    TipoAutenticacao.OAUTH2, TipoAutenticacao.CERTIFICADO),
            true, false),

    WEBHOOK("Recebimento de aviso",
            "O outro lado avisa este sistema assim que algo acontece lá.",
            List.of("Receber aviso de PIX e de boleto pago, na hora em que cai",
                    "Receber baixa e cancelamento vindos do sistema de cobrança",
                    "Receber qualquer mudança de cadastro feita fora daqui",
                    "Servir de ponte: um sistema avisa, e este repassa para outro"),
            List.of("O sistema gera um endereço próprio, com trecho secreto",
                    "Você informa esse endereço no outro sistema",
                    "Todo aviso é guardado bruto, com data e conteúdo",
                    "O tradutor daquele conector interpreta e aplica no financeiro"),
            List.of(TipoAutenticacao.NENHUMA, TipoAutenticacao.ASSINATURA_HMAC,
                    TipoAutenticacao.CHAVE_NO_CABECALHO),
            false, true),

    BANCO_DE_DADOS("Banco de dados",
            "Leitura e gravação direto numa base de fora, sem passar por API.",
            List.of("Espelhar cadastro de cliente que vive em outro banco",
                    "Puxar lançamento de um sistema legado que não tem API",
                    "Alimentar relatório com dado que mora fora",
                    "Sincronizar tabela com outro sistema do grupo"),
            List.of("Informe o endereço do banco e o usuário de acesso",
                    "Cadastre as consultas que podem rodar, uma a uma",
                    "Nenhuma consulta roda sem estar cadastrada aqui",
                    "Todo acesso fica registrado, com data e resultado"),
            List.of(TipoAutenticacao.BASICA, TipoAutenticacao.CHAVE_NO_CABECALHO),
            true, false),

    ARQUIVO("Arquivo",
            "Troca por arquivo: o que entra e o que sai em lote.",
            List.of("Retorno bancário em CNAB, para dar baixa em lote",
                    "Extrato em OFX vindo do internet banking",
                    "Planilha de faturamento para fechar o mês",
                    "Exportação para a contabilidade"),
            List.of("Escolha o formato e o layout do arquivo",
                    "Faça o de-para entre coluna do arquivo e campo daqui",
                    "Cada arquivo processado vira um registro, com o conteúdo guardado",
                    "Arquivo repetido é barrado pela própria identificação dele"),
            List.of(TipoAutenticacao.NENHUMA),
            false, false),

    FILA("Fila de mensagens",
            "Mensagens entrando e saindo por uma fila, sem chamada direta.",
            List.of("Receber evento de um sistema que publica em fila",
                    "Publicar o que acontece aqui para outros sistemas ouvirem",
                    "Segurar volume alto sem perder nada quando o outro lado cai"),
            List.of("Informe o endereço da fila e o acesso",
                    "Escolha quais eventos entram e quais saem",
                    "Mensagem que falha volta para a fila, com espera crescente"),
            List.of(TipoAutenticacao.BASICA, TipoAutenticacao.TOKEN_FIXO),
            true, true);

    private final String rotulo;
    private final String resumo;
    private final List<String> oQueDaParaFazer;
    private final List<String> comoFunciona;
    private final List<TipoAutenticacao> autenticacoes;
    private final boolean chamaParaFora;
    private final boolean recebeDeFora;

    TipoIntegracao(String rotulo, String resumo, List<String> oQueDaParaFazer,
                   List<String> comoFunciona, List<TipoAutenticacao> autenticacoes,
                   boolean chamaParaFora, boolean recebeDeFora) {
        this.rotulo = rotulo;
        this.resumo = resumo;
        this.oQueDaParaFazer = oQueDaParaFazer;
        this.comoFunciona = comoFunciona;
        this.autenticacoes = autenticacoes;
        this.chamaParaFora = chamaParaFora;
        this.recebeDeFora = recebeDeFora;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getResumo() {
        return resumo;
    }

    public List<String> getOQueDaParaFazer() {
        return oQueDaParaFazer;
    }

    public List<String> getComoFunciona() {
        return comoFunciona;
    }

    public List<TipoAutenticacao> getAutenticacoes() {
        return autenticacoes;
    }

    /** Se esta ligação sai daqui para o outro lado. */
    public boolean isChamaParaFora() {
        return chamaParaFora;
    }

    /** Se esta ligação recebe aviso do outro lado. */
    public boolean isRecebeDeFora() {
        return recebeDeFora;
    }
}
