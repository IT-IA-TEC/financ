package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnderecoRepositorio extends JpaRepository<Endereco, UUID> {

    List<Endereco> findByUnidadeIdOrderByTipo(UUID unidadeId);

    Optional<Endereco> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
