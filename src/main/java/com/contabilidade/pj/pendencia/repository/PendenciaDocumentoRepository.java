package com.contabilidade.pj.pendencia.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.contabilidade.pj.pendencia.entity.PendenciaDocumento;

public interface PendenciaDocumentoRepository extends JpaRepository<PendenciaDocumento, Long> {

    long countByEmpresa_Id(Long empresaId);

    long countByClientePessoaFisica_Id(Long clientePessoaFisicaId);

    long countByTemplateDocumento_Id(Long templateDocumentoId);

    List<PendenciaDocumento> findByCompetenciaId(Long competenciaId);

    @Query("""
            select p from PendenciaDocumento p
            left join fetch p.empresa e
            left join fetch p.clientePessoaFisica cf
            join fetch p.templateDocumento t
            join fetch p.competencia c
            where c.ano = :ano and c.mes = :mes
            order by coalesce(e.razaoSocial, cf.nomeCompleto) asc, t.nome asc
            """)
    List<PendenciaDocumento> findByCompetenciaAnoAndMesParaContador(
            @Param("ano") Integer ano,
            @Param("mes") Integer mes
    );

    @Query("""
            select p from PendenciaDocumento p
            join fetch p.templateDocumento t
            join fetch p.competencia c
            left join fetch p.empresa e
            where c.ano = :ano and c.mes = :mes and e.id = :empresaId
            order by t.nome asc
            """)
    List<PendenciaDocumento> findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
            @Param("ano") Integer ano,
            @Param("mes") Integer mes,
            @Param("empresaId") Long empresaId
    );

    @Query("""
            select p from PendenciaDocumento p
            join fetch p.templateDocumento t
            join fetch p.competencia c
            left join fetch p.clientePessoaFisica cf
            where c.ano = :ano and c.mes = :mes and cf.id = :clientePfId
            order by t.nome asc
            """)
    List<PendenciaDocumento> findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
            @Param("ano") Integer ano,
            @Param("mes") Integer mes,
            @Param("clientePfId") Long clientePfId
    );

    Optional<PendenciaDocumento> findByTomadorUidAndTemplateDocumentoIdAndCompetenciaId(
            String tomadorUid,
            Long templateDocumentoId,
            Long competenciaId
    );
}
