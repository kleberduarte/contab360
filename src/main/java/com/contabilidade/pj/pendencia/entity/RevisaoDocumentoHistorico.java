package com.contabilidade.pj.pendencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "revisoes_documentos_historico")
public class RevisaoDocumentoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processamento_id", nullable = false)
    private DocumentoProcessamento processamento;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(length = 500)
    private String motivo;

    @Column(nullable = false, length = 120)
    private String usuarioNome;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public Long getId() {
        return id;
    }

    public DocumentoProcessamento getProcessamento() {
        return processamento;
    }

    public void setProcessamento(DocumentoProcessamento processamento) {
        this.processamento = processamento;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
