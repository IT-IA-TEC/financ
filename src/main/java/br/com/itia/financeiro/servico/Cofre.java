package br.com.itia.financeiro.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Guarda segredo de integracao cifrado.
 *
 * A chave vem da variavel de ambiente FINANCEIRO_CHAVE_COFRE. Se ela nao
 * existir, o sistema cria uma chave local em dados/chave-cofre e avisa no log:
 * serve para rodar na maquina, NAO serve para producao, porque quem tiver o
 * arquivo do banco e a pasta tem os dois lados.
 *
 * Nenhuma chave, nenhum segredo e escrito no codigo.
 */
@Component
public class Cofre {

    private static final Logger LOG = LoggerFactory.getLogger(Cofre.class);
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_ETIQUETA = 128;
    private static final int TAMANHO_TEMPERO = 12;

    private final SecretKey chave;

    public Cofre(@Value("${financeiro.chave-cofre:}") String chaveConfigurada) {
        this.chave = resolver(chaveConfigurada);
    }

    private SecretKey resolver(String chaveConfigurada) {
        if (chaveConfigurada != null && !chaveConfigurada.isBlank()) {
            return new SecretKeySpec(Base64.getDecoder().decode(chaveConfigurada.trim()), "AES");
        }
        Path arquivo = Path.of("dados", "chave-cofre");
        try {
            if (Files.exists(arquivo)) {
                String texto = Files.readString(arquivo, StandardCharsets.UTF_8).trim();
                return new SecretKeySpec(Base64.getDecoder().decode(texto), "AES");
            }
            KeyGenerator gerador = KeyGenerator.getInstance("AES");
            gerador.init(256);
            SecretKey nova = gerador.generateKey();
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, Base64.getEncoder().encodeToString(nova.getEncoded()),
                    StandardCharsets.UTF_8);
            LOG.warn("Chave do cofre criada em dados/chave-cofre. "
                    + "Em producao, use a variavel FINANCEIRO_CHAVE_COFRE.");
            return nova;
        } catch (Exception erro) {
            throw new IllegalStateException("Nao consegui preparar o cofre: " + erro.getMessage(), erro);
        }
    }

    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] tempero = new byte[TAMANHO_TEMPERO];
            new SecureRandom().nextBytes(tempero);

            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_ETIQUETA, tempero));
            byte[] fechado = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            byte[] tudo = new byte[tempero.length + fechado.length];
            System.arraycopy(tempero, 0, tudo, 0, tempero.length);
            System.arraycopy(fechado, 0, tudo, tempero.length, fechado.length);
            return Base64.getEncoder().encodeToString(tudo);
        } catch (Exception erro) {
            throw new IllegalStateException("Nao consegui guardar o segredo.", erro);
        }
    }

    public String decifrar(String cifrado) {
        if (cifrado == null || cifrado.isBlank()) {
            return null;
        }
        try {
            byte[] tudo = Base64.getDecoder().decode(cifrado);
            byte[] tempero = new byte[TAMANHO_TEMPERO];
            System.arraycopy(tudo, 0, tempero, 0, TAMANHO_TEMPERO);

            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_ETIQUETA, tempero));
            byte[] aberto = cifra.doFinal(tudo, TAMANHO_TEMPERO, tudo.length - TAMANHO_TEMPERO);
            return new String(aberto, StandardCharsets.UTF_8);
        } catch (Exception erro) {
            throw new IllegalStateException("Nao consegui ler o segredo guardado. "
                    + "A chave do cofre mudou?", erro);
        }
    }

    /** Mostra so o fim do segredo, para conferencia na tela. */
    public static String mascarar(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            return "";
        }
        if (segredo.length() <= 4) {
            return "••••";
        }
        return "•••• " + segredo.substring(segredo.length() - 4);
    }

    /** Usado so no teste de escrita do arquivo da chave. */
    static void apagarChaveLocal() throws IOException {
        Files.deleteIfExists(Path.of("dados", "chave-cofre"));
    }
}
