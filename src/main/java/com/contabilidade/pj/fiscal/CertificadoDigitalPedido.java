package com.contabilidade.pj.fiscal;

import com.contabilidade.pj.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificados_digitais")
public class CertificadoDigitalPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 14)
    private String documentoSolicitante;

    @NotBlank
    @Column(nullable = false)
    private String titular;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String emailContato;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String tipoCertificado;

    @NotNull
    @Column(nullable = false)
    private Integer validadeMeses;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Previsão de vencimento do certificado (para alertas e lembretes). */
    private LocalDate dataVencimentoPrevista;

    @Column(length = 4000)
    private String observacaoInterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusCertificado status;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusCertificado.EM_ANALISE;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
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

    public StatusCertificado getStatus() {
        return status;
    }

    public void setStatus(StatusCertificado status) {
        this.status = status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
