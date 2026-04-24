package com.contabilidade.pj.empresa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    List<Empresa> findAllByAtivoTrue();
}
