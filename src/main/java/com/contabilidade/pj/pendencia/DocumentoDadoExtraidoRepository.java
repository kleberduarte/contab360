package com.contabilidade.pj.pendencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoDadoExtraidoRepository extends JpaRepository<DocumentoDadoExtraido, Long> {

    void deleteByProcessamento_Id(Long processamentoId);

    List<DocumentoDadoExtraido> findByProcessamento_IdOrderByOrdemAsc(Long processamentoId);
}
