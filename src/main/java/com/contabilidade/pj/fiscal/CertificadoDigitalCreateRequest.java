package com.contabilidade.pj.fiscal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CertificadoDigitalCreateRequest {

    @NotBlank
    private String documentoSolicitante;

    @NotBlank
    private String titular;

    @Email
    @NotBlank
    private String emailContato;

    @NotBlank
    private String tipoCertificado;

    @NotNull
    private Integer validadeMeses;

    private Long empresaId;

    private LocalDate dataVencimentoPrevista;

    private String observacaoInterna;

    public String getDocumentoSolicitante() {
        return documentoSolicitante;
    }

    public void setDocumentoSolicitante(String documentoSolicitante) {
        this.documentoSolicitante = documentoSolicitante;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getEmailContato() {
        return emailContato;
    }

    public void setEmailContato(String emailContato) {
        this.emailContato = emailContato;
    }

    public String getTipoCertificado() {
        return tipoCertificado;
    }

    public void setTipoCertificado(String tipoCertificado) {
        this.tipoCertificado = tipoCertificado;
    }

    public Integer getValidadeMeses() {
        return validadeMeses;
    }

    public void setValidadeMeses(Integer validadeMeses) {
        this.validadeMeses = validadeMeses;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
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
