package com.contabilidade.pj.pendencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDocumentoRepository extends JpaRepository<TemplateDocumento, Long> {

    long countByEmpresa_Id(Long empresaId);

    long countByClientePessoaFisica_Id(Long clientePessoaFisicaId);

    List<TemplateDocumento> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<TemplateDocumento> findByClientePessoaFisicaIdOrderByNomeAsc(Long clientePessoaFisicaId);
}
