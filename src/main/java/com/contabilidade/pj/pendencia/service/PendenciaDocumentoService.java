package com.contabilidade.pj.pendencia.service;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.pendencia.PendenciaTomadorUids;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.contabilidade.pj.pendencia.entity.*;
import com.contabilidade.pj.pendencia.repository.*;

@Service
public class PendenciaDocumentoService {

    private final CompetenciaMensalRepository competenciaMensalRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final EmpresaRepository empresaRepository;
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final CompetenciaArquivamentoService competenciaArquivamentoService;

    public PendenciaDocumentoService(
            CompetenciaMensalRepository competenciaMensalRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            EmpresaRepository empresaRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            CompetenciaArquivamentoService competenciaArquivamentoService
    ) {
        this.competenciaMensalRepository = competenciaMensalRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.empresaRepository = empresaRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.competenciaArquivamentoService = competenciaArquivamentoService;
    }

    @Transactional
    public int gerarPendencias(Integer ano, Integer mes, Integer diaVencimento, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode gerar pendências.");
        }
        if (ano == null || mes == null) {
            throw new IllegalArgumentException("Ano e mês são obrigatórios.");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }
        if (diaVencimento == null || diaVencimento < 1 || diaVencimento > 31) {
            throw new IllegalArgumentException("Dia de vencimento inválido.");
        }

        CompetenciaMensal competencia = competenciaMensalRepository
                .findByAnoAndMes(ano, mes)
                .orElseGet(() -> {
                    CompetenciaMensal nova = new CompetenciaMensal();
                    nova.setAno(ano);
                    nova.setMes(mes);
                    return competenciaMensalRepository.save(nova);
                });

        int totalCriadas = 0;
        LocalDate vencimento = calcularVencimento(ano, mes, diaVencimento);

        List<Empresa> empresas = empresaRepository.findAllByAtivoTrue();
        for (Empresa empresa : empresas) {
            List<TemplateDocumento> templates = templateDocumentoRepository.findByEmpresaIdOrderByNomeAsc(empresa.getId());
            for (TemplateDocumento template : templates) {
                String tomadorUid = PendenciaTomadorUids.empresa(empresa.getId());
                boolean existe = pendenciaDocumentoRepository
                        .findByTomadorUidAndTemplateDocumentoIdAndCompetenciaId(
                                tomadorUid,
                                template.getId(),
                                competencia.getId()
                        )
                        .isPresent();
                if (existe) {
                    continue;
                }
                PendenciaDocumento pendencia = new PendenciaDocumento();
                pendencia.setEmpresa(empresa);
                pendencia.setTemplateDocumento(template);
                pendencia.setCompetencia(competencia);
                pendencia.setStatus(PendenciaStatus.PENDENTE);
                pendencia.setVencimento(vencimento);
                pendenciaDocumentoRepository.save(pendencia);
                totalCriadas++;
            }
        }

        List<ClientePessoaFisica> clientesPf = clientePessoaFisicaRepository.findAllByAtivoTrueOrderByNomeCompletoAsc();
        for (ClientePessoaFisica clientePf : clientesPf) {
            List<TemplateDocumento> templates =
                    templateDocumentoRepository.findByClientePessoaFisicaIdOrderByNomeAsc(clientePf.getId());
            for (TemplateDocumento template : templates) {
                String tomadorUid = PendenciaTomadorUids.clientePessoaFisica(clientePf.getId());
                boolean existe = pendenciaDocumentoRepository
                        .findByTomadorUidAndTemplateDocumentoIdAndCompetenciaId(
                                tomadorUid,
                                template.getId(),
                                competencia.getId()
                        )
                        .isPresent();
                if (existe) {
                    continue;
                }
                PendenciaDocumento pendencia = new PendenciaDocumento();
                pendencia.setClientePessoaFisica(clientePf);
                pendencia.setTemplateDocumento(template);
                pendencia.setCompetencia(competencia);
                pendencia.setStatus(PendenciaStatus.PENDENTE);
                pendencia.setVencimento(vencimento);
                pendenciaDocumentoRepository.save(pendencia);
                totalCriadas++;
            }
        }

