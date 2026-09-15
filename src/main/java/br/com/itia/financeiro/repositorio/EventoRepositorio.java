package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoRepositorio extends JpaRepository<Evento, Long> {

    List<Evento> findTop50ByEmpresaIdOrderByOcorridoEmDesc(UUID empresaId);

    List<Evento> findByEmpresaIdAndEntidadeAndEntidadeIdOrderByOcorridoEmDesc(
            UUID empresaId, String entidade, UUID entidadeId);
}
