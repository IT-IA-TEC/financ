package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.AcompanhaTarefa;
import br.com.itia.financeiro.dominio.AnotacaoDaTarefa;
import br.com.itia.financeiro.dominio.Tarefa;
import br.com.itia.financeiro.repositorio.AcompanhaTarefaRepositorio;
import br.com.itia.financeiro.repositorio.AnotacaoDaTarefaRepositorio;
import br.com.itia.financeiro.repositorio.TarefaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A Cúpula do IT.FC: as tarefas de quem trabalha aqui.
 *
 * É a mesma divisão que o pessoal do financeiro já conhece do sistema matriz:
 *
 *   Central        o que ainda não foi visto, em fila, do mais antigo para o
 *                  mais novo. Some sozinho quando a pessoa abre.
 *   Recebidas      o que é para eu fazer.
 *   Enviadas       o que eu pedi para alguém.
 *   Faço parte     o que eu acompanho sem ser o responsável.
 *   Do setor       o que está aberto esperando alguém pegar.
 *
 * O que este serviço protege:
 *   1. Tarefa não se apaga, se cancela, com motivo e autor.
 *   2. Quem faz não fecha sozinho: termina e fica esperando revisão de quem
 *      pediu, que aceita ou manda refazer explicando o que falta.
 *   3. Tarefa de setor não tem dono até alguém pegar, e some da lista de todos
 *      no momento em que alguém pega.
 *   4. Toda mudança que interessa vira uma linha no fio da tarefa.
 */
@Service
public class Cupula {

    private final TarefaRepositorio tarefas;
    private final AnotacaoDaTarefaRepositorio anotacoes;
    private final AcompanhaTarefaRepositorio acompanham;
    private final ContextoEmpresa contexto;

    public Cupula(TarefaRepositorio tarefas, AnotacaoDaTarefaRepositorio anotacoes,
                  AcompanhaTarefaRepositorio acompanham, ContextoEmpresa contexto) {
        this.tarefas = tarefas;
        this.anotacoes = anotacoes;
        this.acompanham = acompanham;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ listas

    public List<Tarefa> todas() {
        return tarefas.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId());
    }

    /**
     * A Central de pendências: o que ainda não foi visto, mais antigo primeiro.
     *
     * Entra o que é meu e ainda não abri, o que voltou para refazer, o que está
     * esperando a minha revisão e o que está aberto para o setor sem dono.
     */
    public List<Tarefa> central() {
        String eu = contexto.autor();
        List<Tarefa> fila = new ArrayList<>();
        for (Tarefa tarefa : todas()) {
            if (!tarefa.aberta()) {
                continue;
            }
            boolean minhaENova = eu.equals(tarefa.getResponsavel()) && tarefa.naoVista();
            boolean voltouParaMim = eu.equals(tarefa.getResponsavel())
                    && tarefa.voltouParaRefazer();
            boolean esperaMinhaRevisao = eu.equals(tarefa.getPedidaPor())
                    && tarefa.esperandoRevisao();
            boolean semDono = tarefa.doSetor();
            if (minhaENova || voltouParaMim || esperaMinhaRevisao || semDono) {
                fila.add(tarefa);
            }
        }
        fila.sort(Comparator.comparing(Tarefa::getCriadoEm));
        return fila;
    }

    /** O que é para eu fazer. */
    public List<Tarefa> recebidas() {
        String eu = contexto.autor();
        return todas().stream()
                .filter(t -> eu.equals(t.getResponsavel()))
                .toList();
    }

    /** O que eu pedi para alguém. */
    public List<Tarefa> enviadas() {
        String eu = contexto.autor();
        return todas().stream()
                .filter(t -> eu.equals(t.getPedidaPor()))
                .filter(t -> !eu.equals(t.getResponsavel()))
                .toList();
    }

