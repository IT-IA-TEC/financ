package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.AnotacaoDaTarefa;
import br.com.itia.financeiro.dominio.EventoIntegracao;
import br.com.itia.financeiro.dominio.Tarefa;
import br.com.itia.financeiro.repositorio.AnotacaoDaTarefaRepositorio;
import br.com.itia.financeiro.repositorio.TarefaRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * A tarefa que o outro sistema empurra para cá.
 *
 * Fica separado de quem busca de propósito: quem recebe não precisa saber
 * chamar ninguém de fora, e assim os dois lados não ficam dependendo um do
 * outro em círculo.
 *
 * O que este serviço protege:
 *   1. Roda sem ninguém logado: quem chama é o outro sistema, e a empresa vem
 *      do endereço do webhook.
 *   2. O mesmo identificador nunca vira duas tarefas: a segunda vez atualiza.
 *   3. Nunca deixa a exceção subir: o outro sistema não pode receber erro
 *      nosso por causa de um aviso que não soubemos ler.
 */
@Service
public class EntradaDeTarefas {

    private static final Logger LOG = LoggerFactory.getLogger(EntradaDeTarefas.class);

    private final TarefaRepositorio tarefas;
    private final AnotacaoDaTarefaRepositorio anotacoes;
    private final ObjectMapper json = new ObjectMapper();

    public EntradaDeTarefas(TarefaRepositorio tarefas, AnotacaoDaTarefaRepositorio anotacoes) {
        this.tarefas = tarefas;
        this.anotacoes = anotacoes;
    }

    /** Lê o que chegou pelo webhook e vira tarefa na Cúpula. */
    @Transactional
    public void receber(EventoIntegracao evento, UUID empresaId) {
        try {
            JsonNode corpo = json.readTree(evento.getCarga() == null ? "{}" : evento.getCarga());
            JsonNode alvo = corpo.has("tarefa") ? corpo.get("tarefa") : corpo;

            String idExterno = primeiro(alvo, "id_externo", "id", "ticket_id", "codigo");
            String titulo = primeiro(alvo, "titulo", "title", "assunto", "nome");
            if (idExterno == null || titulo == null) {
                return;
            }
            String descricao = primeiro(alvo, "descricao", "description", "detalhe");
            String situacao = traduzirSituacao(primeiro(alvo, "situacao", "status", "estado"));
            String responsavel = primeiro(alvo, "responsavel", "assignee", "quem");
            LocalDate prazo = data(primeiro(alvo, "prazo", "due_date", "vencimento"));
            String prioridade = traduzirPrioridade(primeiro(alvo, "prioridade", "priority"));
            String link = primeiro(alvo, "link", "url", "link_externo");
            String setor = primeiro(alvo, "setor", "departamento");

            guardar(empresaId, idExterno, titulo, descricao, situacao, responsavel, prazo,
                    prioridade, link, setor, null, "API", "sistema de fora");
            evento.marcarProcessado();
        } catch (Exception erro) {
            LOG.debug("não consegui ler a tarefa que chegou: {}", erro.getMessage());
        }
    }

    /**
     * Grava a tarefa que veio de fora, criando ou atualizando.
     *
     * Devolve true quando a tarefa é nova, para quem chamou saber contar.
     */
    @Transactional
    public boolean guardar(UUID empresaId, String idExterno, String titulo, String descricao,
                           String situacao, String responsavel, LocalDate prazo,
                           String prioridade, String link, String setor, UUID fonteId,
                           String origem, String quemPediu) {
        Optional<Tarefa> jaTem = tarefas.findByEmpresaIdAndIdExterno(empresaId, idExterno);
        if (jaTem.isPresent()) {
            Tarefa tarefa = jaTem.get();
            tarefa.atualizarDeFora(titulo, descricao, situacao, responsavel, prazo, prioridade,
                    link);
            tarefas.save(tarefa);
            return false;
        }
        Tarefa tarefa = new Tarefa(empresaId, tarefas.proximoNumero(empresaId), titulo,
                descricao, prioridade == null ? "NORMAL" : prioridade, responsavel,
                setor == null ? "Financeiro" : setor, prazo, null, quemPediu);
        tarefa.veioDeFora(fonteId, idExterno, origem);
        if (situacao != null || link != null) {
            tarefa.atualizarDeFora(null, null, situacao, null, null, null, link);
        }
        tarefas.save(tarefa);
        anotacoes.save(new AnotacaoDaTarefa(tarefa.getId(), empresaId,
                "Chegou de outro sistema (identificador " + idExterno + ").", "SISTEMA",
                quemPediu));
        return true;
    }

    /** Se a tarefa mudou de verdade, para não anotar atualização à toa. */
    public String retrato(UUID empresaId, String idExterno) {
        return tarefas.findByEmpresaIdAndIdExterno(empresaId, idExterno)
                .map(t -> t.getSituacao() + "|" + t.getTitulo() + "|" + t.getResponsavel()
                        + "|" + t.getPrazo())
                .orElse(null);
    }

    @Transactional
    public void anotar(UUID tarefaId, UUID empresaId, String texto, String autor) {
        anotacoes.save(new AnotacaoDaTarefa(tarefaId, empresaId, texto, "SISTEMA", autor));
    }

    public Optional<Tarefa> porIdExterno(UUID empresaId, String idExterno) {
        return tarefas.findByEmpresaIdAndIdExterno(empresaId, idExterno);
    }

    // ------------------------------------------------------------------ apoio

    public String texto(JsonNode linha, String campo) {
        if (campo == null || campo.isBlank()) {
            return null;
        }
        JsonNode valor = linha.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        String lido = valor.asText().trim();
        return lido.isEmpty() ? null : lido;
    }

    private String primeiro(JsonNode alvo, String... campos) {
        for (String campo : campos) {
            String valor = texto(alvo, campo);
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    /** Traduz o nome do estado de lá para o nosso, sem inventar. */
    public String traduzirSituacao(String deLa) {
        if (deLa == null) {
            return null;
        }
        String limpo = deLa.toLowerCase().replace('-', '_').replace(' ', '_');
        return switch (limpo) {
            case "a_fazer", "aberto", "aberta", "todo", "pendente", "nova" -> "A_FAZER";
            case "em_andamento", "andamento", "fazendo", "doing", "em_execucao" ->
                    "EM_ANDAMENTO";
            case "aguardando_revisao", "revisao", "em_revisao", "review" ->
                    "AGUARDANDO_REVISAO";
            case "concluida", "concluido", "feito", "done", "finalizada", "finalizado" ->
                    "CONCLUIDA";
            case "cancelada", "cancelado", "canceled", "cancelled" -> "CANCELADA";
            default -> null;
        };
    }

    public String traduzirPrioridade(String deLa) {
        if (deLa == null) {
            return null;
        }
        return switch (deLa.toLowerCase()) {
            case "baixa", "low" -> "BAIXA";
            case "alta", "high" -> "ALTA";
            case "critica", "crítica", "urgente", "critical" -> "CRITICA";
            case "normal", "media", "média", "medium" -> "NORMAL";
            default -> null;
        };
    }

    /** Aceita data solta e data com hora, que é como os sistemas costumam mandar. */
    public LocalDate data(String valor) {
        if (valor == null || valor.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(valor.substring(0, 10));
        } catch (RuntimeException erro) {
            return null;
        }
    }
}
