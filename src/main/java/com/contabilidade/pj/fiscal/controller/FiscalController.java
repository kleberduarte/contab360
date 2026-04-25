package com.contabilidade.pj.fiscal.controller;

import com.contabilidade.pj.auth.service.AuthContext;
import com.contabilidade.pj.auth.entity.Usuario;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.contabilidade.pj.fiscal.entity.*;
import com.contabilidade.pj.fiscal.service.*;
import com.contabilidade.pj.fiscal.dto.*;

@RestController
@RequestMapping("/api/fiscal")
public class FiscalController {
    private final FiscalService fiscalService;

    public FiscalController(FiscalService fiscalService) {
        this.fiscalService = fiscalService;
    }

    @PostMapping("/notas")
    public ResponseEntity<NotaFiscal> emitirNota(@Valid @RequestBody NotaFiscal notaFiscal) {
        Usuario usuario = AuthContext.get();
        NotaFiscal salva = fiscalService.emitirNota(notaFiscal, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @GetMapping("/notas")
    public List<NotaFiscal> listarNotas(
            @RequestParam(required = false) TipoOperacaoFiscal tipoOperacao,
            @RequestParam(required = false) TipoDocumentoFiscal tipoDocumento
    ) {
        return fiscalService.listarNotas(tipoOperacao, tipoDocumento, AuthContext.get());
    }

    /**
     * PDF estilo DANFE apenas para simulação (marca d'água). Não substitui documento fiscal válido.
     */
    @GetMapping("/notas/{id}/danfe-simulacao.pdf")
    public ResponseEntity<byte[]> danfeSimulacaoPdf(@PathVariable Long id) throws IOException {
        byte[] pdf = fiscalService.gerarDanfeSimulacaoPdf(id, AuthContext.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"danfe-simulacao-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/radar")
    public FiscalService.RadarNotasResponse radar() {
        return fiscalService.radarNotas(AuthContext.get());
    }

    @PostMapping("/cadastros")
    public ResponseEntity<CadastroDocumentoFiscal> cadastrarDocumento(@Valid @RequestBody CadastroDocumentoFiscal cadastro) {
        CadastroDocumentoFiscal salvo = fiscalService.cadastrarDocumento(cadastro, AuthContext.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping("/cadastros")
    public List<CadastroDocumentoFiscal> listarCadastros() {
        return fiscalService.listarCadastros(AuthContext.get());
    }

    @GetMapping("/prefeituras/compatibilidade")
    public Map<String, Object> compatibilidadePrefeituras() {
        return Map.of(
                "totalPrefeiturasCompativeis", 1900,
                "mensagem", "Cobertura nacional com integracao padronizada para NFSe."
        );
    }

    @PostMapping("/certificados")
    public ResponseEntity<CertificadoDigitalPedido> venderCertificado(@Valid @RequestBody CertificadoDigitalCreateRequest pedido) {
        CertificadoDigitalPedido salvo = fiscalService.venderCertificado(pedido, AuthContext.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping("/certificados")
    public List<CertificadoDigitalPedido> listarCertificados() {
        return fiscalService.listarCertificados(AuthContext.get());
    }

    @PatchMapping("/certificados/{id}")
    public CertificadoDigitalPedido atualizarCertificado(
            @PathVariable Long id,
            @RequestBody CertificadoDigitalUpdateRequest body
    ) {
        return fiscalService.atualizarCertificado(id, body, AuthContext.get());
    }

    @PostMapping("/cobrancas")
    public ResponseEntity<Cobranca> criarCobranca(@Valid @RequestBody Cobranca cobranca) {
        Cobranca salva = fiscalService.criarCobranca(cobranca, AuthContext.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @GetMapping("/cobrancas")
    public List<Cobranca> listarCobrancas() {
        return fiscalService.listarCobrancas(AuthContext.get());
    }

    @PostMapping("/alertas/automaticos")
    public Map<String, Integer> gerarAlertasAutomaticos() {
        int criados = fiscalService.gerarAlertasAutomaticos(AuthContext.get());
        return Map.of("alertasCriados", criados);
    }

    @GetMapping("/alertas")
    public List<AlertaFiscal> listarAlertas() {
        return fiscalService.listarAlertas(AuthContext.get());
    }

    @GetMapping("/relatorios/estrategico")
    public FiscalService.RelatorioEstrategicoResponse relatorioEstrategico() {
        return fiscalService.relatorioEstrategico(AuthContext.get());
    }
}
