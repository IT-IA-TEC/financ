package br.com.itia.financeiro.dominio;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * Um arquivo que a pessoa escolheu no computador.
 *
 * Antes o sistema recebia o arquivo do navegador. Agora ele é escolhido na
 * janela, então o que chega aqui é o nome e o conteúdo, e nada mais. É de
 * propósito: quem guarda ou lê o arquivo não precisa saber de onde ele veio.
 */
public record ArquivoRecebido(String nome, byte[] conteudo) {

    public static ArquivoRecebido de(File arquivo) {
        if (arquivo == null) {
            return null;
        }
        try {
            return new ArquivoRecebido(arquivo.getName(), Files.readAllBytes(arquivo.toPath()));
        } catch (IOException naoLeu) {
            throw new UncheckedIOException("Não consegui ler o arquivo " + arquivo.getName(),
                    naoLeu);
        }
    }

    public boolean vazio() {
        return conteudo == null || conteudo.length == 0;
    }

    public long tamanho() {
        return conteudo == null ? 0 : conteudo.length;
    }

    /** O nome do arquivo, sem caminho e sem espaço sobrando. */
    public String nomeLimpo() {
        if (nome == null || nome.isBlank()) {
            return "arquivo";
        }
        String limpo = nome.trim().replace('\\', '/');
        int barra = limpo.lastIndexOf('/');
        return barra >= 0 ? limpo.substring(barra + 1) : limpo;
    }

    /** O texto de dentro do arquivo, para os que são de texto (extrato, planilha). */
    public String texto() {
        return conteudo == null ? "" : new String(conteudo, java.nio.charset.StandardCharsets.UTF_8);
    }
}