    /** O que eu acompanho sem ser o responsável. */
    public List<Tarefa> facoParte() {
        String eu = contexto.autor();
        List<UUID> minhas = acompanham
                .findByEmpresaIdAndQuem(contexto.exigirEmpresaId(), eu).stream()
                .map(AcompanhaTarefa::getTarefaId)
                .toList();
        return todas().stream()
                .filter(t -> minhas.contains(t.getId()))
                .toList();
    }

    /**
     * Tudo que está em aberto no setor, com dono ou sem.
     *
     * É a mesma ideia de "solicitações ao departamento" do sistema matriz: dá
     * para ver o que o setor inteiro tem na mão, não só o que sobrou sem dono.
     * Sem isso, tarefa que chega de fora para outra pessoa não aparece para
     * ninguém que abra a tela.
     */
    public List<Tarefa> doSetor() {
        return todas().stream()
                .filter(Tarefa::aberta)
                .sorted(Comparator.comparing(Tarefa::doSetor).reversed()
                        .thenComparing(Tarefa::getPrazo,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Só as que ainda não têm dono, para a conta do topo e para a Central. */
    public List<Tarefa> semDono() {
        return todas().stream().filter(Tarefa::doSetor).toList();
    }

    /** Separa uma lista nas três colunas do quadro. */
    public Map<String, List<Tarefa>> emColunas(List<Tarefa> lista) {
        Map<String, List<Tarefa>> colunas = new LinkedHashMap<>();
        colunas.put("A_FAZER", new ArrayList<>());
        colunas.put("EM_ANDAMENTO", new ArrayList<>());
        colunas.put("CONCLUIDA", new ArrayList<>());
        for (Tarefa tarefa : lista) {
            colunas.get(tarefa.coluna()).add(tarefa);
        }
        for (List<Tarefa> coluna : colunas.values()) {
            coluna.sort(Comparator.comparing(Tarefa::getPrazo,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Tarefa::getCriadoEm));
        }
        return colunas;
    }

    public Tarefa tarefa(UUID id) {
        return tarefas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada."));
    }

    public List<AnotacaoDaTarefa> fioDe(UUID tarefaId) {
        tarefa(tarefaId);
        return anotacoes.findByTarefaIdOrderByCriadoEm(tarefaId);
    }

    public List<Tarefa> filhasDe(UUID tarefaId) {
        return tarefas.findByPaiIdOrderByCriadoEm(tarefaId);
    }

    public List<String> quemAcompanha(UUID tarefaId) {
        return acompanham.findByTarefaId(tarefaId).stream()
                .map(AcompanhaTarefa::getQuem)
                .toList();
    }

    /** Abrir a tarefa tira ela da Central. */
    @Transactional
    public Tarefa abrir(UUID id) {
        Tarefa tarefa = tarefa(id);
        tarefa.marcarVista();
        return tarefas.save(tarefa);
    }

    // ------------------------------------------------------------------ criar

    @Transactional
    public Tarefa criar(String titulo, String descricao, String prioridade, String responsavel,
                        String setor, LocalDate prazo, UUID paiId, List<String> acompanhantes) {
        return criar("TAREFA", titulo, descricao, prioridade, responsavel, setor, prazo, paiId,
                acompanhantes);
    }

    @Transactional
    public Tarefa criar(String tipo, String titulo, String descricao, String prioridade,
                        String responsavel, String setor, LocalDate prazo, UUID paiId,
                        List<String> acompanhantes) {
        UUID empresaId = contexto.exigirEmpresaId();
        Tarefa tarefa = new Tarefa(empresaId, tarefas.proximoNumero(empresaId), tipo, titulo,
                descricao, prioridade, responsavel, setor, prazo, paiId, contexto.autor());
        tarefas.save(tarefa);
        // a frase evita "Chamado criada": o rotulo muda de genero e o texto nao
        anotar(tarefa, contexto.autor() + " abriu " + tarefa.getCodigo() + " · "
                + tarefa.getRotuloDoTipo()
                + (responsavel == null || responsavel.isBlank()
                ? ", na fila do setor " + setor
                : ", com " + responsavel), "SISTEMA");

        if (acompanhantes != null) {
            for (String quem : acompanhantes) {
                if (quem != null && !quem.isBlank()) {
                    acompanham.save(new AcompanhaTarefa(tarefa.getId(), empresaId, quem.trim()));
                }
            }
        }
        return tarefa;
    }

    /** Um ticket da demanda: o que e para quem, com prazo proprio. */
    public record Ticket(String titulo, String responsavel, String setor, LocalDate prazo) {

        public boolean vazio() {
            return titulo == null || titulo.isBlank();
        }
    }

    /**
     * A demanda com varios tickets de uma vez.
     *
     * A demanda em si e a tarefa pai: ela guarda o nome e serve de guarda-chuva,
     * e cada ticket vira uma tarefa filha com o proprio dono e o proprio prazo.
     * Quem nao escolheu dono no ticket herda o setor da demanda, senao o ticket
     * nasceria sem ninguem para fazer.
     */
    @Transactional
    public Tarefa criarDemanda(String nome, String descricao, String prioridade, String setor,
                               LocalDate prazo, List<Ticket> tickets, List<String> acompanhantes) {
        List<Ticket> validos = tickets == null ? List.of()
                : tickets.stream().filter(t -> !t.vazio()).toList();
        if (validos.isEmpty()) {
            throw new IllegalArgumentException(
                    "A demanda precisa de pelo menos um ticket com titulo.");
        }
        Tarefa demanda = criar("DEMANDA", nome, descricao, prioridade, null, setor, prazo, null,
                acompanhantes);
        for (Ticket t : validos) {
            String comQuem = t.responsavel() == null || t.responsavel().isBlank() ? null
                    : t.responsavel();
            String paraOSetor = comQuem != null ? t.setor()
                    : (t.setor() == null || t.setor().isBlank() ? setor : t.setor());
            criar("TAREFA", t.titulo(), null, prioridade, comQuem, paraOSetor,
                    t.prazo() == null ? prazo : t.prazo(), demanda.getId(), acompanhantes);
        }
        anotar(demanda, validos.size() == 1 ? "1 ticket aberto nesta demanda."
                : validos.size() + " tickets abertos nesta demanda.", "SISTEMA");
        return demanda;
    }

    // ------------------------------------------------------------------ andar

    @Transactional
    public void pegarParaMim(UUID id) {
        Tarefa tarefa = tarefa(id);
        tarefa.pegarParaMim(contexto.autor());
        tarefas.save(tarefa);
        anotar(tarefa, contexto.autor() + " pegou esta tarefa.", "SISTEMA");
    }

    @Transactional
    public void comecar(UUID id) {
        Tarefa tarefa = tarefa(id);
        tarefa.comecar(contexto.autor());
        tarefas.save(tarefa);
        anotar(tarefa, "Trabalho começou.", "SISTEMA");
    }

    @Transactional
    public void terminar(UUID id, String oQueFoiFeito) {
        Tarefa tarefa = tarefa(id);
        tarefa.terminar();
        tarefas.save(tarefa);
        anotar(tarefa, oQueFoiFeito == null || oQueFoiFeito.isBlank()
                ? "Terminou e está esperando revisão."
                : "Terminou: " + oQueFoiFeito, "SISTEMA");
    }

    @Transactional
    public void aceitar(UUID id) {
        Tarefa tarefa = tarefa(id);
        tarefa.aceitar(contexto.autor());
        tarefas.save(tarefa);
        anotar(tarefa, contexto.autor() + " aceitou. Tarefa concluída.", "SISTEMA");
    }

    @Transactional
    public void mandarRefazer(UUID id, String motivo) {
        Tarefa tarefa = tarefa(id);
        tarefa.mandarRefazer(motivo, contexto.autor());
        tarefas.save(tarefa);
        anotar(tarefa, "Voltou para refazer: " + motivo, "SISTEMA");
    }

    @Transactional
    public void cancelar(UUID id, String motivo) {
        Tarefa tarefa = tarefa(id);
        tarefa.cancelar(motivo, contexto.autor());
        tarefas.save(tarefa);
        anotar(tarefa, "Cancelada por " + contexto.autor() + ": " + motivo, "SISTEMA");
    }

    @Transactional
    public void trocarResponsavel(UUID id, String paraQuem) {
        Tarefa tarefa = tarefa(id);
        String antes = tarefa.getResponsavel();
        tarefa.trocarResponsavel(paraQuem);
        tarefas.save(tarefa);
        anotar(tarefa, "Passou de " + (antes == null ? "o setor" : antes)
                + " para " + (paraQuem == null || paraQuem.isBlank() ? "o setor" : paraQuem)
                + ".", "SISTEMA");
    }

    @Transactional
    public void mudarPrazo(UUID id, LocalDate novo) {
        Tarefa tarefa = tarefa(id);
        tarefa.mudarPrazo(novo);
        tarefas.save(tarefa);
        anotar(tarefa, "Prazo mudou para "
                + (novo == null ? "sem data" : novo.toString()) + ".", "SISTEMA");
    }

    /** Cobrar é pedir posição, e a cobrança fica no fio. */
    @Transactional
    public void cobrar(UUID id, String texto) {
        Tarefa tarefa = tarefa(id);
        tarefa.cobrar();
        tarefas.save(tarefa);
        anotar(tarefa, texto == null || texto.isBlank()
                ? contexto.autor() + " pediu posição desta tarefa." : texto, "COBRANCA");
    }

    @Transactional
    public void comentar(UUID id, String texto, String tipo) {
        Tarefa tarefa = tarefa(id);
        anotar(tarefa, texto, tipo == null ? "COMENTARIO" : tipo);
    }

    @Transactional
    public void acrescentarAcompanhante(UUID id, String quem) {
        Tarefa tarefa = tarefa(id);
        if (quem == null || quem.isBlank()) {
            throw new IllegalArgumentException("Diga quem vai acompanhar.");
        }
        boolean jaTem = acompanham.findByTarefaId(id).stream()
                .anyMatch(a -> quem.trim().equalsIgnoreCase(a.getQuem()));
        if (jaTem) {
            return;
        }
        acompanham.save(new AcompanhaTarefa(id, tarefa.getEmpresaId(), quem.trim()));
        anotar(tarefa, quem.trim() + " passou a acompanhar.", "SISTEMA");
    }

    private void anotar(Tarefa tarefa, String texto, String tipo) {
        anotacoes.save(new AnotacaoDaTarefa(tarefa.getId(), tarefa.getEmpresaId(), texto,
                tipo, contexto.autor()));
    }

    // ------------------------------------------------------------------ contas

    /** Os números do topo da tela. */
    public record Resumo(long central, long minhas, long atrasadas, long esperandoRevisao,
                         long doSetor) {
    }

    public Resumo resumo(LocalDate hoje) {
        String eu = contexto.autor();
        List<Tarefa> todas = todas();
        return new Resumo(
                central().size(),
                todas.stream().filter(t -> eu.equals(t.getResponsavel()) && t.aberta()).count(),
                todas.stream().filter(t -> t.atrasada(hoje)).count(),
                todas.stream().filter(t -> eu.equals(t.getPedidaPor())
                        && t.esperandoRevisao()).count(),
                semDono().size());
    }

    /** Quem já aparece no sistema, para escolher responsável sem digitar. */
    public List<String> pessoas() {
        List<String> nomes = new ArrayList<>();
        for (Tarefa tarefa : todas()) {
            if (tarefa.getResponsavel() != null && !nomes.contains(tarefa.getResponsavel())) {
                nomes.add(tarefa.getResponsavel());
            }
            if (tarefa.getPedidaPor() != null && !nomes.contains(tarefa.getPedidaPor())) {
                nomes.add(tarefa.getPedidaPor());
            }
        }
        if (!nomes.contains(contexto.autor())) {
            nomes.add(contexto.autor());
        }
        nomes.sort(String::compareToIgnoreCase);
        return nomes;
    }
}
