package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ImportacaoDeExtrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportacaoDeExtratoRepositorio
        extends JpaRepository<ImportacaoDeExtrato, UUID> {

    Optional<ImportacaoDeExtrato> findByEmpresaIdAndImpressao(UUID empresaId, String impressao);

    List<ImportacaoDeExtrato> findTop20ByEmpresaIdOrderByQuandoDesc(UUID empresaId);
}
