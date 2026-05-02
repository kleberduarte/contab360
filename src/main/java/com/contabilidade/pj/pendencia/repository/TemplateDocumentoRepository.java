package com.contabilidade.pj.pendencia.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.pendencia.entity.TemplateDocumento;

public interface TemplateDocumentoRepository extends JpaRepository<TemplateDocumento, Long> {

    long countByEmpresa_Id(Long empresaId);

    long countByClientePessoaFisica_Id(Long clientePessoaFisicaId);

    List<TemplateDocumento> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<TemplateDocumento> findByClientePessoaFisicaIdOrderByNomeAsc(Long clientePessoaFisicaId);

    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);

    boolean existsByClientePessoaFisicaIdAndNomeIgnoreCase(Long clientePessoaFisicaId, String nome);

    boolean existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(Long empresaId, String nome, Long id);

    boolean existsByClientePessoaFisicaIdAndNomeIgnoreCaseAndIdNot(Long clientePessoaFisicaId, String nome, Long id);
}
