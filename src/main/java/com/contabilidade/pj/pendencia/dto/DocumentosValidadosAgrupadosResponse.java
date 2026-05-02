package com.contabilidade.pj.pendencia.dto;

import java.util.List;
import java.util.Map;

public record DocumentosValidadosAgrupadosResponse(
        Long empresaId,
        String cnpj,
        String razaoSocial,
        Long clientePessoaFisicaId,
        String cpfClientePf,
        String nomeClientePf,
        List<AbaDocumentosResponse> abas
) {

    public record AbaDocumentosResponse(
            String idAba,
            String titulo,
            List<DocumentoValidadoItemResponse> documentos
    ) {
    }

    public record DocumentoValidadoItemResponse(
            Long pendenciaId,
            Long processamentoId,
            String templatePendenciaNome,
            String nomeArquivoOriginal,
            String tipoDocumentoDetectado,
            String atualizadoEm,
            Double confianca,
            List<DadosExtraidosPendenciaDto.CampoExtraido> campos,
            Map<String, Object> detalhamentoDocumento,
            String capturaPerfil,
            String status
    ) {
    }
}
