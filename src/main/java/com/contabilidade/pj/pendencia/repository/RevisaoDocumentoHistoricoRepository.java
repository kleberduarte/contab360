package com.contabilidade.pj.pendencia.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.pendencia.entity.RevisaoDocumentoHistorico;
import com.contabilidade.pj.pendencia.entity.SeveridadeRevisao;

public interface RevisaoDocumentoHistoricoRepository extends JpaRepository<RevisaoDocumentoHistorico, Long> {
    List<RevisaoDocumentoHistorico> findByProcessamentoIdOrderByCriadoEmDesc(Long processamentoId);
}
