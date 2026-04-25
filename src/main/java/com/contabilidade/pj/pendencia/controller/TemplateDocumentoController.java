package com.contabilidade.pj.pendencia.controller;

import com.contabilidade.pj.auth.service.AuthContext;
import com.contabilidade.pj.auth.entity.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.contabilidade.pj.pendencia.service.*;
import com.contabilidade.pj.pendencia.entity.*;

@Validated
@RestController
@RequestMapping("/api/templates-documentos")
public class TemplateDocumentoController {

    private final TemplateDocumentoService templateDocumentoService;

    public TemplateDocumentoController(TemplateDocumentoService templateDocumentoService) {
        this.templateDocumentoService = templateDocumentoService;
    }

    @GetMapping
    public List<TemplateDocumentoResponse> listar(@RequestParam Long empresaId) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return templateDocumentoService.listarPorEmpresa(empresaId, usuario).stream()
                .map(TemplateDocumentoResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TemplateDocumentoResponse> criar(@Valid @RequestBody CriarTemplateDocumentoRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        TemplateDocumento salvo = templateDocumentoService.criar(req.empresaId(), req.nome(), req.obrigatorio(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateDocumentoResponse.fromEntity(salvo));
    }

    public record CriarTemplateDocumentoRequest(
            @NotNull Long empresaId,
            @NotBlank String nome,
            boolean obrigatorio
    ) {
    }

    public record TemplateDocumentoResponse(
            Long id,
            Long empresaId,
            String nome,
            boolean obrigatorio
    ) {
        public static TemplateDocumentoResponse fromEntity(TemplateDocumento template) {
            return new TemplateDocumentoResponse(
                    template.getId(),
                    template.getEmpresa().getId(),
                    template.getNome(),
                    template.isObrigatorio()
            );
        }
    }
}
