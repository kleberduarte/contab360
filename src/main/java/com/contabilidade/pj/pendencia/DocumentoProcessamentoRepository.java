package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoProcessamentoRepository extends JpaRepository<DocumentoProcessamento, Long> {
    Optional<DocumentoProcessamento> findByEntregaId(Long entregaId);
    Optional<DocumentoProcessamento> findTopByEntregaPendenciaIdOrderByAtualizadoEmDesc(Long pendenciaId);
    @Query("""
            select
                dp.entrega.pendencia.id as pendenciaId,
                dp.observacaoProcessamento as observacaoProcessamento
            from DocumentoProcessamento dp
            where dp.entrega.pendencia.id in :pendenciaIds
              and dp.atualizadoEm = (
                select max(dp2.atualizadoEm)
                from DocumentoProcessamento dp2
                where dp2.entrega.pendencia.id = dp.entrega.pendencia.id
              )
            """)
    List<ObservacaoPendenciaView> findUltimasObservacoesByPendenciaIds(@Param("pendenciaIds") List<Long> pendenciaIds);
    List<DocumentoProcessamento> findByStatusOrderByAtualizadoEmDesc(ProcessamentoStatus status);
    List<DocumentoProcessamento> findAllByOrderByAtualizadoEmDesc();

    interface ObservacaoPendenciaView {
        Long getPendenciaId();
        String getObservacaoProcessamento();
    }
}
