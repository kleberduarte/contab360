package com.contabilidade.pj.fiscal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notas_fiscais")
public class NotaFiscal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 14)
    private String documentoEmitente;

    @NotBlank
    @Column(nullable = false, length = 14)
    private String documentoDestinatario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDocumentoFiscal tipoDocumento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoOperacaoFiscal tipoOperacao;

    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @NotNull
    @Column(nullable = false)
    private LocalDate dataEmissao;

    @Column(nullable = false, length = 80)
    private String municipio;

    @Column(nullable = false, length = 2)
    private String uf;

    /** Chave de 44 dígitos (simulada ou retorno SEFAZ). */
    @Column(length = 44)
    private String chaveAcesso;

    @Column(length = 48)
    private String protocoloAutorizacao;

    /** SIMULACAO ou REAL. */
    @Column(length = 20)
    private String sefazModo;

    @Column(length = 500)
    private String sefazMensagem;

    /** Ex.: {@code 00002701} — exibicao no PDF NFS-e SP (opcional). */
    @Column(length = 12)
    private String nfseNumeroExibicao;

    /** Ex.: {@code HXBA-6GMC} — opcional para cartao de teste PMSP. */
    @Column(length = 16)
    private String nfseCodigoVerificacao;

    @Column(length = 200)
    private String nfseRazaoEmitente;

    @Column(length = 200)
    private String nfseRazaoTomador;

    @Column(length = 300)
    private String nfseEnderecoEmitente;

    @Column(length = 300)
    private String nfseEnderecoTomador;

    @Column(length = 24)
    private String nfseInscricaoMunicipalEmitente;

    @Column(length = 24)
    private String nfseInscricaoMunicipalTomador;

    @Column(length = 2000)
    private String nfseDiscriminacao;

    @Column(length = 160)
    private String nfseEmailTomador;

    @Column(length = 500)
    private String nfseCodigoServicoTexto;

    @Column(precision = 14, scale = 2)
    private BigDecimal nfseValorDeducoes;

    @Column(precision = 5, scale = 2)
    private BigDecimal nfseAliquotaIss;

    /** Credito para abatimento do IPTU (quadro da NFS-e SP). */
    @Column(precision = 14, scale = 2)
    private BigDecimal nfseCreditoIptu;

    private LocalDate nfseDataVencimentoIss;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentoEmitente() {
        return documentoEmitente;
    }

    public void setDocumentoEmitente(String documentoEmitente) {
        this.documentoEmitente = documentoEmitente;
    }

    public String getDocumentoDestinatario() {
        return documentoDestinatario;
    }

    public void setDocumentoDestinatario(String documentoDestinatario) {
        this.documentoDestinatario = documentoDestinatario;
    }

    public TipoDocumentoFiscal getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumentoFiscal tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public TipoOperacaoFiscal getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacaoFiscal tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public String getProtocoloAutorizacao() {
        return protocoloAutorizacao;
    }

    public void setProtocoloAutorizacao(String protocoloAutorizacao) {
        this.protocoloAutorizacao = protocoloAutorizacao;
    }

    public String getSefazModo() {
        return sefazModo;
    }

    public void setSefazModo(String sefazModo) {
        this.sefazModo = sefazModo;
    }

    public String getSefazMensagem() {
        return sefazMensagem;
    }

    public void setSefazMensagem(String sefazMensagem) {
        this.sefazMensagem = sefazMensagem;
    }

    public String getNfseNumeroExibicao() {
        return nfseNumeroExibicao;
    }

    public void setNfseNumeroExibicao(String nfseNumeroExibicao) {
        this.nfseNumeroExibicao = nfseNumeroExibicao;
    }

    public String getNfseCodigoVerificacao() {
        return nfseCodigoVerificacao;
    }

    public void setNfseCodigoVerificacao(String nfseCodigoVerificacao) {
        this.nfseCodigoVerificacao = nfseCodigoVerificacao;
    }

    public String getNfseRazaoEmitente() {
        return nfseRazaoEmitente;
    }

    public void setNfseRazaoEmitente(String nfseRazaoEmitente) {
        this.nfseRazaoEmitente = nfseRazaoEmitente;
    }

    public String getNfseRazaoTomador() {
        return nfseRazaoTomador;
    }

    public void setNfseRazaoTomador(String nfseRazaoTomador) {
        this.nfseRazaoTomador = nfseRazaoTomador;
    }

    public String getNfseEnderecoEmitente() {
        return nfseEnderecoEmitente;
    }

    public void setNfseEnderecoEmitente(String nfseEnderecoEmitente) {
        this.nfseEnderecoEmitente = nfseEnderecoEmitente;
    }

    public String getNfseEnderecoTomador() {
        return nfseEnderecoTomador;
    }

    public void setNfseEnderecoTomador(String nfseEnderecoTomador) {
        this.nfseEnderecoTomador = nfseEnderecoTomador;
    }

    public String getNfseInscricaoMunicipalEmitente() {
        return nfseInscricaoMunicipalEmitente;
    }

    public void setNfseInscricaoMunicipalEmitente(String nfseInscricaoMunicipalEmitente) {
        this.nfseInscricaoMunicipalEmitente = nfseInscricaoMunicipalEmitente;
    }

    public String getNfseInscricaoMunicipalTomador() {
        return nfseInscricaoMunicipalTomador;
    }

    public void setNfseInscricaoMunicipalTomador(String nfseInscricaoMunicipalTomador) {
        this.nfseInscricaoMunicipalTomador = nfseInscricaoMunicipalTomador;
    }

    public String getNfseDiscriminacao() {
        return nfseDiscriminacao;
    }

    public void setNfseDiscriminacao(String nfseDiscriminacao) {
        this.nfseDiscriminacao = nfseDiscriminacao;
    }

    public String getNfseEmailTomador() {
        return nfseEmailTomador;
    }

    public void setNfseEmailTomador(String nfseEmailTomador) {
        this.nfseEmailTomador = nfseEmailTomador;
    }

    public String getNfseCodigoServicoTexto() {
        return nfseCodigoServicoTexto;
    }

    public void setNfseCodigoServicoTexto(String nfseCodigoServicoTexto) {
        this.nfseCodigoServicoTexto = nfseCodigoServicoTexto;
    }

    public BigDecimal getNfseValorDeducoes() {
        return nfseValorDeducoes;
    }

    public void setNfseValorDeducoes(BigDecimal nfseValorDeducoes) {
        this.nfseValorDeducoes = nfseValorDeducoes;
    }

    public BigDecimal getNfseAliquotaIss() {
        return nfseAliquotaIss;
    }

    public void setNfseAliquotaIss(BigDecimal nfseAliquotaIss) {
        this.nfseAliquotaIss = nfseAliquotaIss;
    }

    public BigDecimal getNfseCreditoIptu() {
        return nfseCreditoIptu;
    }

    public void setNfseCreditoIptu(BigDecimal nfseCreditoIptu) {
        this.nfseCreditoIptu = nfseCreditoIptu;
    }

    public LocalDate getNfseDataVencimentoIss() {
        return nfseDataVencimentoIss;
    }

    public void setNfseDataVencimentoIss(LocalDate nfseDataVencimentoIss) {
        this.nfseDataVencimentoIss = nfseDataVencimentoIss;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
