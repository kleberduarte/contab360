package com.contabilidade.pj.empresa.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.contabilidade.pj.empresa.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    List<Empresa> findAllByAtivoTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Empresa e set e.ativo = :ativo where e.id = :id")
    int atualizarAtivo(@Param("id") Long id, @Param("ativo") boolean ativo);
}
