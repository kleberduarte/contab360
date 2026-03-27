package com.contabilidade.pj.pendencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_processamento")
public class DocumentoProcessamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false, unique = true)
    private EntregaDocumento entrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessamentoStatus status = ProcessamentoStatus.RECEBIDO;

    @Column(nullable = false)
    private String tipoDocumento = "DESCONHECIDO";

    @Column(nullable = false)
    private Double confianca = 0.0;

    @Column(columnDefinition = "TEXT")
    private String dadosExtraidosJson;

    @Column(length = 500)
    private String observacaoProcessamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeveridadeRevisao severidade = SeveridadeRevisao.MEDIA;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public EntregaDocumento getEntrega() {
        return entrega;
    }

    public void setEntrega(EntregaDocumento entrega) {
        this.entrega = entrega;
    }

    public ProcessamentoStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessamentoStatus status) {
        this.status = status;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Double getConfianca() {
        return confianca;
    }

    public void setConfianca(Double confianca) {
        this.confianca = confianca;
    }

    public String getDadosExtraidosJson() {
        return dadosExtraidosJson;
    }

    public void setDadosExtraidosJson(String dadosExtraidosJson) {
        this.dadosExtraidosJson = dadosExtraidosJson;
    }

    public String getObservacaoProcessamento() {
        return observacaoProcessamento;
    }

    public void setObservacaoProcessamento(String observacaoProcessamento) {
        this.observacaoProcessamento = observacaoProcessamento;
    }

    public SeveridadeRevisao getSeveridade() {
        return severidade;
    }

    public void setSeveridade(SeveridadeRevisao severidade) {
        this.severidade = severidade;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
