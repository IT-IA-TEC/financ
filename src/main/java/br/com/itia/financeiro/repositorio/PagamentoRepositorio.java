package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PagamentoRepositorio extends JpaRepository<Pagamento, UUID> {

    List<Pagamento> findByEmpresaIdOrderByPagoEmDesc(UUID empresaId);

    /** Barra o mesmo PIX entrando duas vezes. */
    boolean existsByEmpresaIdAndTransacaoId(UUID empresaId, String transacaoId);

    java.util.Optional<br.com.itia.financeiro.dominio.Pagamento>
            findByEmpresaIdAndTransacaoId(UUID empresaId, String transacaoId);

    @Query("""
            select coalesce(sum(p.valor), 0) from Pagamento p
            where p.empresa.id = :empresaId and p.pagoEm between :de and :ate
            """)
    BigDecimal somarRecebido(@Param("empresaId") UUID empresaId,
                             @Param("de") LocalDate de,
                             @Param("ate") LocalDate ate);
}
