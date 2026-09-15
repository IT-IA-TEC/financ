package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Contato;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.ForaDoLote;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.ContatoRepositorio;
import br.com.itia.financeiro.repositorio.ForaDoLoteRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.LoteDeMensagemRepositorio;
import br.com.itia.financeiro.repositorio.MensagemRepositorio;
import br.com.itia.financeiro.repositorio.ModeloDeMensagemRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O disparo de cobrança em lote.
 *
 * As regras que este serviço protege:
 *   1. Nada sai sem prévia. O lote nasce mostrando quem entra, quem fica de
 *      fora e por quê, e só sai da prévia com alguém confirmando.
 *   2. Quem pediu para não ser cobrado não é cobrado. Contato sem aceite fica
 *      de fora, com o motivo escrito.
 *   3. O mesmo documento não vai duas vezes no mesmo dia pelo mesmo canal.
 *   4. O texto sai montado e gravado. O modelo pode mudar depois, a prova do
 *      que foi dito não muda.
 *   5. Cada mensagem que sai vira uma linha na história do cliente.
 */
@Service
public class Mensagens {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ModeloDeMensagemRepositorio modelos;
    private final LoteDeMensagemRepositorio lotes;
    private final MensagemRepositorio mensagens;
    private final ForaDoLoteRepositorio foras;
    private final TituloRepositorio titulos;
    private final ContatoRepositorio contatos;
    private final InteracaoRepositorio interacoes;
    private final Regras regras;
    private final MontadorDeCobranca montador;
    private final ContextoEmpresa contexto;

    public Mensagens(ModeloDeMensagemRepositorio modelos, LoteDeMensagemRepositorio lotes,
                     MensagemRepositorio mensagens, ForaDoLoteRepositorio foras,
                     TituloRepositorio titulos, ContatoRepositorio contatos,
                     InteracaoRepositorio interacoes, Regras regras,
                     MontadorDeCobranca montador, ContextoEmpresa contexto) {
        this.modelos = modelos;
        this.lotes = lotes;
        this.mensagens = mensagens;
        this.foras = foras;
        this.titulos = titulos;
        this.contatos = contatos;
        this.interacoes = interacoes;
        this.regras = regras;
        this.montador = montador;
        this.contexto = contexto;
    }

    // ---------------------------------------------------------------- modelos

