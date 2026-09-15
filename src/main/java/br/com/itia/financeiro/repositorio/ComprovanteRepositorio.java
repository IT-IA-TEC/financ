package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Comprovante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComprovanteRepositorio extends JpaRepository<Comprovante, UUID> {

    List<Comprovante> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    Optional<Comprovante> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
