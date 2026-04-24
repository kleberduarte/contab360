package com.contabilidade.pj.ia;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    List<AuditoriaEvento> findTop200ByCriadoEmAfterOrderByCriadoEmDesc(Instant desde);

    long countByCriadoEmAfter(Instant desde);
}
