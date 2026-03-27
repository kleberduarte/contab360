package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class DocumentoInteligenciaService {

    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\b\\d{14}\\b");
    private static final Pattern CNPJ_FORMATADO_PATTERN = Pattern.compile("\\b\\d{2}\\.?\\d{3}\\.?\\d{3}[/\\-]?\\d{4}[\\-]?\\d{2}\\b");
    private static final Pattern VALOR_PATTERN = Pattern.compile("(\\d+[\\.,]\\d{2})");
    private static final Pattern COMPETENCIA_PATTERN = Pattern.compile("\\b(\\d{2})/(\\d{4})\\b|\\b(\\d{4})-(\\d{2})\\b");
    private static final Pattern DATA_PATTERN = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");

    private final DocumentoProcessamentoRepository documentoProcessamentoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentoInteligenciaService(
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository
    ) {
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.revisaoDocumentoHistoricoRepository = revisaoDocumentoHistoricoRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
    }

    @Transactional
    public DocumentoProcessamento iniciarProcessamento(EntregaDocumento entrega) {
        DocumentoProcessamento processamento = documentoProcessamentoRepository.findByEntregaId(entrega.getId())
                .orElseGet(() -> {
                    DocumentoProcessamento novo = new DocumentoProcessamento();
                    novo.setEntrega(entrega);
                    novo.setStatus(ProcessamentoStatus.RECEBIDO);
                    novo.setSeveridade(SeveridadeRevisao.MEDIA);
                    novo.setAtualizadoEm(LocalDateTime.now());
                    return documentoProcessamentoRepository.save(novo);
                });

        processamento.setStatus(ProcessamentoStatus.PROCESSANDO);
        processamento.setAtualizadoEm(LocalDateTime.now());
        documentoProcessamentoRepository.save(processamento);

        try {
            processarArquivo(processamento, Path.of(entrega.getCaminhoArquivo()));
        } catch (Exception ex) {
            processamento.setStatus(ProcessamentoStatus.ERRO);
            processamento.setObservacaoProcessamento("Falha no processamento: " + ex.getMessage());
            processamento.setAtualizadoEm(LocalDateTime.now());
            documentoProcessamentoRepository.save(processamento);
        }

        return processamento;
    }

    @Transactional(readOnly = true)
    public List<DocumentoProcessamento> listar(Usuario usuarioAtual, boolean somenteRevisar) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode revisar documentos.");
        }
        if (somenteRevisar) {
            return documentoProcessamentoRepository.findByStatusOrderByAtualizadoEmDesc(ProcessamentoStatus.REVISAR);
        }
        return documentoProcessamentoRepository.findAllByOrderByAtualizadoEmDesc();
    }

    @Transactional
    public DocumentoProcessamento marcarComoProcessado(Long processamentoId, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode aprovar revisão.");
        }
        DocumentoProcessamento item = documentoProcessamentoRepository.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento de processamento não encontrado."));
        item.setStatus(ProcessamentoStatus.PROCESSADO);
        item.setObservacaoProcessamento("Aprovado em revisão manual.");
        item.setAtualizadoEm(LocalDateTime.now());
        PendenciaDocumento pendencia = item.getEntrega().getPendencia();
        pendencia.setStatus(PendenciaStatus.VALIDADO);
        pendenciaDocumentoRepository.save(pendencia);
        DocumentoProcessamento salvo = documentoProcessamentoRepository.save(item);
        registrarHistorico(salvo, "APROVADO_MANUAL", "Aprovado pelo contador.", usuarioAtual.getNome());
        return salvo;
    }

    @Transactional
    public DocumentoProcessamento marcarComoRejeitado(Long processamentoId, String motivo, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode rejeitar revisão.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório.");
        }
        DocumentoProcessamento item = documentoProcessamentoRepository.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento de processamento não encontrado."));
        item.setStatus(ProcessamentoStatus.REJEITADO);
        item.setSeveridade(SeveridadeRevisao.ALTA);
        item.setObservacaoProcessamento("Rejeitado: " + motivo.trim());
        item.setAtualizadoEm(LocalDateTime.now());
        PendenciaDocumento pendencia = item.getEntrega().getPendencia();
        pendencia.setStatus(PendenciaStatus.REJEITADO);
        pendenciaDocumentoRepository.save(pendencia);
        DocumentoProcessamento salvo = documentoProcessamentoRepository.save(item);
        registrarHistorico(salvo, "REJEITADO_MANUAL", motivo.trim(), usuarioAtual.getNome());
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<RevisaoDocumentoHistorico> listarHistorico(Long processamentoId, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode consultar histórico.");
        }
        return revisaoDocumentoHistoricoRepository.findByProcessamentoIdOrderByCriadoEmDesc(processamentoId);
    }

    private void processarArquivo(DocumentoProcessamento processamento, Path path) throws Exception {
        String nome = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nome.endsWith(".xml")) {
            processarXml(processamento, path);
            return;
        }
        if (nome.endsWith(".txt") || nome.endsWith(".json")) {
            processarTexto(processamento, path);
            return;
        }
        if (nome.endsWith(".pdf")) {
            processarPdf(processamento, path);
            return;
        }
        if (nome.endsWith(".png") || nome.endsWith(".jpg") || nome.endsWith(".jpeg")) {
            processarImagem(processamento, path);
            return;
        }

        processamento.setTipoDocumento("ARQUIVO_NAO_SUPORTADO");
        processamento.setConfianca(0.2);
        processamento.setStatus(ProcessamentoStatus.REJEITADO);
        processamento.setObservacaoProcessamento("Extensão ainda não suportada para leitura automática.");
        processamento.setSeveridade(SeveridadeRevisao.MEDIA);
        processamento.setAtualizadoEm(LocalDateTime.now());
        finalizarProcessamentoAutomatico(processamento, "ARQUIVO_NAO_SUPORTADO");
    }

    private void processarTexto(DocumentoProcessamento processamento, Path path) throws IOException {
        String texto = Files.readString(path, StandardCharsets.UTF_8);
        aplicarResultadoExtraido(processamento, "texto", texto, inferirTipo(texto), 0.8);
    }

    private void processarPdf(DocumentoProcessamento processamento, Path path) throws IOException {
        String textoExtraido = extrairTextoPdf(path);
        double confianca = textoExtraido.isBlank() ? 0.35 : 0.72;
        String tipo = inferirTipo(textoExtraido);
        aplicarResultadoExtraido(processamento, "pdf", textoExtraido, tipo, confianca);
    }

    private void processarImagem(DocumentoProcessamento processamento, Path path) throws IOException, InterruptedException {
        String texto = extrairTextoImagemComTesseract(path);
        if (texto.isBlank()) {
            processamento.setTipoDocumento("OCR_INDISPONIVEL");
            processamento.setConfianca(0.25);
            processamento.setStatus(ProcessamentoStatus.REJEITADO);
            processamento.setObservacaoProcessamento("OCR não disponível. Instale Tesseract no servidor para leitura de imagens.");
            processamento.setSeveridade(SeveridadeRevisao.MEDIA);
            processamento.setAtualizadoEm(LocalDateTime.now());
            finalizarProcessamentoAutomatico(processamento, "OCR_INDISPONIVEL");
            return;
        }

        String tipo = inferirTipo(texto);
        aplicarResultadoExtraido(processamento, "imagem_ocr", texto, tipo, 0.62);
    }

    private String extrairTextoPdf(Path path) throws IOException {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private void processarXml(DocumentoProcessamento processamento, Path path) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        Document doc = dbf.newDocumentBuilder().parse(path.toFile());
        doc.getDocumentElement().normalize();

        String cnpjEmit = coalesce(firstTagValue(doc, "CNPJ"), firstTagValue(doc, "Cnpj"));
        String cnpjDest = coalesce(firstTagValue(doc, "CNPJDest"), firstTagValue(doc, "CPFCNPJTomador"));
        String valor = firstTagValue(doc, "vNF");
        String competencia = coalesce(firstTagValue(doc, "Competencia"), firstTagValue(doc, "competencia"));
        String vencimento = coalesce(firstTagValue(doc, "dVenc"), firstTagValue(doc, "dataVencimento"));
        if (valor == null || valor.isBlank()) {
            valor = firstTagValue(doc, "vServ");
        }

        String tipo = inferirTipoXml(doc);
        ProcessamentoStatus status = ProcessamentoStatus.PROCESSADO;
        double confianca = 0.92;
        List<String> motivosRevisao = new ArrayList<>();

        String cnpjPrincipal = !cnpjEmit.isBlank() ? cnpjEmit : cnpjDest;
        String observacao = "XML processado com alta confiança.";
        String validacao = validarComPendencia(processamento, tipo, cnpjPrincipal, cnpjDest);
        if (!validacao.isBlank()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = 0.7;
            observacao = validacao;
            motivosRevisao.add(validacao);
        }

        if (cnpjPrincipal.isBlank()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.65);
            motivosRevisao.add("CNPJ nao identificado no documento.");
        }
        if (valor == null || valor.isBlank()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.68);
            motivosRevisao.add("Valor principal nao identificado no documento.");
        }

        processamento.setTipoDocumento(tipo);
        processamento.setConfianca(confianca);
        processamento.setStatus(status);
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("cnpjEmitente", safe(cnpjEmit));
        campos.put("cnpjDestinatario", safe(cnpjDest));
        campos.put("valor", safe(valor));
        campos.put("competencia", normalizarCompetencia(competencia));
        campos.put("vencimento", normalizarData(vencimento));
        campos.put("tributo", inferirTributo(tipo));
        aplicarValidacoesContabeis(processamento, tipo, campos, motivosRevisao);
        if (!motivosRevisao.isEmpty()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.67);
            observacao = motivosRevisao.get(0);
        }

        processamento.setDadosExtraidosJson(montarDadosExtraidosJson(
                "xml",
                tipo,
                campos,
                confianca,
                status == ProcessamentoStatus.PROCESSADO ? "SUCESSO" : "REJEITADO",
                motivosRevisao
        ));
        processamento.setObservacaoProcessamento(observacao);
        processamento.setSeveridade(status == ProcessamentoStatus.PROCESSADO ? SeveridadeRevisao.BAIXA : SeveridadeRevisao.MEDIA);
        processamento.setAtualizadoEm(LocalDateTime.now());
        finalizarProcessamentoAutomatico(processamento, "PROCESSAMENTO_XML");
    }

    private String firstTagValue(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }

    private String inferirTipo(String texto) {
        return TipoDocumentoCatalogo.detectarPorTexto(texto);
    }

    private String inferirTipoXml(Document doc) {
        boolean isNfe = !firstTagValue(doc, "infNFe").isBlank() || !firstTagValue(doc, "NFe").isBlank();
        boolean isNfse = !firstTagValue(doc, "CompNfse").isBlank() || !firstTagValue(doc, "InfNfse").isBlank();
        return TipoDocumentoCatalogo.detectarPorXml(isNfe, isNfse);
    }

    private String extrairCnpj(String texto) {
        String conteudo = texto == null ? "" : texto;

        Matcher formatado = CNPJ_FORMATADO_PATTERN.matcher(conteudo);
        while (formatado.find()) {
            String normalizado = formatado.group().replaceAll("\\D", "");
            if (normalizado.length() == 14) {
                return normalizado;
            }
        }

        Matcher bruto = CNPJ_PATTERN.matcher(conteudo.replaceAll("[^0-9]", " "));
        return bruto.find() ? bruto.group() : "";
    }

    private String extrairValor(String texto) {
        Matcher matcher = VALOR_PATTERN.matcher(texto);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extrairCnpjAposRotulo(String texto, String rotuloRegex) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        Pattern rotulo = Pattern.compile(rotuloRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcherRotulo = rotulo.matcher(texto);
        if (!matcherRotulo.find()) {
            return "";
        }

        int inicio = matcherRotulo.start();
        int fim = Math.min(texto.length(), inicio + 240);
        String trecho = texto.substring(inicio, fim);
        return extrairPrimeiroCnpjValido(trecho);
    }

    private String extrairPrimeiroCnpjValido(String texto) {
        Matcher formatado = CNPJ_FORMATADO_PATTERN.matcher(texto == null ? "" : texto);
        while (formatado.find()) {
            String normalizado = formatado.group().replaceAll("\\D", "");
            if (normalizado.length() == 14) {
                return normalizado;
            }
        }
        return "";
    }

    private String extrairCnpjPrestador(String texto) {
        return extrairCnpjAposRotulo(texto, "(prestador|emitente)");
    }

    private String extrairCnpjTomador(String texto) {
        return extrairCnpjAposRotulo(texto, "(tomador|destinatario|cliente)");
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private String montarDadosExtraidosJson(
            String fonte,
            String tipoDocumento,
            Map<String, String> campos,
            double confianca,
            String status,
            List<String> motivosRevisao
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fonte", safe(fonte));
        payload.put("tipoDocumento", safe(tipoDocumento));
        payload.put("status", status);
        payload.put("confianca", confianca);
        payload.put("camposObrigatorios", TipoDocumentoCatalogo.camposObrigatorios(tipoDocumento));
        payload.put("camposExtraidos", campos);
        payload.put("motivosRevisao", motivosRevisao);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"fonte\":\"" + safe(fonte) + "\",\"tipoDocumento\":\"" + safe(tipoDocumento) + "\"}";
        }
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    private String extrairCompetencia(String texto) {
        Matcher matcher = COMPETENCIA_PATTERN.matcher(texto == null ? "" : texto);
        if (!matcher.find()) {
            return "";
        }
        if (matcher.group(1) != null && matcher.group(2) != null) {
            return matcher.group(2) + "-" + matcher.group(1);
        }
        return matcher.group(3) + "-" + matcher.group(4);
    }

    private String extrairData(String texto) {
        Matcher matcher = DATA_PATTERN.matcher(texto == null ? "" : texto);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String normalizarCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return "";
        }
        String c = competencia.trim();
        if (c.matches("\\d{4}-\\d{2}")) {
            return c;
        }
        if (c.matches("\\d{2}/\\d{4}")) {
            String[] p = c.split("/");
            return p[1] + "-" + p[0];
        }
        return c;
    }

    private String normalizarData(String data) {
        if (data == null || data.isBlank()) {
            return "";
        }
        String d = data.trim();
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return d;
        }
        if (d.matches("\\d{2}/\\d{2}/\\d{4}")) {
            try {
                LocalDate parsed = LocalDate.parse(d, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return parsed.toString();
            } catch (DateTimeParseException ignored) {
                return d;
            }
        }
        return d;
    }

    private String inferirTributo(String textoBase) {
        String base = textoBase == null ? "" : textoBase.toUpperCase(Locale.ROOT);
        if (base.contains("ISS")) return "ISS";
        if (base.contains("ICMS")) return "ICMS";
        if (base.contains("PIS")) return "PIS";
        if (base.contains("COFINS")) return "COFINS";
        if (base.contains("IRPJ")) return "IRPJ";
        if (base.contains("CSLL")) return "CSLL";
        if (base.contains("FGTS")) return "FGTS";
        if (base.contains("INSS")) return "INSS";
        return "";
    }

    private void aplicarValidacoesContabeis(
            DocumentoProcessamento processamento,
            String tipoDocumento,
            Map<String, String> campos,
            List<String> motivosRevisao
    ) {
        PendenciaDocumento pendencia = processamento.getEntrega().getPendencia();
        String competenciaEsperada = String.format("%04d-%02d", pendencia.getCompetencia().getAno(), pendencia.getCompetencia().getMes());
        String competenciaExtraida = campos.getOrDefault("competencia", "");
        if (!competenciaExtraida.isBlank() && !competenciaEsperada.equals(competenciaExtraida)) {
            motivosRevisao.add("Competencia do documento difere da pendencia (" + competenciaExtraida + " x " + competenciaEsperada + ").");
        }

        // Vencimento costuma ser confiavel para guias de recolhimento, mas nao para notas.
        if ("GUIA_IMPOSTO".equals(tipoDocumento)) {
            String vencimentoExtraido = campos.getOrDefault("vencimento", "");
            if (!vencimentoExtraido.isBlank()) {
                String vencimentoPendencia = pendencia.getVencimento().toString();
                if (!vencimentoPendencia.equals(vencimentoExtraido)) {
                    motivosRevisao.add("Vencimento do documento difere da pendencia (" + vencimentoExtraido + " x " + vencimentoPendencia + ").");
                }
            }
        }

        if ("GUIA_IMPOSTO".equals(tipoDocumento) && campos.getOrDefault("tributo", "").isBlank()) {
            motivosRevisao.add("Tributo nao identificado na guia de recolhimento.");
        }
    }
    private String extrairTextoImagemComTesseract(Path path) throws IOException, InterruptedException {
        Path outputBase = Files.createTempFile("ocr-", "");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract",
                    path.toAbsolutePath().toString(),
                    outputBase.toAbsolutePath().toString(),
                    "-l",
                    "por"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int code = process.waitFor();
            Path txtPath = Path.of(outputBase.toAbsolutePath() + ".txt");
            if (code != 0 || !Files.exists(txtPath)) {
                return "";
            }
            return Files.readString(txtPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        } finally {
            try {
                Files.deleteIfExists(outputBase);
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(Path.of(outputBase.toAbsolutePath() + ".txt"));
            } catch (IOException ignored) {
            }
        }
    }

    private void registrarHistorico(DocumentoProcessamento processamento, String acao, String motivo, String usuarioNome) {
        RevisaoDocumentoHistorico hist = new RevisaoDocumentoHistorico();
        hist.setProcessamento(processamento);
        hist.setAcao(acao);
        hist.setMotivo(motivo);
        hist.setUsuarioNome(usuarioNome);
        hist.setCriadoEm(LocalDateTime.now());
        revisaoDocumentoHistoricoRepository.save(hist);
    }

    private void aplicarResultadoExtraido(
            DocumentoProcessamento processamento,
            String fonte,
            String texto,
            String tipo,
            double confiancaBase
    ) {
        String cnpjPrestador = extrairCnpjPrestador(texto);
        String cnpjTomador = extrairCnpjTomador(texto);
        String cnpj = !cnpjPrestador.isBlank() ? cnpjPrestador : extrairCnpj(texto);
        String valor = extrairValor(texto);
        String competencia = extrairCompetencia(texto);
        String vencimento = extrairData(texto);
        String tributo = inferirTributo(texto);
        double confianca = "DESCONHECIDO".equals(tipo) ? Math.min(confiancaBase, 0.45) : confiancaBase;
        ProcessamentoStatus status = confianca >= 0.75 ? ProcessamentoStatus.PROCESSADO : ProcessamentoStatus.REJEITADO;
        String observacao = status == ProcessamentoStatus.PROCESSADO
                ? "Documento processado automaticamente."
                : "Rejeitado automaticamente: baixa confiança na leitura automática.";
        SeveridadeRevisao severidade = status == ProcessamentoStatus.PROCESSADO ? SeveridadeRevisao.BAIXA : SeveridadeRevisao.MEDIA;
        List<String> motivosRevisao = new ArrayList<>();

        String validacao = validarComPendencia(processamento, tipo, cnpj, cnpjTomador);
        if (!validacao.isBlank()) {
            status = ProcessamentoStatus.REJEITADO;
            observacao = validacao;
            confianca = Math.min(confianca, 0.7);
            severidade = validacao.toLowerCase(Locale.ROOT).contains("cnpj")
                    ? SeveridadeRevisao.ALTA
                    : SeveridadeRevisao.MEDIA;
            motivosRevisao.add(validacao);
        }

        List<String> camposObrigatorios = TipoDocumentoCatalogo.camposObrigatorios(tipo);
        if (camposObrigatorios.contains("cnpj") && cnpj.isBlank()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.65);
            motivosRevisao.add("CNPJ nao identificado no documento.");
        }
        if (camposObrigatorios.contains("valor") && valor.isBlank()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.68);
            motivosRevisao.add("Valor principal nao identificado no documento.");
        }

        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("cnpj", safe(cnpj));
        campos.put("cnpjPrestador", safe(cnpjPrestador));
        campos.put("cnpjTomador", safe(cnpjTomador));
        campos.put("valor", safe(valor));
        campos.put("competencia", normalizarCompetencia(competencia));
        campos.put("vencimento", normalizarData(vencimento));
        campos.put("tributo", tributo);
        aplicarValidacoesContabeis(processamento, tipo, campos, motivosRevisao);
        if (!motivosRevisao.isEmpty()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.67);
            observacao = motivosRevisao.get(0);
        }

        processamento.setTipoDocumento(tipo);
        processamento.setConfianca(confianca);
        processamento.setStatus(status);
        processamento.setDadosExtraidosJson(montarDadosExtraidosJson(
                fonte,
                tipo,
                campos,
                confianca,
                status == ProcessamentoStatus.PROCESSADO ? "SUCESSO" : "REJEITADO",
                motivosRevisao
        ));
        processamento.setObservacaoProcessamento(observacao);
        processamento.setSeveridade(severidade);
        processamento.setAtualizadoEm(LocalDateTime.now());
        finalizarProcessamentoAutomatico(processamento, "PROCESSAMENTO_TEXTO");
    }

    private String validarComPendencia(
            DocumentoProcessamento processamento,
            String tipoDetectado,
            String cnpjPrincipal,
            String cnpjSecundario
    ) {
        PendenciaDocumento pendencia = processamento.getEntrega().getPendencia();
        String nomeTemplate = pendencia.getTemplateDocumento().getNome().toLowerCase(Locale.ROOT);
        String cnpjEmpresa = pendencia.getEmpresa().getCnpj();

        boolean tipoFiscal = tipoDetectado.contains("NOTA") || tipoDetectado.contains("NFE") || tipoDetectado.contains("NFSE");
        if (tipoFiscal) {
            if (!cnpjPrincipal.isBlank() || !cnpjSecundario.isBlank()) {
                boolean cnpjCompativel = cnpjEmpresa.equals(cnpjPrincipal) || cnpjEmpresa.equals(cnpjSecundario);
                if (!cnpjCompativel) {
                    return "CNPJ do documento difere da empresa da pendência. Revisar.";
                }
            }
        } else if (!cnpjPrincipal.isBlank() && !cnpjEmpresa.equals(cnpjPrincipal)) {
            return "CNPJ do documento difere da empresa da pendência. Revisar.";
        } else if (!cnpjSecundario.isBlank() && !cnpjEmpresa.equals(cnpjSecundario) && cnpjPrincipal.isBlank()) {
            return "CNPJ do documento difere da empresa da pendência. Revisar.";
        }

        boolean tipoCompativel = true;
        if (nomeTemplate.contains("nota") && !tipoFiscal) {
            tipoCompativel = false;
        } else if (nomeTemplate.contains("extrato") && !tipoDetectado.contains("EXTRATO")) {
            tipoCompativel = false;
        } else if ((nomeTemplate.contains("holerite") || nomeTemplate.contains("folha")) && !tipoDetectado.contains("FOLHA")) {
            tipoCompativel = false;
        }

        if (!tipoCompativel) {
            return "Tipo detectado não combina com o template da pendência. Revisar.";
        }

        boolean temTemplate = templateDocumentoRepository
                .findByEmpresaIdOrderByNomeAsc(pendencia.getEmpresa().getId())
                .stream()
                .anyMatch(t -> t.getId().equals(pendencia.getTemplateDocumento().getId()));
        if (!temTemplate) {
            return "Template não encontrado para esta empresa. Revisar.";
        }

        return "";
    }

    private void finalizarProcessamentoAutomatico(DocumentoProcessamento processamento, String origem) {
        PendenciaDocumento pendencia = processamento.getEntrega().getPendencia();
        boolean aprovado = processamento.getStatus() == ProcessamentoStatus.PROCESSADO;
        pendencia.setStatus(aprovado ? PendenciaStatus.VALIDADO : PendenciaStatus.REJEITADO);
        pendenciaDocumentoRepository.save(pendencia);
        DocumentoProcessamento salvo = documentoProcessamentoRepository.save(processamento);
        String acao = aprovado ? "APROVADO_IA" : "REJEITADO_IA";
        String motivo = processamento.getObservacaoProcessamento();
        if (motivo == null || motivo.isBlank()) {
            motivo = aprovado ? "Documento aprovado automaticamente pela IA." : "Documento rejeitado automaticamente pela IA.";
        }
        registrarHistorico(salvo, acao, "[" + origem + "] " + motivo, "IA");
    }

}
