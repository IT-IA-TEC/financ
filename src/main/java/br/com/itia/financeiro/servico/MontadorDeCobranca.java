package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Contato;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.ContatoRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem monta uma mensagem de cobrança a partir de um documento.
 *
 * Fica separado de propósito: o disparo em lote e a régua de cadência usam
 * exatamente as mesmas regras de escolha de destino e de preenchimento do
 * texto. Se fossem dois códigos parecidos, um dia iam divergir, e a empresa
 * mandaria textos diferentes pelo mesmo motivo.
 *
 * Nada aqui usa a sessão: a empresa vem sempre de fora, porque a régua roda
 * sozinha, de madrugada, sem ninguém logado.
 */
@Service
public class MontadorDeCobranca {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContatoRepositorio contatos;
    private final MensagemRepositorio mensagens;

    public MontadorDeCobranca(ContatoRepositorio contatos, MensagemRepositorio mensagens) {
        this.contatos = contatos;
        this.mensagens = mensagens;
    }

    /**
     * O que saiu da montagem: ou uma mensagem pronta, ou o motivo de ficar de fora.
     */
    public record Pronta(Mensagem mensagem, String motivoDeFora) {
        public boolean entrou() {
            return mensagem != null;
        }

        static Pronta fora(String motivo) {
            return new Pronta(null, motivo);
        }
    }

    /**
     * Monta a mensagem de um título, ou diz por que ele não entra.
     *
     * A mensagem volta gravada, para que quem chamou só precise ligar ao lote.
     */
    public Pronta montar(Empresa empresa, Titulo titulo, ModeloDeMensagem modelo, String canal,
                         RegraDaEmpresa regra, String autor, LocalDate hoje) {
        UUID empresaId = empresa.getId();
        ClienteEspelho cliente = titulo.getCliente();
        OffsetDateTime inicioDoDia = hoje.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        if (mensagens.quantasJaForam(empresaId, titulo.getId(), canal, inicioDoDia) > 0) {
            return Pronta.fora("já recebeu mensagem deste documento hoje");
        }

        Escolhido escolhido = escolherDestino(cliente, canal);
        if (escolhido.motivo() != null) {
            return Pronta.fora(escolhido.motivo());
        }

        Map<String, String> valores = valoresDe(titulo, cliente, empresa, escolhido.nome(),
                regra, hoje);
        Mensagem mensagem = new Mensagem(empresaId, canal, "SAIDA", escolhido.destino(),
                modelo.montar(valores), autor);
        mensagem.ligarAoCliente(cliente.getPagador() == null ? null
                : cliente.getPagador().getId(), cliente.getId(), escolhido.contatoId());
        mensagem.ligarAoDocumento(titulo.getId(), null);
        String assunto = modelo.montarAssunto(valores);
        if (assunto != null) {
            mensagem.definirAssunto(assunto);
        }
        return new Pronta(mensagens.save(mensagem), null);
    }

    /** Para quem a mensagem vai, ou o motivo de não ir. */
    public record Escolhido(UUID contatoId, String nome, String destino, String motivo) {
        static Escolhido fora(String motivo) {
            return new Escolhido(null, null, null, motivo);
        }
    }

    public Escolhido escolherDestino(ClienteEspelho cliente, String canal) {
        List<Contato> lista = new ArrayList<>(
                contatos.findByUnidadeIdOrderByPrioridade(cliente.getId()));
        if (cliente.getPagador() != null) {
            lista.addAll(contatos.findByPagadorIdOrderByPrioridade(cliente.getPagador().getId()));
        }

        boolean algumRecusou = false;
        for (Contato contato : lista) {
            if (!contato.isAtivo()) {
                continue;
            }
            if (!contato.isAceitaCobranca()) {
                algumRecusou = true;
                continue;
            }
            String destino = "EMAIL".equals(canal) ? contato.getEmail() : contato.getWhatsapp();
            if (destino != null && !destino.isBlank()) {
                return new Escolhido(contato.getId(), contato.getNome(), destino.trim(), null);
            }
        }

        // Sem contato cadastrado, tenta o que está na ficha do cliente.
        String daFicha = "EMAIL".equals(canal) ? cliente.getEmail() : cliente.getTelefone();
        if (daFicha != null && !daFicha.isBlank()) {
            String nome = cliente.getResponsavel() == null || cliente.getResponsavel().isBlank()
                    ? cliente.getRazaoSocial() : cliente.getResponsavel();
            return new Escolhido(null, nome, daFicha.trim(), null);
        }

        if (algumRecusou) {
            return Escolhido.fora("contato pediu para não ser cobrado por aqui");
        }
        return Escolhido.fora("EMAIL".equals(canal)
                ? "sem e-mail cadastrado" : "sem WhatsApp cadastrado");
    }

    private Map<String, String> valoresDe(Titulo titulo, ClienteEspelho cliente, Empresa empresa,
                                          String nomeDoContato, RegraDaEmpresa regra,
                                          LocalDate hoje) {
        BigDecimal saldo = titulo.getSaldo();
        BigDecimal acrescimo = regra.acrescimoDe(saldo, titulo.getVencimento(), hoje);
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("cliente", cliente.getRazaoSocial());
        valores.put("contato", nomeDoContato == null ? cliente.getRazaoSocial() : nomeDoContato);
        valores.put("empresa", empresa.getNome());
        valores.put("numero", titulo.getIdentificadorPix() == null
                ? String.valueOf(titulo.getNumero()) : titulo.getIdentificadorPix());
        valores.put("descricao", titulo.getDescricao());
        valores.put("valor", dinheiro(saldo));
        valores.put("valor_atualizado", dinheiro(saldo.add(acrescimo)));
        valores.put("vencimento", titulo.getVencimento().format(BR));
        valores.put("atraso", String.valueOf(Math.max(0, titulo.diasDeAtraso(hoje))));
        valores.put("chave_pix", empresa.getChavePix() == null
                ? "(chave PIX não cadastrada)" : empresa.getChavePix());
        valores.put("identificador", titulo.getIdentificadorPix() == null
                ? "" : titulo.getIdentificadorPix());
        return valores;
    }

    public static String dinheiro(BigDecimal valor) {
        return "R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.2f", valor);
    }
}
