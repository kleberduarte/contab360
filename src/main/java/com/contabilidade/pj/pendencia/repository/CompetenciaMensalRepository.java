package com.contabilidade.pj.pendencia.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.pendencia.entity.CompetenciaMensal;

public interface CompetenciaMensalRepository extends JpaRepository<CompetenciaMensal, Long> {
    Optional<CompetenciaMensal> findByAnoAndMes(Integer ano, Integer mes);
}
