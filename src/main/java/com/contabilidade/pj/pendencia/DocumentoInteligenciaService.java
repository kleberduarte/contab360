package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
    private static final Pattern VALOR_PATTERN = Pattern.compile("(\\d+[\\.,]\\d{2})");

    private final DocumentoProcessamentoRepository documentoProcessamentoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository;

    public DocumentoInteligenciaService(
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository
    ) {
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.revisaoDocumentoHistoricoRepository = revisaoDocumentoHistoricoRepository;
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
        item.setStatus(ProcessamentoStatus.REVISAR);
        item.setSeveridade(SeveridadeRevisao.ALTA);
        item.setObservacaoProcessamento("Rejeitado: " + motivo.trim());
        item.setAtualizadoEm(LocalDateTime.now());
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
        processamento.setStatus(ProcessamentoStatus.REVISAR);
        processamento.setObservacaoProcessamento("Extensão ainda não suportada para leitura automática.");
        processamento.setSeveridade(SeveridadeRevisao.MEDIA);
        processamento.setAtualizadoEm(LocalDateTime.now());
        documentoProcessamentoRepository.save(processamento);
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
            processamento.setStatus(ProcessamentoStatus.REVISAR);
            processamento.setObservacaoProcessamento("OCR não disponível. Instale Tesseract no servidor para leitura de imagens.");
            processamento.setSeveridade(SeveridadeRevisao.MEDIA);
            processamento.setAtualizadoEm(LocalDateTime.now());
            documentoProcessamentoRepository.save(processamento);
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
        if (valor == null || valor.isBlank()) {
            valor = firstTagValue(doc, "vServ");
        }

        String tipo = inferirTipoXml(doc);
        ProcessamentoStatus status = ProcessamentoStatus.PROCESSADO;
        double confianca = 0.92;

        String cnpjPrincipal = !cnpjEmit.isBlank() ? cnpjEmit : cnpjDest;
        String observacao = "XML processado com alta confiança.";
        String validacao = validarComPendencia(processamento, tipo, cnpjPrincipal);
        if (!validacao.isBlank()) {
            status = ProcessamentoStatus.REVISAR;
            confianca = 0.7;
            observacao = validacao;
        }

        processamento.setTipoDocumento(tipo);
        processamento.setConfianca(confianca);
        processamento.setStatus(status);
        processamento.setDadosExtraidosJson(
                "{\"fonte\":\"xml\",\"tipoDocumento\":\"" + safe(tipo) + "\",\"cnpjEmitente\":\"" + safe(cnpjEmit)
                        + "\",\"cnpjDestinatario\":\"" + safe(cnpjDest) + "\",\"valor\":\"" + safe(valor) + "\"}"
        );
        processamento.setObservacaoProcessamento(observacao);
        processamento.setSeveridade(status == ProcessamentoStatus.PROCESSADO ? SeveridadeRevisao.BAIXA : SeveridadeRevisao.MEDIA);
        processamento.setAtualizadoEm(LocalDateTime.now());
        documentoProcessamentoRepository.save(processamento);
    }

    private String firstTagValue(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }

    private String inferirTipo(String texto) {
        String base = texto.toLowerCase(Locale.ROOT);
        if (base.contains("nota fiscal") || base.contains("nfse") || base.contains("nfs-e")) {
            return "NOTA_FISCAL";
        }
        if (base.contains("extrato")) {
            return "EXTRATO_BANCARIO";
        }
        if (base.contains("holerite") || base.contains("folha")) {
            return "FOLHA_PAGAMENTO";
        }
        return "DESCONHECIDO";
    }

    private String inferirTipoXml(Document doc) {
        if (!firstTagValue(doc, "infNFe").isBlank() || !firstTagValue(doc, "NFe").isBlank()) {
            return "NFE_XML";
        }
        if (!firstTagValue(doc, "CompNfse").isBlank() || !firstTagValue(doc, "InfNfse").isBlank()) {
            return "NFSE_XML";
        }
        return "DOCUMENTO_FISCAL_XML";
    }

    private String extrairCnpj(String texto) {
        Matcher matcher = CNPJ_PATTERN.matcher(texto.replaceAll("[^0-9]", " "));
        return matcher.find() ? matcher.group() : "";
    }

    private String extrairValor(String texto) {
        Matcher matcher = VALOR_PATTERN.matcher(texto);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    private void aplicarResultadoExtraido(
            DocumentoProcessamento processamento,
            String fonte,
            String texto,
            String tipo,
            double confiancaBase
    ) {
        String cnpj = extrairCnpj(texto);
        String valor = extrairValor(texto);
        double confianca = "DESCONHECIDO".equals(tipo) ? Math.min(confiancaBase, 0.45) : confiancaBase;
        ProcessamentoStatus status = confianca >= 0.75 ? ProcessamentoStatus.PROCESSADO : ProcessamentoStatus.REVISAR;
        String observacao = status == ProcessamentoStatus.PROCESSADO
                ? "Documento processado automaticamente."
                : "Baixa confiança na leitura automática.";
        SeveridadeRevisao severidade = status == ProcessamentoStatus.PROCESSADO ? SeveridadeRevisao.BAIXA : SeveridadeRevisao.MEDIA;

        String validacao = validarComPendencia(processamento, tipo, cnpj);
        if (!validacao.isBlank()) {
            status = ProcessamentoStatus.REVISAR;
            observacao = validacao;
            confianca = Math.min(confianca, 0.7);
            severidade = validacao.toLowerCase(Locale.ROOT).contains("cnpj")
                    ? SeveridadeRevisao.ALTA
                    : SeveridadeRevisao.MEDIA;
        }

        processamento.setTipoDocumento(tipo);
        processamento.setConfianca(confianca);
        processamento.setStatus(status);
        processamento.setDadosExtraidosJson(
                "{\"fonte\":\"" + safe(fonte) + "\",\"tipoDocumento\":\"" + safe(tipo)
                        + "\",\"cnpj\":\"" + safe(cnpj) + "\",\"valor\":\"" + safe(valor) + "\"}"
        );
        processamento.setObservacaoProcessamento(observacao);
        processamento.setSeveridade(severidade);
        processamento.setAtualizadoEm(LocalDateTime.now());
        documentoProcessamentoRepository.save(processamento);
    }

    private String validarComPendencia(DocumentoProcessamento processamento, String tipoDetectado, String cnpjDetectado) {
        PendenciaDocumento pendencia = processamento.getEntrega().getPendencia();
        String nomeTemplate = pendencia.getTemplateDocumento().getNome().toLowerCase(Locale.ROOT);

        if (!cnpjDetectado.isBlank()) {
            String cnpjEmpresa = pendencia.getEmpresa().getCnpj();
            if (!cnpjEmpresa.equals(cnpjDetectado)) {
                return "CNPJ do documento difere da empresa da pendência. Revisar.";
            }
        }

        boolean tipoCompativel = true;
        if (nomeTemplate.contains("nota") && !(tipoDetectado.contains("NOTA") || tipoDetectado.contains("NFE") || tipoDetectado.contains("NFSE"))) {
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
}
