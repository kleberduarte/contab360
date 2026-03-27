package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pendencias/{pendenciaId}/entregas")
public class EntregaDocumentoController {

    private final EntregaDocumentoService entregaDocumentoService;

    public EntregaDocumentoController(EntregaDocumentoService entregaDocumentoService) {
        this.entregaDocumentoService = entregaDocumentoService;
    }

    @GetMapping
    public List<EntregaDocumentoResponse> listar(@PathVariable Long pendenciaId) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return entregaDocumentoService.listarPorPendencia(pendenciaId, usuario).stream()
                .map(EntregaDocumentoResponse::fromEntity)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntregaDocumentoResponse> anexar(
            @PathVariable Long pendenciaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "observacao", required = false) String observacao
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        EntregaDocumento entrega = entregaDocumentoService.anexar(pendenciaId, arquivo, observacao, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntregaDocumentoResponse.fromEntity(entrega));
    }

    public record EntregaDocumentoResponse(
            Long id,
            Long pendenciaId,
            String nomeArquivoOriginal,
            LocalDateTime enviadoEm,
            String observacao
    ) {
        public static EntregaDocumentoResponse fromEntity(EntregaDocumento entrega) {
            return new EntregaDocumentoResponse(
                    entrega.getId(),
                    entrega.getPendencia().getId(),
                    entrega.getNomeArquivoOriginal(),
                    entrega.getEnviadoEm(),
                    entrega.getObservacao()
            );
        }
    }
}
