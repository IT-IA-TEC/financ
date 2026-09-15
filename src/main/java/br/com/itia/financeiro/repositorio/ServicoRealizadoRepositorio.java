package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ServicoRealizado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRealizadoRepositorio extends JpaRepository<ServicoRealizado, UUID> {

    List<ServicoRealizado> findByEmpresaIdOrderByRealizadoEmDescCriadoEmDesc(UUID empresaId);

    Optional<ServicoRealizado> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<ServicoRealizado> findByPagadorIdOrderByRealizadoEmDesc(UUID pagadorId);

    /** O que ja foi feito para este cliente, neste servico, dentro da janela. */
    List<ServicoRealizado> findByPagadorIdAndServicoIdAndRealizadoEmBetween(
            UUID pagadorId, UUID servicoId, LocalDate de, LocalDate ate);
}
