package com.contabilidade.pj.pendencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetenciaMensalRepository extends JpaRepository<CompetenciaMensal, Long> {
    Optional<CompetenciaMensal> findByAnoAndMes(Integer ano, Integer mes);
}
