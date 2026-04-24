package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendenciaDocumentoRepository extends JpaRepository<PendenciaDocumento, Long> {

    long countByEmpresa_Id(Long empresaId);

    List<PendenciaDocumento> findByCompetenciaId(Long competenciaId);

    List<PendenciaDocumento> findByCompetenciaAnoAndCompetenciaMesOrderByEmpresaRazaoSocialAscTemplateDocumentoNomeAsc(
            Integer ano,
            Integer mes
    );

    List<PendenciaDocumento> findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
            Integer ano,
            Integer mes,
            Long empresaId
    );

    Optional<PendenciaDocumento> findByEmpresaIdAndTemplateDocumentoIdAndCompetenciaId(
            Long empresaId,
            Long templateDocumentoId,
            Long competenciaId
    );

    Optional<PendenciaDocumento> findById(Long id);
}
