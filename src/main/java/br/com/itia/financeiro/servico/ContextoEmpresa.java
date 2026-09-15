package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.repositorio.EmpresaRepositorio;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.UUID;

/**
 * Guarda quem entrou e em qual empresa a pessoa esta trabalhando agora.
 *
 * E o unico lugar do sistema que responde "qual empresa?". Serviço nenhum
 * adivinha: ou recebe a empresa daqui, ou nao roda. E o que garante que quem
 * cuida de duas empresas nunca veja as duas misturadas.
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ContextoEmpresa implements Serializable {

    private final transient EmpresaRepositorio empresas;

    private UUID empresaId;
    private UUID usuarioId;
    private String nome;
    private String email;
    /** A alçada desta pessoa NESTA empresa. Muda quando ela troca de empresa. */
    private br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel papel;

    public ContextoEmpresa(EmpresaRepositorio empresas) {
        this.empresas = empresas;
    }

    // ------------------------------------------------------------- quem entrou

    /** Entrada sem trava ainda: o login de verdade entra numa proxima etapa. */
    public void entrar(String nome, String email) {
        this.nome = nome == null || nome.isBlank() ? "Convidado" : nome.trim();
        this.email = email == null || email.isBlank() ? null : email.trim();
    }

    public void identificar(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void assumirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel papel) {
        this.papel = papel;
    }

    public br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel getPapel() {
        return papel;
    }

    /**
     * Barra quem não tem alçada para a ação.
     *
     * A ordem é OPERADOR, GESTOR, DIRETOR: quem está acima pode tudo que quem
     * está abaixo pode.
     */
    public void exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel minimo) {
        if (papel == null || papel.ordinal() < minimo.ordinal()) {
            throw new SemAlcada(minimo);
        }
    }

    public boolean estaLogado() {
        return nome != null;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    /** As duas letras que aparecem na bolinha do topo. */
    public String getIniciais() {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
    }

    public void sair() {
        this.nome = null;
        this.email = null;
        this.empresaId = null;
        this.usuarioId = null;
        this.papel = null;
    }

    // ----------------------------------------------------------- qual empresa

    public boolean temEmpresaEscolhida() {
        return empresaId != null;
    }

    public void escolher(UUID empresaId) {
        Empresa empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada."));
        if (!empresa.isAtiva()) {
            throw new IllegalStateException("Esta empresa esta desativada.");
        }
        this.empresaId = empresa.getId();
    }

    public void largarEmpresa() {
        this.empresaId = null;
        this.papel = null;
    }

    public UUID exigirEmpresaId() {
        if (empresaId == null) {
            throw new EmpresaNaoEscolhida();
        }
        return empresaId;
    }

    public Empresa exigirEmpresa() {
        return empresas.findById(exigirEmpresaId())
                .orElseThrow(EmpresaNaoEscolhida::new);
    }

    /** Quem assina o que for gravado. */
    public String autor() {
        return nome == null ? "sistema" : nome;
    }

    public static class EmpresaNaoEscolhida extends RuntimeException {
        public EmpresaNaoEscolhida() {
            super("Escolha a empresa antes de continuar.");
        }
    }

    /** Falta de alçada: a ação existe, mas não para esta pessoa. */
    public static class SemAlcada extends RuntimeException {
        public SemAlcada(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel minimo) {
            super("Esta ação é de " + minimo.name().toLowerCase()
                    + ". Peça para quem tem essa alçada.");
        }
    }

    public static class NaoEntrou extends RuntimeException {
        public NaoEntrou() {
            super("Entre no sistema para continuar.");
        }
    }
}
