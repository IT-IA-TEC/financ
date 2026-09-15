package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Bem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BemRepositorio extends JpaRepository<Bem, UUID> {

    List<Bem> findByEmpresaIdOrderByNumeroPatrimonial(UUID empresaId);

    List<Bem> findByEmpresaIdAndAtivoTrueOrderByNumeroPatrimonial(UUID empresaId);

    Optional<Bem> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Bem> findByEmpresaIdAndNumeroPatrimonial(UUID empresaId, String numeroPatrimonial);
}
