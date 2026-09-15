package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.DocumentoDoAcordo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoDoAcordoRepositorio extends JpaRepository<DocumentoDoAcordo, UUID> {

    List<DocumentoDoAcordo> findByAcordoId(UUID acordoId);

    List<DocumentoDoAcordo> findByTituloId(UUID tituloId);
}
