package com.contabilidade.pj.pendencia.service;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.contabilidade.pj.pendencia.PendenciaClienteDono;
import com.contabilidade.pj.pendencia.entity.*;
import com.contabilidade.pj.pendencia.repository.*;

@Service
public class EntregaDocumentoService {

    private static final Logger log = LoggerFactory.getLogger(EntregaDocumentoService.class);
    /** Limite de arquivos por requisição de lote (transação independente por arquivo). */
    private static final int ENTREGA_LOTE_MAX_ARQUIVOS = 25;

    private final EntregaDocumentoRepository entregaDocumentoRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final DocumentoInteligenciaService documentoInteligenciaService;
    private final CompetenciaArquivamentoService competenciaArquivamentoService;
    private final Path uploadBasePath;
    private final TransactionTemplate transactionTemplate;

    public EntregaDocumentoService(
            EntregaDocumentoRepository entregaDocumentoRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            DocumentoInteligenciaService documentoInteligenciaService,
            CompetenciaArquivamentoService competenciaArquivamentoService,
            PlatformTransactionManager transactionManager,
            @Value("${app.upload.dir:uploads/pendencias}") String uploadDir
    ) {
        this.entregaDocumentoRepository = entregaDocumentoRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.documentoInteligenciaService = documentoInteligenciaService;
        this.competenciaArquivamentoService = competenciaArquivamentoService;
        this.uploadBasePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Um arquivo — persiste entrega em transação própria; processamento automático roda só após o commit
     * (evita lock wait no MySQL: INSERT em {@code documentos_processamento} não pode esperar a mesma transação
     * que ainda não liberou a {@code entrega} referenciada).
     */
    public EntregaDocumento anexar(Long pendenciaId, MultipartFile arquivo, String observacao, Usuario usuarioAtual) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo obrigatório.");
        }
        EntregaDocumento salva = transactionTemplate.execute(
                status -> persistirEntregaTransacional(pendenciaId, arquivo, observacao, usuarioAtual));
        iniciarProcessamentoComSeguranca(salva);
        return salva;
    }

    /**
     * Vários arquivos na mesma pendência: cada arquivo em transação própria (commit mesmo se outro falhar).
     */
    public EntregaLoteResultado anexarLote(
            Long pendenciaId,
            List<MultipartFile> arquivos,
            String observacao,
            Usuario usuarioAtual
    ) {
        List<MultipartFile> naoVazios = arquivos == null
                ? List.of()
                : arquivos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (naoVazios.isEmpty()) {
            throw new IllegalArgumentException("Envie pelo menos um arquivo.");
        }
        if (naoVazios.size() > ENTREGA_LOTE_MAX_ARQUIVOS) {
            throw new IllegalArgumentException("No máximo " + ENTREGA_LOTE_MAX_ARQUIVOS + " arquivos por envio.");
        }
        carregarPendenciaComAcesso(pendenciaId, usuarioAtual);

        List<EntregaDocumento> sucesso = new ArrayList<>();
        List<EntregaLoteFalha> falhas = new ArrayList<>();
        for (MultipartFile arquivo : naoVazios) {
            try {
                EntregaDocumento salva = transactionTemplate.execute(
                        status -> persistirEntregaTransacional(pendenciaId, arquivo, observacao, usuarioAtual)
                );
                sucesso.add(salva);
                iniciarProcessamentoComSeguranca(salva);
            } catch (RuntimeException ex) {
                String nome = arquivo.getOriginalFilename() == null ? "(sem nome)" : arquivo.getOriginalFilename();
                String msg = ex.getMessage() != null ? ex.getMessage() : "Erro ao processar arquivo.";
                falhas.add(new EntregaLoteFalha(nome, msg));
                log.warn("Falha no arquivo do lote pendenciaId={} arquivo={}: {}", pendenciaId, nome, msg);
            }
        }
        return new EntregaLoteResultado(sucesso, falhas);
    }

    private EntregaDocumento persistirEntregaTransacional(
            Long pendenciaId,
            MultipartFile arquivo,
            String observacao,
            Usuario usuarioAtual
    ) {
        PendenciaDocumento pendencia = carregarPendenciaComAcesso(pendenciaId, usuarioAtual);
        return salvarArquivoECriarEntrega(pendencia, arquivo, observacao);
    }

    private PendenciaDocumento carregarPendenciaComAcesso(Long pendenciaId, Usuario usuarioAtual) {
        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE
                && !PendenciaClienteDono.clienteEhDonoDaPendencia(usuarioAtual, pendencia)) {
            throw new IllegalArgumentException("Cliente sem acesso a esta pendência.");
        }
        return pendencia;
    }

    private EntregaDocumento salvarArquivoECriarEntrega(
            PendenciaDocumento pendencia,
            MultipartFile arquivo,
            String observacao
    ) {
        String nomeOriginal = arquivo.getOriginalFilename() == null ? "arquivo.bin" : arquivo.getOriginalFilename();
        String extensao = "";
        int idx = nomeOriginal.lastIndexOf('.');
        if (idx >= 0 && idx < nomeOriginal.length() - 1) {
            extensao = nomeOriginal.substring(idx + 1).toLowerCase();
        }
        Long pendenciaId = pendencia.getId();
        String sufixoSalvo = extensao.isEmpty() ? "bin" : extensao;
        String nomeSalvo = "pendencia-" + pendenciaId + "-" + UUID.randomUUID() + "." + sufixoSalvo;

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
            return salva;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Falha ao salvar arquivo.");
        }
    }

    /** Após commit da entrega — {@link DocumentoInteligenciaService#iniciarProcessamento(EntregaDocumento)}. */
    private void iniciarProcessamentoComSeguranca(EntregaDocumento salva) {
        Long pendenciaId = salva.getPendencia() != null ? salva.getPendencia().getId() : null;
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
