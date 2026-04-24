package com.contabilidade.pj.fiscal.sefaz;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Emissão de NFS-e (municipal). Independent do bloco {@code contab360.sefaz} voltado a NFe estadual.
 * Por padrão a integração PMSP fica desligada ({@link #integracaoPmspAtiva} false): não instancia o fluxo SOAP real.
 */
@ConfigurationProperties(prefix = "contab360.nfse")
public class NfseProperties {

    /**
     * Quando {@code false} (padrão), o bean de emissão municipal nunca usa {@code NfseEmissaoRealSaoPaulo}; apenas
     * simulação ou desligado conforme {@link #modo}. Para habilitar o fluxo real, defina {@code true} e então
     * {@code modo=REAL}, endpoints, certificado e {@link SaoPaulo#setTransmissaoHabilitada(boolean)}.
     */
    private boolean integracaoPmspAtiva = false;

    /**
     * {@code SIMULACAO}: chave/protocolo fictícios (comportamento atual).
     * {@code REAL}: só efetivo se {@link #integracaoPmspAtiva}; usa fluxo PMSP quando {@link SaoPaulo#isTransmissaoHabilitada()}; senão retorna pendência explicativa.
     * {@code DESLIGADO}: não autoriza automaticamente (sem chave) — útil até homologar integração.
     */
    private Modo modo = Modo.SIMULACAO;

    private SaoPaulo saoPaulo = new SaoPaulo();

    public boolean isIntegracaoPmspAtiva() {
        return integracaoPmspAtiva;
    }

    public void setIntegracaoPmspAtiva(boolean integracaoPmspAtiva) {
        this.integracaoPmspAtiva = integracaoPmspAtiva;
    }

    public Modo getModo() {
        return modo;
    }

    public void setModo(Modo modo) {
        this.modo = modo;
    }

    public SaoPaulo getSaoPaulo() {
        return saoPaulo;
    }

    public void setSaoPaulo(SaoPaulo saoPaulo) {
        this.saoPaulo = saoPaulo;
    }

    public enum Modo {
        SIMULACAO,
        REAL,
        DESLIGADO
    }

    public static final class SaoPaulo {

        /**
         * {@code false} por padrão: estrutura e validação prontas, sem chamada à prefeitura.
         * {@code true} + {@code NfseProperties.Modo#REAL}: executa cliente real (ou stub até implementar SOAP/REST PMSP).
         */
        private boolean transmissaoHabilitada = false;

        private String ambiente = "HOMOLOGACAO";

        /**
         * URL do serviço de envio (conforme manual técnico ISS Digital PMSP — preencher ao integrar).
         */
        private String endpointEnvio = "";

        private String endpointConsultaLote = "";

        private String endpointCancelamento = "";

        /** Opcional; se vazio, reutiliza {@link SefazNfeProperties#getCertificadoPath()}. */
        private String certificadoPath = "";

        private String certificadoSenha = "";

        /**
         * Namespace do corpo SOAP (manual PMSP costuma usar algo como {@code http://www.prefeitura.sp.gov.br/nfe}).
         */
        private String soapNamespace = "http://www.prefeitura.sp.gov.br/nfe";

        /**
         * Elemento filho de Body, ex.: {@code RecepcionarLoteRpsRequest} (conferir WSDL/manual vigente).
         */
        private String soapRequestElementLocal = "RecepcionarLoteRpsRequest";

        /** Nome do elemento que envolve o XML do RPS (ex.: MensagemXML). */
        private String soapMensagemXmlElement = "MensagemXML";

        /** Conteúdo do elemento Versao dentro do request SOAP (ex.: 1). */
        private String soapVersaoElemento = "1";

        /** {@code 1.2} (application/soap+xml) ou {@code 1.1} (text/xml + SOAPAction). */
        private String soapProtocolo = "1.2";

        /** Obrigatório para SOAP 1.1; opcional em 1.2 (alguns servidores exigem). */
        private String soapAction = "";

        /** Namespace XML do lote (layout ABRASF usado como base; PMSP pode exigir outro — substitua conforme XSD oficial). */
        private String abrasfNamespace = "http://www.abrasf.org.br/nfse.xsd";

        /** Versão no atributo {@code versao} de {@code LoteRps} (ex.: 2.03). */
        private String abrasfVersaoLayout = "2.03";

        /** Município do serviço: 3550308 = São Paulo capital. */
        private String codigoMunicipioIbge = "3550308";

        private String serieRpsPadrao = "RPS";

        /**
         * Somente homologação interna: envia XML sem assinatura (a prefeitura deve rejeitar).
         * Não use em produção.
         */
        private boolean pularAssinaturaXml = false;

        /** {@code true} = RSA-SHA1 (comum em legados municipais); {@code false} = SHA256. */
        private boolean assinaturaSha1 = true;

        public boolean isTransmissaoHabilitada() {
            return transmissaoHabilitada;
        }

        public void setTransmissaoHabilitada(boolean transmissaoHabilitada) {
            this.transmissaoHabilitada = transmissaoHabilitada;
        }

        public String getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(String ambiente) {
            this.ambiente = ambiente;
        }

        public String getEndpointEnvio() {
            return endpointEnvio;
        }

        public void setEndpointEnvio(String endpointEnvio) {
            this.endpointEnvio = endpointEnvio;
        }

        public String getEndpointConsultaLote() {
            return endpointConsultaLote;
        }

        public void setEndpointConsultaLote(String endpointConsultaLote) {
            this.endpointConsultaLote = endpointConsultaLote;
        }

        public String getEndpointCancelamento() {
            return endpointCancelamento;
        }

        public void setEndpointCancelamento(String endpointCancelamento) {
            this.endpointCancelamento = endpointCancelamento;
        }

        public String getCertificadoPath() {
            return certificadoPath;
        }

        public void setCertificadoPath(String certificadoPath) {
            this.certificadoPath = certificadoPath;
        }

        public String getCertificadoSenha() {
            return certificadoSenha;
        }

        public void setCertificadoSenha(String certificadoSenha) {
            this.certificadoSenha = certificadoSenha;
        }

        public String getSoapNamespace() {
            return soapNamespace;
        }

        public void setSoapNamespace(String soapNamespace) {
            this.soapNamespace = soapNamespace;
        }

        public String getSoapRequestElementLocal() {
            return soapRequestElementLocal;
        }

        public void setSoapRequestElementLocal(String soapRequestElementLocal) {
            this.soapRequestElementLocal = soapRequestElementLocal;
        }

        public String getSoapMensagemXmlElement() {
            return soapMensagemXmlElement;
        }

        public void setSoapMensagemXmlElement(String soapMensagemXmlElement) {
            this.soapMensagemXmlElement = soapMensagemXmlElement;
        }

        public String getSoapVersaoElemento() {
            return soapVersaoElemento;
        }

        public void setSoapVersaoElemento(String soapVersaoElemento) {
            this.soapVersaoElemento = soapVersaoElemento;
        }

        public String getSoapProtocolo() {
            return soapProtocolo;
        }

        public void setSoapProtocolo(String soapProtocolo) {
            this.soapProtocolo = soapProtocolo;
        }

        public String getSoapAction() {
            return soapAction;
        }

        public void setSoapAction(String soapAction) {
            this.soapAction = soapAction;
        }

        public String getAbrasfNamespace() {
            return abrasfNamespace;
        }

        public void setAbrasfNamespace(String abrasfNamespace) {
            this.abrasfNamespace = abrasfNamespace;
        }

        public String getAbrasfVersaoLayout() {
            return abrasfVersaoLayout;
        }

        public void setAbrasfVersaoLayout(String abrasfVersaoLayout) {
            this.abrasfVersaoLayout = abrasfVersaoLayout;
        }

        public String getCodigoMunicipioIbge() {
            return codigoMunicipioIbge;
        }

        public void setCodigoMunicipioIbge(String codigoMunicipioIbge) {
            this.codigoMunicipioIbge = codigoMunicipioIbge;
        }

        public String getSerieRpsPadrao() {
            return serieRpsPadrao;
        }

        public void setSerieRpsPadrao(String serieRpsPadrao) {
            this.serieRpsPadrao = serieRpsPadrao;
        }

        public boolean isPularAssinaturaXml() {
            return pularAssinaturaXml;
        }

        public void setPularAssinaturaXml(boolean pularAssinaturaXml) {
            this.pularAssinaturaXml = pularAssinaturaXml;
        }

        public boolean isAssinaturaSha1() {
            return assinaturaSha1;
        }

        public void setAssinaturaSha1(boolean assinaturaSha1) {
            this.assinaturaSha1 = assinaturaSha1;
        }
    }
}
