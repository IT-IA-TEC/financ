package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.FaturadoDoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaturadoDoPeriodoRepositorio extends JpaRepository<FaturadoDoPeriodo, UUID> {

    List<FaturadoDoPeriodo> findByEmpresaIdAndReferenciaOrderByDocumento(UUID empresaId,
                                                                        String referencia);

    void deleteByEmpresaIdAndReferencia(UUID empresaId, String referencia);
}
