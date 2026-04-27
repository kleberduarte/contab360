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

    @Query("""
            select dp from DocumentoProcessamento dp
            join fetch dp.entrega e
            join fetch e.pendencia p
            join fetch p.competencia c
            join fetch p.templateDocumento
            where p.empresa.id = :empresaId
              and dp.status = :status
            order by dp.atualizadoEm desc
            """)
    List<DocumentoProcessamento> findByEmpresaIdAndStatus(
            @Param("empresaId") Long empresaId,
            @Param("status") ProcessamentoStatus status
    );

    /**
     * Mesma árvore que {@link #findByEmpresaIdAndStatus}, mas exclui documentos cuja competência mensal está arquivada.
     * Filtro aplicado no SQL (evita inconsistência com stream em memória).
     */
    @Query("""
            select dp from DocumentoProcessamento dp
            join fetch dp.entrega e
            join fetch e.pendencia p
            join fetch p.competencia c
            join fetch p.templateDocumento
            where p.empresa.id = :empresaId
              and dp.status = :status
              and c.arquivada = false
            order by dp.atualizadoEm desc
            """)
    List<DocumentoProcessamento> findByEmpresaIdAndStatusExcluindoCompetenciaArquivada(
            @Param("empresaId") Long empresaId,
            @Param("status") ProcessamentoStatus status
    );

    @Query("""
            select dp from DocumentoProcessamento dp
            join fetch dp.entrega e
            join fetch e.pendencia p
            join fetch p.competencia c
            join fetch p.templateDocumento
            where p.clientePessoaFisica.id = :clientePfId
              and dp.status = :status
            order by dp.atualizadoEm desc
            """)
    List<DocumentoProcessamento> findByClientePessoaFisicaIdAndStatus(
            @Param("clientePfId") Long clientePfId,
            @Param("status") ProcessamentoStatus status
    );

    @Query("""
            select dp from DocumentoProcessamento dp
            join fetch dp.entrega e
            join fetch e.pendencia p
            join fetch p.competencia c
            join fetch p.templateDocumento
            where p.clientePessoaFisica.id = :clientePfId
              and dp.status = :status
              and c.arquivada = false
            order by dp.atualizadoEm desc
            """)
    List<DocumentoProcessamento> findByClientePessoaFisicaIdAndStatusExcluindoCompetenciaArquivada(
            @Param("clientePfId") Long clientePfId,
            @Param("status") ProcessamentoStatus status
    );

    interface ObservacaoPendenciaView {
        Long getPendenciaId();
        String getObservacaoProcessamento();
    }
}
