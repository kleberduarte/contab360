package com.contabilidade.pj.fiscal.dto;

import java.time.LocalDate;
import com.contabilidade.pj.fiscal.entity.StatusCertificado;

public class CertificadoDigitalUpdateRequest {

    private StatusCertificado status;
    private LocalDate dataVencimentoPrevista;
    private String observacaoInterna;

    public StatusCertificado getStatus() {
        return status;
    }

    public void setStatus(StatusCertificado status) {
        this.status = status;
    }

    public LocalDate getDataVencimentoPrevista() {
        return dataVencimentoPrevista;
    }

    public void setDataVencimentoPrevista(LocalDate dataVencimentoPrevista) {
        this.dataVencimentoPrevista = dataVencimentoPrevista;
    }

    public String getObservacaoInterna() {
        return observacaoInterna;
    }

    public void setObservacaoInterna(String observacaoInterna) {
        this.observacaoInterna = observacaoInterna;
    }
}
