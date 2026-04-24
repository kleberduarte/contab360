package com.contabilidade.pj.fiscal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CertificadoDigitalPedidoRepository extends JpaRepository<CertificadoDigitalPedido, Long> {

    @Query("SELECT DISTINCT c FROM CertificadoDigitalPedido c LEFT JOIN FETCH c.empresa ORDER BY c.criadoEm DESC")
    List<CertificadoDigitalPedido> findAllWithEmpresaOrderByCriadoEmDesc();
}
