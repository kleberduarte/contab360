package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inteligencia/documentos")
public class DocumentoInteligenciaController {

    private final DocumentoInteligenciaService documentoInteligenciaService;

    public DocumentoInteligenciaController(DocumentoInteligenciaService documentoInteligenciaService) {
        this.documentoInteligenciaService = documentoInteligenciaService;
    }

    @GetMapping
    public List<DocumentoProcessamentoResponse> listar(
            @RequestParam(defaultValue = "false") boolean somenteRevisar
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return documentoInteligenciaService.listar(usuario, somenteRevisar).stream()
                .map(DocumentoProcessamentoResponse::fromEntity)
                .toList();
    }

    @PostMapping("/{processamentoId}/aprovar")
    public DocumentoProcessamentoResponse aprovarRevisao(@PathVariable Long processamentoId) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        DocumentoProcessamento atualizado = documentoInteligenciaService.marcarComoProcessado(processamentoId, usuario);
        return DocumentoProcessamentoResponse.fromEntity(atualizado);
    }

    @PostMapping("/{processamentoId}/rejeitar")
    public DocumentoProcessamentoResponse rejeitarRevisao(
            @PathVariable Long processamentoId,
            @Valid @org.springframework.web.bind.annotation.RequestBody RejeitarRevisaoRequest req
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        DocumentoProcessamento atualizado = documentoInteligenciaService
                .marcarComoRejeitado(processamentoId, req.motivo(), usuario);
        return DocumentoProcessamentoResponse.fromEntity(atualizado);
    }

    @GetMapping("/{processamentoId}/historico")
    public List<HistoricoRevisaoResponse> historico(@PathVariable Long processamentoId) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return documentoInteligenciaService.listarHistorico(processamentoId, usuario).stream()
                .map(HistoricoRevisaoResponse::fromEntity)
                .toList();
    }

    public record RejeitarRevisaoRequest(@NotBlank String motivo) {
    }

    public record HistoricoRevisaoResponse(
            Long id,
            String acao,
            String motivo,
            String usuarioNome,
            LocalDateTime criadoEm
    ) {
        static HistoricoRevisaoResponse fromEntity(RevisaoDocumentoHistorico h) {
            return new HistoricoRevisaoResponse(h.getId(), h.getAcao(), h.getMotivo(), h.getUsuarioNome(), h.getCriadoEm());
        }
    }

    public record DocumentoProcessamentoResponse(
            Long id,
            Long entregaId,
            Long pendenciaId,
            String nomeArquivoOriginal,
            String status,
            String severidade,
            String tipoDocumento,
            Double confianca,
            String dadosExtraidosJson,
            String observacaoProcessamento,
            LocalDateTime atualizadoEm
    ) {
        static DocumentoProcessamentoResponse fromEntity(DocumentoProcessamento item) {
            return new DocumentoProcessamentoResponse(
                    item.getId(),
                    item.getEntrega().getId(),
                    item.getEntrega().getPendencia().getId(),
                    item.getEntrega().getNomeArquivoOriginal(),
                    item.getStatus().name(),
                    item.getSeveridade().name(),
                    item.getTipoDocumento(),
                    item.getConfianca(),
                    item.getDadosExtraidosJson(),
                    item.getObservacaoProcessamento(),
                    item.getAtualizadoEm()
            );
        }
    }
}
