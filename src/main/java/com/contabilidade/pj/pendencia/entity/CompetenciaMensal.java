package com.contabilidade.pj.pendencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "competencias_mensais",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ano", "mes"})
)
public class CompetenciaMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(2000)
    @Max(2100)
    @Column(nullable = false)
    private Integer ano;

    @Min(1)
    @Max(12)
    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private boolean arquivada = false;

    @Column
    private LocalDateTime arquivadaEm;

    public Long getId() {
        return id;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public boolean isArquivada() {
        return arquivada;
    }

    public void setArquivada(boolean arquivada) {
        this.arquivada = arquivada;
    }

    public LocalDateTime getArquivadaEm() {
        return arquivadaEm;
    }

    public void setArquivadaEm(LocalDateTime arquivadaEm) {
        this.arquivadaEm = arquivadaEm;
    }
}
