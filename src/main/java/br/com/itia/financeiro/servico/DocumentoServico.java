package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.repositorio.DocumentoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.itia.financeiro.dominio.ArquivoRecebido;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Guarda os arquivos anexados ao cliente.
 *
 * O arquivo vai para uma pasta da empresa, com nome próprio, e o banco guarda
 * só o caminho. Nome de arquivo enviado por alguém nunca vira caminho direto:
 * isso é o que impede alguém escrever fora da pasta.
 */
@Service
public class DocumentoServico {

    private static final long TAMANHO_MAXIMO = 20L * 1024 * 1024;

    private final DocumentoRepositorio documentos;
    private final ContextoEmpresa contexto;

    public DocumentoServico(DocumentoRepositorio documentos, ContextoEmpresa contexto) {
        this.documentos = documentos;
        this.contexto = contexto;
    }

    @Transactional
    public Documento anexar(ArquivoRecebido arquivo, UUID pagadorId, UUID unidadeId,
                            UUID contratoId, String tipo, LocalDate validade, String observacao) {
        if (arquivo == null || arquivo.vazio()) {
            throw new IllegalArgumentException("Escolha um arquivo para anexar.");
        }
        if (arquivo.tamanho() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Arquivo maior que 20 MB.");
        }

        UUID empresaId = contexto.exigirEmpresaId();
        String nomeOriginal = limparNome(arquivo.nomeLimpo());
        UUID id = UUID.randomUUID();
        Path pasta = Path.of("dados", "documentos", empresaId.toString());
        Path destino = pasta.resolve(id + "-" + nomeOriginal);

        try {
            Files.createDirectories(pasta);
            java.nio.file.Files.write(destino.toAbsolutePath(), arquivo.conteudo());
        } catch (IOException erro) {
            throw new IllegalStateException("Não consegui guardar o arquivo: " + erro.getMessage());
        }

        Documento documento = new Documento(empresaId, pagadorId, unidadeId, contratoId, tipo,
                nomeOriginal, destino.toString(), arquivo.tamanho(), validade, observacao,
                contexto.autor());
        return documentos.save(documento);
    }

    /** Liga um arquivo ja anexado a uma conta a pagar. */
    @Transactional
    public void vincularAObrigacao(Documento documento, java.util.UUID obrigacaoId) {
        documento.vincularAObrigacao(obrigacaoId);
        documentos.save(documento);
    }

    public java.util.List<Documento> daObrigacao(java.util.UUID obrigacaoId) {
        return documentos.findByObrigacaoIdOrderByAnexadoEmDesc(obrigacaoId);
    }

    public byte[] ler(UUID documentoId) {
        Documento documento = documentos.findByIdAndEmpresaId(documentoId,
                        contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        try {
            return Files.readAllBytes(Path.of(documento.getCaminho()));
        } catch (IOException erro) {
            throw new IllegalStateException("O arquivo não está mais na pasta.");
        }
    }

    public Documento buscar(UUID documentoId) {
        return documentos.findByIdAndEmpresaId(documentoId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
    }

    /** Tira caminho e caractere estranho do nome que veio de fora. */
    private String limparNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "arquivo";
        }
        String so = Path.of(nome).getFileName().toString();
        return so.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
