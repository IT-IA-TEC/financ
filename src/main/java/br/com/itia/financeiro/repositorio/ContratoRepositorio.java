package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratoRepositorio extends JpaRepository<Contrato, UUID> {

    List<Contrato> findByPagadorIdOrderByNumero(UUID pagadorId);

    List<Contrato> findByEmpresaIdOrderByNumero(UUID empresaId);

    Optional<Contrato> findByIdAndEmpresaId(UUID id, UUID empresaId);

    @Query("select count(c) from Contrato c where c.empresaId = :empresaId")
    long quantos(@Param("empresaId") UUID empresaId);
}
