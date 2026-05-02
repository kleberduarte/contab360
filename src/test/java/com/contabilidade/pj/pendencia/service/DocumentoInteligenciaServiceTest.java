package com.contabilidade.pj.pendencia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.pendencia.entity.DocumentoProcessamento;
import com.contabilidade.pj.pendencia.entity.PendenciaDocumento;
import com.contabilidade.pj.pendencia.entity.TemplateDocumento;
import com.contabilidade.pj.pendencia.repository.DocumentoDadoExtraidoRepository;
import com.contabilidade.pj.pendencia.repository.DocumentoProcessamentoRepository;
import com.contabilidade.pj.pendencia.repository.PendenciaDocumentoRepository;
import com.contabilidade.pj.pendencia.repository.RevisaoDocumentoHistoricoRepository;
import com.contabilidade.pj.pendencia.repository.TemplateDocumentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentoInteligenciaServiceTest {

    @Mock
    private DocumentoProcessamentoRepository documentoProcessamentoRepository;
    @Mock
    private DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository;
    @Mock
    private TemplateDocumentoRepository templateDocumentoRepository;
    @Mock
    private RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository;
    @Mock
    private PendenciaDocumentoRepository pendenciaDocumentoRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    @Mock
    private CompetenciaArquivamentoService competenciaArquivamentoService;
    @Mock
    private DocTabMapperService docTabMapperService;

    private DocumentoInteligenciaService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoInteligenciaService(
                documentoProcessamentoRepository,
                documentoDadoExtraidoRepository,
                templateDocumentoRepository,
                revisaoDocumentoHistoricoRepository,
                pendenciaDocumentoRepository,
                empresaRepository,
                clientePessoaFisicaRepository,
                competenciaArquivamentoService,
                docTabMapperService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void extrairDetalhamentoGuiaIrpfDeveMapearTopicosCompletos() throws Exception {
        String texto = """
                COMPROVANTE DE RENDIMENTOS PAGOS E DE IMPOSTO SOBRE A RENDA RETIDO NA FONTE
                EMPRESA TESTE LTDA
                Nome Empresarial / Nome Completo
                CNPJ / CPF 12.345.678/0001-95

                2. Pessoa Física Beneficiária dos Rendimentos
                JOAO DA SILVA
                Nome Completo 123.456.789-01 CPF
                1888 - TRABALHO SEM VINCULO EMPREGATICIO

                3. Rendimentos Tributáveis, Deduções e Imposto sobre a Renda Retido na Fonte (IRRF)
                01. Total dos rendimentos (inclusive férias) 150.574,56
                02. Contribuição Previdenciária Oficial 11.419,44
                03. Contribuição a Entidades de Previdência Complementar, pública ou privada, e a Fundo de Aposentadoria Programada Individual - FAPI 0,00
                04. Pensão Alimentícia 0,00
                05. Imposto sobre a Renda Retido na Fonte (IRRF) 26.162,45

                4. Rendimentos Isentos e Não Tributáveis
                01. Lucros e dividendos 1.000,00
                02. Indenizações 0,00

                5. Rendimentos Sujeitos à Tributação Exclusiva (Rendimento Líquido)
                01. Décimo terceiro salário 10.000,00

                6. Rendimentos Recebidos Acumuladamente Art. 12-A da Lei nº 7.713, de 1988 (sujeitos à tributação exclusiva)
                01. Parcela 2.500,00

                7. Informações Complementares
                Conteudo complementar de teste para validacao.

                8. Responsável pelas Informações
                Nome DATA Assinatura
                MARIA RESPONSAVEL 30/04/2026
                """;

        Map<String, String> camposBase = new LinkedHashMap<>();
        camposBase.put("cnpj", "12345678000195");

        Method extrair = DocumentoInteligenciaService.class.getDeclaredMethod(
                "extrairDetalhamentoGuiaImpostoIrpf",
                String.class,
                Map.class
        );
        extrair.setAccessible(true);
        Map<String, Object> det = (Map<String, Object>) extrair.invoke(service, texto, camposBase);

        assertNotNull(det);
        List<Map<String, Object>> secoes = (List<Map<String, Object>>) det.get("secoes");
        assertNotNull(secoes);
        assertEquals(8, secoes.size());

        assertEquals("150574.56", camposBase.get("totalDosRendimentosInclusiveFerias"));
        assertEquals("11419.44", camposBase.get("contribuicaoPrevidenciariaOficial"));
        assertEquals("0.00", camposBase.get("contribuicaoEntidadesPrevidenciaComplementarPublicaOuPrivadaEFapi"));
        assertEquals("0.00", camposBase.get("pensaoAlimenticia"));
        assertEquals("26162.45", camposBase.get("impostoSobreARendaRetidoNaFonte"));
        assertEquals("JOAO DA SILVA", camposBase.get("nomeBeneficiario"));
        assertEquals("12345678901", camposBase.get("cpfBeneficiario"));
        assertEquals("Maria Responsavel", camposBase.get("nomeResponsavelInformacoes"));
        assertEquals("Maria Responsavel", camposBase.get("Nome"));
        assertTrue(camposBase.containsKey("secao4RendimentosIsentosItem01"));
        assertTrue(camposBase.containsKey("secao5TributacaoExclusivaItem01"));
        assertTrue(camposBase.containsKey("secao6RendimentosAcumuladosItem01"));

        Method completa = DocumentoInteligenciaService.class.getDeclaredMethod(
                "guiaIrpfTopicosCompletos",
                Map.class
        );
        completa.setAccessible(true);
        boolean ok = (boolean) completa.invoke(null, camposBase);
        assertTrue(ok);
    }

    @Test
    void guiaIrpfTopicosCompletosDeveRetornarFalsoQuandoFaltarCampoEssencial() throws Exception {
        Map<String, String> camposBase = new LinkedHashMap<>();
        camposBase.put("totalDosRendimentosInclusiveFerias", "100.00");
        camposBase.put("contribuicaoPrevidenciariaOficial", "10.00");
        camposBase.put("contribuicaoEntidadesPrevidenciaComplementarPublicaOuPrivadaEFapi", "0.00");
        camposBase.put("pensaoAlimenticia", "0.00");
        camposBase.put("impostoSobreARendaRetidoNaFonte", "20.00");
        camposBase.put("nomeFontePagadora", "EMPRESA TESTE");
        camposBase.put("cnpjCpfFontePagadora", "12345678000195");
        camposBase.put("nomeBeneficiario", "JOAO");
        camposBase.put("cpfBeneficiario", "12345678901");
        camposBase.put("naturezaRendimento", "1888 - TESTE");
        camposBase.put("informacoesComplementaresTexto", "texto");
        camposBase.put("dataResponsavelInformacoes", "2026-04-30");
        // nomeResponsavelInformacoes ausente para forcar reprovação.

        Method completa = DocumentoInteligenciaService.class.getDeclaredMethod(
                "guiaIrpfTopicosCompletos",
                Map.class
        );
        completa.setAccessible(true);
        boolean ok = (boolean) completa.invoke(null, camposBase);
        assertFalse(ok);
    }

    @Test
    @SuppressWarnings("unchecked")
    void montarDadosExtraidosJsonDeveIncluirCamposDaSecaoNoCamposExtraidos() throws Exception {
        Map<String, String> camposBase = new LinkedHashMap<>();
        camposBase.put("cnpj", "12345678000195");

        Map<String, Object> secao8 = new LinkedHashMap<>();
        secao8.put("id", "secao8");
        secao8.put("titulo", "8. Responsável pelas Informações");
        List<Map<String, Object>> camposSecao8 = new ArrayList<>();
        camposSecao8.add(Map.of("nome", "Nome", "valor", "Rosangela Mota Nagem", "tipo", "TEXTO"));
        camposSecao8.add(Map.of("nome", "Data", "valor", "2026-02-24", "tipo", "DATA"));
        camposSecao8.add(Map.of("nome", "Assinatura", "valor", "Dispensada (SRF 149/98)", "tipo", "TEXTO"));
        secao8.put("campos", camposSecao8);

        Map<String, Object> det = new LinkedHashMap<>();
        det.put("tipoEstrutura", "GUIA_IMPOSTO_IRPF");
        det.put("secoes", List.of(secao8));

        Method montar = DocumentoInteligenciaService.class.getDeclaredMethod(
                "montarDadosExtraidosJson",
                String.class,
                String.class,
                Map.class,
                double.class,
                String.class,
                List.class,
                Object.class,
                String.class
        );
        montar.setAccessible(true);
        String json = (String) montar.invoke(
                service,
                "OCR",
                "GUIA_IRPF",
                camposBase,
                0.95d,
                "SUCESSO",
                List.of(),
                det,
                "GUIA_IMPOSTO_IRPF_COMPLETO"
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode camposExtraidos = root.path("camposExtraidos");
        assertEquals("Rosangela Mota Nagem", camposExtraidos.path("Nome").asText());
        assertEquals("2026-02-24", camposExtraidos.path("Data").asText());
        assertEquals("Dispensada (SRF 149/98)", camposExtraidos.path("Assinatura").asText());
        assertEquals("Rosangela Mota Nagem", camposExtraidos.path("nomeResponsavelInformacoes").asText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void extrairDetalhamentoGuiaIrpfNaoDeveUsarBeneficiarioComoResponsavel() throws Exception {
        String texto = """
                1. Fonte Pagadora Pessoa Jurídica ou Pessoa Física
                Nome Empresarial / Nome Completo
                Banco Votorantim S/A
                CNPJ / CPF 59.588.111/0001-03

                2. Pessoa Física Beneficiária dos Rendimentos
                Nome Completo
                Kleber Duarte Laurindo De Souza
                CPF 349.701.218-18
                Natureza do Rendimento 0561- Rendimentos do Trabalho Assalariado - Brasil

                3. Rendimentos Tributáveis, Deduções e Imposto sobre a Renda Retido na Fonte (IRRF)
                01. Total dos rendimentos (inclusive férias) 150.574,66
                02. Contribuição Previdenciária Oficial 11.419,44
                03. Contribuição a Entidades de Previdência Complementar, pública ou privada, e a Fundo de Aposentadoria Programada Individual - FAPI 0,00
                04. Pensão Alimentícia 0,00
                05. Imposto sobre a Renda Retido na Fonte (IRRF) 26.162,49

                7. Informações Complementares
                Conteudo complementar de teste.

                8. Responsável pelas Informações
                Data
                24/02/2026
                Assinatura
                Dispensada (SRF 149/98)
                """;

        Map<String, String> camposBase = new LinkedHashMap<>();
        camposBase.put("cnpj", "59588111000103");
        camposBase.put("nomeBeneficiario", "Kleber Duarte Laurindo De Souza");

        Method extrair = DocumentoInteligenciaService.class.getDeclaredMethod(
                "extrairDetalhamentoGuiaImpostoIrpf",
                String.class,
                Map.class
        );
        extrair.setAccessible(true);
        Map<String, Object> det = (Map<String, Object>) extrair.invoke(service, texto, camposBase);

        assertNotNull(det);
        assertEquals("", camposBase.get("nomeResponsavelInformacoes"));
    }

    @Test
    void motivoGuiaIrpfIncompletaDeveSerEspecificoQuandoFaltarApenasNomeResponsavel() throws Exception {
        Map<String, String> camposBase = new LinkedHashMap<>();
        camposBase.put("totalDosRendimentosInclusiveFerias", "100.00");
        camposBase.put("contribuicaoPrevidenciariaOficial", "10.00");
        camposBase.put("contribuicaoEntidadesPrevidenciaComplementarPublicaOuPrivadaEFapi", "0.00");
        camposBase.put("pensaoAlimenticia", "0.00");
        camposBase.put("impostoSobreARendaRetidoNaFonte", "20.00");
        camposBase.put("nomeFontePagadora", "EMPRESA TESTE");
        camposBase.put("cnpjCpfFontePagadora", "12345678000195");
        camposBase.put("nomeBeneficiario", "JOAO");
        camposBase.put("cpfBeneficiario", "12345678901");
        camposBase.put("naturezaRendimento", "1888 - TESTE");
        camposBase.put("informacoesComplementaresTexto", "texto");
        camposBase.put("dataResponsavelInformacoes", "2026-04-30");
        camposBase.put("nomeResponsavelInformacoes", "");

        Method motivo = DocumentoInteligenciaService.class.getDeclaredMethod(
                "motivoGuiaIrpfIncompleta",
                Map.class
        );
        motivo.setAccessible(true);
        String msg = (String) motivo.invoke(null, camposBase);
        assertEquals("Seção 8: campo Nome (conforme PDF) não identificado no documento.", msg);
    }

    @Test
    void extrairNomeResponsavelBloco8DeveLerRotuloNomeEmLinhaIsolada() throws Exception {
        Method extrair = DocumentoInteligenciaService.class.getDeclaredMethod("extrairNomeResponsavelBloco8", String.class);
        extrair.setAccessible(true);
        String bloco = "Nome\nRosangela Mota Nagem\nDATA\n24/02/2026\n";
        assertEquals("Rosangela Mota Nagem", extrair.invoke(null, bloco));
    }

    @Test
    void guiaIrpfTopicosCompletosAceitaSomenteChaveNomeNaSecao8() throws Exception {
        Map<String, String> camposBase = new LinkedHashMap<>();
        camposBase.put("totalDosRendimentosInclusiveFerias", "100.00");
        camposBase.put("contribuicaoPrevidenciariaOficial", "10.00");
        camposBase.put("contribuicaoEntidadesPrevidenciaComplementarPublicaOuPrivadaEFapi", "0.00");
        camposBase.put("pensaoAlimenticia", "0.00");
        camposBase.put("impostoSobreARendaRetidoNaFonte", "20.00");
        camposBase.put("nomeFontePagadora", "EMPRESA TESTE");
        camposBase.put("cnpjCpfFontePagadora", "12345678000195");
        camposBase.put("nomeBeneficiario", "JOAO");
        camposBase.put("cpfBeneficiario", "12345678901");
        camposBase.put("naturezaRendimento", "1888 - TESTE");
        camposBase.put("informacoesComplementaresTexto", "texto");
        camposBase.put("dataResponsavelInformacoes", "2026-04-30");
        camposBase.put("Nome", "Maria Responsavel");

        Method completa = DocumentoInteligenciaService.class.getDeclaredMethod(
                "guiaIrpfTopicosCompletos",
                Map.class
        );
        completa.setAccessible(true);
        assertTrue((boolean) completa.invoke(null, camposBase));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preencherCamposQuadroIssNfseDeveCapturarTributosEResumoFinanceiro() throws Exception {
        String texto = """
                Valor dos Servicos: R$ 2.500,00
                Deducoes: R$ 0,00
                Aliquota ISS: 5,00%
                Valor ISS: R$ 125,00
                PIS: R$ 16,25
                COFINS: R$ 75,00
                INSS: R$ 0,00
                IRRF: R$ 37,50
                CSLL: R$ 25,00
                Valor Liquido: R$ 2.375,00
                """;

        Method preencher = DocumentoInteligenciaService.class.getDeclaredMethod(
                "preencherCamposQuadroIssNfse",
                String.class,
                Map.class
        );
        preencher.setAccessible(true);

        Map<String, String> campos = new LinkedHashMap<>();
        preencher.invoke(service, texto, campos);

        assertEquals("2500.00", campos.get("valorServicos"));
        assertEquals("0.00", campos.get("deducoesNfse"));
        assertEquals("5.00", campos.get("aliquotaIssPercentual"));
        assertEquals("125.00", campos.get("valorIss"));
        assertEquals("16.25", campos.get("pis"));
        assertEquals("75.00", campos.get("cofins"));
        assertEquals("0.00", campos.get("inss"));
        assertEquals("37.50", campos.get("irrf"));
        assertEquals("25.00", campos.get("csll"));
        assertEquals("2375.00", campos.get("valorLiquido"));
    }

    @Test
    void extrairVencimentoNaoDeveUsarDataDeEmissaoComoFallbackEmNotaFiscal() throws Exception {
        Method extrairVencimento = DocumentoInteligenciaService.class.getDeclaredMethod(
                "extrairVencimento",
                String.class,
                String.class
        );
        extrairVencimento.setAccessible(true);

        String textoSemRotuloVencimento = """
                Nota Fiscal de Servico Eletronica
                Data de Emissao: 21/04/2026
                Competencia: 2026-04
                Valor Liquido: R$ 2.375,00
                """;
        String semRotulo = (String) extrairVencimento.invoke(service, textoSemRotuloVencimento, "NOTA_FISCAL");
        assertEquals("", semRotulo);

        String textoComRotuloVencimento = """
                Nota Fiscal de Servico Eletronica
                Data de Emissao: 21/04/2026
                Vencimento: 30/04/2026
                Valor Liquido: R$ 2.375,00
                """;
        String comRotulo = (String) extrairVencimento.invoke(service, textoComRotuloVencimento, "NOTA_FISCAL");
        assertEquals("30/04/2026", comRotulo);
    }

    @Test
    void resolverAbaDocumentoDevePriorizarTipoDetectadoPelaIa() throws Exception {
        DocumentoProcessamento processamento = new DocumentoProcessamento();
        processamento.setTipoDocumento("NFE_XML");
        PendenciaDocumento pendencia = new PendenciaDocumento();
        TemplateDocumento template = new TemplateDocumento();
        template.setNome("Guia de imposto mensal");
        pendencia.setTemplateDocumento(template);

        when(docTabMapperService.idAbaParaTipoDetectado("NFE_XML")).thenReturn("NFE");

        Method resolver = DocumentoInteligenciaService.class.getDeclaredMethod(
                "resolverAbaDocumento",
                DocumentoProcessamento.class,
                PendenciaDocumento.class
        );
        resolver.setAccessible(true);
        String aba = (String) resolver.invoke(service, processamento, pendencia);

        assertEquals("NFE", aba);
        verify(docTabMapperService).idAbaParaTipoDetectado("NFE_XML");
    }

    @Test
    void resolverAbaDocumentoDeveUsarTemplateQuandoTipoDetectadoForOutros() throws Exception {
        DocumentoProcessamento processamento = new DocumentoProcessamento();
        processamento.setTipoDocumento("DESCONHECIDO");
        PendenciaDocumento pendencia = new PendenciaDocumento();
        TemplateDocumento template = new TemplateDocumento();
        template.setNome("Guia de imposto mensal");
        pendencia.setTemplateDocumento(template);

        when(docTabMapperService.idAbaParaTipoDetectado("DESCONHECIDO")).thenReturn("OUTROS");
        when(docTabMapperService.idAbaParaTemplateNome("Guia de imposto mensal")).thenReturn("GUIA_IRPF");

        Method resolver = DocumentoInteligenciaService.class.getDeclaredMethod(
                "resolverAbaDocumento",
                DocumentoProcessamento.class,
                PendenciaDocumento.class
        );
        resolver.setAccessible(true);
        String aba = (String) resolver.invoke(service, processamento, pendencia);

        assertEquals("GUIA_IRPF", aba);
        verify(docTabMapperService).idAbaParaTipoDetectado("DESCONHECIDO");
        verify(docTabMapperService).idAbaParaTemplateNome("Guia de imposto mensal");
    }

    @Test
    void prepararConteudoXmlParaParse_removeBomComentariosHashEspacosAntesDoXml() throws Exception {
        String inner = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nfeProc><NFe><infNFe>"
                + "<emit><CNPJ>12345678000195</CNPJ></emit>"
                + "<dest><CNPJ>99887766000155</CNPJ></dest>"
                + "<total><ICMSTot><vNF>10.00</vNF></ICMSTot></total>"
                + "</infNFe></NFe></nfeProc>";
        String comCabecalho = "# metadados\n# mais linhas\n\n  " + inner;
        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] payload = comCabecalho.getBytes(StandardCharsets.UTF_8);
        byte[] input = new byte[bom.length + payload.length];
        System.arraycopy(bom, 0, input, 0, bom.length);
        System.arraycopy(payload, 0, input, bom.length, payload.length);

        Method m = DocumentoInteligenciaService.class.getDeclaredMethod("prepararConteudoXmlParaParse", byte[].class);
        m.setAccessible(true);
        byte[] out = (byte[]) m.invoke(null, (Object) input);
        assertTrue(new String(out, StandardCharsets.UTF_8).startsWith("<?xml"));
    }
}
