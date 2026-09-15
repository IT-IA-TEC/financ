package br.com.itia.financeiro.dominio;

import java.util.Arrays;
import java.util.List;

/**
 * O catalogo de colunas da tela de clientes.
 *
 * So existe coluna aqui para campo que o sistema realmente tem para mostrar.
 * Nao da para inventar uma coluna com nome solto: se o campo nao existe, ele
 * nao aparece nesta lista.
 *
 * Cada coluna pertence a um nivel. As de PESSOA ficam junto do nome, ocupando
 * a altura de todas as unidades daquela pessoa. As de UNIDADE ficam na linha
 * da propria unidade.
 */
public enum ColunaCarteira {

    // ------------------------------------------------------------ da pessoa
    CLIENTE("Cliente", Nivel.PESSOA, Formato.TEXTO, true),
    PESSOA_DOCUMENTOS("Unidades da pessoa", Nivel.PESSOA, Formato.TEXTO, false),
    PESSOA_WHATSAPP("WhatsApp", Nivel.PESSOA, Formato.TEXTO, false),
    PESSOA_TELEFONE("Telefone", Nivel.PESSOA, Formato.TEXTO, false),
    PESSOA_EMAIL("E-mail da pessoa", Nivel.PESSOA, Formato.TEXTO, false),
    PESSOA_EM_ABERTO("Em aberto (total)", Nivel.PESSOA, Formato.DINHEIRO, false),
    PESSOA_VENCIDO("Vencido (total)", Nivel.PESSOA, Formato.DINHEIRO, false),
    PESSOA_SITUACAO("Situação geral", Nivel.PESSOA, Formato.SITUACAO, false),
    PESSOA_CLIENTE_DESDE("Cliente desde", Nivel.PESSOA, Formato.DATA, false),

    // ----------------------------------------------------------- da unidade
    UNIDADE("Unidade", Nivel.UNIDADE, Formato.TEXTO, true),
    UNIDADE_DOCUMENTO("Documento", Nivel.UNIDADE, Formato.TEXTO, false),
    UNIDADE_CODIGO("Código de origem", Nivel.UNIDADE, Formato.TEXTO, false),
    UNIDADE_SITUACAO("Situação", Nivel.UNIDADE, Formato.SITUACAO, false),
    UNIDADE_EM_ABERTO("Em aberto", Nivel.UNIDADE, Formato.DINHEIRO, false),
    UNIDADE_VENCIDO("Vencido", Nivel.UNIDADE, Formato.DINHEIRO, false),
    UNIDADE_ATRASO("Atraso em dias", Nivel.UNIDADE, Formato.NUMERO, false),
    UNIDADE_PROXIMO_VENCIMENTO("Próximo vencimento", Nivel.UNIDADE, Formato.DATA, false),
    UNIDADE_ULTIMO_RECEBIMENTO("Último recebimento", Nivel.UNIDADE, Formato.DATA, false),
    UNIDADE_TITULOS("Títulos", Nivel.UNIDADE, Formato.NUMERO, false),
    UNIDADE_RESPONSAVEL("Responsável", Nivel.UNIDADE, Formato.TEXTO, false),
    UNIDADE_TELEFONE("Telefone da unidade", Nivel.UNIDADE, Formato.TEXTO, false),
    UNIDADE_EMAIL("E-mail da unidade", Nivel.UNIDADE, Formato.TEXTO, false);

    /** Onde a coluna vive: junto da pessoa ou junto da unidade. */
    public enum Nivel { PESSOA, UNIDADE }

    /** Como o valor e desenhado na celula. */
    public enum Formato { TEXTO, DINHEIRO, SITUACAO, DATA, NUMERO }

    private final String rotulo;
    private final Nivel nivel;
    private final Formato formato;
    private final boolean fixa;

    ColunaCarteira(String rotulo, Nivel nivel, Formato formato, boolean fixa) {
        this.rotulo = rotulo;
        this.nivel = nivel;
        this.formato = formato;
        this.fixa = fixa;
    }

    public String getRotulo() {
        return rotulo;
    }

    public Nivel getNivel() {
        return nivel;
    }

    public Formato getFormato() {
        return formato;
    }

    /** Coluna fixa nao pode ser tirada: sem ela a tabela perde o sentido. */
    public boolean isFixa() {
        return fixa;
    }

    public boolean ehDaPessoa() {
        return nivel == Nivel.PESSOA;
    }

    public boolean alinhaADireita() {
        return formato == Formato.DINHEIRO || formato == Formato.NUMERO;
    }

    /** As situacoes possiveis, para o filtro da coluna de situacao. */
    public static List<String> situacoes() {
        return List.of("em dia", "em atraso", "crítico", "sem débito");
    }

    /** O que a tela mostra quando ninguem escolheu nada ainda. */
    public static List<ColunaCarteira> padrao() {
        return List.of(CLIENTE, UNIDADE, UNIDADE_DOCUMENTO, UNIDADE_SITUACAO,
                UNIDADE_EM_ABERTO, UNIDADE_VENCIDO, UNIDADE_ATRASO,
                UNIDADE_PROXIMO_VENCIMENTO, UNIDADE_ULTIMO_RECEBIMENTO, PESSOA_WHATSAPP);
    }

    public static List<ColunaCarteira> daPessoa() {
        return Arrays.stream(values()).filter(ColunaCarteira::ehDaPessoa).toList();
    }

    public static List<ColunaCarteira> daUnidade() {
        return Arrays.stream(values()).filter(c -> !c.ehDaPessoa()).toList();
    }
}
