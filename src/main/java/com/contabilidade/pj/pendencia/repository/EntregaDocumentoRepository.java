package com.contabilidade.pj.pendencia.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.pendencia.entity.EntregaDocumento;
import com.contabilidade.pj.pendencia.entity.PendenciaStatus;

public interface EntregaDocumentoRepository extends JpaRepository<EntregaDocumento, Long> {
    void deleteByPendencia_IdIn(Collection<Long> pendenciaIds);

    List<EntregaDocumento> findByPendenciaIdOrderByEnviadoEmDesc(Long pendenciaId);
}
