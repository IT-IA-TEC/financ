package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Contato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContatoRepositorio extends JpaRepository<Contato, UUID> {

    List<Contato> findByPagadorIdOrderByPrioridade(UUID pagadorId);

    List<Contato> findByUnidadeIdOrderByPrioridade(UUID unidadeId);

    Optional<Contato> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
