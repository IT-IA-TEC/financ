package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Etiqueta;
import br.com.itia.financeiro.repositorio.EtiquetaRepositorio;
import br.com.itia.financeiro.repositorio.VinculoDeEtiquetaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * O gerenciador de etiquetas.
 *
 * Papel de contato, marcação de pessoa, de unidade e de contrato: tudo passa
 * por aqui. Cada etiqueta tem um código estável, gerado do nome na criação e
 * que nunca muda depois. É esse código que permite espelhar a mesma etiqueta
 * em outro banco do grupo: o texto na tela pode ser diferente, o código não.
 */
@Service
public class EtiquetaServico {

    /** Onde uma etiqueta pode ser usada. */
    public static final List<String> ESCOPOS =
            List.of("CONTATO", "PESSOA", "UNIDADE", "CONTRATO", "DOCUMENTO");

    /** O que já vem pronto quando a empresa ainda não tem nenhuma. */
    private static final List<String[]> SUGESTAO_INICIAL = List.of(
            new String[]{"Paga", "Quem efetivamente faz o pagamento", "CONTATO"},
            new String[]{"Assina", "Representante legal que assina contrato", "CONTATO"},
            new String[]{"Recado", "Recebe recado, mas não decide", "CONTATO"},
            new String[]{"Contador", "Cuida da parte fiscal", "CONTATO"},
            new String[]{"Financeiro", "Responsável pelo financeiro do cliente", "CONTATO"},
            new String[]{"Sócio", "Sócio da empresa", "CONTATO"});

    private final EtiquetaRepositorio etiquetas;
    private final VinculoDeEtiquetaRepositorio vinculos;
    private final ContextoEmpresa contexto;

    public EtiquetaServico(EtiquetaRepositorio etiquetas, VinculoDeEtiquetaRepositorio vinculos,
                           ContextoEmpresa contexto) {
        this.etiquetas = etiquetas;
        this.vinculos = vinculos;
        this.contexto = contexto;
    }

    public List<Etiqueta> todas() {
        return etiquetas.findByEmpresaIdOrderByNome(contexto.exigirEmpresaId());
    }

    public List<Etiqueta> ativasDoEscopo(String escopo) {
        return etiquetas.findByEmpresaIdAndEscopoAndAtivoTrueOrderByNome(
                contexto.exigirEmpresaId(), escopo);
    }

    @Transactional
    public Etiqueta cadastrar(String nome, String descricao, String escopo, String cor) {
        UUID empresaId = contexto.exigirEmpresaId();
        String codigo = Etiqueta.codigoDe(nome);
        etiquetas.findByEmpresaIdAndCodigo(empresaId, codigo).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Já existe a etiqueta " + existente.getNome() + " com esse mesmo código.");
        });
        return etiquetas.save(new Etiqueta(contexto.exigirEmpresa(), nome, descricao, escopo, cor));
    }

    @Transactional
    public void salvar(UUID id, String nome, String descricao, String escopo, String cor,
                       boolean ativo) {
        etiquetas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Etiqueta não encontrada."))
                .ajustar(nome, descricao, escopo, cor, ativo);
    }

    /** Quantas coisas estão usando cada etiqueta. */
    public long quantosUsos(UUID etiquetaId) {
        return vinculos.findByEmpresaIdAndEntidade(contexto.exigirEmpresaId(), "CONTATO").stream()
                .filter(v -> v.getEtiqueta().getId().equals(etiquetaId))
                .count();
    }

    /** Cria o conjunto inicial, só quando a empresa ainda não tem etiqueta nenhuma. */
    @Transactional
    public int criarSugestaoInicial() {
        if (!todas().isEmpty()) {
            return 0;
        }
        int criadas = 0;
        for (String[] s : SUGESTAO_INICIAL) {
            etiquetas.save(new Etiqueta(contexto.exigirEmpresa(), s[0], s[1], s[2], null));
            criadas++;
        }
        return criadas;
    }
}
