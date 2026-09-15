package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.UnidadeDeCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadeDeCobrancaRepositorio extends JpaRepository<UnidadeDeCobranca, UUID> {

    List<UnidadeDeCobranca> findByEmpresaIdOrderByNome(UUID empresaId);

    List<UnidadeDeCobranca> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<UnidadeDeCobranca> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
