package com.contabilidade.pj.pendencia;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "pendencias_documentos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "template_documento_id", "competencia_id"})
)
public class PendenciaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

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

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
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
