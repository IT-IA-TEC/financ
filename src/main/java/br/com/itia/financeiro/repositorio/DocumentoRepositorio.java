package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepositorio extends JpaRepository<Documento, UUID> {

    List<Documento> findByPagadorIdOrderByAnexadoEmDesc(UUID pagadorId);

    List<Documento> findByUnidadeIdOrderByAnexadoEmDesc(UUID unidadeId);

    List<Documento> findByRealizadoIdOrderByAnexadoEmDesc(UUID realizadoId);

    List<Documento> findByObrigacaoIdOrderByAnexadoEmDesc(UUID obrigacaoId);

    Optional<Documento> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
