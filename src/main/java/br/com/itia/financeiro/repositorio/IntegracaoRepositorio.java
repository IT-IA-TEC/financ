package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Integracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegracaoRepositorio extends JpaRepository<Integracao, UUID> {

    List<Integracao> findByEmpresaIdOrderByNome(UUID empresaId);

    Optional<Integracao> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