    public List<ModeloDeMensagem> modelos() {
        return modelos.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public List<ModeloDeMensagem> modelosAtivos() {
        return modelos.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public ModeloDeMensagem modelo(UUID id) {
        return modelos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Modelo não encontrado."));
    }

    @Transactional
    public ModeloDeMensagem salvarModelo(UUID id, String nome, String canal, String assunto,
                                         String corpo, String tom, String momento,
                                         boolean ativo) {
        UUID empresaId = contexto.exigirEmpresaId();
        ModeloDeMensagem modelo;
        if (id == null) {
            modelo = new ModeloDeMensagem(empresaId, nome, canal, assunto, corpo, tom,
                    momento, contexto.autor());
        } else {
            modelo = modelo(id);
            modelo.ajustar(nome, canal, assunto, corpo, tom, momento, ativo);
        }
        List<String> desconhecidos = modelo.espacosDesconhecidos();
        if (!desconhecidos.isEmpty()) {
            throw new IllegalArgumentException("O texto usa espaço que não existe: "
                    + String.join(", ", desconhecidos)
                    + ". Use só os espaços da lista ao lado.");
        }
        return modelos.save(modelo);
    }

    @Transactional
    public void desativarModelo(UUID id) {
        ModeloDeMensagem modelo = modelo(id);
        modelo.desativar();
        modelos.save(modelo);
    }

    /** Três textos prontos, para a empresa não começar da folha em branco. */
    @Transactional
    public void prepararEmpresa() {
        UUID empresaId = contexto.exigirEmpresaId();
        if (modelos.countByEmpresaId(empresaId) > 0) {
            return;
        }
        String quem = contexto.autor();
        modelos.save(new ModeloDeMensagem(empresaId, "Aviso antes de vencer", "WHATSAPP", null,
                """
                Oi {contato}, tudo bem?

                Passando para lembrar do {descricao}, de {valor}, que vence em {vencimento}.

                A chave PIX é {chave_pix}.
                Qualquer dúvida é só responder por aqui.

                {empresa}""", "AMIGAVEL", "ANTES_VENCER", quem));
        modelos.save(new ModeloDeMensagem(empresaId, "Vence hoje", "WHATSAPP", null,
                """
                Oi {contato}, tudo bem?

                O {descricao}, de {valor}, vence hoje, {vencimento}.

                A chave PIX é {chave_pix}.
                Se já tiver pago, é só mandar o comprovante por aqui.

                {empresa}""", "AMIGAVEL", "NO_DIA", quem));
        modelos.save(new ModeloDeMensagem(empresaId, "Cobrança em atraso", "WHATSAPP", null,
                """
                Oi {contato}, tudo bem?

                O {descricao}, de {valor}, venceu em {vencimento} e está com {atraso} dias de atraso.
                Hoje o valor atualizado é {valor_atualizado}.

                A chave PIX é {chave_pix}.
                Se precisar combinar um prazo, me chama por aqui.

                {empresa}""", "FIRME", "APOS_VENCER", quem));
    }

    // ------------------------------------------------------------------ lotes

    public List<LoteDeMensagem> lotes() {
        return lotes.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId());
    }

    public LoteDeMensagem lote(UUID id) {
        return lotes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Disparo não encontrado."));
    }

    public List<Mensagem> mensagensDo(UUID loteId) {
        lote(loteId);
        return mensagens.findByLoteIdOrderByCriadoEm(loteId);
    }

    public List<ForaDoLote> forasDo(UUID loteId) {
        lote(loteId);
        return foras.findByLoteIdOrderByQuem(loteId);
    }

    /** O nome do cliente de cada título, para a tela não mostrar id. */
    public Map<UUID, String> nomesDosTitulos() {
        Map<UUID, String> nomes = new LinkedHashMap<>();
        for (Titulo titulo : titulos.findByEmpresaIdOrderByVencimentoDesc(
                contexto.exigirEmpresaId())) {
            nomes.put(titulo.getId(), titulo.getCliente().getRazaoSocial());
        }
        return nomes;
    }

    // ------------------------------------------------------------------ prévia

    /**
     * Monta a prévia do disparo.
     *
     * A visão diz o recorte: vencidos, a vencer nos próximos dias, faixa de
     * atraso ou tudo que está em aberto.
     */
    @Transactional
    public LoteDeMensagem montarPrevia(String nome, UUID modeloId, String canal, String visao,
                                       Integer de, Integer ate) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.OPERADOR);
        UUID empresaId = contexto.exigirEmpresaId();
        Empresa empresa = contexto.exigirEmpresa();
        ModeloDeMensagem modelo = modelo(modeloId);
        String canalUsado = canal == null || canal.isBlank() ? modelo.getCanal() : canal;
        LocalDate hoje = LocalDate.now();
        RegraDaEmpresa regra = regras.daEmpresa();

        LoteDeMensagem lote = new LoteDeMensagem(empresaId, nome, modeloId, canalUsado,
                descreverFiltro(visao, de, ate), contexto.autor());
        lotes.save(lote);

