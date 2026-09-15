package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Favorecido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavorecidoRepositorio extends JpaRepository<Favorecido, UUID> {

    List<Favorecido> findByEmpresaIdOrderByNome(UUID empresaId);

    List<Favorecido> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<Favorecido> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Favorecido> findByEmpresaIdAndDocumento(UUID empresaId, String documento);
}
