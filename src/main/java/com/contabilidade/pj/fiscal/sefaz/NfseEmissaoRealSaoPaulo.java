package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fluxo real São Paulo (ISS Digital / NFS-e PMSP): valida configuração e, quando habilitado, delega à transmissão.
 * A chamada HTTP/SOAP propriamente dita fica centralizada em {@link NfseSaoPauloCliente} (stub até integrar o manual oficial).
 */
public final class NfseEmissaoRealSaoPaulo implements NfseEmissaoPort {

    private final NfseProperties nfseProperties;
    private final SefazNfeProperties sefazNfeProperties;
    private final NfseSaoPauloCliente cliente;

    public NfseEmissaoRealSaoPaulo(
            NfseProperties nfseProperties,
            SefazNfeProperties sefazNfeProperties,
            NfseSaoPauloCliente cliente
    ) {
        this.nfseProperties = nfseProperties;
        this.sefazNfeProperties = sefazNfeProperties;
        this.cliente = cliente;
    }

    @Override
    public ResultadoEmissaoSefaz emitir(NotaFiscal nota) {
        if (!nfseProperties.isIntegracaoPmspAtiva()) {
            return ResultadoEmissaoSefaz.nfseRealPendenteConfig(
                    "NFSe PMSP: integração desligada (contab360.nfse.integracao-pmsp-ativa=false)."
            );
        }
        NfseProperties.SaoPaulo sp = nfseProperties.getSaoPaulo();
        if (!sp.isTransmissaoHabilitada()) {
            return ResultadoEmissaoSefaz.nfseRealPendenteConfig(
                    "NFSe PMSP: transmissão desligada (contab360.nfse.sao-paulo.transmissao-habilitada=false). "
                            + "Estrutura pronta — após credenciais e endpoints, altere para true."
            );
        }
        String cert = certificadoEfetivo(sp);
        if (cert.isBlank() || !Files.isRegularFile(Path.of(cert))) {
            return ResultadoEmissaoSefaz.nfseRealPendenteConfig(
                    "Certificado A1 (.pfx) obrigatório: contab360.nfse.sao-paulo.certificado-path "
                            + "ou contab360.sefaz.certificado-path apontando para arquivo existente."
            );
        }
        if (sp.getCertificadoSenha().isBlank() && sefazNfeProperties.getCertificadoSenha().isBlank()) {
            return ResultadoEmissaoSefaz.nfseRealPendenteConfig(
                    "Senha do certificado: defina contab360.nfse.sao-paulo.certificado-senha ou contab360.sefaz.certificado-senha."
            );
        }
        if (sp.getEndpointEnvio() == null || sp.getEndpointEnvio().isBlank()) {
            return ResultadoEmissaoSefaz.nfseRealPendenteConfig(
                    "Endpoint de envio PMSP não configurado: contab360.nfse.sao-paulo.endpoint-envio "
                            + "(conforme ambiente homologação/produção da documentação municipal)."
            );
        }
        String senha = sp.getCertificadoSenha() != null && !sp.getCertificadoSenha().isBlank()
                ? sp.getCertificadoSenha()
                : sefazNfeProperties.getCertificadoSenha();
        return cliente.transmitirNfse(nota, sp, cert, senha);
    }

    private String certificadoEfetivo(NfseProperties.SaoPaulo sp) {
        if (sp.getCertificadoPath() != null && !sp.getCertificadoPath().isBlank()) {
            return sp.getCertificadoPath().trim();
        }
        return sefazNfeProperties.getCertificadoPath() == null
                ? ""
                : sefazNfeProperties.getCertificadoPath().trim();
    }
}
