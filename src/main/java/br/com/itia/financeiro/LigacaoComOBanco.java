package br.com.itia.financeiro;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descobre em qual banco o sistema vai trabalhar.
 *
 * Se existir o arquivo de ligação, o sistema usa o banco de verdade. Se não
 * existir, usa um arquivo na própria máquina e continua abrindo: ninguém fica
 * travado por causa de internet.
 *
 * A senha do banco mora só nesse arquivo, nunca dentro do programa.
 */
final class LigacaoComOBanco {

    /** postgresql://usuario:senha@endereco:porta/banco */
    private static final Pattern FORMATO =
            Pattern.compile("^postgres(?:ql)?://([^:]+):(.+)@([^/]+)/(.+)$");

    private LigacaoComOBanco() {
    }

    /**
     * As configurações que ligam o sistema no banco de verdade. Vem vazio
     * quando não há arquivo de ligação, e aí vale o banco da máquina.
     */
    static Map<String, Object> descobrir() {
        Path arquivo = ondeMoraOArquivo();
        if (arquivo == null) {
            return Map.of();
        }
        try {
            for (String linha : Files.readString(arquivo, StandardCharsets.UTF_8).split("\\R")) {
                String limpa = linha.trim();
                if (limpa.startsWith("#") || !limpa.startsWith("BANCO=")) {
                    continue;
                }
                Matcher pedacos = FORMATO.matcher(limpa.substring(6).trim());
                if (!pedacos.matches()) {
                    return Map.of();
                }
                Map<String, Object> configuracao = new HashMap<>();
                configuracao.put("spring.profiles.active", "postgres");
                configuracao.put("FINANCEIRO_DB_URL", "jdbc:postgresql://" + pedacos.group(3)
                        + "/" + pedacos.group(4).split("\\?")[0] + "?sslmode=require");
                configuracao.put("FINANCEIRO_DB_USUARIO",
                        URLDecoder.decode(pedacos.group(1), StandardCharsets.UTF_8));
                configuracao.put("FINANCEIRO_DB_SENHA",
                        URLDecoder.decode(pedacos.group(2), StandardCharsets.UTF_8));
                return configuracao;
            }
        } catch (Exception naoLeu) {
            return Map.of();
        }
        return Map.of();
    }

    /**
     * Dois lugares, nesta ordem: ao lado do sistema (é onde fica durante o
     * desenvolvimento) e na pasta de dados dos programas (é onde fica depois de
     * instalado, porque a pasta do programa não aceita gravação).
     */
    private static Path ondeMoraOArquivo() {
        Path aoLado = Path.of("dados", "banco.env");
        if (Files.exists(aoLado)) {
            return aoLado;
        }
        String appData = System.getenv("LOCALAPPDATA");
        if (appData != null && !appData.isBlank()) {
            Path instalado = Path.of(appData, "IT.FC", "banco.env");
            if (Files.exists(instalado)) {
                return instalado;
            }
        }
        return null;
    }
}
