package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Usuario;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.UsuarioEmpresaRepositorio;
import br.com.itia.financeiro.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quem é a pessoa e o que ela pode fazer em cada empresa.
 *
 * As regras que este serviço protege:
 *   1. A pessoa é a mesma em todas as empresas; a alçada é por empresa.
 *   2. Quem abre uma empresa pela primeira vez entra como diretor dela. Quem
 *      chega depois entra como operador, e o diretor sobe quem precisar.
 *   3. Ninguém tira a própria alçada: a empresa nunca fica sem diretor.
 */
@Service
public class Acessos {

    private final UsuarioRepositorio usuarios;
    private final UsuarioEmpresaRepositorio crachas;

    public Acessos(UsuarioRepositorio usuarios, UsuarioEmpresaRepositorio crachas) {
        this.usuarios = usuarios;
        this.crachas = crachas;
    }

    /** Acha a pessoa pelo e-mail, ou cria se for a primeira vez dela aqui. */
    @Transactional
    public Usuario identificar(String nome, String email) {
        String chave = (email == null || email.isBlank())
                ? apelidoDe(nome) + "@local" : email.trim().toLowerCase();
        return usuarios.findByEmail(chave)
                .orElseGet(() -> usuarios.save(new Usuario(
                        nome == null || nome.isBlank() ? "Convidado" : nome.trim(),
                        chave, null)));
    }

    /**
     * O crachá desta pessoa nesta empresa.
     *
     * Empresa sem ninguém ainda: quem entrar primeiro vira diretor, senão o
     * sistema ficaria sem quem pudesse dar alçada a alguém.
     */
    @Transactional
    public UsuarioEmpresa crachaDe(UUID usuarioId, UUID empresaId) {
        return crachas.cracha(usuarioId, empresaId).orElseGet(() -> {
            UsuarioEmpresa.Papel papel = crachas.quantosNaEmpresa(empresaId) == 0
                    ? UsuarioEmpresa.Papel.DIRETOR : UsuarioEmpresa.Papel.OPERADOR;
            return crachas.save(new UsuarioEmpresa(usuarioId, empresaId, papel));
        });
    }

    public List<UsuarioEmpresa> daEmpresa(UUID empresaId) {
        return crachas.daEmpresa(empresaId);
    }

    /** O nome de cada pessoa da empresa, para a tela não mostrar só o código. */
    public Map<UUID, Usuario> pessoas(List<UsuarioEmpresa> lista) {
        return usuarios.findAllById(lista.stream().map(UsuarioEmpresa::getUsuarioId).toList())
                .stream().collect(Collectors.toMap(Usuario::getId, u -> u));
    }

    @Transactional
    public void mudarPapel(UUID usuarioId, UUID empresaId, UsuarioEmpresa.Papel papel,
                           UUID quemEstaMudando) {
        if (usuarioId.equals(quemEstaMudando) && papel != UsuarioEmpresa.Papel.DIRETOR) {
            throw new IllegalStateException(
                    "Você não pode tirar a própria alçada. Peça para outro diretor.");
        }
        UsuarioEmpresa cracha = crachas.cracha(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Esta pessoa não tem acesso a esta empresa."));
        crachas.save(new UsuarioEmpresa(cracha.getUsuarioId(), cracha.getEmpresaId(), papel));
    }

    private String apelidoDe(String nome) {
        if (nome == null || nome.isBlank()) {
            return "convidado";
        }
        return nome.trim().toLowerCase().replaceAll("[^a-z0-9]", ".");
    }
}
