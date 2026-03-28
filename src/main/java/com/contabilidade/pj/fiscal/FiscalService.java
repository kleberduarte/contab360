package com.contabilidade.pj.fiscal;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import com.contabilidade.pj.fiscal.sefaz.AutorizacaoFiscalPort;
import com.contabilidade.pj.fiscal.sefaz.DanfeSimulacaoPdfService;
import com.contabilidade.pj.fiscal.sefaz.NfseEmissaoPort;
import com.contabilidade.pj.fiscal.sefaz.ResultadoEmissaoSefaz;
import java.time.LocalDate;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalService {
    private final NotaFiscalRepository notaFiscalRepository;
    private final CadastroDocumentoFiscalRepository cadastroDocumentoFiscalRepository;
    private final CobrancaRepository cobrancaRepository;
    private final CertificadoDigitalPedidoRepository certificadoDigitalPedidoRepository;
    private final AlertaFiscalRepository alertaFiscalRepository;
    private final EmpresaRepository empresaRepository;
    private final AutorizacaoFiscalPort autorizacaoFiscalPort;
    private final NfseEmissaoPort nfseEmissaoPort;
    private final DanfeSimulacaoPdfService danfeSimulacaoPdfService;

    public FiscalService(
            NotaFiscalRepository notaFiscalRepository,
            CadastroDocumentoFiscalRepository cadastroDocumentoFiscalRepository,
            CobrancaRepository cobrancaRepository,
            CertificadoDigitalPedidoRepository certificadoDigitalPedidoRepository,
            AlertaFiscalRepository alertaFiscalRepository,
            EmpresaRepository empresaRepository,
            AutorizacaoFiscalPort autorizacaoFiscalPort,
            NfseEmissaoPort nfseEmissaoPort,
            DanfeSimulacaoPdfService danfeSimulacaoPdfService
    ) {
        this.notaFiscalRepository = notaFiscalRepository;
        this.cadastroDocumentoFiscalRepository = cadastroDocumentoFiscalRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.certificadoDigitalPedidoRepository = certificadoDigitalPedidoRepository;
        this.alertaFiscalRepository = alertaFiscalRepository;
        this.empresaRepository = empresaRepository;
        this.autorizacaoFiscalPort = autorizacaoFiscalPort;
        this.nfseEmissaoPort = nfseEmissaoPort;
        this.danfeSimulacaoPdfService = danfeSimulacaoPdfService;
    }

    @Transactional
    public NotaFiscal emitirNota(NotaFiscal notaFiscal, Usuario usuarioAtual) {
        validarAcessoContador(usuarioAtual);
        NotaFiscal salva = notaFiscalRepository.save(notaFiscal);
        ResultadoEmissaoSefaz r = salva.getTipoDocumento() == TipoDocumentoFiscal.NFSE
                ? nfseEmissaoPort.emitir(salva)
                : autorizacaoFiscalPort.solicitarAutorizacao(salva);
        salva.setChaveAcesso(r.chaveAcesso() != null && !r.chaveAcesso().isBlank() ? r.chaveAcesso() : null);
        salva.setProtocoloAutorizacao(r.protocoloAutorizacao() != null && !r.protocoloAutorizacao().isBlank() ? r.protocoloAutorizacao() : null);
        salva.setSefazModo(r.modo());
        salva.setSefazMensagem(r.mensagem());
        if (r.nfseNumeroExibicao() != null && !r.nfseNumeroExibicao().isBlank()) {
            salva.setNfseNumeroExibicao(r.nfseNumeroExibicao().trim());
        }
        if (r.nfseCodigoVerificacao() != null && !r.nfseCodigoVerificacao().isBlank()) {
            salva.setNfseCodigoVerificacao(r.nfseCodigoVerificacao().trim());
        }
        return notaFiscalRepository.save(salva);
    }

    @Transactional(readOnly = true)
    public byte[] gerarDanfeSimulacaoPdf(Long notaId, Usuario usuarioAtual) throws IOException {
        validarAcessoContador(usuarioAtual);
        NotaFiscal nota = notaFiscalRepository.findById(notaId)
                .orElseThrow(() -> new IllegalArgumentException("Nota fiscal nao encontrada."));
        return danfeSimulacaoPdfService.gerar(nota);
    }

    @Transactional(readOnly = true)
    public List<NotaFiscal> listarNotas(TipoOperacaoFiscal tipoOperacao, TipoDocumentoFiscal tipoDocumento, Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        if (tipoOperacao != null) {
            return notaFiscalRepository.findByTipoOperacaoOrderByDataEmissaoDesc(tipoOperacao);
        }
        if (tipoDocumento != null) {
            return notaFiscalRepository.findByTipoDocumentoOrderByDataEmissaoDesc(tipoDocumento);
        }
        return notaFiscalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RadarNotasResponse radarNotas(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        List<NotaFiscal> notas = notaFiscalRepository.findAll();
        Map<TipoOperacaoFiscal, Long> porOperacao = new EnumMap<>(TipoOperacaoFiscal.class);
        Map<TipoDocumentoFiscal, Long> porTipoDocumento = new EnumMap<>(TipoDocumentoFiscal.class);

        for (TipoOperacaoFiscal tipo : TipoOperacaoFiscal.values()) {
            porOperacao.put(tipo, 0L);
        }
        for (TipoDocumentoFiscal tipo : TipoDocumentoFiscal.values()) {
            porTipoDocumento.put(tipo, 0L);
        }
        for (NotaFiscal nota : notas) {
            porOperacao.computeIfPresent(nota.getTipoOperacao(), (k, v) -> v + 1);
            porTipoDocumento.computeIfPresent(nota.getTipoDocumento(), (k, v) -> v + 1);
        }
        return new RadarNotasResponse(notas.size(), porOperacao, porTipoDocumento);
    }

    @Transactional
    public CadastroDocumentoFiscal cadastrarDocumento(CadastroDocumentoFiscal cadastro, Usuario usuarioAtual) {
        validarAcessoContador(usuarioAtual);
        return cadastroDocumentoFiscalRepository.save(cadastro);
    }

    @Transactional(readOnly = true)
    public List<CadastroDocumentoFiscal> listarCadastros(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        return cadastroDocumentoFiscalRepository.findAll();
    }

    @Transactional
    public Cobranca criarCobranca(Cobranca cobranca, Usuario usuarioAtual) {
        validarAcessoContador(usuarioAtual);
        return cobrancaRepository.save(cobranca);
    }

    @Transactional(readOnly = true)
    public List<Cobranca> listarCobrancas(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        return cobrancaRepository.findAll();
    }

    @Transactional
    public CertificadoDigitalPedido venderCertificado(CertificadoDigitalPedido pedido, Usuario usuarioAtual) {
        validarAcessoContador(usuarioAtual);
        return certificadoDigitalPedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<CertificadoDigitalPedido> listarCertificados(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        return certificadoDigitalPedidoRepository.findAll();
    }

    @Transactional
    public int gerarAlertasAutomaticos(Usuario usuarioAtual) {
        validarAcessoContador(usuarioAtual);
        List<Empresa> empresas = empresaRepository.findAll();
        int alertasCriados = 0;
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(30);

        for (Empresa empresa : empresas) {
            if (!empresa.isMei()) {
                continue;
            }
            if (empresa.getVencimentoDas() != null && !empresa.getVencimentoDas().isBefore(hoje)
                    && !empresa.getVencimentoDas().isAfter(limite)) {
                AlertaFiscal alerta = new AlertaFiscal();
                alerta.setTipo(TipoAlertaFiscal.DAS);
                alerta.setDocumentoReferencia(empresa.getCnpj());
                alerta.setDataAlvo(empresa.getVencimentoDas());
                alerta.setMensagem("Pagamento do DAS proximo do vencimento.");
                alertaFiscalRepository.save(alerta);
                alertasCriados++;
            }
            if (empresa.getVencimentoCertificadoMei() != null && !empresa.getVencimentoCertificadoMei().isBefore(hoje)
                    && !empresa.getVencimentoCertificadoMei().isAfter(limite)) {
                AlertaFiscal alerta = new AlertaFiscal();
                alerta.setTipo(TipoAlertaFiscal.CERTIFICADO_MEI);
                alerta.setDocumentoReferencia(empresa.getCnpj());
                alerta.setDataAlvo(empresa.getVencimentoCertificadoMei());
                alerta.setMensagem("Certificado digital MEI proximo do vencimento.");
                alertaFiscalRepository.save(alerta);
                alertasCriados++;
            }
        }
        return alertasCriados;
    }

    @Transactional(readOnly = true)
    public List<AlertaFiscal> listarAlertas(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        return alertaFiscalRepository.findByResolvidoFalseOrderByDataAlvoAsc();
    }

    @Transactional(readOnly = true)
    public RelatorioEstrategicoResponse relatorioEstrategico(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        long totalNotas = notaFiscalRepository.count();
        long totalCadastros = cadastroDocumentoFiscalRepository.count();
        long totalCobrancas = cobrancaRepository.count();
        long totalCertificados = certificadoDigitalPedidoRepository.count();
        long totalAlertasAbertos = alertaFiscalRepository.findByResolvidoFalseOrderByDataAlvoAsc().size();

        return new RelatorioEstrategicoResponse(
                totalNotas,
                totalCadastros,
                totalCobrancas,
                totalCertificados,
                totalAlertasAbertos,
                1900
        );
    }

    private void validarAutenticado(Usuario usuarioAtual) {
        if (usuarioAtual == null) {
            throw new IllegalArgumentException("Usuario nao autenticado.");
        }
    }

    private void validarAcessoContador(Usuario usuarioAtual) {
        validarAutenticado(usuarioAtual);
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Perfil sem permissao para operacao fiscal.");
        }
    }

    public record RadarNotasResponse(
            int totalNotas,
            Map<TipoOperacaoFiscal, Long> porOperacao,
            Map<TipoDocumentoFiscal, Long> porTipoDocumento
    ) {
    }

    public record RelatorioEstrategicoResponse(
            long totalNotasEmitidas,
            long totalCadastrosCpfCnpj,
            long totalCobrancasGeradas,
            long totalCertificadosVendidos,
            long totalAlertasAbertos,
            int totalPrefeiturasCompativeis
    ) {
    }
}
