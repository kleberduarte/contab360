package com.contabilidade.pj.ia.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.ia.entity.AuditoriaEvento;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    List<AuditoriaEvento> findTop200ByCriadoEmAfterOrderByCriadoEmDesc(Instant desde);

    long countByCriadoEmAfter(Instant desde);
}
