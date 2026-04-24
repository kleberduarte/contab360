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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "documentos_dados_extraidos")
public class DocumentoDadoExtraido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processamento_id", nullable = false)
    private DocumentoProcessamento processamento;

    @Column(nullable = false, length = 120)
    private String nomeCampo;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private TipoCampoExtraido tipoCampo = TipoCampoExtraido.TEXTO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DocumentoProcessamento getProcessamento() {
        return processamento;
    }

    public void setProcessamento(DocumentoProcessamento processamento) {
        this.processamento = processamento;
    }

    public String getNomeCampo() {
        return nomeCampo;
    }

    public void setNomeCampo(String nomeCampo) {
        this.nomeCampo = nomeCampo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public TipoCampoExtraido getTipoCampo() {
        return tipoCampo;
    }

    public void setTipoCampo(TipoCampoExtraido tipoCampo) {
        this.tipoCampo = tipoCampo == null ? TipoCampoExtraido.TEXTO : tipoCampo;
    }
}
