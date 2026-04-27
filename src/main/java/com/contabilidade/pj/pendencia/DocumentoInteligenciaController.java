package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Cliente: dados da empresa vinculada ao usuário. Contador: informar {@code empresaId} (CNPJ cadastrado).
     */
    @GetMapping("/portal/validados-por-aba")
    public DocumentosValidadosAgrupadosResponse listarValidadosPorAba(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long clientePessoaFisicaId,
            @RequestParam(required = false) String incluirCompetenciasArquivadas
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        boolean incluir = incluirCompetenciasArquivadas != null
                && (incluirCompetenciasArquivadas.equals("1")
                || "true".equalsIgnoreCase(incluirCompetenciasArquivadas));
        return documentoInteligenciaService.listarDocumentosValidadosPorAba(
                usuario, empresaId, clientePessoaFisicaId, incluir);
    }

    @GetMapping
    public List<DocumentoProcessamentoResponse> listar(
            @RequestParam(defaultValue = "false") boolean somenteRevisar,
            @RequestParam(defaultValue = "false") boolean incluirConcluidosNaRevisao
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return documentoInteligenciaService.listar(usuario, somenteRevisar, incluirConcluidosNaRevisao).stream()
                .map(DocumentoProcessamentoResponse::fromEntity)
                .toList();
    }

    @PatchMapping("/{processamentoId}/campos")
    public DadosExtraidosPendenciaDto atualizarCamposExtraidos(
            @PathVariable Long processamentoId,
            @RequestBody AtualizarCamposBody body
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        List<Map<String, String>> campos = body == null ? List.of() : body.campos();
        return documentoInteligenciaService.atualizarCamposExtraidos(processamentoId, campos, usuario);
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

    public record AtualizarCamposBody(List<Map<String, String>> campos) {
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