        List<Titulo> emAberto = titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(
                empresaId, List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL));

        int dentro = 0;
        int fora = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (Titulo titulo : emAberto) {
            if (!entraNoRecorte(titulo, visao, de, ate, hoje)) {
                continue;
            }
            ClienteEspelho cliente = titulo.getCliente();
            MontadorDeCobranca.Pronta pronta = montador.montar(empresa, titulo, modelo,
                    canalUsado, regra, contexto.autor(), hoje);
            if (!pronta.entrou()) {
                foras.save(new ForaDoLote(empresaId, lote.getId(), cliente.getId(),
                        titulo.getId(), cliente.getRazaoSocial(), pronta.motivoDeFora()));
                fora++;
                continue;
            }
            pronta.mensagem().ligarAoLote(lote.getId(), modeloId);
            mensagens.save(pronta.mensagem());
            dentro++;
            total = total.add(titulo.getSaldo());
        }

        lote.contar(dentro, fora, total);
        return lotes.save(lote);
    }

    private boolean entraNoRecorte(Titulo titulo, String visao, Integer de, Integer ate,
                                   LocalDate hoje) {
        long atraso = titulo.diasDeAtraso(hoje);
        long faltam = java.time.temporal.ChronoUnit.DAYS.between(hoje, titulo.getVencimento());
        return switch (visao == null ? "vencidos" : visao) {
            case "vencidos" -> atraso > 0;
            case "vence_em" -> faltam >= 0 && faltam <= (ate == null ? 7 : ate);
            case "no_dia" -> faltam == 0;
            case "faixa_de_atraso" -> atraso >= (de == null ? 1 : de)
                    && atraso <= (ate == null ? 30 : ate);
            default -> true;
        };
    }

    private String descreverFiltro(String visao, Integer de, Integer ate) {
        return switch (visao == null ? "vencidos" : visao) {
            case "vencidos" -> "tudo que está vencido";
            case "vence_em" -> "vence nos próximos " + (ate == null ? 7 : ate) + " dias";
            case "no_dia" -> "vence hoje";
            case "faixa_de_atraso" -> "atraso de " + (de == null ? 1 : de)
                    + " a " + (ate == null ? 30 : ate) + " dias";
            default -> "tudo que está em aberto";
        };
    }

    // ------------------------------------------------------- confirmar e soltar

    @Transactional
    public void confirmar(UUID loteId, LocalDate dia, LocalTime hora) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        LoteDeMensagem lote = lote(loteId);
        OffsetDateTime quando = dia == null ? null
                : dia.atTime(hora == null ? LocalTime.of(9, 0) : hora)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();
        lote.confirmar(contexto.autor(), quando);
        if (quando != null) {
            for (Mensagem mensagem : mensagens.findByLoteIdOrderByCriadoEm(loteId)) {
                if (mensagem.naFila()) {
                    mensagem.agendar(quando);
                    mensagens.save(mensagem);
                }
            }
        }
        lotes.save(lote);
    }

    @Transactional
    public void cancelarLote(UUID loteId) {
        LoteDeMensagem lote = lote(loteId);
        for (Mensagem mensagem : mensagens.findByLoteIdOrderByCriadoEm(loteId)) {
            if (mensagem.naFila()) {
                mensagem.cancelar("disparo cancelado");
                mensagens.save(mensagem);
            }
        }
        lote.cancelar();
        lotes.save(lote);
    }

    @Transactional
    public void tirarDoLote(UUID mensagemId) {
        Mensagem mensagem = mensagem(mensagemId);
        mensagem.cancelar("tirado do disparo antes de sair");
        mensagens.save(mensagem);
        recontar(mensagem.getLoteId());
    }

    public Mensagem mensagem(UUID id) {
        return mensagens.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));
    }

    private void recontar(UUID loteId) {
        if (loteId == null) {
            return;
        }
        LoteDeMensagem lote = lote(loteId);
        int dentro = 0;
        for (Mensagem mensagem : mensagens.findByLoteIdOrderByCriadoEm(loteId)) {
            if (!"CANCELADA".equals(mensagem.getSituacao())) {
                dentro++;
            }
        }
        lote.contar(dentro, lote.getFora(), lote.getValorTotal());
        lotes.save(lote);
    }

    /**
     * Marca que a mensagem saiu.
     *
     * Enquanto o canal do WhatsApp não está ligado, é isto que a equipe usa
     * depois de mandar pelo aparelho: o texto é o mesmo, e a história do
     * cliente fica registrada do mesmo jeito.
     */
    @Transactional
    public void marcarEnviada(UUID mensagemId, String idExterno) {
        Mensagem mensagem = mensagem(mensagemId);
        if (!mensagem.naFila()) {
            throw new IllegalStateException("Esta mensagem já saiu da fila.");
        }
        mensagem.marcarEnviada(idExterno);
        mensagens.save(mensagem);
        interacoes.save(new Interacao(mensagem.getEmpresaId(), mensagem.getPagadorId(),
                mensagem.getUnidadeId(), "COBRANCA_ENVIADA",
                "Mensagem enviada por " + mensagem.getCanal().toLowerCase()
                        + " para " + mensagem.getDestino(),
                null, null, mensagem.getCanal(), contexto.autor()));
        fecharSeAcabou(mensagem.getLoteId());
    }

    @Transactional
    public void marcarFalha(UUID mensagemId, String motivo) {
        Mensagem mensagem = mensagem(mensagemId);
        mensagem.marcarFalha(motivo == null || motivo.isBlank() ? "não foi entregue" : motivo);
        mensagens.save(mensagem);
        fecharSeAcabou(mensagem.getLoteId());
    }

    private void fecharSeAcabou(UUID loteId) {
        if (loteId == null) {
            return;
        }
        LoteDeMensagem lote = lote(loteId);
        if (!lote.confirmado()) {
            return;
        }
        boolean sobrou = mensagens.findByLoteIdOrderByCriadoEm(loteId).stream()
                .anyMatch(Mensagem::naFila);
        if (!sobrou) {
            lote.concluir();
            lotes.save(lote);
        }
    }

    // ------------------------------------------------------------------ painel

    public List<Mensagem> naFila() {
        return mensagens.findByEmpresaIdAndSituacaoOrderByCriadoEm(
                contexto.exigirEmpresaId(), "NA_FILA");
    }

    public long quantasNaFila() {
        return mensagens.countByEmpresaIdAndSituacao(contexto.exigirEmpresaId(), "NA_FILA");
    }
}
