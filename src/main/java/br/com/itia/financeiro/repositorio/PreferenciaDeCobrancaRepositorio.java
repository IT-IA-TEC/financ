package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.PreferenciaDeCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PreferenciaDeCobrancaRepositorio
        extends JpaRepository<PreferenciaDeCobranca, UUID> {
}
