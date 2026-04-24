package com.contabilidade.pj.pendencia;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quando todas as pendências de uma competência estão {@link PendenciaStatus#VALIDADO}, marca a competência como
 * arquivada (histórico). Caso contrário, mantém ou reabre para a fila ativa do contador.
 */
@Service
public class CompetenciaArquivamentoService {

    private final CompetenciaMensalRepository competenciaMensalRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;

    public CompetenciaArquivamentoService(
            CompetenciaMensalRepository competenciaMensalRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository
    ) {
        this.competenciaMensalRepository = competenciaMensalRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
    }

    @Transactional
    public void sincronizarArquivamentoCompetencia(Long competenciaId) {
        if (competenciaId == null) {
            return;
        }
        CompetenciaMensal competencia = competenciaMensalRepository.findById(competenciaId).orElse(null);
        if (competencia == null) {
            return;
        }
        List<PendenciaDocumento> lista = pendenciaDocumentoRepository.findByCompetenciaId(competenciaId);
        if (lista.isEmpty()) {
            if (competencia.isArquivada()) {
                competencia.setArquivada(false);
                competencia.setArquivadaEm(null);
                competenciaMensalRepository.save(competencia);
            }
            return;
        }
        boolean todasValidadas = lista.stream().allMatch(p -> p.getStatus() == PendenciaStatus.VALIDADO);
        if (todasValidadas) {
            if (!competencia.isArquivada()) {
                competencia.setArquivada(true);
                competencia.setArquivadaEm(LocalDateTime.now());
                competenciaMensalRepository.save(competencia);
            }
        } else if (competencia.isArquivada()) {
            competencia.setArquivada(false);
            competencia.setArquivadaEm(null);
            competenciaMensalRepository.save(competencia);
        }
    }
}
