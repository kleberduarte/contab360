package com.contabilidade.pj.pendencia.service;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.pendencia.PendenciaClienteDono;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado;
import com.contabilidade.pj.pendencia.holerite.HoleriteTextoParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
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
import org.xml.sax.InputSource;
import com.contabilidade.pj.pendencia.entity.*;
import com.contabilidade.pj.pendencia.repository.*;
import com.contabilidade.pj.pendencia.dto.*;

@Service
public class DocumentoInteligenciaService {
    private static final int LIMITE_NOME_CAMPO_PERSISTIDO = 120;

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
    private final DocTabMapperService docTabMapperService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentoInteligenciaService(
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            EmpresaRepository empresaRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            CompetenciaArquivamentoService competenciaArquivamentoService,
            DocTabMapperService docTabMapperService
    ) {
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.documentoDadoExtraidoRepository = documentoDadoExtraidoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.revisaoDocumentoHistoricoRepository = revisaoDocumentoHistoricoRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.empresaRepository = empresaRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.competenciaArquivamentoService = competenciaArquivamentoService;
        this.docTabMapperService = docTabMapperService;
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
            throw new IllegalStateException("Falha no processamento: " + ex.getMessage(), ex);
        }

        return processamento;
    }

    @Transactional
    public DocumentoProcessamento reprocessar(Long processamentoId, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode reprocessar documentos.");
        }
        DocumentoProcessamento processamento = documentoProcessamentoRepository.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Processamento não encontrado."));
        processamento.setStatus(ProcessamentoStatus.PROCESSANDO);
        processamento.setAtualizadoEm(LocalDateTime.now());
        documentoProcessamentoRepository.save(processamento);
        try {
            processarArquivo(processamento, Path.of(processamento.getEntrega().getCaminhoArquivo()));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha no reprocessamento: " + ex.getMessage(), ex);
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

    private static final List<ProcessamentoStatus> STATUS_VISIVEIS_CONTADOR = List.of(
            ProcessamentoStatus.PROCESSADO, ProcessamentoStatus.REJEITADO);

    private List<DocumentoProcessamento> carregarProcessamentosPj(
            PerfilUsuario perfil,
            Long empresaId,
            boolean incluirCompetenciasArquivadas
    ) {
        if (perfil == PerfilUsuario.CONTADOR) {
            if (!incluirCompetenciasArquivadas) {
                return documentoProcessamentoRepository.findByEmpresaIdAndStatusInExcluindoCompetenciaArquivada(
                        empresaId, STATUS_VISIVEIS_CONTADOR);
            }
            return documentoProcessamentoRepository.findByEmpresaIdAndStatusIn(
                    empresaId, STATUS_VISIVEIS_CONTADOR);
        }
        return documentoProcessamentoRepository.findByEmpresaIdAndStatus(empresaId, ProcessamentoStatus.PROCESSADO);
    }

    private List<DocumentoProcessamento> carregarProcessamentosPf(
            PerfilUsuario perfil,
            Long clientePfId,
            boolean incluirCompetenciasArquivadas
    ) {
        if (perfil == PerfilUsuario.CONTADOR) {
            if (!incluirCompetenciasArquivadas) {
                return documentoProcessamentoRepository.findByClientePessoaFisicaIdAndStatusInExcluindoCompetenciaArquivada(
                        clientePfId, STATUS_VISIVEIS_CONTADOR);
            }
            return documentoProcessamentoRepository.findByClientePessoaFisicaIdAndStatusIn(
                    clientePfId, STATUS_VISIVEIS_CONTADOR);
        }
        return documentoProcessamentoRepository.findByClientePessoaFisicaIdAndStatus(
                clientePfId, ProcessamentoStatus.PROCESSADO);
    }

    private DocumentosValidadosAgrupadosResponse abasVaziasTomador(
            Long empresaId,
            String cnpj,
            String razaoSocial,
            Long clientePessoaFisicaId,
            String cpfClientePf,
            String nomeClientePf
    ) {
        List<DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse> abas = docTabMapperService.ordemAbas().stream()
                .map(id -> new DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse(
                        id,
                        docTabMapperService.tituloAba(id),
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
        for (String aba : docTabMapperService.ordemAbas()) {
            porAba.put(aba, new ArrayList<>());
        }
        for (DocumentoProcessamento dp : processamentos) {
            PendenciaDocumento pend = dp.getEntrega().getPendencia();
            String idAba = resolverAbaDocumento(dp, pend);
            List<DadosExtraidosPendenciaDto.CampoExtraido> campos = listarCamposDoProcessamento(dp);
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
                            extrairCapturaPerfilDoJson(jsonProc),
                            dp.getStatus().name()
                    );
            if (!porAba.containsKey(idAba)) {
                idAba = "OUTROS";
            }
            porAba.get(idAba).add(item);
        }
        List<DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse> abas = docTabMapperService.ordemAbas().stream()
                .map(id -> new DocumentosValidadosAgrupadosResponse.AbaDocumentosResponse(
                        id,
                        docTabMapperService.tituloAba(id),
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

    /**
     * Prioriza a classificação da IA (tipo detectado) e usa o template como fallback.
     */
    private String resolverAbaDocumento(DocumentoProcessamento processamento, PendenciaDocumento pendencia) {
        String idAba = docTabMapperService.idAbaParaTipoDetectado(processamento.getTipoDocumento());
        if (!"OUTROS".equals(idAba)) {
            return idAba;
        }
        return docTabMapperService.idAbaParaTemplateNome(pendencia.getTemplateDocumento().getNome());
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
            String nomeCampo = c.nome() == null ? "" : c.nome().trim();
            if (nomeCampo.isBlank() || nomeCampo.length() > LIMITE_NOME_CAMPO_PERSISTIDO) {
                continue;
            }
            DocumentoDadoExtraido linha = new DocumentoDadoExtraido();
            linha.setProcessamento(processamento);
            linha.setNomeCampo(nomeCampo);
            linha.setValor(c.valor());
            linha.setOrdem(ordem++);
            linha.setTipoCampo(inferirTipoCampo(nomeCampo));
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
                || n.contains("valoriss") || n.contains("creditoiptu")
                || n.equals("pis") || n.equals("cofins") || n.equals("inss")
                || n.equals("irrf") || n.equals("csll")
                || n.contains("valorservicos") || n.contains("valorliquido")) {
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

    @Transactional(readOnly = true)
    public ArquivoOriginalDocumento obterArquivoOriginal(Long processamentoId, Usuario usuarioAtual) {
        DocumentoProcessamento processamento = documentoProcessamentoRepository.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento de processamento nao encontrado."));
        PendenciaDocumento pend = processamento.getEntrega().getPendencia();
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (!PendenciaClienteDono.clienteEhDonoDaPendencia(usuarioAtual, pend)) {
                throw new IllegalArgumentException("Cliente sem permissão para baixar este documento.");
            }
        } else if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Perfil não autorizado a baixar arquivo.");
        }
        String nomeArquivo = processamento.getEntrega().getNomeArquivoOriginal();
        String caminhoArquivo = processamento.getEntrega().getCaminhoArquivo();
        try {
            Path path = Path.of(caminhoArquivo);
            byte[] conteudo = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return new ArquivoOriginalDocumento(nomeArquivo, contentType, conteudo);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo original.", ex);
        }
    }

    public record ArquivoOriginalDocumento(String nomeArquivo, String contentType, byte[] conteudo) {}

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

    /**
     * Evita {@code SAXParseException: Content is not allowed in prolog.} por BOM UTF-8, XML gzipado,
     * UTF-16 com BOM, linhas iniciais de metadado ({@code # ...}) ou texto antes do {@code <?xml} / primeiro {@code <}.
     */
    private static byte[] prepararConteudoXmlParaParse(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) {
            return raw == null ? new byte[0] : raw;
        }
        if (raw.length >= 3 && raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF) {
            raw = Arrays.copyOfRange(raw, 3, raw.length);
        }
        if (raw.length >= 2 && (raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B) {
            try (GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                raw = gzin.readAllBytes();
            }
            if (raw.length >= 3 && raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF) {
                raw = Arrays.copyOfRange(raw, 3, raw.length);
            }
        }
        if (raw.length >= 2 && raw[0] == (byte) 0xFF && raw[1] == (byte) 0xFE) {
            raw = new String(raw, StandardCharsets.UTF_16LE).getBytes(StandardCharsets.UTF_8);
        } else if (raw.length >= 2 && raw[0] == (byte) 0xFE && raw[1] == (byte) 0xFF) {
            raw = new String(raw, StandardCharsets.UTF_16BE).getBytes(StandardCharsets.UTF_8);
        }
        raw = removerLinhasIniciaisMetadadoHash(raw);
        return cortarAtePrimeiroMarcadorXml(raw);
    }

    private static boolean isAsciiWs(byte b) {
        return b == 9 || b == 10 || b == 13 || b == 32;
    }

    /** Remove blocos iniciais de linhas comentário ({@code #}) — ex.: massa de testes / export com cabeçalho. */
    private static byte[] removerLinhasIniciaisMetadadoHash(byte[] raw) {
        int start = 0;
        while (start < raw.length) {
            int lineEnd = start;
            while (lineEnd < raw.length && raw[lineEnd] != '\n' && raw[lineEnd] != '\r') {
                lineEnd++;
            }
            int a = start;
            while (a < lineEnd && isAsciiWs(raw[a])) {
                a++;
            }
            int b = lineEnd;
            while (b > a && isAsciiWs(raw[b - 1])) {
                b--;
            }
            if (a >= b) {
                start = lineEnd;
                while (start < raw.length && (raw[start] == '\n' || raw[start] == '\r')) {
                    start++;
                }
                continue;
            }
            if (raw[a] == '#') {
                start = lineEnd;
                while (start < raw.length && (raw[start] == '\n' || raw[start] == '\r')) {
                    start++;
                }
                continue;
            }
            break;
        }
        return start > 0 ? Arrays.copyOfRange(raw, start, raw.length) : raw;
    }

    private static int indexOfBytes(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) {
            return needle.length == 0 ? 0 : -1;
        }
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int indexOfByte(byte[] haystack, byte needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] trimLeadingAsciiWs(byte[] raw) {
        int i = 0;
        while (i < raw.length && isAsciiWs(raw[i])) {
            i++;
        }
        return i > 0 ? Arrays.copyOfRange(raw, i, raw.length) : raw;
    }

    private static byte[] cortarAtePrimeiroMarcadorXml(byte[] raw) {
        raw = trimLeadingAsciiWs(raw);
        if (raw.length == 0) {
            return raw;
        }
        if (raw[0] == '<') {
            return raw;
        }
        int p = indexOfBytes(raw, "<?xml".getBytes(StandardCharsets.US_ASCII));
        if (p < 0) {
            p = indexOfBytes(raw, "<?XML".getBytes(StandardCharsets.US_ASCII));
        }
        if (p >= 0) {
            return Arrays.copyOfRange(raw, p, raw.length);
        }
        p = indexOfByte(raw, (byte) '<');
        if (p > 0) {
            return Arrays.copyOfRange(raw, p, raw.length);
        }
        return raw;
    }

    private void processarXml(DocumentoProcessamento processamento, Path path) throws Exception {
        byte[] bruto = Files.readAllBytes(path);
        byte[] xmlBytes = prepararConteudoXmlParaParse(bruto);
        if (xmlBytes.length == 0) {
            throw new IllegalArgumentException("Arquivo XML vazio.");
        }
        DocumentBuilderFactory dbf = criarDbfSeguro();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xmlBytes)));
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
                null,
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
        if ("NOTA_FISCAL".equals(tipoDocumento) || "NFCE".equals(tipoDocumento)) {
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

    private String extrairCnpjPrincipalGuiaImposto(String texto, String cnpjTomador, String cnpjPrestador) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        if (!cnpjTomador.isBlank()) {
            return cnpjTomador;
        }

        // Modelo do comprovante IRRF: "CNPJ / CPF" seguido do CNPJ da fonte pagadora.
        String porRotuloPrincipal = extrairCnpjAposRotulo(texto, "cnpj\\s*/\\s*cpf");
        if (!porRotuloPrincipal.isBlank()) {
            return porRotuloPrincipal;
        }

        // Fallback: evita capturar CNPJ de "Informacoes Complementares" (ex.: convenio medico).
        String base = texto;
        Pattern secoesComplementares = Pattern.compile("(?i)\\b7\\.?\\s*informa[cç][oõ]es\\s+complementares\\b");
        Matcher mSecao = secoesComplementares.matcher(texto);
        if (mSecao.find()) {
            base = texto.substring(0, mSecao.start());
        }
        String porTrechoPrincipal = extrairCnpj(base);
        if (!porTrechoPrincipal.isBlank()) {
            return porTrechoPrincipal;
        }

        if (!cnpjPrestador.isBlank()) {
            return cnpjPrestador;
        }
        return extrairCnpj(texto);
    }

    private static String normalizarQuebras(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String extrairBloco(String texto, String regexInicio, String regexFim) {
        String base = normalizarQuebras(texto);
        Pattern p = Pattern.compile("(?is)" + regexInicio + "\\s*(.*?)\\s*(?=" + regexFim + "|$)");
        Matcher m = p.matcher(base);
        if (!m.find()) {
            return "";
        }
        return m.group(1).trim();
    }

    private static Map<String, Object> campoDetalhe(String nome, String valor, String tipo) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("nome", nome);
        item.put("valor", valor == null ? "" : valor.trim());
        item.put("tipo", tipo);
        return item;
    }

    private static void addCampoDetalhe(List<Map<String, Object>> out, String nome, String valor, String tipo) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        out.add(campoDetalhe(nome, valor, tipo));
    }

    private static Map<String, Object> secaoDetalhe(String id, String titulo, List<Map<String, Object>> campos) {
        Map<String, Object> sec = new LinkedHashMap<>();
        sec.put("id", id);
        sec.put("titulo", titulo);
        sec.put("campos", campos);
        return sec;
    }

    private static List<Map<String, Object>> ordenarCamposResponsavelInformacoes(List<Map<String, Object>> campos) {
        if (campos == null || campos.isEmpty()) {
            return campos;
        }
        Map<String, Integer> ordem = Map.of(
                "nome", 0,
                "data", 1,
                "assinatura", 2
        );
        List<Map<String, Object>> ordenados = new ArrayList<>(campos);
        ordenados.sort((a, b) -> {
            String nomeA = String.valueOf(a.getOrDefault("nome", "")).trim().toLowerCase(Locale.ROOT);
            String nomeB = String.valueOf(b.getOrDefault("nome", "")).trim().toLowerCase(Locale.ROOT);
            int ordemA = ordem.getOrDefault(nomeA, 99);
            int ordemB = ordem.getOrDefault(nomeB, 99);
            if (ordemA != ordemB) {
                return Integer.compare(ordemA, ordemB);
            }
            return nomeA.compareTo(nomeB);
        });
        return ordenados;
    }

    private static List<Map<String, Object>> parseItensNumeradosComValoresMoeda(String bloco) {
        List<Map<String, Object>> itens = new ArrayList<>();
        if (bloco == null || bloco.isBlank()) {
            return itens;
        }
        String base = normalizarQuebras(bloco);
        Pattern moedaPattern = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");
        Matcher itemMatcher = Pattern.compile("(?ms)^\\s*(\\d{2})\\.\\s*([\\p{L}].+?)(?=^\\s*\\d{2}\\.\\s*[\\p{L}]|\\z)").matcher(base);
        while (itemMatcher.find()) {
            String numero = itemMatcher.group(1).trim();
            String conteudo = itemMatcher.group(2).trim();
            if (conteudo.isBlank()) {
                continue;
            }
            String valor = "";
            Matcher valoresNoItem = moedaPattern.matcher(conteudo);
            while (valoresNoItem.find()) {
                valor = normalizarValorMonetarioBr(valoresNoItem.group(1));
            }
            // Remove monetary values absorbed into the description (two-column PDF layout artifact)
            String descricao = moedaPattern.matcher(conteudo).replaceAll("").replaceAll("\\s+", " ").trim();
            String nome = numero + ". " + descricao;
            itens.add(campoDetalhe(nome, valor, "MOEDA"));
        }
        // PDFs com layout de duas colunas colocam todas as descrições primeiro e os valores depois como bloco.
        // Padrão: o último item absorve todos os valores (trailing block); os demais ficam sem valor.
        // Quando isso ocorre, coleta os valores do bloco em ordem e redistribui um para cada item.
        if (itens.size() > 1) {
            long semValorPrimeiros = itens.stream().limit(itens.size() - 1)
                    .filter(i -> String.valueOf(i.getOrDefault("valor", "")).isBlank()).count();
            String valorUltimo = String.valueOf(itens.get(itens.size() - 1).getOrDefault("valor", "")).trim();
            if (semValorPrimeiros == itens.size() - 1 && !valorUltimo.isBlank()) {
                List<String> todosValores = new ArrayList<>();
                Matcher mTodos = moedaPattern.matcher(base);
                while (mTodos.find()) {
                    todosValores.add(normalizarValorMonetarioBr(mTodos.group(1)));
                }
                if (todosValores.size() == itens.size()) {
                    for (int i = 0; i < itens.size(); i++) {
                        itens.get(i).put("valor", todosValores.get(i));
                    }
                }
            }
        }
        return itens;
    }

    private static String toTitleCase(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String[] palavras = texto.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : palavras) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String normalizarNomeCampoLivre(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String t = texto.toLowerCase(Locale.ROOT);
        t = t.replaceAll("[áàâãä]", "a");
        t = t.replaceAll("[éèêë]", "e");
        t = t.replaceAll("[íìîï]", "i");
        t = t.replaceAll("[óòôõö]", "o");
        t = t.replaceAll("[úùûü]", "u");
        t = t.replaceAll("[ç]", "c");
        t = t.replaceAll("[^a-z0-9]+", " ").trim();
        return t;
    }

    private static String extrairNumeroItem(String nomeCampo) {
        if (nomeCampo == null || nomeCampo.isBlank()) {
            return "";
        }
        Matcher m = Pattern.compile("^\\s*(\\d{2})\\b").matcher(nomeCampo);
        if (!m.find()) {
            return "";
        }
        return m.group(1);
    }

    private static void preencherCamposCanonicosIrpfSecao3(
            List<Map<String, Object>> itensSecao3,
            Map<String, String> camposBase
    ) {
        if (itensSecao3 == null || itensSecao3.isEmpty() || camposBase == null) {
            return;
        }
        for (Map<String, Object> item : itensSecao3) {
            String nome = String.valueOf(item.getOrDefault("nome", ""));
            String valor = String.valueOf(item.getOrDefault("valor", "")).trim();
            if (valor.isBlank()) {
                continue;
            }
            String numero = extrairNumeroItem(nome);
            String nomeNorm = normalizarNomeCampoLivre(nome);
            if ("01".equals(numero) || nomeNorm.contains("total dos rendimentos")) {
                camposBase.put("totalDosRendimentosInclusiveFerias", valor);
                continue;
            }
            if ("02".equals(numero) || nomeNorm.contains("contribuicao previdenciaria oficial")) {
                camposBase.put("contribuicaoPrevidenciariaOficial", valor);
                continue;
            }
            if ("03".equals(numero) || nomeNorm.contains("fapi")) {
                camposBase.put("contribuicaoEntidadesPrevidenciaComplementarPublicaOuPrivadaEFapi", valor);
                continue;
            }
            if ("04".equals(numero) || nomeNorm.contains("pensao alimenticia")) {
                camposBase.put("pensaoAlimenticia", valor);
                continue;
            }
            if ("05".equals(numero) || nomeNorm.contains("imposto sobre a renda retido na fonte")) {
                camposBase.put("impostoSobreARendaRetidoNaFonte", valor);
            }
        }
    }

    private static void preencherCamposCanonicosItensSecao(
            String prefixoCampo,
            List<Map<String, Object>> itens,
            Map<String, String> camposBase
    ) {
        if (prefixoCampo == null || prefixoCampo.isBlank() || itens == null || itens.isEmpty() || camposBase == null) {
            return;
        }
        for (Map<String, Object> item : itens) {
            String nome = String.valueOf(item.getOrDefault("nome", ""));
            String valor = String.valueOf(item.getOrDefault("valor", "")).trim();
            String numero = extrairNumeroItem(nome);
            if (numero.isBlank()) {
                continue;
            }
            camposBase.put(prefixoCampo + "Item" + numero, valor);
        }
    }

    private static boolean guiaIrpfSecao3Completa(Map<String, String> camposBase) {
        if (camposBase == null) {
            return false;
        }
        return !camposBase.getOrDefault("totalDosRendimentosInclusiveFerias", "").isBlank()
                && !camposBase.getOrDefault("contribuicaoPrevidenciariaOficial", "").isBlank()
                && !camposBase.getOrDefault("contribuicaoEntidadesPrevidenciaComplementarPublicaOuPrivadaEFapi", "").isBlank()
                && !camposBase.getOrDefault("pensaoAlimenticia", "").isBlank()
                && !camposBase.getOrDefault("impostoSobreARendaRetidoNaFonte", "").isBlank();
    }

    private static boolean guiaIrpfTopicosCompletos(Map<String, String> camposBase) {
        if (camposBase == null) {
            return false;
        }
        if (!guiaIrpfSecao3Completa(camposBase)) {
            return false;
        }
        boolean secao1Ok = !camposBase.getOrDefault("nomeFontePagadora", "").isBlank()
                && !camposBase.getOrDefault("cnpjCpfFontePagadora", "").isBlank();
        boolean secao2Ok = !camposBase.getOrDefault("nomeBeneficiario", "").isBlank()
                && !camposBase.getOrDefault("cpfBeneficiario", "").isBlank()
                && !camposBase.getOrDefault("naturezaRendimento", "").isBlank();
        boolean secao7Ok = !camposBase.getOrDefault("informacoesComplementaresTexto", "").isBlank();
        boolean secao8NomeOk = !camposBase.getOrDefault("nomeResponsavelInformacoes", "").isBlank()
                || !camposBase.getOrDefault("Nome", "").isBlank();
        boolean secao8Ok = !camposBase.getOrDefault("dataResponsavelInformacoes", "").isBlank() && secao8NomeOk;
        return secao1Ok && secao2Ok && secao7Ok && secao8Ok;
    }

    private static String motivoGuiaIrpfIncompleta(Map<String, String> camposBase) {
        if (guiaIrpfTopicosCompletos(camposBase)) {
            return "";
        }
        boolean faltouNomeResponsavel = camposBase == null
                || (camposBase.getOrDefault("nomeResponsavelInformacoes", "").isBlank()
                && camposBase.getOrDefault("Nome", "").isBlank());
        boolean faltouApenasNomeResponsavel = faltouNomeResponsavel
                && camposBase != null
                && !camposBase.getOrDefault("dataResponsavelInformacoes", "").isBlank()
                && !camposBase.getOrDefault("nomeFontePagadora", "").isBlank()
                && !camposBase.getOrDefault("cnpjCpfFontePagadora", "").isBlank()
                && !camposBase.getOrDefault("nomeBeneficiario", "").isBlank()
                && !camposBase.getOrDefault("cpfBeneficiario", "").isBlank()
                && !camposBase.getOrDefault("naturezaRendimento", "").isBlank()
                && !camposBase.getOrDefault("informacoesComplementaresTexto", "").isBlank()
                && guiaIrpfSecao3Completa(camposBase);
        if (faltouApenasNomeResponsavel) {
            return "Seção 8: campo Nome (conforme PDF) não identificado no documento.";
        }
        return "Guia IRPF com campos incompletos para preenchimento automático seguro (tópicos 1-8).";
    }

    /**
     * Extrai valores monetários que aparecem logo após o cabeçalho {@code regexHeader} e antes do
     * primeiro item numerado {@code \d{2}\.} da seção seguinte — padrão do PDFBox para documentos
     * IRPF com layout de duas colunas.
     */
    private static List<String> extrairValoresEntreHeaderEPrimeiroItemIrpf(String base, String regexHeader) {
        Pattern pHeader = Pattern.compile("(?i)" + regexHeader);
        Matcher mHeader = pHeader.matcher(base);
        if (!mHeader.find()) {
            return List.of();
        }
        int posAposHeader = mHeader.end();
        // \d{2}\. seguido de espaço e letra (ex.: "01. Parcela") — evita match em valores como "26.162,49"
        Matcher mPrimeiroItem = Pattern.compile("(?m)^\\s*\\d{2}\\.\\s*[\\p{L}]").matcher(base);
        mPrimeiroItem.region(posAposHeader, base.length());
        int fimRegiao = mPrimeiroItem.find() ? mPrimeiroItem.start() : base.length();
        String regiao = base.substring(posAposHeader, fimRegiao);
        Pattern moeda = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");
        List<String> valores = new ArrayList<>();
        Matcher mV = moeda.matcher(regiao);
        while (mV.find()) {
            valores.add(normalizarValorMonetarioBr(mV.group(1)));
        }
        return valores;
    }

    /**
     * Remove os primeiros {@code n} valores monetários de um bloco de texto — usado para descartar
     * valores da seção 3 que o PDFBox inclui incorretamente no início do bloco da seção 4.
     */
    private static String removerValoresIniciais(String bloco, int n) {
        if (n <= 0 || bloco == null || bloco.isBlank()) {
            return bloco == null ? "" : bloco;
        }
        Pattern moeda = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");
        Matcher m = moeda.matcher(bloco);
        int count = 0;
        int ultimoFim = 0;
        while (m.find() && count < n) {
            ultimoFim = m.end();
            count++;
        }
        if (count < n) {
            return bloco;
        }
        return bloco.substring(ultimoFim).stripLeading();
    }

    /**
     * Extrator dedicado para a seção 5 do IRPF. O PDFBox coloca alguns valores antes das descrições
     * dos itens e mistura itens/valores com a seção 6 por causa do layout de colunas do PDF.
     * Estratégia: extrai os valores posicionalmente e os mapeia pelas labels conhecidas.
     */
    private static List<Map<String, Object>> extrairItensSecao5Irpf(String base) {
        Pattern moeda = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b");
        String regexSec5 = "5\\.\\s*Rendimentos\\s*Sujeitos\\s+[àa]\\s*Tributa[cç][aã]o\\s+Exclusiva\\s*\\(Rendimento\\s+L[ií]quido\\)";
        String regexSec6 = "6\\.\\s*Rendimentos\\s+Recebidos\\s+Acumuladamente";

        Matcher mSec5 = Pattern.compile("(?i)" + regexSec5).matcher(base);
        if (!mSec5.find()) {
            return List.of();
        }
        int posSec5 = mSec5.end();
        Matcher mSec6 = Pattern.compile("(?i)" + regexSec6).matcher(base);
        int posSec6 = mSec6.find(posSec5) ? mSec6.start() : base.length();
        Matcher mSec7 = Pattern.compile("(?i)7\\.\\s*Informa[cç][oõ]es\\s+Complementares").matcher(base);
        int posSec7 = mSec7.find(posSec6) ? mSec7.start() : base.length();

        // Valores que o PDFBox coloca antes dos itens numerados dentro do bloco da seção 5.
        // Os itens da seção 5 são sempre 01, 02, 03 (com zero à esquerda).
        // Usamos "0\d\." para não confundir com valores monetários como "30.109,97".
        Matcher mPrimeiroItem5 = Pattern.compile("(?m)^\\s*0\\d\\.\\s*").matcher(base);
        mPrimeiroItem5.region(posSec5, posSec6);
        int posItens5 = mPrimeiroItem5.find() ? mPrimeiroItem5.start() : posSec6;
        List<String> valoresPreItens = new ArrayList<>();
        Matcher mVPre = moeda.matcher(base.substring(posSec5, posItens5));
        while (mVPre.find()) {
            valoresPreItens.add(normalizarValorMonetarioBr(mVPre.group(1)));
        }

        // Itens numerados que aparecem no bloco da seção 5 (podem ser apenas 01 e 02).
        String blocoSec5Items = base.substring(posItens5, posSec6);
        List<String> labelsOrdem = new ArrayList<>();
        Matcher mItems = Pattern.compile("(?ms)^\\s*(0\\d)\\.\\s*(.+?)(?=^\\s*0\\d\\.\\s*|\\z)").matcher(blocoSec5Items);
        Map<String, String> labelPorNumero = new LinkedHashMap<>();
        while (mItems.find()) {
            String num = mItems.group(1).trim();
            String label = moeda.matcher(mItems.group(2)).replaceAll("").replaceAll("\\s+", " ").trim();
            labelPorNumero.put(num, label);
            labelsOrdem.add(num);
        }

        // Itens numerados que aparecem no bloco da seção 6 mas pertencem à seção 5 (item 03, etc.)
        // O PDFBox coloca o item 03 (Outros/PLR) e seu valor na região da seção 6.
        // O valor do item 02 (IRRF sobre 13º) aparece adjacente ao item 03 no bloco da seção 6.
        String blocoSec6 = base.substring(posSec6, posSec7);
        Matcher mItems6 = Pattern.compile("(?ms)^\\s*(0\\d)\\.\\s*(.+?)(?=^\\s*0\\d\\.\\s*|\\z)").matcher(blocoSec6);
        Map<String, String> valoresSec6PorNumero = new LinkedHashMap<>();
        while (mItems6.find()) {
            String num = mItems6.group(1).trim();
            String label = moeda.matcher(mItems6.group(2)).replaceAll("").replaceAll("\\(.*?\\)", "").replaceAll("\\s+", " ").trim();
            String numNorm = normalizarNomeCampoLivre(label);
            if (!labelPorNumero.containsKey(num) && (numNorm.contains("outros") || numNorm.contains("plr"))) {
                labelPorNumero.put(num, label);
                labelsOrdem.add(num);
                // O valor do item 02 aparece logo após o item 03 no bloco da seção 6.
                Matcher mValSec6 = moeda.matcher(mItems6.group(2));
                if (mValSec6.find()) {
                    valoresSec6PorNumero.put(num, normalizarValorMonetarioBr(mValSec6.group(1)));
                } else {
                    int posAposItem = posSec6 + mItems6.end();
                    Matcher mVApos = moeda.matcher(base.substring(posAposItem, Math.min(posAposItem + 200, posSec7)));
                    if (mVApos.find()) {
                        valoresSec6PorNumero.put(num, normalizarValorMonetarioBr(mVApos.group(1)));
                    }
                }
            }
        }

        // Layout PDFBox para seção 5 do IRPF (duas colunas):
        //   valoresPreItens[0] → item 01 (13º salário)
        //   valoresPreItens[1] → item 03 (Outros/PLR)
        //   valoresSec6["03"]  → item 02 (IRRF sobre 13º) — valor aparece após "03. Outros" no bloco sec6
        // O item 02 não tem valor pré-item; seu valor fica na região da seção 6 junto ao item 03.
        List<String> numerosOrdenados = labelsOrdem.stream().distinct().sorted().toList();
        // Atribui valoresPreItens aos itens em ordem, pulando o item "02".
        List<String> numerosParaPreItens = numerosOrdenados.stream()
                .filter(n -> !n.equals("02")).toList();
        Map<String, String> valorPorNumero = new LinkedHashMap<>();
        for (int i = 0; i < numerosParaPreItens.size() && i < valoresPreItens.size(); i++) {
            valorPorNumero.put(numerosParaPreItens.get(i), valoresPreItens.get(i));
        }
        if (valoresSec6PorNumero.containsKey("03") && labelPorNumero.containsKey("02")) {
            valorPorNumero.put("02", valoresSec6PorNumero.get("03"));
        }
        List<Map<String, Object>> itens = new ArrayList<>();
        for (String num : numerosOrdenados) {
            String label = labelPorNumero.getOrDefault(num, "");
            String valor = valorPorNumero.getOrDefault(num, "");
            itens.add(campoDetalhe(num + ". " + label, valor, "MOEDA"));
        }
        return itens;
    }

    private Map<String, Object> extrairDetalhamentoGuiaImpostoIrpf(String texto, Map<String, String> camposBase) {
        String base = normalizarQuebras(texto);
        if (base.isBlank()) {
            return null;
        }
        List<Map<String, Object>> secoes = new ArrayList<>();

        List<Map<String, Object>> secao1 = new ArrayList<>();
        String nomeFonte = "";
        Matcher mNomeFonte = Pattern.compile("(?is)\\n([^\\n]+?)\\n\\s*Nome\\s+Empresarial\\s*/\\s*Nome\\s+Completo\\b").matcher(base);
        if (mNomeFonte.find()) {
            nomeFonte = mNomeFonte.group(1).trim();
        }
        addCampoDetalhe(secao1, "Nome Empresarial / Nome Completo", nomeFonte, "TEXTO");
        addCampoDetalhe(secao1, "CNPJ / CPF", camposBase.getOrDefault("cnpj", ""), "CNPJ");
        camposBase.put("nomeFontePagadora", safe(nomeFonte));
        camposBase.put("cnpjCpfFontePagadora", safe(camposBase.getOrDefault("cnpj", "")));
        secoes.add(secaoDetalhe("secao1", "1. Fonte Pagadora Pessoa Jurídica ou Pessoa Física", secao1));

        List<Map<String, Object>> secao2 = new ArrayList<>();
        String nomeBeneficiario = "";
        Matcher mNomeBen = Pattern.compile("(?is)2\\.\\s*Pessoa\\s+F[ií]sica\\s+Benefici[aá]ria\\s+dos\\s+Rendimentos\\s*(.*?)\\s*Nome\\s+Completo").matcher(base);
        if (mNomeBen.find()) {
            nomeBeneficiario = mNomeBen.group(1).trim();
        }
        String cpfBenef = "";
        Matcher mCpfBen = Pattern.compile("(?is)Nome\\s+Completo\\s*(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})\\s*CPF").matcher(base);
        if (mCpfBen.find()) {
            cpfBenef = mCpfBen.group(1).replaceAll("\\D", "");
        }
        String natureza = "";
        Matcher mNatureza = Pattern.compile("(?im)^\\s*\\d{4}\\s*-\\s*.+$").matcher(base);
        if (mNatureza.find()) {
            natureza = mNatureza.group().trim();
        }
        addCampoDetalhe(secao2, "Nome Completo", nomeBeneficiario, "TEXTO");
        addCampoDetalhe(secao2, "CPF", cpfBenef, "CPF");
        addCampoDetalhe(secao2, "Natureza do Rendimento", natureza, "TEXTO");
        camposBase.put("nomeBeneficiario", safe(primeiroNaoVazio(
                nomeBeneficiario,
                camposBase.get("nomeBeneficiario"),
                camposBase.get("Nome Completo")
        )));
        camposBase.put("cpfBeneficiario", safe(primeiroNaoVazio(cpfBenef, camposBase.get("cpfBeneficiario"), camposBase.get("CPF"))));
        camposBase.put("naturezaRendimento", safe(primeiroNaoVazio(natureza, camposBase.get("naturezaRendimento"), camposBase.get("Natureza do Rendimento"))));
        secoes.add(secaoDetalhe("secao2", "2. Pessoa Física Beneficiária dos Rendimentos", secao2));

        // Seção 3: labels estão no bloco3, mas o PDFBox coloca os valores logo após o cabeçalho da seção 4.
        String bloco3 = extrairBloco(base,
                "3\\.\\s*Rendimentos\\s+Tribut[aá]veis,\\s*Dedu[cç][oõ]es\\s+e\\s+Imposto\\s+sobre\\s+a\\s+Renda\\s+Retido\\s+na\\s+Fonte\\s*\\(IRRF\\)",
                "4\\.\\s*Rendimentos\\s+Isentos\\s+e\\s+N[aã]o\\s+Tribut[aá]veis");
        List<Map<String, Object>> itensSecao3 = parseItensNumeradosComValoresMoeda(bloco3);
        if (!itensSecao3.isEmpty()) {
            // Busca os valores da seção 3 na região entre o cabeçalho da seção 4 e o primeiro item "01." dela.
            List<String> valoresSec3 = extrairValoresEntreHeaderEPrimeiroItemIrpf(base,
                    "4\\.\\s*Rendimentos\\s+Isentos\\s+e\\s+N[aã]o\\s+Tribut[aá]veis");
            if (valoresSec3.size() == itensSecao3.size()) {
                for (int i = 0; i < itensSecao3.size(); i++) {
                    itensSecao3.get(i).put("valor", valoresSec3.get(i));
                }
            }
        }
        preencherCamposCanonicosIrpfSecao3(itensSecao3, camposBase);
        secoes.add(secaoDetalhe("secao3",
                "3. Rendimentos Tributáveis, Deduções e Imposto sobre a Renda Retido na Fonte (IRRF)",
                itensSecao3));

        String bloco4 = extrairBloco(base,
                "4\\.\\s*Rendimentos\\s+Isentos\\s+e\\s+N[aã]o\\s+Tribut[aá]veis",
                "5\\.\\s*Rendimentos\\s*Sujeitos\\s+[àa]\\s*Tributa[cç][aã]o\\s+Exclusiva");
        // Remove valores que pertencem à seção 3 (aparecem no início do bloco4 por causa do layout PDF).
        String bloco4SemValoresSec3 = removerValoresIniciais(bloco4, itensSecao3.size());
        List<Map<String, Object>> itensSecao4 = parseItensNumeradosComValoresMoeda(bloco4SemValoresSec3);
        preencherCamposCanonicosItensSecao("secao4RendimentosIsentos", itensSecao4, camposBase);
        secoes.add(secaoDetalhe("secao4", "4. Rendimentos Isentos e Não Tributáveis", itensSecao4));

        // Seção 5: o PDFBox coloca alguns valores antes das descrições dos itens; usa extrator dedicado.
        List<Map<String, Object>> itensSecao5 = extrairItensSecao5Irpf(base);
        preencherCamposCanonicosItensSecao("secao5TributacaoExclusiva", itensSecao5, camposBase);
        secoes.add(secaoDetalhe("secao5",
                "5. Rendimentos Sujeitos à Tributação Exclusiva (Rendimento Líquido)",
                itensSecao5));

        String bloco6 = extrairBloco(base,
                "6\\.\\s*Rendimentos\\s+Recebidos\\s+Acumuladamente\\s+Art\\.\\s*12-A\\s+da\\s+Lei\\s+n[ºo]\\s*7\\.713,\\s*de\\s*1988\\s*\\(sujeitos\\s+[àa]\\s*tributa[cç][aã]o\\s+exclusiva\\)",
                "7\\.\\s*Informa[cç][oõ]es\\s+Complementares");
        List<Map<String, Object>> itensSecao6 = parseItensNumeradosComValoresMoeda(bloco6);
        preencherCamposCanonicosItensSecao("secao6RendimentosAcumulados", itensSecao6, camposBase);
        secoes.add(secaoDetalhe("secao6",
                "6. Rendimentos Recebidos Acumuladamente Art. 12-A da Lei nº 7.713, de 1988 (sujeitos à tributação exclusiva)",
                itensSecao6));

        String bloco7 = extrairBloco(base,
                "7\\.\\s*Informa[cç][oõ]es\\s+Complementares",
                "8\\.\\s*Respons[aá]vel\\s+pelas\\s+Informa[cç][oõ]es");
        List<Map<String, Object>> secao7 = new ArrayList<>();
        addCampoDetalhe(secao7, "Texto", bloco7, "TEXTO_LONGO");
        camposBase.put("informacoesComplementaresTexto", safe(bloco7));
        secoes.add(secaoDetalhe("secao7", "7. Informações Complementares", secao7));

        String bloco8 = extrairBloco(base,
                "8\\.\\s*Respons[aá]vel\\s+pelas\\s+Informa[cç][oõ]es",
                "P[aá]gina\\s+\\d+\\s+de\\s+\\d+");
        List<Map<String, Object>> secao8 = new ArrayList<>();
        String nomeResponsavel = extrairNomeResponsavelBloco8(bloco8);
        if (nomeResponsavel.isBlank()) {
            nomeResponsavel = fallbackNomeResponsavel(camposBase);
        }
        addCampoDetalhe(secao8, "Nome", nomeResponsavel, "TEXTO");
        Matcher mData = Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b").matcher(bloco8);
        String dataResponsavel = mData.find() ? normalizarData(mData.group()) : "";
        addCampoDetalhe(secao8, "Data", dataResponsavel, "DATA");
        boolean dispensaAssinatura = bloco8.toLowerCase(Locale.ROOT).contains("dispensa");
        addCampoDetalhe(secao8, "Assinatura", dispensaAssinatura ? "Dispensada (SRF 149/98)" : "Não obrigatório", "TEXTO");
        camposBase.put("dataResponsavelInformacoes", safe(dataResponsavel));
        camposBase.put("nomeResponsavelInformacoes", safe(nomeResponsavel));
        if (!nomeResponsavel.isBlank()) {
            camposBase.put("Nome", safe(nomeResponsavel));
        }
        secoes.add(secaoDetalhe("secao8", "8. Responsável pelas Informações", ordenarCamposResponsavelInformacoes(secao8)));

        Map<String, Object> det = new LinkedHashMap<>();
        det.put("tipoEstrutura", "GUIA_IMPOSTO_IRPF");
        det.put("secoes", secoes);
        return det;
    }

    private static String extrairNomeResponsavelBloco8(String bloco8) {
        if (bloco8 == null || bloco8.isBlank()) {
            return "";
        }
        // 0) Layout do PDF: rótulo "Nome" sozinho na linha e valor na linha seguinte.
        String porRotulo = extrairNomeAposRotuloNomeEmLinhaIsolada(bloco8);
        if (!porRotulo.isBlank()) {
            return porRotulo;
        }
        // 1) Layout clássico: "Nome DATA Assinatura" seguido da linha de valores.
        Matcher mCabecalho = Pattern.compile("(?i)Nome[^\\n]*DATA[^\\n]*\\n([^\\n]+)").matcher(bloco8);
        if (mCabecalho.find()) {
            String linhaValores = mCabecalho.group(1).trim();
            String nome = linhaValores.replaceAll("\\s*\\d{2}[/-]\\d{2}[/-]\\d{4}.*", "").trim();
            if (!nome.isBlank()) {
                return toTitleCase(nome);
            }
        }
        // 2) Nome e data na mesma linha (sem quebra após cabeçalho).
        Matcher mNomeDataLinha = Pattern.compile("(?im)^\\s*([\\p{L}][\\p{L}\\s.'-]{4,}?)\\s+\\d{2}[/-]\\d{2}[/-]\\d{4}(?:\\s+.*)?$")
                .matcher(bloco8);
        while (mNomeDataLinha.find()) {
            String candidato = mNomeDataLinha.group(1).trim();
            if (nomeResponsavelValido(candidato)) {
                return toTitleCase(candidato);
            }
        }
        // 3) Valor após rótulo "Nome" (mesma linha ou próxima).
        Matcher mCampoNome = Pattern.compile("(?is)\\bNome\\b\\s*[:\\-]?\\s*(?:\\n\\s*)?([\\p{L}][\\p{L}\\s.'-]{4,})")
                .matcher(bloco8);
        if (mCampoNome.find()) {
            String candidato = mCampoNome.group(1)
                    .replaceAll("\\s*\\bDATA\\b.*", "")
                    .replaceAll("\\s*\\d{2}[/-]\\d{2}[/-]\\d{4}.*", "")
                    .trim();
            if (nomeResponsavelValido(candidato)) {
                return toTitleCase(candidato);
            }
        }
        // 4) Fallback: linha isolada de texto com nome plausível.
        Matcher mFallback = Pattern.compile("(?im)^\\s*([\\p{L}][\\p{L}\\s.'-]{6,})\\s*$").matcher(bloco8);
        while (mFallback.find()) {
            String candidato = mFallback.group(1).trim();
            if (nomeResponsavelValido(candidato)) {
                return toTitleCase(candidato);
            }
        }
        // 5) Trecho antes da primeira data dd/mm/aaaa: linha com 2+ palavras (nome composto).
        Matcher mPrimeiraData = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(bloco8);
        int cut = mPrimeiraData.find() ? mPrimeiraData.start() : bloco8.length();
        String trecho = bloco8.substring(0, cut);
        for (String linha : trecho.split("\\R")) {
            String t = linha.trim();
            if (t.isEmpty()) {
                continue;
            }
            String lc = t.toLowerCase(Locale.ROOT);
            if (lc.matches("(?i)nome|data|assinatura") || lc.startsWith("8.")) {
                continue;
            }
            if (lc.contains("dispensa") || lc.contains("chancela") || lc.contains("srf")) {
                continue;
            }
            if (t.split("\\s+").length >= 2 && nomeResponsavelValido(t)) {
                return toTitleCase(t);
            }
        }
        return "";
    }

    /**
     * PDF costuma trazer o quadro da seção 8 como "Nome" em uma linha e o valor na seguinte.
     */
    private static String extrairNomeAposRotuloNomeEmLinhaIsolada(String bloco8) {
        String[] linhas = normalizarQuebras(bloco8).split("\\R");
        for (int i = 0; i < linhas.length - 1; i++) {
            String rotulo = linhas[i].trim();
            if (!rotulo.equalsIgnoreCase("nome")) {
                continue;
            }
            for (int j = i + 1; j < linhas.length; j++) {
                String candidato = linhas[j].trim();
                if (candidato.isEmpty()) {
                    continue;
                }
                String lc = candidato.toLowerCase(Locale.ROOT);
                if (lc.equals("data") || lc.equals("assinatura") || lc.matches("\\d{2}[/-]\\d{2}[/-]\\d{4}.*")) {
                    break;
                }
                if (nomeResponsavelValido(candidato)) {
                    return toTitleCase(candidato);
                }
                break;
            }
        }
        return "";
    }

    private static boolean nomeResponsavelValido(String candidato) {
        if (candidato == null || candidato.isBlank()) {
            return false;
        }
        String lc = candidato.toLowerCase(Locale.ROOT);
        return !lc.contains("data")
                && !lc.contains("nome")
                && !lc.contains("assinatura")
                && !lc.contains("dispensa")
                && !lc.contains("chancela")
                && !lc.contains("responsavel")
                && !lc.contains("responsável")
                && !lc.contains("inform");
    }

    private static String fallbackNomeResponsavel(Map<String, String> camposBase) {
        if (camposBase == null || camposBase.isEmpty()) {
            return "";
        }
        String[] candidatos = new String[] {
                camposBase.getOrDefault("nomeResponsavelInformacoes", ""),
                camposBase.getOrDefault("Nome", ""),
                camposBase.getOrDefault("nomeResponsavel", "")
        };
        for (String c : candidatos) {
            String nome = c == null ? "" : c.trim();
            if (!nome.isBlank() && nomeResponsavelValido(nome)) {
                return toTitleCase(nome);
            }
        }
        return "";
    }

    private static String primeiroNaoVazio(String... valores) {
        if (valores == null || valores.length == 0) {
            return "";
        }
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
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
            Object detalhamentoDocumento,
            String capturaPerfil
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
            if (capturaPerfil != null && capturaPerfil.startsWith("GUIA_IMPOSTO_IRPF")) {
                mesclarCamposSecoesGuiaIrpf(detalhamentoDocumento, camposParaJson);
                sincronizarNomeSecao8GuiaIrpf(camposParaJson);
            }
        }
        payload.put("camposExtraidos", camposParaJson);
        payload.put("motivosRevisao", motivosRevisao);
        if (detalhamentoDocumento != null) {
            payload.put("detalhamentoDocumento", detalhamentoDocumento);
        }
        if (capturaPerfil != null && !capturaPerfil.isBlank()) {
            payload.put("capturaPerfil", capturaPerfil);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"fonte\":\"" + safe(fonte) + "\",\"tipoDocumento\":\"" + safe(tipoDocumento) + "\"}";
        }
    }

    private void mesclarCamposSecoesGuiaIrpf(Object detalhamentoDocumento, Map<String, String> camposParaJson) {
        if (detalhamentoDocumento == null || camposParaJson == null) {
            return;
        }
        try {
            JsonNode det = objectMapper.valueToTree(detalhamentoDocumento);
            JsonNode secoes = det.get("secoes");
            if (secoes == null || !secoes.isArray()) {
                return;
            }
            for (JsonNode secao : secoes) {
                JsonNode campos = secao.get("campos");
                if (campos == null || !campos.isArray()) {
                    continue;
                }
                for (JsonNode campo : campos) {
                    String nome = campo.path("nome").asText("").trim();
                    String valor = campo.path("valor").asText("").trim();
                    if (nome.isBlank() || valor.isBlank()) {
                        continue;
                    }
                    camposParaJson.putIfAbsent(nome, valor);
                }
            }
        } catch (Exception ignored) {
            // Mantém o processamento robusto mesmo se o detalhamento vier fora do formato esperado.
        }
    }

    /** Alinha chave canônica interna com o rótulo do PDF ("Nome" na seção 8). */
    private static void sincronizarNomeSecao8GuiaIrpf(Map<String, String> camposParaJson) {
        if (camposParaJson == null) {
            return;
        }
        String nome = primeiroNaoVazio(
                camposParaJson.get("Nome"),
                camposParaJson.get("nomeResponsavelInformacoes")
        );
        if (nome == null || nome.isBlank()) {
            return;
        }
        nome = nome.trim();
        camposParaJson.put("Nome", nome);
        camposParaJson.put("nomeResponsavelInformacoes", nome);
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

    private String extrairDataPorRotulo(String texto, String... rotulos) {
        if (texto == null || texto.isBlank() || rotulos == null || rotulos.length == 0) {
            return "";
        }
        String base = texto;
        for (String rotulo : rotulos) {
            if (rotulo == null || rotulo.isBlank()) {
                continue;
            }
            String r = Pattern.quote(rotulo);
            Pattern rotuloAntesData = Pattern.compile("(?i)\\b" + r + "\\b[^0-9]{0,24}(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})");
            Matcher mAntes = rotuloAntesData.matcher(base);
            if (mAntes.find()) {
                return mAntes.group(1);
            }
            Pattern dataAntesRotulo = Pattern.compile("(?i)(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})[^A-Za-z0-9]{0,16}\\b" + r + "\\b");
            Matcher mDepois = dataAntesRotulo.matcher(base);
            if (mDepois.find()) {
                return mDepois.group(1);
            }
        }
        return "";
    }

    /**
     * Extrai vencimento apenas quando houver rotulo confiavel para evitar confusao com data de emissao.
     */
    private String extrairVencimento(String texto, String tipoDocumento) {
        String vencimentoRotulado = extrairDataPorRotulo(texto, "vencimento", "vencto", "vcto");
        if (!vencimentoRotulado.isBlank()) {
            return vencimentoRotulado;
        }
        if (!TipoDocumentoCatalogo.ehGuiaReceitaFederal(tipoDocumento)) {
            return "";
        }
        String base = texto == null ? "" : texto;
        Matcher matcher = DATA_PATTERN.matcher(base);
        while (matcher.find()) {
            String data = matcher.group(1);
            int ini = Math.max(0, matcher.start() - 48);
            int fim = Math.min(base.length(), matcher.end() + 16);
            String contexto = base.substring(ini, fim).toLowerCase(Locale.ROOT);
            boolean rotuloVencimento = contexto.contains("vencimento") || contexto.contains("vcto");
            boolean rotuloEmissao = contexto.contains("emissao") || contexto.contains("emissão");
            if (rotuloVencimento && !rotuloEmissao) {
                return data;
            }
        }
        // Sem rotulo de vencimento confiavel: melhor nao validar vencimento do que rejeitar por data de emissao.
        return "";
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
        if (base.contains("IRRF") || base.contains("IMPOSTO SOBRE A RENDA RETIDO NA FONTE")) {
            return "IRRF";
        }
        if (base.contains("IPI") || base.contains("IMPOSTO SOBRE PRODUTOS INDUSTRIALIZADOS")) {
            return "IPI";
        }
        if (base.contains(" IOF") || base.contains("\nIOF") || base.contains("IMPOSTO SOBRE OPERAÇÕES FINANCEIRAS")
                || base.contains("IMPOSTO SOBRE OPERACOES FINANCEIRAS")) {
            return "IOF";
        }
        if (Pattern.compile("\\bCIDE\\b").matcher(base).find()) {
            return "CIDE";
        }
        if (base.contains("IMPOSTO DE IMPORTA") || base.contains("IMPOSTO DE IMPORTACAO")) {
            return "II";
        }
        if (base.contains("ITR") || base.contains("IMPOSTO TERRITORIAL RURAL")) {
            return "ITR";
        }
        if (base.contains("FUNRURAL") || base.contains("FUNREA")) {
            return "FUNRURAL";
        }
        if (base.contains("DARF") || base.contains("GPS") || base.contains("DAS")) {
            return "DARF";
        }
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
        if (TipoDocumentoCatalogo.ehGuiaReceitaFederal(tipoDocumento)) {
            String vencimentoExtraido = campos.getOrDefault("vencimento", "");
            if (!vencimentoExtraido.isBlank()) {
                String vencimentoPendencia = pendencia.getVencimento().toString();
                if (!vencimentoPendencia.equals(vencimentoExtraido)) {
                    motivosRevisao.add("Vencimento do documento difere da pendencia (" + vencimentoExtraido + " x " + vencimentoPendencia + ").");
                }
            }
        }

        if (TipoDocumentoCatalogo.ehGuiaReceitaFederal(tipoDocumento) && campos.getOrDefault("tributo", "").isBlank()) {
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
        String valorServicos = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)valor\\s+dos\\s+servi[cç]os.{0,220}?" + PAT_VAL_BR.pattern()));
        String ded = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)deduc.{0,220}?" + PAT_VAL_BR.pattern()));
        String base = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)base\\s+de\\s+c[aá\u00E1]lculo.{0,220}?" + PAT_VAL_BR.pattern()));
        String vIss = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)valor\\s+(?:do\\s+)?iss.{0,220}?" + PAT_VAL_BR.pattern()));
        String pis = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)\\bpis\\b.{0,120}?" + PAT_VAL_BR.pattern()));
        String cofins = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)\\bcofins\\b.{0,120}?" + PAT_VAL_BR.pattern()));
        String inss = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)\\binss\\b.{0,120}?" + PAT_VAL_BR.pattern()));
        String irrf = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)\\birrf\\b.{0,120}?" + PAT_VAL_BR.pattern()));
        String csll = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)\\bcsll\\b.{0,120}?" + PAT_VAL_BR.pattern()));
        String valorLiquido = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)valor\\s+l[ií]quido.{0,220}?" + PAT_VAL_BR.pattern()));
        String iptu = nfseMontanteAposRotulo(t, Pattern.compile(
                "(?is)(?:cr[eé\u00E9]dito\\s+p/|abatimento\\s+iptu).{0,260}?" + PAT_VAL_BR.pattern()));
        String aliq = "";
        Matcher ma = Pattern.compile("(?is)al[ií\u00ED]quota.{0,260}?(\\d{1,2},\\d{2})\\s*%").matcher(t);
        if (ma.find()) {
            aliq = normalizarValorMonetarioBr(ma.group(1));
        }
        campos.put("valorServicos", safe(valorServicos));
        campos.put("deducoesNfse", safe(ded));
        campos.put("baseCalculoIss", safe(base));
        campos.put("aliquotaIssPercentual", safe(aliq));
        campos.put("valorIss", safe(vIss));
        campos.put("pis", safe(pis));
        campos.put("cofins", safe(cofins));
        campos.put("inss", safe(inss));
        campos.put("irrf", safe(irrf));
        campos.put("csll", safe(csll));
        campos.put("valorLiquido", safe(valorLiquido));
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
        tipo = TipoDocumentoCatalogo.refinarGuiaReceitaFederal(texto, tipo);
        String cnpjPrestador = extrairCnpjPrestador(texto);
        String cnpjTomador = extrairCnpjTomador(texto);
        String cnpj = !cnpjPrestador.isBlank() ? cnpjPrestador : extrairCnpj(texto);
        if (TipoDocumentoCatalogo.ehGuiaReceitaFederal(tipo)) {
            cnpj = extrairCnpjPrincipalGuiaImposto(texto, cnpjTomador, cnpjPrestador);
        }
        String valor = extrairValor(texto, tipo);
        String competencia = extrairCompetencia(texto, processamento);
        String vencimento = extrairVencimento(texto, tipo);
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
        String capturaPerfil = null;
        if ("NOTA_FISCAL".equals(tipo) || "NFCE".equals(tipo)) {
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
                capturaPerfil = "HOLERITE_ESCRITORIO_COMPLETO";
            }
        }
        if ("GUIA_IRPF".equals(tipo) || "GUIA_IMPOSTO".equals(tipo)) {
            Map<String, Object> detGuia = extrairDetalhamentoGuiaImpostoIrpf(texto, campos);
            if (detGuia != null && !detGuia.isEmpty()) {
                detalhamentoDocumento = detGuia;
                capturaPerfil = "GUIA_IMPOSTO_IRPF_COMPLETO";
                if (!guiaIrpfTopicosCompletos(campos)) {
                    motivosRevisao.add(motivoGuiaIrpfIncompleta(campos));
                }
            }
        }
        aplicarValidacoesContabeis(processamento, tipo, campos, motivosRevisao);
        if (!motivosRevisao.isEmpty()) {
            status = ProcessamentoStatus.REJEITADO;
            confianca = Math.min(confianca, 0.67);
            observacao = motivosRevisao.get(0);
        }

        if (status == ProcessamentoStatus.REJEITADO && motivosRevisao.isEmpty()
                && ("NOTA_FISCAL".equals(tipo) || "NFCE".equals(tipo))) {
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
                detalhamentoDocumento,
                capturaPerfil
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
                || tipoDetectado.contains("NFSE")
                || tipoDetectado.contains("NFCE")
                || tipoDetectado.contains("CTE")
                || tipoDetectado.contains("MDFE")
                || tipoDetectado.contains("CTE_OS");

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
