package com.contabilidade.pj.pendencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisaoDocumentoHistoricoRepository extends JpaRepository<RevisaoDocumentoHistorico, Long> {
    List<RevisaoDocumentoHistorico> findByProcessamentoIdOrderByCriadoEmDesc(Long processamentoId);
}
