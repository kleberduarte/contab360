package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@Validated
@RestController
@RequestMapping("/api/pendencias")
public class PendenciaDocumentoController {

    private final PendenciaDocumentoService pendenciaDocumentoService;
    private final DocumentoProcessamentoRepository documentoProcessamentoRepository;
    private final DocumentoInteligenciaService documentoInteligenciaService;

    public PendenciaDocumentoController(
            PendenciaDocumentoService pendenciaDocumentoService,
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            DocumentoInteligenciaService documentoInteligenciaService
    ) {
        this.pendenciaDocumentoService = pendenciaDocumentoService;
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.documentoInteligenciaService = documentoInteligenciaService;
    }

    @GetMapping("/{pendenciaId}/dados-extraidos")
    public DadosExtraidosPendenciaDto dadosExtraidos(@PathVariable Long pendenciaId) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return documentoInteligenciaService.obterDadosExtraidosPorPendencia(pendenciaId, usuario);
    }

    @GetMapping("/{pendenciaId}/dados-extraidos/export")
    public ResponseEntity<byte[]> exportarDadosExtraidos(
            @PathVariable Long pendenciaId,
            @RequestParam(defaultValue = "csv") String formato
    ) throws IOException {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] body = pdf
                ? documentoInteligenciaService.exportarDadosExtraidosPdf(pendenciaId, usuario)
                : documentoInteligenciaService.exportarDadosExtraidosCsv(pendenciaId, usuario);
        String nomeArquivo = "dados-extraidos-" + pendenciaId + (pdf ? ".pdf" : ".csv");
        MediaType tipo = pdf
                ? MediaType.APPLICATION_PDF
                : new MediaType("text", "csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(tipo)
                .body(body);
    }

    @GetMapping
    public List<PendenciaResponse> listar(
            @RequestParam Integer ano,
            @RequestParam Integer mes,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) PendenciaStatus status,
            @RequestParam(defaultValue = "false") boolean incluirArquivadas
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        List<PendenciaDocumento> pendencias = pendenciaDocumentoService.listar(
                ano, mes, empresaId, status, usuario, incluirArquivadas
        );
        Map<Long, String> observacoes = mapearObservacoes(pendencias);
        return pendencias.stream()
                .map(p -> PendenciaResponse.fromEntity(p, observacoes.get(p.getId())))
                .toList();
    }

    /**
     * Situação de arquivo da competência (contador). Cliente recebe sempre "não arquivada" para não expor flag.
     */
    @GetMapping("/competencia-arquivo")
    public CompetenciaArquivoResponse competenciaArquivo(@RequestParam Integer ano, @RequestParam Integer mes) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        PendenciaDocumentoService.CompetenciaArquivoInfo info =
                pendenciaDocumentoService.obterInfoArquivoCompetencia(ano, mes, usuario);
        return new CompetenciaArquivoResponse(info.existeCompetencia(), info.arquivada(), info.arquivadaEm());
    }

    @PostMapping("/gerar")
    public Map<String, Integer> gerar(@Valid @RequestBody GerarPendenciasRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        int criadas = pendenciaDocumentoService.gerarPendencias(req.ano(), req.mes(), req.diaVencimento(), usuario);
        return Map.of("pendenciasCriadas", criadas);
    }

    @PostMapping("/{pendenciaId}/status")
    public PendenciaResponse atualizarStatus(
            @PathVariable Long pendenciaId,
            @Valid @RequestBody AtualizarStatusRequest req
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        PendenciaDocumento atualizada = pendenciaDocumentoService.atualizarStatus(pendenciaId, req.status(), usuario);
        Map<Long, String> observacoes = mapearObservacoes(List.of(atualizada));
        return PendenciaResponse.fromEntity(atualizada, observacoes.get(atualizada.getId()));
    }

    private Map<Long, String> mapearObservacoes(List<PendenciaDocumento> pendencias) {
        if (pendencias.isEmpty()) {
            return Map.of();
        }
        List<Long> pendenciaIds = pendencias.stream().map(PendenciaDocumento::getId).toList();
        return documentoProcessamentoRepository.findUltimasObservacoesByPendenciaIds(pendenciaIds).stream()
                .collect(Collectors.toMap(
                        DocumentoProcessamentoRepository.ObservacaoPendenciaView::getPendenciaId,
                        DocumentoProcessamentoRepository.ObservacaoPendenciaView::getObservacaoProcessamento,
                        (valorAtual, ignorar) -> valorAtual
                ));
    }

    public record GerarPendenciasRequest(
            @NotNull @Min(2000) @Max(2100) Integer ano,
            @NotNull @Min(1) @Max(12) Integer mes,
            @NotNull @Min(1) @Max(31) Integer diaVencimento
    ) {
    }

    public record AtualizarStatusRequest(@NotNull PendenciaStatus status) {
    }

    public record CompetenciaArquivoResponse(boolean existeCompetencia, boolean arquivada, LocalDateTime arquivadaEm) {
    }

    public record PendenciaResponse(
            Long id,
            Long empresaId,
            String empresaRazaoSocial,
            String empresaCnpj,
            Long templateDocumentoId,
            String templateDocumentoNome,
            Integer competenciaAno,
            Integer competenciaMes,
            String status,
            LocalDate vencimento,
            String observacaoAnalise
    ) {
        public static PendenciaResponse fromEntity(PendenciaDocumento pendencia, String observacaoAnalise) {
            return new PendenciaResponse(
                    pendencia.getId(),
                    pendencia.getEmpresa().getId(),
                    pendencia.getEmpresa().getRazaoSocial(),
                    pendencia.getEmpresa().getCnpj(),
                    pendencia.getTemplateDocumento().getId(),
                    pendencia.getTemplateDocumento().getNome(),
                    pendencia.getCompetencia().getAno(),
                    pendencia.getCompetencia().getMes(),
                    pendencia.getStatus().name(),
                    pendencia.getVencimento(),
                    observacaoAnalise
            );
        }
    }
}