        if (totalCriadas > 0) {
            competenciaArquivamentoService.sincronizarArquivamentoCompetencia(competencia.getId());
        }
        return totalCriadas;
    }

    @Transactional(readOnly = true)
    public List<PendenciaDocumento> listar(
            Integer ano,
            Integer mes,
            Long empresaId,
            Long clientePessoaFisicaId,
            PendenciaStatus status,
            Usuario usuarioAtual,
            boolean incluirArquivadas
    ) {
        if (ano == null || mes == null) {
            throw new IllegalArgumentException("Ano e mês são obrigatórios.");
        }

        Long empresaFiltrada = empresaId;
        Long clientePfFiltrado = clientePessoaFisicaId;

        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() != null) {
                empresaFiltrada = usuarioAtual.getEmpresa().getId();
                clientePfFiltrado = null;
                if (empresaId != null && !empresaId.equals(empresaFiltrada)) {
                    throw new IllegalArgumentException("Empresa inválida para este usuário.");
                }
            } else if (usuarioAtual.getClientePessoaFisica() != null) {
                clientePfFiltrado = usuarioAtual.getClientePessoaFisica().getId();
                empresaFiltrada = null;
                if (clientePessoaFisicaId != null && !clientePessoaFisicaId.equals(clientePfFiltrado)) {
                    throw new IllegalArgumentException("Cadastro PF inválido para este usuário.");
                }
            } else {
                return List.of();
            }
        } else if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR) {
            if (empresaFiltrada != null && clientePfFiltrado != null) {
                throw new IllegalArgumentException("Informe apenas empresaId ou clientePessoaFisicaId.");
            }
        }

        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR && !incluirArquivadas) {
            if (competenciaMensalRepository.findByAnoAndMes(ano, mes).map(CompetenciaMensal::isArquivada).orElse(false)) {
                return List.of();
            }
        }

        List<PendenciaDocumento> pendencias;
        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR) {
            if (empresaFiltrada != null) {
                pendencias = pendenciaDocumentoRepository
                        .findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
                                ano,
                                mes,
                                empresaFiltrada
                        );
            } else if (clientePfFiltrado != null) {
                pendencias = pendenciaDocumentoRepository
                        .findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
                                ano,
                                mes,
                                clientePfFiltrado
                        );
            } else {
                pendencias = pendenciaDocumentoRepository.findByCompetenciaAnoAndMesParaContador(ano, mes);
            }
        } else if (empresaFiltrada != null) {
            pendencias = pendenciaDocumentoRepository
                    .findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
                            ano,
                            mes,
                            empresaFiltrada
                    );
        } else {
            pendencias = pendenciaDocumentoRepository
                    .findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
                            ano,
                            mes,
                            clientePfFiltrado
                    );
        }

        if (status == null) {
            return pendencias;
        }

        List<PendenciaDocumento> filtradas = new ArrayList<>();
        for (PendenciaDocumento pendencia : pendencias) {
            if (pendencia.getStatus() == status) {
                filtradas.add(pendencia);
            }
        }
        return filtradas;
    }

    @Transactional
    public PendenciaDocumento atualizarStatus(Long pendenciaId, PendenciaStatus novoStatus, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode validar/rejeitar pendências.");
        }
        if (novoStatus == null) {
            throw new IllegalArgumentException("Status é obrigatório.");
        }
        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));
        pendencia.setStatus(novoStatus);
        PendenciaDocumento salva = pendenciaDocumentoRepository.save(pendencia);
        competenciaArquivamentoService.sincronizarArquivamentoCompetencia(pendencia.getCompetencia().getId());
        return salva;
    }

    private LocalDate calcularVencimento(Integer ano, Integer mes, Integer diaVencimento) {
        int ultimoDia = LocalDate.of(ano, mes, 1).lengthOfMonth();
        int diaSeguro = Math.min(diaVencimento, ultimoDia);
        try {
            return LocalDate.of(ano, mes, diaSeguro);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Competência inválida.");
        }
    }

    @Transactional(readOnly = true)
    public CompetenciaArquivoInfo obterInfoArquivoCompetencia(Integer ano, Integer mes, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            return new CompetenciaArquivoInfo(false, false, null);
        }
        return competenciaMensalRepository.findByAnoAndMes(ano, mes)
                .map(c -> new CompetenciaArquivoInfo(true, c.isArquivada(), c.getArquivadaEm()))
                .orElse(new CompetenciaArquivoInfo(false, false, null));
    }

    public record CompetenciaArquivoInfo(boolean existeCompetencia, boolean arquivada, LocalDateTime arquivadaEm) {
    }
}
