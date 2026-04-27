package com.contabilidade.pj.pendencia.controller;

import com.contabilidade.pj.auth.service.AuthContext;
import com.contabilidade.pj.auth.entity.Usuario;
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
import com.contabilidade.pj.pendencia.service.*;
import com.contabilidade.pj.pendencia.entity.*;

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

    @PostMapping(value = "/lote", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntregaLoteHttpResponse> anexarLote(
            @PathVariable Long pendenciaId,
            @RequestParam("arquivos") List<MultipartFile> arquivos,
            @RequestParam(value = "observacao", required = false) String observacao
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        EntregaLoteResultado resultado = entregaDocumentoService.anexarLote(pendenciaId, arquivos, observacao, usuario);
        EntregaLoteHttpResponse body = EntregaLoteHttpResponse.from(resultado);
        if (body.entregas().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        if (body.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    public record EntregaLoteHttpResponse(
            List<EntregaDocumentoResponse> entregas,
            List<LoteArquivoErroResponse> erros,
            String message
    ) {
        static EntregaLoteHttpResponse from(EntregaLoteResultado r) {
            List<EntregaDocumentoResponse> ents =
                    r.sucesso().stream().map(EntregaDocumentoResponse::fromEntity).toList();
            List<LoteArquivoErroResponse> errs = r.falhas().stream()
                    .map(f -> new LoteArquivoErroResponse(f.nomeArquivoOriginal(), f.mensagem()))
                    .toList();
            String msg = null;
            if (ents.isEmpty() && !errs.isEmpty()) {
                msg = "Nenhum arquivo foi aceito.";
            } else if (!ents.isEmpty() && !errs.isEmpty()) {
                msg = errs.size() + " arquivo(s) com falha; " + ents.size() + " enviado(s) com sucesso.";
            }
            return new EntregaLoteHttpResponse(ents, errs, msg);
        }
    }

    public record LoteArquivoErroResponse(String nomeArquivoOriginal, String mensagem) {}

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
