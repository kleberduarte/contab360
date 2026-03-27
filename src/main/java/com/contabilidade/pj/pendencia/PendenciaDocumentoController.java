package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    public PendenciaDocumentoController(PendenciaDocumentoService pendenciaDocumentoService) {
        this.pendenciaDocumentoService = pendenciaDocumentoService;
    }

    @GetMapping
    public List<PendenciaResponse> listar(
            @RequestParam Integer ano,
            @RequestParam Integer mes,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) PendenciaStatus status
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return pendenciaDocumentoService.listar(ano, mes, empresaId, status, usuario).stream()
                .map(PendenciaResponse::fromEntity)
                .toList();
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
        return PendenciaResponse.fromEntity(atualizada);
    }

    public record GerarPendenciasRequest(
            @NotNull @Min(2000) @Max(2100) Integer ano,
            @NotNull @Min(1) @Max(12) Integer mes,
            @NotNull @Min(1) @Max(31) Integer diaVencimento
    ) {
    }

    public record AtualizarStatusRequest(@NotNull PendenciaStatus status) {
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
            LocalDate vencimento
    ) {
        public static PendenciaResponse fromEntity(PendenciaDocumento pendencia) {
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
                    pendencia.getVencimento()
            );
        }
    }
}
