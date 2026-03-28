package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Map;

public record DadosExtraidosPendenciaDto(
        Long pendenciaId,
        Long processamentoId,
        String statusProcessamento,
        String tipoDocumento,
        Double confianca,
        String nomeArquivoOriginal,
        List<CampoExtraido> campos,
        /** Presente quando a IA capturou estrutura completa (ex.: holerite). */
        Map<String, Object> detalhamentoDocumento,
        String capturaPerfil
) {
    public record CampoExtraido(String nome, String valor, String tipo) {
        public CampoExtraido(String nome, String valor) {
            this(nome, valor, "TEXTO");
        }
    }
}
