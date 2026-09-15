package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toda busca aqui pede a empresa. E de proposito: nao existe consulta que
 * atravesse duas empresas.
 */
public interface ClienteRepositorio extends JpaRepository<ClienteEspelho, UUID> {

    List<ClienteEspelho> findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(UUID empresaId);

    Optional<ClienteEspelho> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<ClienteEspelho> findByEmpresaIdAndErpClienteId(UUID empresaId, UUID erpClienteId);

    long countByEmpresaIdAndAtivoTrue(UUID empresaId);

    long countByEmpresaId(UUID empresaId);
}
