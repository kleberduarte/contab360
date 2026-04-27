package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EntregaDocumentoService {

    private static final Logger log = LoggerFactory.getLogger(EntregaDocumentoService.class);
    private static final java.util.Set<String> EXTENSOES_PERMITIDAS = java.util.Set.of("pdf", "xml", "png", "jpg", "jpeg");

    private final EntregaDocumentoRepository entregaDocumentoRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final DocumentoInteligenciaService documentoInteligenciaService;
    private final CompetenciaArquivamentoService competenciaArquivamentoService;
    private final Path uploadBasePath;

    public EntregaDocumentoService(
            EntregaDocumentoRepository entregaDocumentoRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            DocumentoInteligenciaService documentoInteligenciaService,
            CompetenciaArquivamentoService competenciaArquivamentoService,
            @Value("${app.upload.dir:uploads/pendencias}") String uploadDir
    ) {
        this.entregaDocumentoRepository = entregaDocumentoRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.documentoInteligenciaService = documentoInteligenciaService;
        this.competenciaArquivamentoService = competenciaArquivamentoService;
        this.uploadBasePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public EntregaDocumento anexar(Long pendenciaId, MultipartFile arquivo, String observacao, Usuario usuarioAtual) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo obrigatório.");
        }

        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));

        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE
                && !PendenciaClienteDono.clienteEhDonoDaPendencia(usuarioAtual, pendencia)) {
            throw new IllegalArgumentException("Cliente sem acesso a esta pendência.");
        }

        String nomeOriginal = arquivo.getOriginalFilename() == null ? "arquivo.bin" : arquivo.getOriginalFilename();
        String extensao = "";
        int idx = nomeOriginal.lastIndexOf('.');
        if (idx >= 0 && idx < nomeOriginal.length() - 1) {
            extensao = nomeOriginal.substring(idx + 1).toLowerCase();
        }
        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Envie PDF, XML, PNG ou JPG.");
        }
        String nomeSalvo = "pendencia-" + pendenciaId + "-" + UUID.randomUUID() + "." + extensao;

        try {
            Files.createDirectories(uploadBasePath);
            Path destino = uploadBasePath.resolve(nomeSalvo);
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            EntregaDocumento entrega = new EntregaDocumento();
            entrega.setPendencia(pendencia);
            entrega.setNomeArquivoOriginal(nomeOriginal);
            entrega.setCaminhoArquivo(destino.toString());
            entrega.setObservacao(observacao);
            entrega.setEnviadoEm(LocalDateTime.now());

            pendencia.setStatus(PendenciaStatus.ENVIADO);
            pendenciaDocumentoRepository.save(pendencia);
            competenciaArquivamentoService.sincronizarArquivamentoCompetencia(pendencia.getCompetencia().getId());
            EntregaDocumento salva = entregaDocumentoRepository.save(entrega);
            /*
             * Processamento da IA na mesma transação: qualquer exceção não tratada fazia rollback de entrega + status.
             * O envio do cliente deve permanecer gravado; falhas na leitura automática ficam só no log (a fila pode ser reprocessada).
             */
            try {
                documentoInteligenciaService.iniciarProcessamento(salva);
            } catch (Exception ex) {
                log.warn(
                        "Entrega id={} pendenciaId={} salva no disco, mas falhou ao iniciar processamento pela IA: {}",
                        salva.getId(),
                        pendenciaId,
                        ex.getMessage(),
                        ex
                );
            }
            return salva;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Falha ao salvar arquivo.");
        }
    }

    @Transactional(readOnly = true)
    public List<EntregaDocumento> listarPorPendencia(Long pendenciaId, Usuario usuarioAtual) {
        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE
                && !PendenciaClienteDono.clienteEhDonoDaPendencia(usuarioAtual, pendencia)) {
            throw new IllegalArgumentException("Cliente sem acesso a esta pendência.");
        }
        return entregaDocumentoRepository.findByPendenciaIdOrderByEnviadoEmDesc(pendenciaId);
    }
}
