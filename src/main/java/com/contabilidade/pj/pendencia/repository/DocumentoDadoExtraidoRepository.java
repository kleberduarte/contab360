package com.contabilidade.pj.pendencia.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.pendencia.entity.DocumentoDadoExtraido;
import com.contabilidade.pj.pendencia.entity.TipoCampoExtraido;

public interface DocumentoDadoExtraidoRepository extends JpaRepository<DocumentoDadoExtraido, Long> {

    void deleteByProcessamento_Id(Long processamentoId);

    List<DocumentoDadoExtraido> findByProcessamento_IdOrderByOrdemAsc(Long processamentoId);
}
