package com.contabilidade.pj.empresa.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.empresa.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    List<Empresa> findAllByAtivoTrue();
}
