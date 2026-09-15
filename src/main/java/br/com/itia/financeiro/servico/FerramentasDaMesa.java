package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.AcordoRepositorio;
import br.com.itia.financeiro.repositorio.CasoDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * As ferramentas que quem cobra usa o dia inteiro.
 *
 * Tudo aqui é de cobrança: segunda via, saldo somado, simulação de
 * parcelamento, promessa, retorno, ligação, nota interna, pausa da régua e
 * pedido de bloqueio. Nada aqui manda dinheiro nem dá baixa: essas duas coisas
 * continuam em outro lugar, com outra alçada.
 */
@Service
public class FerramentasDaMesa {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final TituloRepositorio titulos;
    private final ClienteRepositorio clientes;
    private final CasoDeCobrancaRepositorio casos;
    private final InteracaoRepositorio interacoes;
    private final AcordoRepositorio acordos;
    private final Regras regras;
    private final ContextoEmpresa contexto;

    public FerramentasDaMesa(TituloRepositorio titulos, ClienteRepositorio clientes,
                             CasoDeCobrancaRepositorio casos, InteracaoRepositorio interacoes,
                             AcordoRepositorio acordos, Regras regras,
                             ContextoEmpresa contexto) {
        this.titulos = titulos;
        this.clientes = clientes;
        this.casos = casos;
        this.interacoes = interacoes;
        this.acordos = acordos;
        this.regras = regras;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ listas

    public List<Titulo> emAberto(UUID unidadeId) {
        return titulos.findByClienteIdOrderByVencimentoDesc(unidadeId).stream()
                .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                        || t.getSituacao() == SituacaoTitulo.PARCIAL)
                .sorted(Comparator.comparing(Titulo::getVencimento))
                .toList();
    }

    /** Uma linha da aba de pagamentos. */
    public record Entrada(LocalDate quando, String documento, BigDecimal valor, long atraso,
                          String forma) {
    }

