package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoProcessamentoRepository extends JpaRepository<DocumentoProcessamento, Long> {
    Optional<DocumentoProcessamento> findByEntregaId(Long entregaId);
    Optional<DocumentoProcessamento> findTopByEntregaPendenciaIdOrderByAtualizadoEmDesc(Long pendenciaId);
    List<DocumentoProcessamento> findByStatusOrderByAtualizadoEmDesc(ProcessamentoStatus status);
    List<DocumentoProcessamento> findAllByOrderByAtualizadoEmDesc();
}
