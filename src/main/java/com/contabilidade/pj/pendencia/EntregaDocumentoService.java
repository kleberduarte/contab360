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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EntregaDocumentoService {

    private final EntregaDocumentoRepository entregaDocumentoRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final DocumentoInteligenciaService documentoInteligenciaService;
    private final Path uploadBasePath;

    public EntregaDocumentoService(
            EntregaDocumentoRepository entregaDocumentoRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            DocumentoInteligenciaService documentoInteligenciaService,
            @Value("${app.upload.dir:uploads/pendencias}") String uploadDir
    ) {
        this.entregaDocumentoRepository = entregaDocumentoRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.documentoInteligenciaService = documentoInteligenciaService;
        this.uploadBasePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public EntregaDocumento anexar(Long pendenciaId, MultipartFile arquivo, String observacao, Usuario usuarioAtual) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo obrigatório.");
        }

        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));

        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() == null || !usuarioAtual.getEmpresa().getId().equals(pendencia.getEmpresa().getId())) {
                throw new IllegalArgumentException("Cliente sem acesso a esta pendência.");
            }
        }

        String nomeOriginal = arquivo.getOriginalFilename() == null ? "arquivo.bin" : arquivo.getOriginalFilename();
        String extensao = "";
        int idx = nomeOriginal.lastIndexOf('.');
        if (idx >= 0 && idx < nomeOriginal.length() - 1) {
            extensao = "." + nomeOriginal.substring(idx + 1);
        }
        String nomeSalvo = "pendencia-" + pendenciaId + "-" + UUID.randomUUID() + extensao;

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
            EntregaDocumento salva = entregaDocumentoRepository.save(entrega);
            documentoInteligenciaService.iniciarProcessamento(salva);
            return salva;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Falha ao salvar arquivo.");
        }
    }

    @Transactional(readOnly = true)
    public List<EntregaDocumento> listarPorPendencia(Long pendenciaId, Usuario usuarioAtual) {
        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() == null || !usuarioAtual.getEmpresa().getId().equals(pendencia.getEmpresa().getId())) {
                throw new IllegalArgumentException("Cliente sem acesso a esta pendência.");
            }
        }
        return entregaDocumentoRepository.findByPendenciaIdOrderByEnviadoEmDesc(pendenciaId);
    }
}
