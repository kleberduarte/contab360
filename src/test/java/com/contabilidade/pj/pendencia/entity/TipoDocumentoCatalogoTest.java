package com.contabilidade.pj.pendencia.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TipoDocumentoCatalogoTest {

    @Test
    void refinarGuiaReceitaFederal_incidenciaSemAcentoNaoDeveVirarCide() {
        // OCR e textos ASCII costumam vir "INCIDENCIA"; substring "CIDE" dispara falso positivo com contains().
        String texto = "DARF competencia 02/2026 INCIDENCIA imposto de renda pessoa fisica IRPF";
        assertEquals("GUIA_IRPF", TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, "GUIA_IRPF"));
    }

    @Test
    void refinarGuiaReceitaFederal_cideComoPalavraMantemCide() {
        String texto = "CONTRIBUIÇÃO CIDE SOBRE COMBUSTÍVEIS";
        assertEquals("GUIA_CIDE", TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, "GUIA_IRPF"));
    }

    @Test
    void refinarGuiaReceitaFederal_fontePagadoraPjNaoEhIrpj() {
        String texto = """
                COMPROVANTE ANUAL
                FONTE PAGADORA PESSOA JURÍDICA
                IMPOSTO SOBRE A RENDA RETIDO NA FONTE
                """;
        assertEquals("GUIA_IRPF", TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, "GUIA_IMPOSTO"));
    }

    @Test
    void refinarGuiaReceitaFederal_lucroPresumidoContinuaIrpj() {
        String texto = "DARF IRPJ LUCRO PRESUMIDO IMPOSTO DE RENDA PESSOA JURÍDICA";
        assertEquals("GUIA_IRPJ", TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, "GUIA_IMPOSTO"));
    }

    @Test
    void refinarGuiaReceitaFederal_corrigeIrpjErradoQuandoEhInformePf() {
        String texto = "FONTE PAGADORA PESSOA JURÍDICA IMPOSTO DE RENDA RETIDO NA FONTE";
        assertEquals("GUIA_IRPF", TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, "GUIA_IRPJ"));
    }

    @Test
    void refinarGuiaReceitaFederal_secaoRendimentosTributaveisEhIrpf() {
        String texto = "BANCO XYZ - INFORME 2025 RENDIMENTOS TRIBUTÁVEIS RECEBIDOS DE PESSOA JURÍDICA";
        assertEquals("GUIA_IRPF", TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, "GUIA_IMPOSTO"));
    }
}
