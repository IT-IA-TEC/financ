package br.com.itia.financeiro.dominio;

import java.util.Arrays;
import java.util.List;

/**
 * O que um modelo de inteligência pode ler e o que pode fazer nesta empresa.
 *
 * Esta lista é fechada de propósito. Não existe "acesso total": cada coisa é
 * uma chave ligada na mão, e o sistema confere a chave em dois momentos
 * diferentes:
 *
 *   na leitura, montando o texto que vai para o modelo: o que não está
 *   liberado nem chega lá, então o modelo não tem como falar sobre isso;
 *
 *   na ação, antes de executar: sem a chave, a ação é recusada e a recusa
 *   fica registrada com o motivo.
 *
 * É essa conferência nos dois lados que faz a restrição valer de verdade, em
 * vez de ser um aviso bonito numa tela de configuração.
 */
public enum PermissaoDaIa {

    // ---------------------------------------------------------------- leitura

    LER_SALDO(Especie.LEITURA, "Ver o que o cliente deve",
            "Documentos em aberto, valores, vencimentos e dias de atraso.",
            "Sem isso o modelo não sabe de quanto está falando."),

    LER_CONVERSA(Especie.LEITURA, "Ler a conversa",
            "As mensagens trocadas com este cliente.",
            "Sem isso ele responde sem saber o que já foi dito."),

    LER_CADASTRO(Especie.LEITURA, "Ver o cadastro",
            "Razão social, responsável, telefone e tom de cobrança combinado.",
            "Dado de cadastro, sem documento nem valor."),

    LER_HISTORICO(Especie.LEITURA, "Ver a história do cliente",
            "Promessas, acordos, contestações e observações registradas.",
            "É o que evita oferecer acordo a quem acabou de quebrar um."),

    LER_PAGAMENTOS(Especie.LEITURA, "Ver os pagamentos",
            "Quando e quanto o cliente pagou nos últimos meses.",
            "Ajuda a entender o comportamento de quem costuma atrasar."),

    LER_REGRAS(Especie.LEITURA, "Ver as regras da empresa",
            "Juros, multa, carência e chave PIX.",
            "Sem isso ele não consegue dizer o valor atualizado."),

    // ------------------------------------------------------------------- ação

    RESPONDER_CLIENTE(Especie.ACAO, "Falar com o cliente",
            "Mandar a mensagem que ele escreveu, sem alguém revisar antes.",
            "Só faz sentido depois de um tempo lendo o que ele sugeriu."),

    SUGERIR_RESPOSTA(Especie.ACAO, "Sugerir resposta",
            "Escrever a mensagem e deixar na caixa, para a pessoa revisar e mandar.",
            "É o jeito seguro de começar."),

    REGISTRAR_PROMESSA(Especie.ACAO, "Anotar promessa de pagamento",
            "Guardar a data que o cliente disse que vai pagar.",
            "Não move dinheiro: só anota o combinado."),

    MARCAR_CASO(Especie.ACAO, "Mudar a situação do caso",
            "Marcar como contestado, parado, em acordo.",
            "Muda a fila e a cobrança automática."),

    PEDIR_COMPROVANTE(Especie.ACAO, "Pedir comprovante",
            "Pedir a prova de pagamento e deixar o caso esperando conferência.",
            "Nunca dá baixa: a baixa continua sendo de gente."),

    PROPOR_ACORDO(Especie.ACAO, "Propor parcelamento",
            "Sugerir dividir a dívida, dentro do teto de valor configurado.",
            "Nunca fecha o acordo sozinho, e nunca passa do teto."),

    PASSAR_PARA_PESSOA(Especie.ACAO, "Passar para uma pessoa",
            "Marcar que o assunto precisa de gente e avisar o cliente.",
            "Deveria estar sempre ligada.");

    /** Se a chave é sobre ver alguma coisa ou sobre fazer alguma coisa. */
    public enum Especie { LEITURA, ACAO }

    private final Especie especie;
    private final String rotulo;
    private final String explicacao;
    private final String porQue;

    PermissaoDaIa(Especie especie, String rotulo, String explicacao, String porQue) {
        this.especie = especie;
        this.rotulo = rotulo;
        this.explicacao = explicacao;
        this.porQue = porQue;
    }

    public Especie getEspecie() {
        return especie;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public String getMotivo() {
        return porQue;
    }

    public boolean deLeitura() {
        return especie == Especie.LEITURA;
    }

    public static List<PermissaoDaIa> leituras() {
        return Arrays.stream(values()).filter(PermissaoDaIa::deLeitura).toList();
    }

    public static List<PermissaoDaIa> acoes() {
        return Arrays.stream(values()).filter(p -> !p.deLeitura()).toList();
    }

    /**
     * Ações que NUNCA existem, nem ligando chave nenhuma.
     *
     * Estão aqui escritas para quem lê o código saber que a ausência é
     * proposital, e não esquecimento: dinheiro entrando ou saindo, desconto e
     * cancelamento continuam sendo decisão de gente.
     */
    public static final List<String> NUNCA = List.of(
            "dar baixa em documento",
            "conceder desconto",
            "cancelar cobrança",
            "fechar acordo sozinho",
            "mexer em contas a pagar",
            "mandar dinheiro para qualquer lugar");
}
