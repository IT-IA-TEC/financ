package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.MudancaDePreco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MudancaDePrecoRepositorio extends JpaRepository<MudancaDePreco, Long> {

    List<MudancaDePreco> findByServicoIdOrderByQuandoDesc(UUID servicoId);
}
