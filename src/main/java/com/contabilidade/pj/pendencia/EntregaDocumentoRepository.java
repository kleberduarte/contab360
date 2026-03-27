package com.contabilidade.pj.pendencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaDocumentoRepository extends JpaRepository<EntregaDocumento, Long> {
    List<EntregaDocumento> findByPendenciaIdOrderByEnviadoEmDesc(Long pendenciaId);
}
