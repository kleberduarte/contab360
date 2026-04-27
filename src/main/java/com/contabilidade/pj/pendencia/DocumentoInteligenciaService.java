package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado;
import com.contabilidade.pj.pendencia.holerite.HoleriteTextoParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
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
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class DocumentoInteligenciaService {

    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\b\\d{14}\\b");
    private static final Pattern CNPJ_FORMATADO_PATTERN = Pattern.compile("\\b\\d{2}\\.?\\d{3}\\.?\\d{3}[/\\-]?\\d{4}[\\-]?\\d{2}\\b");
    private static final Pattern CPF_FORMATADO_PATTERN = Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b");
    /** Evita {@code 123.45} vindo de PIS no holerite. */
    private static final Pattern PIS_PASEP_FORMATADO_PATTERN = Pattern.compile("\\b\\d{3}\\.\\d{5}\\.\\d{2}-\\d{1}\\b");
    private static final Pattern VALOR_PATTERN = Pattern.compile("(\\d+[\\.,]\\d{2})");
    /**
     * Folha: valor líquido (formato BR). Ordem: mais permissivo primeiro (evita falha por espaços).
     * Falsos positivos no fallback: CNPJ ({@code 12.34}), CPF ({@code 123.45}), PIS — ver {@link #textoSemIdsParaExtracaoValor}.
     */
    private static final Pattern[] VALOR_LIQUIDO_FOLHA_PATTERNS = new Pattern[] {
            Pattern.compile("(?i)l[ií]quido[^:\\n]{0,52}:\\s*(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b"),
            Pattern.compile(
                    "(?i)(?:valor\\s+)?l[ií]quido(?:\\s+a\\s+receber)?\\s*:\\s*(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b"),
            Pattern.compile("(?i)total\\s+l[ií]quido\\s*:\\s*(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b"),
    };
    private static final Pattern COMPETENCIA_PATTERN = Pattern.compile("\\b(\\d{2})/(\\d{4})\\b|\\b(\\d{4})-(\\d{2})\\b");
    /** mm/aaaa logo após rótulo de competência/referência (evita pegar 03/2022 de "15/03/2022"). */
    private static final Pattern COMPETENCIA_COM_ROTULO = Pattern.compile(
            "(?i)(?:compet[êe]ncia|refer[êe]ncia)\\s*[:\\-]?\\s*(\\d{2})/(\\d{4})\\b");
    private static final Pattern DATA_PATTERN = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");

    private final DocumentoProcessamentoRepository documentoProcessamentoRepository;
    private final DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final EmpresaRepository empresaRepository;
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final CompetenciaArquivamentoService competenciaArquivamentoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentoInteligenciaService(
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            EmpresaRepository empresaRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            CompetenciaArquivamentoService competenciaArquivamentoService
    ) {
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.documentoDadoExtraidoRepository = documentoDadoExtraidoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.revisaoDocumentoHistoricoRepository = revisaoDocumentoHistoricoRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.empresaRepository = empresaRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.competenciaArquivamentoService = competenciaArquivamentoService;
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
    public List<DocumentoProcessamento> listar(
            Usuario usuarioAtual,
            boolean somenteRevisar,
            boolean incluirConcluidosNaRevisao
    ) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode revisar documentos.");
        }
        List<DocumentoProcessamento> base = somenteRevisar
                ? documentoProcessamentoRepository.findByStatusOrderByAtualizadoEmDesc(ProcessamentoStatus.REVISAR)
                : documentoProcessamentoRepository.findAllByOrderByAtualizadoEmDesc();
        if (incluirConcluidosNaRevisao) {
            return base;
        }
        return base.stream().filter(this::manterVisivelNaRevisaoContador).toList();
    }

    private boolean manterVisivelNaRevisaoContador(DocumentoProcessamento dp) {
        PendenciaDocumento p = dp.getEntrega().getPendencia();
        if (p.getStatus() == PendenciaStatus.VALIDADO) {
            return false;
        }
        return !p.getCompetencia().isArquivada();
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
        competenciaArquivamentoService.sincronizarArquivamentoCompetencia(pendencia.getCompetencia().getId());
        DocumentoProcessamento salvo = documentoProcessamentoRepository.save(item);
        sincronizarCamposPersistidos(salvo);
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
        competenciaArquivamentoService.sincronizarArquivamentoCompetencia(pendencia.getCompetencia().getId());
        DocumentoProcessamento salvo = documentoProcessamentoRepository.save(item);
        sincronizarCamposPersistidos(salvo);
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

    @Transactional(readOnly = true)
    public DadosExtraidosPendenciaDto obterDadosExtraidosPorPendencia(Long pendenciaId, Usuario usuarioAtual) {
        if (usuarioAtual == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE
                && !PendenciaClienteDono.clienteEhDonoDaPendencia(usuarioAtual, pendencia)) {
            throw new IllegalArgumentException("Cliente sem acesso a esta pendência.");
        }
        DocumentoProcessamento processamento = documentoProcessamentoRepository
                .findTopByEntregaPendenciaIdOrderByAtualizadoEmDesc(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum documento processado para esta pendência."));
        if (processamento.getStatus() != ProcessamentoStatus.PROCESSADO) {
            throw new IllegalArgumentException(
                    "Documento ainda não foi validado com sucesso. Status atual: " + processamento.getStatus().name()
            );
        }
        List<DadosExtraidosPendenciaDto.CampoExtraido> campos = listarCamposDoProcessamento(processamento);
        String jsonBruto = processamento.getDadosExtraidosJson();
        return new DadosExtraidosPendenciaDto(
                pendenciaId,
                processamento.getId(),
                processamento.getStatus().name(),
                processamento.getTipoDocumento(),
                processamento.getConfianca(),
                processamento.getEntrega().getNomeArquivoOriginal(),
                campos,
                extrairDetalhamentoDocumentoDoJson(jsonBruto),
                extrairCapturaPerfilDoJson(jsonBruto)
        );
    }

    private Map<String, Object> extrairDetalhamentoDocumentoDoJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode det = root.get("detalhamentoDocumento");
            if (det == null || det.isNull() || !det.isObject()) {
                return null;
            }
            return objectMapper.convertValue(det, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String extrairCapturaPerfilDoJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode c = root.get("capturaPerfil");
            if (c == null || !c.isTextual()) {
                return null;
            }
            String t = c.asText();
            return t.isBlank() ? null : t;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<DadosExtraidosPendenciaDto.CampoExtraido> listarCamposDoProcessamento(DocumentoProcessamento processamento) {
        List<DadosExtraidosPendenciaDto.CampoExtraido> campos = documentoDadoExtraidoRepository
                .findByProcessamento_IdOrderByOrdemAsc(processamento.getId())
                .stream()
                .map(r -> new DadosExtraidosPendenciaDto.CampoExtraido(
                        r.getNomeCampo(),
                        r.getValor(),
                        r.getTipoCampo() != null ? r.getTipoCampo().name() : inferirTipoCampo(r.getNomeCampo()).name()
                ))
                .toList();
        if (campos.isEmpty()) {
            campos = extrairCamposDoJson(processamento.getDadosExtraidosJson());
        }
        return campos;
    }

    @Transactional(readOnly = true)
    public DocumentosValidadosAgrupadosResponse listarDocumentosValidadosPorAba(
            Usuario usuarioAtual,
            Long empresaIdParam,
            Long clientePessoaFisicaIdParam,
            boolean incluirCompetenciasArquivadas
    ) {
        if (usuarioAtual == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        if (empresaIdParam != null && clientePessoaFisicaIdParam != null) {
            throw new IllegalArgumentException("Informe apenas empresaId ou clientePessoaFisicaId.");
        }
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() != null) {
                Long id = usuarioAtual.getEmpresa().getId();
                if (empresaIdParam != null && !empresaIdParam.equals(id)) {
                    throw new IllegalArgumentException("Empresa inválida para este usuário.");
                }
                if (clientePessoaFisicaIdParam != null) {
                    throw new IllegalArgumentException("Parâmetro clientePessoaFisicaId não se aplica a este usuário.");
                }
                Empresa empresa = empresaRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
                List<DocumentoProcessamento> processamentos = carregarProcessamentosPj(
                        usuarioAtual.getPerfil(), empresa.getId(), incluirCompetenciasArquivadas);
                return montarDocumentosValidadosPorAba(
                        empresa.getId(),
                        empresa.getCnpj(),
                        empresa.getRazaoSocial(),
                        null,
                        null,
                        null,
                        processamentos
                );
            }
            if (usuarioAtual.getClientePessoaFisica() != null) {
                Long id = usuarioAtual.getClientePessoaFisica().getId();
                if (clientePessoaFisicaIdParam != null && !clientePessoaFisicaIdParam.equals(id)) {
                    throw new IllegalArgumentException("Cadastro PF inválido para este usuário.");
                }
                if (empresaIdParam != null) {
                    throw new IllegalArgumentException("Parâmetro empresaId não se aplica a este usuário.");
                }
                ClientePessoaFisica pf = clientePessoaFisicaRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
                List<DocumentoProcessamento> processamentos = carregarProcessamentosPf(
                        usuarioAtual.getPerfil(), pf.getId(), incluirCompetenciasArquivadas);
                return montarDocumentosValidadosPorAba(
                        null,
                        null,
                        null,
                        pf.getId(),
                        pf.getCpf(),
                        pf.getNomeCompleto(),
                        processamentos
                );
            }
            return abasVaziasTomador(null, null, null, null, null, null);
        }
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Perfil não autorizado a este recurso.");
        }
        if (empresaIdParam != null) {
            Empresa empresa = empresaRepository.findById(empresaIdParam)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
            List<DocumentoProcessamento> processamentos = carregarProcessamentosPj(
                    PerfilUsuario.CONTADOR, empresa.getId(), incluirCompetenciasArquivadas);
            return montarDocumentosValidadosPorAba(
                    empresa.getId(),
                    empresa.getCnpj(),
                    empresa.getRazaoSocial(),
                    null,
                    null,
                    null,
                    processamentos
            );
        }
        if (clientePessoaFisicaIdParam != null) {
            ClientePessoaFisica pf = clientePessoaFisicaRepository.findById(clientePessoaFisicaIdParam)
                    .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
            List<DocumentoProcessamento> processamentos = carregarProcessamentosPf(
                    PerfilUsuario.CONTADOR, pf.getId(), incluirCompetenciasArquivadas);
            return montarDocumentosValidadosPorAba(
                    null,
                    null,
                    null,
                    pf.getId(),
                    pf.getCpf(),
                    pf.getNomeCompleto(),
                    processamentos
            );
        }
        throw new IllegalArgumentException("Informe empresaId ou clientePessoaFisicaId.");
    }

    private List<DocumentoProcessamento> carregarProcessamentosPj(
            PerfilUsuario perfil,
            Long empresaId,
            boolean incluirCompetenciasArquivadas
    ) {
        if (perfil == PerfilUsuario.CONTADOR && !incluirCompetenciasArquivadas) {
            return documentoProcessamentoRepository.findByEmpresaIdAndStatusExcluindoCompetenciaArquivada(
                    empresaId,
                    ProcessamentoStatus.PROCESSADO
            );
        }
        return documentoProcessamentoRepository.findByEmpresaIdAndStatus(empresaId, ProcessamentoStatus.PROCESSADO);
    }

    private List<DocumentoProcessamento> carregarProcessamentosPf(
            PerfilUsuario perfil,
            Long clientePfId,
            boolean incluirCompetenciasArquivadas
    ) {
        if (perfil == PerfilUsuario.CONTADOR && !incluirCompetenciasArquivadas) {
            return documentoProcessamentoRepository.findByClientePessoaFisicaIdAndStatusExcluindoCompetenciaArquivada(
                    clientePfId,
                    ProcessamentoStatus.PROCESSADO
            );
        }
        return documentoProcessamentoRepository.findByClientePessoaFisicaIdAndStatus(
                clientePfId,
                ProcessamentoStatus.PROCESSADO
        );
    }

    private static DocumentosValidadosAgrupadosResponse abasVaziasTomador(
            Long empresaId,
            String cnpj,
            String razaoSocial,
            Long clientePessoaFisicaId,
            String cpfClientePf,
            String nomeClientePf
    ) {
        List<DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse> abas = TipoDocumentoAba.ordemAbas().stream()
                .map(id -> new DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse(
                        id,
                        TipoDocumentoAba.tituloAba(id),
                        List.of()
                ))
                .toList();
        return new DocumentosValidadosAgrupadosResponse(
                empresaId,
                cnpj,
                razaoSocial,
                clientePessoaFisicaId,
                cpfClientePf,
                nomeClientePf,
                abas
        );
    }

    private DocumentosValidadosAgrupadosResponse montarDocumentosValidadosPorAba(
            Long empresaId,
            String cnpj,
            String razaoSocial,
            Long clientePessoaFisicaId,
            String cpfClientePf,
            String nomeClientePf,
            List<DocumentoProcessamento> processamentos
    ) {
        Map<String, List<DocumentosValidadosAgrupadosResponse.DocumentoValidadoItemResponse>> porAba = new LinkedHashMap<>();
        for (String aba : TipoDocumentoAba.ordemAbas()) {
            porAba.put(aba, new ArrayList<>());
        }
        for (DocumentoProcessamento dp : processamentos) {
            String idAba = TipoDocumentoAba.idAbaParaTipoDetectado(dp.getTipoDocumento());
            List<DadosExtraidosPendenciaDto.CampoExtraido> campos = listarCamposDoProcessamento(dp);
            PendenciaDocumento pend = dp.getEntrega().getPendencia();
            String jsonProc = dp.getDadosExtraidosJson();
            DocumentosValidadosAgrupadosResponse.DocumentoValidadoItemResponse item =
                    new DocumentosValidadosAgrupadosResponse.DocumentoValidadoItemResponse(
                            pend.getId(),
                            dp.getId(),
                            pend.getTemplateDocumento().getNome(),
                            dp.getEntrega().getNomeArquivoOriginal(),
                            dp.getTipoDocumento(),
                            dp.getAtualizadoEm().toString(),
                            dp.getConfianca(),
                            campos,
                            extrairDetalhamentoDocumentoDoJson(jsonProc),
                            extrairCapturaPerfilDoJson(jsonProc)
                    );
            if (!porAba.containsKey(idAba)) {
                idAba = "OUTROS";
            }
            porAba.get(idAba).add(item);
        }
        List<DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse> abas = TipoDocumentoAba.ordemAbas().stream()
                .map(id -> new DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse(
                        id,
                        TipoDocumentoAba.tituloAba(id),
                        List.copyOf(porAba.getOrDefault(id, List.of()))
                ))
                .toList();
        return new DocumentosValidadosAgrupadosResponse(
                empresaId,
                cnpj,
                razaoSocial,
                clientePessoaFisicaId,
                cpfClientePf,
                nomeClientePf,
                abas
        );
    }

    private static String valorCampoJsonParaExibicao(JsonNode n) {
        if (n == null || n.isNull()) {
            return "";
        }
        if (n.isTextual() || n.isNumber() || n.isBoolean()) {
            return n.asText();
        }
        return n.toString();
    }

    private List<DadosExtraidosPendenciaDto.CampoExtraido> extrairCamposDoJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode camposNode = root.get("camposExtraidos");
            if (camposNode == null || !camposNode.isObject()) {
                return List.of();
            }
            List<DadosExtraidosPendenciaDto.CampoExtraido> lista = new ArrayList<>();
            camposNode.fields().forEachRemaining(e -> lista.add(
                    new DadosExtraidosPendenciaDto.CampoExtraido(
                            e.getKey(),
                            valorCampoJsonParaExibicao(e.getValue()),
                            inferirTipoCampo(e.getKey()).name()
                    )
            ));
            return lista;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private void sincronizarCamposPersistidos(DocumentoProcessamento processamento) {
        if (processamento.getId() == null) {
            return;
        }
        documentoDadoExtraidoRepository.deleteByProcessamento_Id(processamento.getId());
        if (processamento.getStatus() != ProcessamentoStatus.PROCESSADO) {
            return;
        }
        List<DadosExtraidosPendenciaDto.CampoExtraido> campos = extrairCamposDoJson(processamento.getDadosExtraidosJson());
        int ordem = 0;
        for (DadosExtraidosPendenciaDto.CampoExtraido c : campos) {
            DocumentoDadoExtraido linha = new DocumentoDadoExtraido();
            linha.setProcessamento(processamento);
            linha.setNomeCampo(c.nome());
            linha.setValor(c.valor());
            linha.setOrdem(ordem++);
            linha.setTipoCampo(inferirTipoCampo(c.nome()));
            documentoDadoExtraidoRepository.save(linha);
        }
    }

    private static TipoCampoExtraido inferirTipoCampo(String nomeCampo) {
        if (nomeCampo == null) {
            return TipoCampoExtraido.TEXTO;
        }
        String n = nomeCampo.toLowerCase(Locale.ROOT);
        if (n.contains("cnpj")) {
            return TipoCampoExtraido.CNPJ;
        }
        if (n.contains("cpf")) {
            return TipoCampoExtraido.CPF;
        }
        if (n.contains("vencimento") || n.contains("data")
                || n.contains("emissao") || n.contains("competencia")) {
            return TipoCampoExtraido.DATA;
        }
        if (n.contains("valor") || n.contains("preco") || n.contains("total")) {
            return TipoCampoExtraido.MOEDA;
        }
        if (n.contains("aliquota") || n.contains("deducoesnfse") || n.contains("basecalculo")
                || n.contains("valoriss") || n.contains("creditoiptu")) {
            return TipoCampoExtraido.MOEDA;
        }
        return TipoCampoExtraido.TEXTO;
    }

    @Transactional
    public DadosExtraidosPendenciaDto atualizarCamposExtraidos(
            Long processamentoId,
            List<Map<String, String>> camposRequest,
            Usuario usuarioAtual
    ) {
        DocumentoProcessamento processamento = documentoProcessamentoRepository.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento de processamento nao encontrado."));
        PendenciaDocumento pend = processamento.getEntrega().getPendencia();
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (!PendenciaClienteDono.clienteEhDonoDaPendencia(usuarioAtual, pend)) {
                throw new IllegalArgumentException("Cliente sem permissão para editar este documento.");
            }
        } else if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Perfil não autorizado a editar campos extraídos.");
        }
        if (processamento.getStatus() != ProcessamentoStatus.PROCESSADO) {
            throw new IllegalArgumentException("Somente documentos validados permitem edicao de campos.");
        }
        if (camposRequest == null || camposRequest.isEmpty()) {
            throw new IllegalArgumentException("Lista de campos obrigatoria.");
        }
        Map<String, String> valoresAnteriores = documentoDadoExtraidoRepository
                .findByProcessamento_IdOrderByOrdemAsc(processamentoId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoDadoExtraido::getNomeCampo,
                        d -> d.getValor() == null ? "" : d.getValor(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        documentoDadoExtraidoRepository.deleteByProcessamento_Id(processamentoId);

        Map<String, String> novoMap = new LinkedHashMap<>();
        int ordem = 0;
        for (Map<String, String> linha : camposRequest) {
            String nome = linha.get("nome");
            if (nome == null || nome.isBlank()) {
                continue;
            }
            nome = nome.trim();
            String valor = linha.get("valor");
            if (valor == null) {
                valor = "";
            }
            DocumentoDadoExtraido ent = new DocumentoDadoExtraido();
            ent.setProcessamento(processamento);
            ent.setNomeCampo(nome);
            ent.setValor(valor);
            ent.setOrdem(ordem++);
            ent.setTipoCampo(inferirTipoCampo(nome));
            documentoDadoExtraidoRepository.save(ent);
            novoMap.put(nome, valor);
        }
        if (novoMap.isEmpty()) {
            throw new IllegalArgumentException("Nenhum campo valido informado.");
        }
        atualizarJsonCamposExtraidos(processamento, novoMap);
        processamento.setAtualizadoEm(LocalDateTime.now());
        documentoProcessamentoRepository.save(processamento);

        String resumo = montarResumoAlteracaoCampos(valoresAnteriores, novoMap);
        registrarHistorico(processamento, "CAMPOS_EDITADOS", resumo, usuarioAtual.getNome());

        Long pendenciaId = processamento.getEntrega().getPendencia().getId();
        return obterDadosExtraidosPorPendencia(pendenciaId, usuarioAtual);
    }

    private static String montarResumoAlteracaoCampos(Map<String, String> anterior, Map<String, String> novo) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : novo.entrySet()) {
            String key = e.getKey();
            String va = anterior.getOrDefault(key, "");
            String vn = e.getValue();
            if (!va.equals(vn)) {
                if (!sb.isEmpty()) {
                    sb.append(" | ");
                }
                sb.append(key).append(": \"").append(va).append("\" -> \"").append(vn).append("\"");
            }
        }
        if (sb.isEmpty()) {
            return "Campos salvos sem alteracao de valores.";
        }
        String s = sb.toString();
        if (s.length() > 1800) {
            return s.substring(0, 1797) + "...";
        }
        return s;
    }

    private void atualizarJsonCamposExtraidos(DocumentoProcessamento processamento, Map<String, String> campos) {
        try {
            ObjectNode root;
            if (processamento.getDadosExtraidosJson() == null || processamento.getDadosExtraidosJson().isBlank()) {
                root = objectMapper.createObjectNode();
            } else {
                JsonNode node = objectMapper.readTree(processamento.getDadosExtraidosJson());
                root = node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode();
            }
            ObjectNode camposNode = objectMapper.createObjectNode();
            campos.forEach(camposNode::put);
            root.set("camposExtraidos", camposNode);
            processamento.setDadosExtraidosJson(objectMapper.writeValueAsString(root));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Falha ao atualizar JSON consolidado do documento.");
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportarDadosExtraidosCsv(Long pendenciaId, Usuario usuarioAtual) {
        DadosExtraidosPendenciaDto dto = obterDadosExtraidosPorPendencia(pendenciaId, usuarioAtual);
        StringBuilder sb = new StringBuilder();
        sb.append("campo;valor;tipo\n");
        for (DadosExtraidosPendenciaDto.CampoExtraido c : dto.campos()) {
            sb.append(escaparCsv(c.nome())).append(";")
                    .append(escaparCsv(c.valor())).append(";")
                    .append(escaparCsv(c.tipo()))
                    .append("\n");
        }
        if (dto.detalhamentoDocumento() != null && !dto.detalhamentoDocumento().isEmpty()) {
            Map<String, String> flat = new LinkedHashMap<>();
            flattenJsonNodeParaCampos("", objectMapper.valueToTree(dto.detalhamentoDocumento()), flat);
            for (Map.Entry<String, String> e : flat.entrySet()) {
                sb.append(escaparCsv("holerite." + e.getKey())).append(";")
                        .append(escaparCsv(e.getValue())).append(";TEXTO\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Achata objeto/array JSON em chaves tipo {@code proventos[0].descricao} (exportação). */
    private static void flattenJsonNodeParaCampos(String prefix, JsonNode n, Map<String, String> out) {
        if (n == null || n.isNull()) {
            return;
        }
        if (n.isObject()) {
            n.fields().forEachRemaining(e -> {
                String p = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                flattenJsonNodeParaCampos(p, e.getValue(), out);
            });
        } else if (n.isArray()) {
            for (int i = 0; i < n.size(); i++) {
                flattenJsonNodeParaCampos(prefix + "[" + i + "]", n.get(i), out);
            }
        } else {
            String v;
            if (n.isTextual()) {
                v = n.asText();
            } else if (n.isNumber()) {
                v = n.asText();
            } else if (n.isBoolean()) {
                v = Boolean.toString(n.booleanValue());
            } else {
                v = n.toString();
            }
            out.put(prefix, v);
        }
    }

    private static String escaparCsv(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\"", "\"\"");
        if (t.contains(";") || t.contains("\n") || t.contains("\"")) {
            return "\"" + t + "\"";
        }
        return t;
    }

    @Transactional(readOnly = true)
    public byte[] exportarDadosExtraidosPdf(Long pendenciaId, Usuario usuarioAtual) throws IOException {
        DadosExtraidosPendenciaDto dto = obterDadosExtraidosPorPendencia(pendenciaId, usuarioAtual);
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        List<String> linhas = new ArrayList<>();
        linhas.add("Contab360 - Dados extraidos");
        linhas.add("Arquivo: " + sanitizeParaPdf(dto.nomeArquivoOriginal()));
        linhas.add("Tipo: " + sanitizeParaPdf(dto.tipoDocumento()) + " | Conf.: "
                + String.format(Locale.ROOT, "%.2f", dto.confianca()));
        linhas.add("");
        for (DadosExtraidosPendenciaDto.CampoExtraido c : dto.campos()) {
            linhas.add(sanitizeParaPdf(formatarRotuloCampoPdf(c.nome())) + " (" + c.tipo() + "):");
            String val = c.valor() == null ? "" : sanitizeParaPdf(c.valor());
            for (String parte : quebrarTextoPdf(val, 95)) {
                linhas.add("  " + parte);
            }
            linhas.add("");
        }
        if (dto.detalhamentoDocumento() != null && !dto.detalhamentoDocumento().isEmpty()) {
            linhas.add("--- Detalhamento holerite ---");
            Map<String, String> flat = new LinkedHashMap<>();
            flattenJsonNodeParaCampos("", objectMapper.valueToTree(dto.detalhamentoDocumento()), flat);
            for (Map.Entry<String, String> e : flat.entrySet()) {
                linhas.add(sanitizeParaPdf(formatarRotuloCampoPdf("holerite." + e.getKey())) + " (TEXTO):");
                String val = e.getValue() == null ? "" : sanitizeParaPdf(e.getValue());
                for (String parte : quebrarTextoPdf(val, 95)) {
                    linhas.add("  " + parte);
                }
                linhas.add("");
            }
        }
        float margin = 48;
        float lineH = 12;
        float minY = 52;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(doc, page);
            stream.beginText();
            stream.setFont(font, 10);
            stream.newLineAtOffset(margin, 740);
            float yPos = 740;
            for (String linha : linhas) {
                if (yPos < minY + lineH * 2) {
                    stream.endText();
                    stream.close();
                    page = new PDPage();
                    doc.addPage(page);
                    stream = new PDPageContentStream(doc, page);
                    stream.beginText();
                    stream.setFont(font, 10);
                    stream.newLineAtOffset(margin, 740);
                    yPos = 740;
                }
                stream.newLineAtOffset(0, -lineH);
                yPos -= lineH;
                String texto = linha.length() > 400 ? linha.substring(0, 397) + "..." : linha;
                stream.showText(texto);
            }
            stream.endText();
            stream.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static String sanitizeParaPdf(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c < 127) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static String formatarRotuloCampoPdf(String chave) {
        if (chave == null) {
            return "";
        }
        return chave.replace('_', ' ');
    }

    private static List<String> quebrarTextoPdf(String s, int maxChars) {
        List<String> partes = new ArrayList<>();
        if (s == null || s.isBlank()) {
            partes.add("");
            return partes;
        }
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(i + maxChars, s.length());
            partes.add(s.substring(i, end));
            i = end;
        }
        return partes;
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
        // Texto nativo do PDF (ex.: NFS-e simulada) é confiável; 0,72 ficava sempre abaixo do corte 0,75 e gerava falso REJEITADO.
        double confianca = textoExtraido.isBlank() ? 0.35 : 0.82;
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

    private static DocumentBuilderFactory criarDbfSeguro() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(false);
            return dbf;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Falha ao configurar XML parser seguro", e);
        }
    }

    private void processarXml(DocumentoProcessamento processamento, Path path) throws Exception {
        DocumentBuilderFactory dbf = criarDbfSeguro();
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
                motivosRevisao,
                null
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

    /**
     * Para {@link TipoDocumentoCatalogo} FOLHA_PAGAMENTO, tenta ler o valor líquido (contracheque).
     * Caso contrário, ou se não achar rótulo, usa o primeiro valor com centavos no texto (heurística fraca).
     */
    private String extrairValor(String texto, String tipoDocumento) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        if ("FOLHA_PAGAMENTO".equals(tipoDocumento)) {
            String liquido = extrairValorLiquidoFolha(texto);
            if (!liquido.isBlank()) {
                return liquido;
            }
        }
        if ("NOTA_FISCAL".equals(tipoDocumento)) {
            String totalNota = extrairValorTotalDaNotaFiscal(texto);
            if (!totalNota.isBlank()) {
                return totalNota;
            }
        }
        return extrairValorGenerico(texto);
    }

    /**
     * Remove CNPJ/CPF/PIS do texto para {@link #VALOR_PATTERN} não capturar trechos como {@code 12.34} ou {@code 123.45}.
     */
    private static String textoSemIdsParaExtracaoValor(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String t = CNPJ_FORMATADO_PATTERN.matcher(texto).replaceAll(" ");
        t = t.replaceAll("\\b\\d{14}\\b", " ");
        t = CPF_FORMATADO_PATTERN.matcher(t).replaceAll(" ");
        t = PIS_PASEP_FORMATADO_PATTERN.matcher(t).replaceAll(" ");
        return t;
    }

    /** R$ com espaços opcionais (NBSP, R $). */
    private static final Pattern VALOR_RS_BR = Pattern.compile("[Rr]\\s*\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");
    private static final Pattern TEM_LIQUIDO = Pattern.compile("(?i)l[ií]quido");
    private static final Pattern TEM_RECEBER = Pattern.compile("(?i)receber");
    /** Líquido e valor podem quebrar linha em PDF/OCR; evita depender de uma única linha. */
    private static final Pattern VALOR_LIQUIDO_MULTILINHA = Pattern.compile(
            "(?is)l[ií]quido.{0,240}?receber.{0,160}?[Rr]\\s*\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");

    private static String normalizarEspacosParaValor(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ');
    }

    /**
     * Cada {@code R$ 1.234,56} do texto: se nos caracteres anteriores houver "líquido" e "receber",
     * é o valor líquido (evita pegar o primeiro {@code 3.500,00} do salário base).
     */
    private static String extrairValorLiquidoPorRsEContexto(String t) {
        Matcher m = VALOR_RS_BR.matcher(t);
        String ultimoValido = "";
        while (m.find()) {
            int start = m.start();
            int ctx = Math.max(0, start - 560);
            String antes = t.substring(ctx, start);
            if (TEM_LIQUIDO.matcher(antes).find() && TEM_RECEBER.matcher(antes).find()) {
                ultimoValido = normalizarValorMonetarioBr(m.group(1));
            }
        }
        return ultimoValido;
    }

    private static String extrairValorLiquidoFolha(String texto) {
        String t = normalizarEspacosParaValor(texto);
        String porContexto = extrairValorLiquidoPorRsEContexto(t);
        if (!porContexto.isBlank()) {
            return porContexto;
        }
        Matcher multi = VALOR_LIQUIDO_MULTILINHA.matcher(t);
        if (multi.find()) {
            return normalizarValorMonetarioBr(multi.group(1));
        }
        for (Pattern p : VALOR_LIQUIDO_FOLHA_PATTERNS) {
            Matcher m = p.matcher(t);
            if (m.find()) {
                return normalizarValorMonetarioBr(m.group(1));
            }
        }
        for (String linha : t.split("\\R")) {
            String linhaTrim = linha.trim();
            if (!Pattern.compile("(?i)l[ií]quido").matcher(linhaTrim).find()) {
                continue;
            }
            if (!Pattern.compile("(?i)receber").matcher(linhaTrim).find()) {
                continue;
            }
            Matcher vr = VALOR_RS_BR.matcher(linhaTrim);
            if (vr.find()) {
                return normalizarValorMonetarioBr(vr.group(1));
            }
        }
        return "";
    }

    /**
     * Converte valor brasileiro ({@code 3.217,50} ou {@code 145,60}) para string com ponto decimal ({@code 3217.50}).
     */
    private static String normalizarValorMonetarioBr(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return "";
        }
        String t = bruto.trim();
        if (!t.contains(",")) {
            return t;
        }
        int ultimaVirgula = t.lastIndexOf(',');
        String parteEsquerda = t.substring(0, ultimaVirgula).replace(".", "");
        String centavos = t.substring(ultimaVirgula + 1);
        return parteEsquerda + "." + centavos;
    }

    /**
     * Prioriza valor no formato brasileiro ({@code 3.500,00}). Evita {@code 3.50} extraído do meio de {@code 3.500,00}.
     */
    private static String extrairValorGenerico(String texto) {
        String limpo = textoSemIdsParaExtracaoValor(texto);
        Matcher formatoBr = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b").matcher(limpo);
        if (formatoBr.find()) {
            return normalizarValorMonetarioBr(formatoBr.group(1));
        }
        Matcher matcher = VALOR_PATTERN.matcher(limpo);
        while (matcher.find()) {
            int end = matcher.end();
            if (end < limpo.length() && Character.isDigit(limpo.charAt(end))) {
                continue;
            }
            return matcher.group(1);
        }
        return "";
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
            List<String> motivosRevisao,
            Object detalhamentoDocumento
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fonte", safe(fonte));
        payload.put("tipoDocumento", safe(tipoDocumento));
        payload.put("status", status);
        payload.put("confianca", confianca);
        payload.put("camposObrigatorios", TipoDocumentoCatalogo.camposObrigatorios(tipoDocumento));
        Map<String, String> camposParaJson = campos;
        if (detalhamentoDocumento != null) {
            camposParaJson = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : campos.entrySet()) {
                if (!e.getKey().startsWith("holerite.")) {
                    camposParaJson.put(e.getKey(), e.getValue());
                }
            }
        }
        payload.put("camposExtraidos", camposParaJson);
        payload.put("motivosRevisao", motivosRevisao);
        if (detalhamentoDocumento != null) {
            payload.put("detalhamentoDocumento", detalhamentoDocumento);
            payload.put("capturaPerfil", "HOLERITE_ESCRITORIO_COMPLETO");
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"fonte\":\"" + safe(fonte) + "\",\"tipoDocumento\":\"" + safe(tipoDocumento) + "\"}";
        }
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    /**
     * Extrai competência (aaaa-mm): prioriza linhas com rótulo; ignora {@code mm/aaaa} que é parte de data
     * {@code dd/mm/aaaa} (ex.: admissão 15/03/2022 gerava 2022-03 antes do campo COMPETÊNCIA).
     */
    private String extrairCompetencia(String texto, DocumentoProcessamento processamento) {
        String base = texto == null ? "" : texto;
        Matcher rotulo = COMPETENCIA_COM_ROTULO.matcher(base);
        if (rotulo.find()) {
            int mes = Integer.parseInt(rotulo.group(1), 10);
            int ano = Integer.parseInt(rotulo.group(2), 10);
            if (mes >= 1 && mes <= 12 && ano >= 1990 && ano <= 2100) {
                return String.format("%04d-%02d", ano, mes);
            }
        }
        List<String> candidatos = new ArrayList<>();
        Matcher matcher = COMPETENCIA_PATTERN.matcher(base);
        while (matcher.find()) {
            if (matcher.group(1) != null && matcher.group(2) != null) {
                int start = matcher.start();
                if (start >= 3) {
                    String tresAntes = base.substring(start - 3, start);
                    if (tresAntes.matches("\\d{2}/")) {
                        continue;
                    }
                }
                int mes = Integer.parseInt(matcher.group(1), 10);
                int ano = Integer.parseInt(matcher.group(2), 10);
                if (mes >= 1 && mes <= 12 && ano >= 1990 && ano <= 2100) {
                    candidatos.add(String.format("%04d-%02d", ano, mes));
                }
            } else if (matcher.group(3) != null && matcher.group(4) != null) {
                String c = matcher.group(3) + "-" + matcher.group(4);
                if (competenciaYmPlausivel(c)) {
                    candidatos.add(c);
                }
            }
        }
        if (candidatos.isEmpty()) {
            return "";
        }
        if (processamento != null && processamento.getEntrega() != null && processamento.getEntrega().getPendencia() != null) {
            var pend = processamento.getEntrega().getPendencia();
            String esperado = String.format("%04d-%02d", pend.getCompetencia().getAno(), pend.getCompetencia().getMes());
            if (candidatos.contains(esperado)) {
                return esperado;
            }
            int anoRef = pend.getCompetencia().getAno();
            String melhor = candidatos.get(0);
            int menorDist = Integer.MAX_VALUE;
            for (String c : candidatos) {
                int y = Integer.parseInt(c.substring(0, 4), 10);
                int dist = Math.abs(y - anoRef);
                if (dist < menorDist) {
                    menorDist = dist;
                    melhor = c;
                }
            }
            return melhor;
        }
        return candidatos.get(candidatos.size() - 1);
    }

    private String extrairData(String texto) {
        Matcher matcher = DATA_PATTERN.matcher(texto == null ? "" : texto);
        return matcher.find() ? matcher.group(1) : "";
    }

    private boolean competenciaYmPlausivel(String yyyyMm) {
        if (yyyyMm == null || !yyyyMm.matches("\\d{4}-\\d{2}")) {
            return false;
        }
        int ano = Integer.parseInt(yyyyMm.substring(0, 4), 10);
        int mes = Integer.parseInt(yyyyMm.substring(5, 7), 10);
        if (mes < 1 || mes > 12) {
            return false;
        }
        if (ano < 1990 || ano > 2100) {
            return false;
        }
        return true;
    }

    private String normalizarCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return "";
        }
        String c = competencia.trim();
        if (c.matches("\\d{4}-\\d{2}")) {
            return competenciaYmPlausivel(c) ? c : "";
        }
        if (c.matches("\\d{2}/\\d{4}")) {
            String[] p = c.split("/");
            String norm = p[1] + "-" + p[0];
            return competenciaYmPlausivel(norm) ? norm : "";
        }
        return "";
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
        /* INSS antes de ISS: senão "INSS" casa o substring "ISS" e vira falso ISS. */
        if (base.contains("INSS")) {
            return "INSS";
        }
        if (base.contains("ISS")) {
            return "ISS";
        }
        if (base.contains("ICMS")) {
            return "ICMS";
        }
        if (base.contains("PIS")) {
            return "PIS";
        }
        if (base.contains("COFINS")) {
            return "COFINS";
        }
        if (base.contains("IRPJ")) {
            return "IRPJ";
        }
        if (base.contains("CSLL")) {
            return "CSLL";
        }
        if (base.contains("FGTS")) {
            return "FGTS";
        }
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

    /**
     * Valor principal da nota (faixa "VALOR TOTAL DA NOTA = R$ …"), evitando pegar o primeiro 0,00 do quadro ISS.
     */
    private static String extrairValorTotalDaNotaFiscal(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String t = normalizarEspacosParaValor(texto);
        Matcher m = Pattern.compile(
                        "(?is)valor\\s+total\\s+da\\s+nota\\s*=\\s*[Rr]\\s*\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b")
                .matcher(t);
        if (m.find()) {
            return normalizarValorMonetarioBr(m.group(1));
        }
        return "";
    }

    private static final Pattern PAT_VAL_BR = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");

    private static String nfseMontanteAposRotulo(String texto, Pattern rotuloComValor) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        Matcher m = rotuloComValor.matcher(texto);
        if (!m.find()) {
            return "";
        }
        return normalizarValorMonetarioBr(m.group(1));
    }

    private void preencherCamposQuadroIssNfse(String textoBruto, Map<String, String> campos) {
        String t = normalizarEspacosParaValor(textoBruto);
        String ded = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)deduc.{0,220}?" + PAT_VAL_BR.pattern()));
        String base = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)base\\s+de\\s+c[aá\u00E1]lculo.{0,220}?" + PAT_VAL_BR.pattern()));
        String vIss = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)valor\\s+do\\s+iss.{0,220}?" + PAT_VAL_BR.pattern()));
        String iptu = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)(?:cr[eé\u00E9]dito\\s+p/|abatimento\\s+iptu).{0,260}?" + PAT_VAL_BR.pattern()));
        String aliq = "";
        Matcher ma = Pattern.compile("(?is)al[ií\u00ED]quota.{0,260}?(\\d{1,2},\\d{2})\\s*%").matcher(t);
        if (ma.find()) {
            aliq = normalizarValorMonetarioBr(ma.group(1));
        }
        campos.put("deducoesNfse", safe(ded));
        campos.put("baseCalculoIss", safe(base));
        campos.put("aliquotaIssPercentual", safe(aliq));
        campos.put("valorIss", safe(vIss));
        campos.put("creditoIptu", safe(iptu));
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
        String valor = extrairValor(texto, tipo);
        String competencia = extrairCompetencia(texto, processamento);
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
        campos.put("tipoDocumento", tipo);
        Object detalhamentoDocumento = null;
        if ("NOTA_FISCAL".equals(tipo)) {
            preencherCamposQuadroIssNfse(texto, campos);
        }
        if ("FOLHA_PAGAMENTO".equals(tipo)) {
            HoleriteDetalhado hol = HoleriteTextoParser.parse(texto);
            if (hol.preenchidoMinimo()) {
                detalhamentoDocumento = objectMapper.convertValue(hol, java.util.Map.class);
                if (hol.totais() != null && hol.totais().valorLiquidoNumerico() != null
                        && !hol.totais().valorLiquidoNumerico().isBlank()) {
                    campos.put("valor", safe(hol.totais().valorLiquidoNumerico()));
                }
                if (hol.empresa() != null && hol.empresa().cnpjDigitos() != null
                        && !hol.empresa().cnpjDigitos().isBlank()) {
                    campos.put("cnpj", safe(hol.empresa().cnpjDigitos()));
                    if (cnpjPrestador.isBlank()) {
                        campos.put("cnpjPrestador", safe(hol.empresa().cnpjDigitos()));
                    }
                }
                String compHol = hol.periodo() != null ? hol.periodo().competencia() : "";
                if (compHol != null && compHol.matches("\\d{2}/\\d{4}")) {
                    String[] p = compHol.split("/");
                    String ym = p[1] + "-" + p[0];
                    if (competenciaYmPlausivel(ym)) {
                        campos.put("competencia", ym);
                    }
                }
                campos.put("tributo", "");
            }
        }
        aplicarValidacoesContabeis(processamento, tipo, campos, motivosRevisao);
        if (!motivosRevisao.isEmpty()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.67);
            observacao = motivosRevisao.get(0);
        }

        if (status == ProcessamentoStatus.REJEITADO && motivosRevisao.isEmpty() && "NOTA_FISCAL".equals(tipo)) {
            List<String> obr = TipoDocumentoCatalogo.camposObrigatorios(tipo);
            boolean cnpjOk = !obr.contains("cnpj") || !campos.getOrDefault("cnpj", "").isBlank();
            boolean valorOk = !obr.contains("valor") || !campos.getOrDefault("valor", "").isBlank();
            if (cnpjOk && valorOk) {
                status = ProcessamentoStatus.PROCESSADO;
                observacao = "Documento processado automaticamente.";
                severidade = SeveridadeRevisao.BAIXA;
                confianca = Math.max(confianca, 0.76);
            }
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
                motivosRevisao,
                detalhamentoDocumento
        ));
        processamento.setObservacaoProcessamento(observacao);
        processamento.setSeveridade(severidade);
        processamento.setAtualizadoEm(LocalDateTime.now());
        finalizarProcessamentoAutomatico(processamento, "PROCESSAMENTO_TEXTO");
    }

    private static String somenteDigitosIdDoc(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\D", "");
    }

    private List<TemplateDocumento> templatesDoTomador(PendenciaDocumento pendencia) {
        if (pendencia.getEmpresa() != null) {
            return templateDocumentoRepository.findByEmpresaIdOrderByNomeAsc(pendencia.getEmpresa().getId());
        }
        if (pendencia.getClientePessoaFisica() != null) {
            return templateDocumentoRepository.findByClientePessoaFisicaIdOrderByNomeAsc(
                    pendencia.getClientePessoaFisica().getId());
        }
        return List.of();
    }

    private String validarComPendencia(
            DocumentoProcessamento processamento,
            String tipoDetectado,
            String cnpjPrincipal,
            String cnpjSecundario
    ) {
        PendenciaDocumento pendencia = processamento.getEntrega().getPendencia();
        String nomeTemplate = pendencia.getTemplateDocumento().getNome().toLowerCase(Locale.ROOT);
        String doc1 = somenteDigitosIdDoc(cnpjPrincipal);
        String doc2 = somenteDigitosIdDoc(cnpjSecundario);

        boolean tipoFiscal = tipoDetectado.contains("NOTA")
                || tipoDetectado.contains("NFE")
                || tipoDetectado.contains("NFSE");

        if (pendencia.getEmpresa() != null) {
            String cnpjEmpresa = pendencia.getEmpresa().getCnpj();
            if (tipoFiscal) {
                if (!doc1.isBlank() || !doc2.isBlank()) {
                    boolean cnpjCompativel = cnpjEmpresa.equals(doc1) || cnpjEmpresa.equals(doc2);
                    if (!cnpjCompativel) {
                        return "CNPJ do documento difere da empresa da pendência. Revisar.";
                    }
                }
            } else if (!doc1.isBlank() && !cnpjEmpresa.equals(doc1)) {
                return "CNPJ do documento difere da empresa da pendência. Revisar.";
            } else if (!doc2.isBlank() && !cnpjEmpresa.equals(doc2) && doc1.isBlank()) {
                return "CNPJ do documento difere da empresa da pendência. Revisar.";
            }
        } else if (pendencia.getClientePessoaFisica() != null) {
            String cpfTomador = pendencia.getClientePessoaFisica().getCpf();
            if (tipoFiscal) {
                boolean algumOnze = (doc1.length() == 11) || (doc2.length() == 11);
                if (algumOnze) {
                    boolean cpfCompat = cpfTomador.equals(doc1) || cpfTomador.equals(doc2);
                    if (!cpfCompat) {
                        return "CPF do documento fiscal não confere com o cadastro da pendência. Revisar.";
                    }
                }
            } else if (!doc1.isBlank() && doc1.length() == 11 && !cpfTomador.equals(doc1)) {
                return "CPF do documento difere do cadastro da pendência. Revisar.";
            } else if (!doc2.isBlank() && doc2.length() == 11 && !cpfTomador.equals(doc2) && doc1.isBlank()) {
                return "CPF do documento difere do cadastro da pendência. Revisar.";
            }
        }

        boolean tipoCompativel = true;
        if (nomeTemplate.contains("nota") && !tipoFiscal) {
            tipoCompativel = false;
        } else if (nomeTemplate.contains("extrato") && !tipoDetectado.contains("EXTRATO")) {
            tipoCompativel = false;
        } else if ((nomeTemplate.contains("holerite")
                || nomeTemplate.contains("folha")
                || nomeTemplate.contains("contracheque")
                || nomeTemplate.contains("contra-cheque")
                || nomeTemplate.contains("contra cheque"))
                && !tipoDetectado.contains("FOLHA")) {
            tipoCompativel = false;
        }

        if (!tipoCompativel) {
            return "Tipo detectado não combina com o template da pendência. Revisar.";
        }

        boolean temTemplate = templatesDoTomador(pendencia).stream()
                .anyMatch(t -> t.getId().equals(pendencia.getTemplateDocumento().getId()));
        if (!temTemplate) {
            return "Template não encontrado para o tomador desta pendência. Revisar.";
        }

        return "";
    }

    private void finalizarProcessamentoAutomatico(DocumentoProcessamento processamento, String origem) {
        PendenciaDocumento pendencia = processamento.getEntrega().getPendencia();
        boolean aprovado = processamento.getStatus() == ProcessamentoStatus.PROCESSADO;
        pendencia.setStatus(aprovado ? PendenciaStatus.VALIDADO : PendenciaStatus.REJEITADO);
        pendenciaDocumentoRepository.save(pendencia);
        competenciaArquivamentoService.sincronizarArquivamentoCompetencia(pendencia.getCompetencia().getId());
        DocumentoProcessamento salvo = documentoProcessamentoRepository.save(processamento);
        sincronizarCamposPersistidos(salvo);
        String acao = aprovado ? "APROVADO_IA" : "REJEITADO_IA";
        String motivo = processamento.getObservacaoProcessamento();
        if (motivo == null || motivo.isBlank()) {
            motivo = aprovado ? "Documento aprovado automaticamente pela IA." : "Documento rejeitado automaticamente pela IA.";
        }
        registrarHistorico(salvo, acao, "[" + origem + "] " + motivo, "IA");
    }

}
