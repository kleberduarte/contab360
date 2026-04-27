package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.clientepf.ClientePessoaFisica;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "pendencias_documentos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pendencias_tomador_template_competencia",
                columnNames = {"tomador_uid", "template_documento_id", "competencia_id"}
        )
)
public class PendenciaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_pessoa_fisica_id")
    private ClientePessoaFisica clientePessoaFisica;

    @Column(name = "tomador_uid", nullable = false, length = 24)
    private String tomadorUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_documento_id", nullable = false)
    private TemplateDocumento templateDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private CompetenciaMensal competencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PendenciaStatus status = PendenciaStatus.PENDENTE;

    @Column(nullable = false)
    private LocalDate vencimento;

    @PrePersist
    @PreUpdate
    void sincronizarTomadorUid() {
        if (empresa != null) {
            tomadorUid = PendenciaTomadorUids.empresa(empresa.getId());
        } else if (clientePessoaFisica != null) {
            tomadorUid = PendenciaTomadorUids.clientePessoaFisica(clientePessoaFisica.getId());
        }
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public ClientePessoaFisica getClientePessoaFisica() {
        return clientePessoaFisica;
    }

    public void setClientePessoaFisica(ClientePessoaFisica clientePessoaFisica) {
        this.clientePessoaFisica = clientePessoaFisica;
    }

    public String getTomadorUid() {
        return tomadorUid;
    }

    public void setTomadorUid(String tomadorUid) {
        this.tomadorUid = tomadorUid;
    }

    public TemplateDocumento getTemplateDocumento() {
        return templateDocumento;
    }

    public void setTemplateDocumento(TemplateDocumento templateDocumento) {
        this.templateDocumento = templateDocumento;
    }

    public CompetenciaMensal getCompetencia() {
        return competencia;
    }

    public void setCompetencia(CompetenciaMensal competencia) {
        this.competencia = competencia;
    }

    public PendenciaStatus getStatus() {
        return status;
    }

    public void setStatus(PendenciaStatus status) {
        this.status = status;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }
}