    public List<Entrada> pagamentos(UUID unidadeId) {
        List<Entrada> entradas = new ArrayList<>();
        for (Titulo titulo : titulos.findByClienteIdOrderByVencimentoDesc(unidadeId)) {
            for (Pagamento pagamento : titulo.getPagamentos()) {
                entradas.add(new Entrada(pagamento.getPagoEm(), titulo.getIdentificadorPix(),
                        pagamento.getValor(),
                        pagamento.getPagoEm() == null ? 0
                                : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                                titulo.getVencimento(), pagamento.getPagoEm())),
                        pagamento.getForma()));
            }
        }
        entradas.sort(Comparator.comparing(Entrada::quando,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return entradas;
    }

    public List<Acordo> acordosDe(UUID unidadeId) {
        return acordos.findByEmpresaIdAndUnidadeIdOrderByCriadoEmDesc(
                contexto.exigirEmpresaId(), unidadeId);
    }

    /** As outras unidades do mesmo dono, com o saldo de cada uma. */
    public Map<String, BigDecimal> unidadesDoMesmoDono(UUID unidadeId) {
        Map<String, BigDecimal> unidades = new LinkedHashMap<>();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId,
                contexto.exigirEmpresaId()).orElse(null);
        if (cliente == null || cliente.getPagador() == null) {
            return unidades;
        }
        for (ClienteEspelho irma : clientes.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(
                contexto.exigirEmpresaId())) {
            if (irma.getPagador() != null
                    && irma.getPagador().getId().equals(cliente.getPagador().getId())) {
                unidades.put(irma.getRazaoSocial(), somar(emAberto(irma.getId())));
            }
        }
        return unidades;
    }

    private BigDecimal somar(List<Titulo> lista) {
        return lista.stream().map(Titulo::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --------------------------------------------------------------- textos

    /**
     * Os textos prontos de cobrança, já com os números deste cliente.
     *
     * São os comandos que a pessoa digita com barra na caixa de escrever.
     */
    public Map<String, String> comandos(UUID unidadeId, LocalDate hoje) {
        Empresa empresa = contexto.exigirEmpresa();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId,
                        contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        List<Titulo> abertos = emAberto(unidadeId);
        BigDecimal total = somar(abertos);
        BigDecimal atualizado = total;
        for (Titulo titulo : abertos) {
            atualizado = atualizado.add(regras.daEmpresa().acrescimoDe(titulo.getSaldo(),
                    titulo.getVencimento(), hoje));
        }
        String chave = empresa.getChavePix() == null || empresa.getChavePix().isBlank()
                ? "(a empresa ainda não tem chave PIX cadastrada)" : empresa.getChavePix();
        String nome = primeiroNome(cliente);

        StringBuilder lista = new StringBuilder();
        for (Titulo titulo : abertos) {
            lista.append("· ").append(titulo.getCompetencia() == null ? titulo.getDescricao()
                            : titulo.getCompetencia().format(MES))
                    .append(": ").append(MontadorDeCobranca.dinheiro(titulo.getSaldo()))
                    .append(", vence em ").append(titulo.getVencimento().format(BR));
            long atraso = titulo.diasDeAtraso(hoje);
            if (atraso > 0) {
                lista.append(" (").append(atraso).append(" dias)");
            }
            lista.append("\n");
        }

        Map<String, String> comandos = new LinkedHashMap<>();
        comandos.put("saldo", "Oi " + nome + "! Hoje consta em aberto:\n"
                + (lista.length() == 0 ? "nada em aberto.\n" : lista)
                + "\nTotal: " + MontadorDeCobranca.dinheiro(total) + ".");
        comandos.put("pix", "A chave PIX é " + chave + "."
                + (abertos.isEmpty() ? ""
                : "\nColoque na descrição o código " + abertos.get(0).getIdentificadorPix()
                        + ": assim a baixa cai sozinha e ninguém precisa conferir comprovante."));
        comandos.put("atualizado", "Com os encargos até hoje, o total fica "
                + MontadorDeCobranca.dinheiro(atualizado) + ".");
        comandos.put("2via", abertos.isEmpty() ? "Não há documento em aberto para reenviar."
                : "Segue a segunda via:\n" + segundaVia(abertos.get(0), chave));
        comandos.put("tudo", "Oi " + nome + ", segue o resumo de tudo que está em aberto:\n"
                + lista + "\nTotal: " + MontadorDeCobranca.dinheiro(total)
                + "\nChave PIX: " + chave);
        comandos.put("promessa", "Consegue me dizer um dia para eu anotar aqui? Assim eu"
                + " seguro a cobrança até lá.");
        comandos.put("comprovante", "Se já tiver pago, me manda o comprovante por aqui que eu"
                + " confiro e dou baixa na hora.");
        comandos.put("acordo", "Dá para dividir esse valor em parcelas. Me diz em quantas vezes"
                + " fica bom que eu monto e te mando para confirmar.");

        Map<String, BigDecimal> unidades = unidadesDoMesmoDono(unidadeId);
        if (unidades.size() > 1) {
            StringBuilder porUnidade = new StringBuilder("Somando as empresas:\n");
            for (Map.Entry<String, BigDecimal> linha : unidades.entrySet()) {
                porUnidade.append("· ").append(linha.getKey()).append(": ")
                        .append(MontadorDeCobranca.dinheiro(linha.getValue())).append("\n");
            }
            comandos.put("unidades", porUnidade.toString());
        }
        return comandos;
    }

    private String segundaVia(Titulo titulo, String chave) {
        return "Documento " + titulo.getIdentificadorPix() + "\n"
                + titulo.getDescricao() + "\n"
                + "Valor: " + MontadorDeCobranca.dinheiro(titulo.getSaldo()) + "\n"
                + "Vencimento: " + titulo.getVencimento().format(BR) + "\n"
                + "Chave PIX: " + chave + "\n"
                + "Código para a descrição: " + titulo.getIdentificadorPix();
    }

    private String primeiroNome(ClienteEspelho cliente) {
        String quem = cliente.getResponsavel() == null || cliente.getResponsavel().isBlank()
                ? cliente.getRazaoSocial() : cliente.getResponsavel();
        return quem.split("\\s+")[0];
    }

    /**
     * Simula um parcelamento sem criar nada.
     *
     * Serve para a pessoa falar número certo na conversa antes de montar o
     * acordo de verdade.
     */
    public String simular(UUID unidadeId, int vezes, LocalDate primeiro, LocalDate hoje) {
        if (vezes < 1 || vezes > 24) {
            throw new IllegalArgumentException("O parcelamento vai de uma a vinte e quatro vezes.");
        }
        List<Titulo> abertos = emAberto(unidadeId);
        BigDecimal total = somar(abertos);
        for (Titulo titulo : abertos) {
            total = total.add(regras.daEmpresa().acrescimoDe(titulo.getSaldo(),
                    titulo.getVencimento(), hoje));
        }
        if (total.signum() <= 0) {
            return "Este cliente não tem nada em aberto para parcelar.";
        }
        BigDecimal cada = total.divide(BigDecimal.valueOf(vezes), 2, RoundingMode.DOWN);
        BigDecimal ultima = total.subtract(cada.multiply(BigDecimal.valueOf(vezes - 1)));
        LocalDate data = primeiro == null ? hoje.plusDays(7) : primeiro;

        StringBuilder texto = new StringBuilder();
        texto.append("Fechando assim: ").append(vezes).append(" parcelas");
        if (cada.compareTo(ultima) == 0) {
            texto.append(" de ").append(MontadorDeCobranca.dinheiro(cada));
        } else {
            texto.append(" de ").append(MontadorDeCobranca.dinheiro(cada))
                    .append(" e a última de ").append(MontadorDeCobranca.dinheiro(ultima));
        }
        texto.append(", a primeira em ").append(data.format(BR)).append(".");
        texto.append("\nTotal: ").append(MontadorDeCobranca.dinheiro(total)).append(".");
        texto.append("\nConfirma que eu monto o acordo?");
        return texto.toString();
    }

    // ---------------------------------------------------------------- acoes

    @Transactional
    public void registrarLigacao(UUID unidadeId, String oQueFalou) {
        anotar(unidadeId, "LIGACAO", oQueFalou == null || oQueFalou.isBlank()
                ? "Ligação feita." : oQueFalou, "TELEFONE");
    }

    @Transactional
    public void notaInterna(UUID unidadeId, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escreva a nota antes de guardar.");
        }
        anotar(unidadeId, "OBSERVACAO", texto, "INTERNO");
    }

    @Transactional
    public void pedirBloqueio(UUID unidadeId, String motivo) {
        anotar(unidadeId, "BLOQUEIO", motivo == null || motivo.isBlank()
                ? "Bloqueio pedido pela cobrança." : motivo, "INTERNO");
        CasoDeCobranca caso = caso(unidadeId);
        caso.ajustar(caso.getSituacao(), caso.getResponsavel(),
                "acompanhar o pedido de bloqueio", LocalDate.now().plusDays(2),
                caso.getObservacao(), contexto.autor());
        casos.save(caso);
    }

    @Transactional
    public void pausarRegua(UUID unidadeId, LocalDate ate, String motivo) {
        CasoDeCobranca caso = caso(unidadeId);
        caso.pausar(ate, motivo, contexto.autor());
        casos.save(caso);
        anotar(unidadeId, "OBSERVACAO", "Régua pausada até " + ate.format(BR)
                + (motivo == null || motivo.isBlank() ? "" : ": " + motivo), "INTERNO");
    }

    @Transactional
    public void voltarACobrar(UUID unidadeId) {
        CasoDeCobranca caso = caso(unidadeId);
        caso.voltarACobrar(contexto.autor());
        casos.save(caso);
        anotar(unidadeId, "OBSERVACAO", "Régua religada.", "INTERNO");
    }

    @Transactional
    public void agendarRetorno(UUID unidadeId, LocalDate quando, String oQueFazer) {
        if (quando == null) {
            throw new IllegalArgumentException("Diga o dia do retorno.");
        }
        CasoDeCobranca caso = caso(unidadeId);
        caso.ajustar(caso.getSituacao(), contexto.autor(),
                oQueFazer == null || oQueFazer.isBlank() ? "voltar a falar com o cliente"
                        : oQueFazer, quando, caso.getObservacao(), contexto.autor());
        casos.save(caso);
    }

    @Transactional
    public void transferir(UUID unidadeId, String paraQuem) {
        if (paraQuem == null || paraQuem.isBlank()) {
            throw new IllegalArgumentException("Diga para quem vai o caso.");
        }
        CasoDeCobranca caso = caso(unidadeId);
        caso.ajustar(caso.getSituacao(), paraQuem.trim(), caso.getProximaAcao(),
                caso.getProximaData(), caso.getObservacao(), contexto.autor());
        casos.save(caso);
        anotar(unidadeId, "OBSERVACAO", "Caso passado para " + paraQuem.trim() + ".", "INTERNO");
    }

    private CasoDeCobranca caso(UUID unidadeId) {
        UUID empresaId = contexto.exigirEmpresaId();
        return casos.findByEmpresaIdAndUnidadeId(empresaId, unidadeId)
                .orElseGet(() -> new CasoDeCobranca(empresaId, unidadeId));
    }

    private void anotar(UUID unidadeId, String tipo, String descricao, String canal) {
        UUID empresaId = contexto.exigirEmpresaId();
        ClienteEspelho cliente = clientes.findByIdAndEmpresaId(unidadeId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        interacoes.save(new Interacao(empresaId, cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), unidadeId, tipo, descricao, null, null,
                canal, contexto.autor()));
    }
}
