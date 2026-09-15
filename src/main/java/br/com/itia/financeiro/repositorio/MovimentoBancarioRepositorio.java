package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.MovimentoBancario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimentoBancarioRepositorio extends JpaRepository<MovimentoBancario, UUID> {

    List<MovimentoBancario> findByEmpresaIdOrderByOcorridoEmDesc(UUID empresaId);

    Optional<MovimentoBancario> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<MovimentoBancario> findByEmpresaIdAndIdentificador(UUID empresaId,
                                                                String identificador);

    List<MovimentoBancario> findByEmpresaIdAndOcorridoEmBetweenOrderByOcorridoEm(
            UUID empresaId, LocalDate de, LocalDate ate);
}
