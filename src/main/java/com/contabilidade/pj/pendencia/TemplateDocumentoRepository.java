package com.contabilidade.pj.pendencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDocumentoRepository extends JpaRepository<TemplateDocumento, Long> {
    long countByEmpresa_Id(Long empresaId);

    List<TemplateDocumento> findByEmpresaIdOrderByNomeAsc(Long empresaId);
}
