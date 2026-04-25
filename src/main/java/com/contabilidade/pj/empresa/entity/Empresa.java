package com.contabilidade.pj.empresa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 14, max = 14)
    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @NotBlank
    @Column(nullable = false)
    private String razaoSocial;

    @Column(length = 11)
    @Pattern(regexp = "^\\d{11}$", message = "cpfResponsavel deve conter 11 digitos.")
    private String cpfResponsavel;

    @Column(nullable = false)
    private boolean mei;

    private LocalDate vencimentoDas;

    private LocalDate vencimentoCertificadoMei;

    /** Quando {@code false}, a empresa está desativada (exclusão lógica) e não entra em listagens operacionais. */
    @Column(nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 1")
    private boolean ativo = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCpfResponsavel() {
        return cpfResponsavel;
    }

    public void setCpfResponsavel(String cpfResponsavel) {
        this.cpfResponsavel = cpfResponsavel;
    }

    public boolean isMei() {
        return mei;
    }

    public void setMei(boolean mei) {
        this.mei = mei;
    }

    public LocalDate getVencimentoDas() {
        return vencimentoDas;
    }

    public void setVencimentoDas(LocalDate vencimentoDas) {
        this.vencimentoDas = vencimentoDas;
    }

    public LocalDate getVencimentoCertificadoMei() {
        return vencimentoCertificadoMei;
    }

    public void setVencimentoCertificadoMei(LocalDate vencimentoCertificadoMei) {
        this.vencimentoCertificadoMei = vencimentoCertificadoMei;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
