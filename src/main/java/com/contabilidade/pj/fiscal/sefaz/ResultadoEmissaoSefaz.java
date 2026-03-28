package com.contabilidade.pj.fiscal.sefaz;

/**
 * Resultado do envio à SEFAZ (ou simulação). Quando {@code autorizado} for falso, {@link #mensagem()} explica o motivo.
 */
public record ResultadoEmissaoSefaz(
        boolean autorizado,
        String chaveAcesso,
        String protocoloAutorizacao,
        String modo,
        String mensagem,
        /** Preenchido em emissão NFSe real (ex.: número impresso na NFS-e). */
        String nfseNumeroExibicao,
        /** Código de verificação retornado pela prefeitura. */
        String nfseCodigoVerificacao
) {
    public static ResultadoEmissaoSefaz simulacaoOk(String chave, String protocolo, String mensagem) {
        return new ResultadoEmissaoSefaz(true, chave, protocolo, "SIMULACAO", mensagem, null, null);
    }

    public static ResultadoEmissaoSefaz realPendente(String mensagem) {
        return new ResultadoEmissaoSefaz(false, "", "", "REAL", mensagem, null, null);
    }

    /** NFS-e municipal desligada explicitamente ({@code contab360.nfse.modo=DESLIGADO}). */
    public static ResultadoEmissaoSefaz nfseModoDesligado(String mensagem) {
        return new ResultadoEmissaoSefaz(false, "", "", "NFSE_DESLIGADO", mensagem, null, null);
    }

    /** Configuração PMSP incompleta ou transmissão ainda desabilitada. */
    public static ResultadoEmissaoSefaz nfseRealPendenteConfig(String mensagem) {
        return new ResultadoEmissaoSefaz(false, "", "", "NFSE_REAL_PENDENTE", mensagem, null, null);
    }

    /** Certificado e endpoint OK; falta implementar cliente HTTP/SOAP no código. */
    public static ResultadoEmissaoSefaz nfseTransmissaoNaoImplementada(String mensagem) {
        return new ResultadoEmissaoSefaz(false, "", "", "NFSE_TRANSMISSAO_STUB", mensagem, null, null);
    }

    /** NFS-e autorizada pela prefeitura (ajuste namespaces/XML ao XSD oficial se necessário). */
    public static ResultadoEmissaoSefaz nfseRealSucesso(
            String identificadorOuChave,
            String protocolo,
            String nfseNumero,
            String codigoVerificacao,
            String mensagem
    ) {
        return new ResultadoEmissaoSefaz(
                true,
                identificadorOuChave == null ? "" : identificadorOuChave,
                protocolo == null ? "" : protocolo,
                "NFSE_REAL",
                mensagem,
                nfseNumero,
                codigoVerificacao
        );
    }

    /** Erro de transporto, validação ou retorno com falha após chamada ao webservice. */
    public static ResultadoEmissaoSefaz nfseRealErro(String mensagem) {
        return new ResultadoEmissaoSefaz(false, "", "", "NFSE_REAL_ERRO", mensagem, null, null);
    }
}
