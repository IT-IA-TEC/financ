package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Consentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentimentoRepositorio extends JpaRepository<Consentimento, UUID> {

    List<Consentimento> findByPagadorIdOrderByRegistradoEmDesc(UUID pagadorId);
}
