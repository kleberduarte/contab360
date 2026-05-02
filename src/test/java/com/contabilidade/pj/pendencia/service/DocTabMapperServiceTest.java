package com.contabilidade.pj.pendencia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.contabilidade.pj.pendencia.config.DocTabsProperties;
import org.junit.jupiter.api.Test;

class DocTabMapperServiceTest {

    @Test
    void idAbaParaTemplateNome_incidenciaNaoCasaCide() {
        DocTabsProperties props = new DocTabsProperties();
        DocTabMapperService mapper = new DocTabMapperService(props);
        assertEquals(
                "GUIA_IRPF",
                mapper.idAbaParaTemplateNome("Guia IRPF — texto padrão incidência tributária"));
    }

    @Test
    void idAbaParaTemplateNome_cideExplicito() {
        DocTabsProperties props = new DocTabsProperties();
        DocTabMapperService mapper = new DocTabMapperService(props);
        assertEquals("GUIA_CIDE", mapper.idAbaParaTemplateNome("Guia CIDE combustíveis"));
    }
}
