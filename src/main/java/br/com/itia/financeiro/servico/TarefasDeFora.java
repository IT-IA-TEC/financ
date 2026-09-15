package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.FonteDeTarefas;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.FonteDeTarefasRepositorio;
import br.com.itia.financeiro.repositorio.IntegracaoRepositorio;
import br.com.itia.financeiro.repositorio.OperacaoIntegracaoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * As tarefas que o IT.FC vai buscar no outro sistema.
 *
 * O caminho contrário (o outro sistema empurrando para cá) mora em
 * EntradaDeTarefas, de propósito: quem recebe não depende de quem busca.
 *
 * O que este serviço protege:
 *   1. A mesma tarefa de lá nunca vira duas aqui. A chave é o identificador do
 *      outro sistema.
 *   2. O mapeamento é da empresa: cada sistema chama os campos do jeito dele,
 *      e quem configura diz qual é qual.
 *   3. Falha de rede não apaga nada: o erro fica escrito na fonte, com data.
 */
@Service
public class TarefasDeFora {

    private static final Logger LOG = LoggerFactory.getLogger(TarefasDeFora.class);

    private final FonteDeTarefasRepositorio fontes;
    private final EntradaDeTarefas entrada;
    private final IntegracaoRepositorio integracoes;
    private final OperacaoIntegracaoRepositorio operacoes;
    private final IntegracaoServico integracaoServico;
    private final ContextoEmpresa contexto;
    private final ObjectMapper json = new ObjectMapper();

    public TarefasDeFora(FonteDeTarefasRepositorio fontes, EntradaDeTarefas entrada,
                         IntegracaoRepositorio integracoes,
                         OperacaoIntegracaoRepositorio operacoes,
                         IntegracaoServico integracaoServico, ContextoEmpresa contexto) {
        this.fontes = fontes;
        this.entrada = entrada;
        this.integracoes = integracoes;
        this.operacoes = operacoes;
        this.integracaoServico = integracaoServico;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ fontes

    public List<FonteDeTarefas> todas() {
        return fontes.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public FonteDeTarefas fonte(UUID id) {
        return fontes.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Fonte não encontrada."));
    }

    public List<Integracao> integracoesPossiveis() {
        return integracoes.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public List<OperacaoIntegracao> operacoesDe(UUID integracaoId) {
        return integracaoId == null ? List.of()
                : operacoes.findByIntegracaoIdOrderByNome(integracaoId);
    }

    @Transactional
    public FonteDeTarefas salvar(UUID id, String nome, UUID integracaoId, UUID operacaoId,
                                 String campoId, String campoTitulo, String campoDescricao,
                                 String campoSituacao, String campoResponsavel,
                                 String campoPrazo, String campoPrioridade, String campoLink,
                                 String filtro, String setorPadrao, boolean ativa) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.GESTOR);
        UUID empresaId = contexto.exigirEmpresaId();
        integracoes.findByIdAndEmpresaId(integracaoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Integração não encontrada."));

        FonteDeTarefas fonte = id == null
                ? new FonteDeTarefas(empresaId, nome, integracaoId, contexto.autor())
                : fonte(id);
        fonte.ajustar(nome, integracaoId, operacaoId, campoId, campoTitulo, campoDescricao,
                campoSituacao, campoResponsavel, campoPrazo, campoPrioridade, campoLink,
                filtro, setorPadrao, ativa);
        return fontes.save(fonte);
    }

    // ------------------------------------------------------------------ puxar

    /** O que aconteceu numa busca. */
    public record Puxada(int novas, int atualizadas, int ignoradas, String erro) {
        public boolean deuCerto() {
            return erro == null;
        }

        public String resumo() {
            return erro != null ? erro
                    : novas + " nova(s), " + atualizadas + " atualizada(s), "
                            + ignoradas + " sem mudança";
        }
    }

    /**
     * Busca as tarefas no outro sistema e traz para cá.
     *
     * A chamada de fora fica fora de transação nossa: rede que cai não pode
     * desfazer o que já entrou.
     */
    public Puxada puxar(UUID fonteId) {
        FonteDeTarefas fonte = fonte(fonteId);
        if (fonte.getOperacaoId() == null) {
            return anotarResultado(fonteId, new Puxada(0, 0, 0,
                    "Escolha qual operação da integração traz a lista."));
        }
        String resposta;
        try {
            resposta = integracaoServico.executar(fonte.getIntegracaoId(),
                    fonte.getOperacaoId(), null);
        } catch (RuntimeException erro) {
            LOG.debug("busca de tarefas falhou: {}", erro.getMessage());
            return anotarResultado(fonteId, new Puxada(0, 0, 0,
                    "Não consegui falar com o outro sistema: " + erro.getMessage()));
        }
        return anotarResultado(fonteId, guardar(fonte, resposta));
    }

    @Transactional
    public Puxada anotarResultado(UUID fonteId, Puxada puxada) {
        FonteDeTarefas guardada = fonte(fonteId);
        guardada.anotarPuxada(puxada.resumo());
        fontes.save(guardada);
        return puxada;
    }

    /** Lê a resposta e grava cada linha como tarefa. */
    public Puxada guardar(FonteDeTarefas fonte, String resposta) {
        JsonNode raiz;
        try {
            raiz = json.readTree(resposta == null ? "[]" : resposta);
        } catch (Exception erro) {
            return new Puxada(0, 0, 0, "A resposta não veio em formato que eu saiba ler.");
        }
        JsonNode lista = raiz.isArray() ? raiz : raiz.path("data");
        if (!lista.isArray()) {
            return new Puxada(0, 0, 0,
                    "A resposta não trouxe uma lista de tarefas. Confira a operação.");
        }

        int novas = 0;
        int atualizadas = 0;
        int ignoradas = 0;
        for (JsonNode linha : lista) {
            String idExterno = entrada.texto(linha, fonte.getCampoId());
            String titulo = entrada.texto(linha, fonte.getCampoTitulo());
            if (idExterno == null || titulo == null) {
                ignoradas++;
                continue;
            }
            String antes = entrada.retrato(fonte.getEmpresaId(), idExterno);
            boolean eNova = entrada.guardar(fonte.getEmpresaId(), idExterno, titulo,
                    entrada.texto(linha, fonte.getCampoDescricao()),
                    entrada.traduzirSituacao(entrada.texto(linha, fonte.getCampoSituacao())),
                    entrada.texto(linha, fonte.getCampoResponsavel()),
                    entrada.data(entrada.texto(linha, fonte.getCampoPrazo())),
                    entrada.traduzirPrioridade(entrada.texto(linha, fonte.getCampoPrioridade())),
                    entrada.texto(linha, fonte.getCampoLink()),
                    fonte.getSetorPadrao(), fonte.getId(), "SINCRONIZADA", fonte.getNome());

            if (eNova) {
                novas++;
                continue;
            }
            String depois = entrada.retrato(fonte.getEmpresaId(), idExterno);
            if (antes != null && antes.equals(depois)) {
                ignoradas++;
            } else {
                atualizadas++;
                entrada.porIdExterno(fonte.getEmpresaId(), idExterno).ifPresent(tarefa ->
                        entrada.anotar(tarefa.getId(), fonte.getEmpresaId(),
                                "Atualizada pelo " + fonte.getNome() + ".", "sincronização"));
            }
        }
        return new Puxada(novas, atualizadas, ignoradas, null);
    }

}
