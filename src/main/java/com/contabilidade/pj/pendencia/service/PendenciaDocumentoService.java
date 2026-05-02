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
import java.util.Optional;
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

    /**
     * @param empresaId              opcional; se informado com {@code clientePessoaFisicaId}, erro.
     * @param clientePessoaFisicaId    opcional; mutuamente exclusivo com {@code empresaId}.
     * @param templateDocumentoId      opcional; se informado, gera apenas uma pendência (PJ ou PF) para esse template.
     */
    @Transactional
    public int gerarPendencias(
            Integer ano,
            Integer mes,
            Integer diaVencimento,
            Long empresaId,
            Long clientePessoaFisicaId,
            Long templateDocumentoId,
            Usuario usuarioAtual
    ) {
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
        if (empresaId != null && clientePessoaFisicaId != null) {
            throw new IllegalArgumentException("Informe apenas empresaId ou clientePessoaFisicaId.");
        }

        CompetenciaMensal competencia = competenciaMensalRepository
                .findByAnoAndMes(ano, mes)
                .orElseGet(() -> {
                    CompetenciaMensal nova = new CompetenciaMensal();
                    nova.setAno(ano);
                    nova.setMes(mes);
                    return competenciaMensalRepository.save(nova);
                });

        LocalDate vencimento = calcularVencimento(ano, mes, diaVencimento);

        int totalCriadas;
        if (templateDocumentoId != null) {
            totalCriadas = gerarPendenciaIndividual(
                    competencia,
                    vencimento,
                    empresaId,
                    clientePessoaFisicaId,
                    templateDocumentoId
            );
        } else if (empresaId != null) {
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
            if (!empresa.isAtivo()) {
                throw new IllegalArgumentException("Empresa inativa.");
            }
            totalCriadas = gerarParaEmpresa(empresa, competencia, vencimento);
        } else if (clientePessoaFisicaId != null) {
            ClientePessoaFisica clientePf = clientePessoaFisicaRepository.findById(clientePessoaFisicaId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente PF não encontrado."));
            if (!clientePf.isAtivo()) {
                throw new IllegalArgumentException("Cliente PF inativo.");
            }
            totalCriadas = gerarParaClientePf(clientePf, competencia, vencimento);
        } else {
            totalCriadas = gerarParaTodosTomadores(competencia, vencimento);
        }

        if (totalCriadas > 0) {
            competenciaArquivamentoService.sincronizarArquivamentoCompetencia(competencia.getId());
        }
        return totalCriadas;
    }

    private int gerarPendenciaIndividual(
            CompetenciaMensal competencia,
            LocalDate vencimento,
            Long empresaId,
            Long clientePessoaFisicaId,
            Long templateDocumentoId
    ) {
        if (empresaId == null && clientePessoaFisicaId == null) {
            throw new IllegalArgumentException("Para pendência individual, informe empresaId ou clientePessoaFisicaId.");
        }
        TemplateDocumento template = templateDocumentoRepository.findById(templateDocumentoId)
                .orElseThrow(() -> new IllegalArgumentException("Template de documento não encontrado."));
        if (empresaId != null) {
            if (template.getEmpresa() == null || !template.getEmpresa().getId().equals(empresaId)) {
                throw new IllegalArgumentException("O template não pertence à empresa informada.");
            }
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
            if (!empresa.isAtivo()) {
                throw new IllegalArgumentException("Empresa inativa.");
            }
            return criarPendenciaSeNaoExiste(empresa, null, template, competencia, vencimento);
        }
        if (template.getClientePessoaFisica() == null
                || !template.getClientePessoaFisica().getId().equals(clientePessoaFisicaId)) {
            throw new IllegalArgumentException("O template não pertence ao cliente PF informado.");
        }
        ClientePessoaFisica clientePf = clientePessoaFisicaRepository.findById(clientePessoaFisicaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente PF não encontrado."));
        if (!clientePf.isAtivo()) {
            throw new IllegalArgumentException("Cliente PF inativo.");
        }
        return criarPendenciaSeNaoExiste(null, clientePf, template, competencia, vencimento);
    }

    private int gerarParaTodosTomadores(CompetenciaMensal competencia, LocalDate vencimento) {
        int total = 0;
        List<Empresa> empresas = empresaRepository.findAllByAtivoTrue();
        for (Empresa empresa : empresas) {
            total += gerarParaEmpresa(empresa, competencia, vencimento);
        }
        List<ClientePessoaFisica> clientesPf = clientePessoaFisicaRepository.findAllByAtivoTrueOrderByNomeCompletoAsc();
        for (ClientePessoaFisica clientePf : clientesPf) {
            total += gerarParaClientePf(clientePf, competencia, vencimento);
        }
        return total;
    }

    private int gerarParaEmpresa(Empresa empresa, CompetenciaMensal competencia, LocalDate vencimento) {
        int total = 0;
        List<TemplateDocumento> templates = templateDocumentoRepository.findByEmpresaIdOrderByNomeAsc(empresa.getId());
        for (TemplateDocumento template : templates) {
            total += criarPendenciaSeNaoExiste(empresa, null, template, competencia, vencimento);
        }
        return total;
    }

    private int gerarParaClientePf(ClientePessoaFisica clientePf, CompetenciaMensal competencia, LocalDate vencimento) {
        int total = 0;
        List<TemplateDocumento> templates =
                templateDocumentoRepository.findByClientePessoaFisicaIdOrderByNomeAsc(clientePf.getId());
        for (TemplateDocumento template : templates) {
            total += criarPendenciaSeNaoExiste(null, clientePf, template, competencia, vencimento);
        }
        return total;
    }

    private int criarPendenciaSeNaoExiste(
            Empresa empresa,
            ClientePessoaFisica clientePf,
            TemplateDocumento template,
            CompetenciaMensal competencia,
            LocalDate vencimento
    ) {
        if ((empresa == null) == (clientePf == null)) {
            throw new IllegalStateException("Informe empresa ou cliente PF, exclusivamente.");
        }
        String tomadorUid = empresa != null
                ? PendenciaTomadorUids.empresa(empresa.getId())
                : PendenciaTomadorUids.clientePessoaFisica(clientePf.getId());
        boolean existe = pendenciaDocumentoRepository
                .findByTomadorUidAndTemplateDocumentoIdAndCompetenciaId(
                        tomadorUid,
                        template.getId(),
                        competencia.getId()
                )
                .isPresent();
        if (existe) {
            return 0;
        }
        PendenciaDocumento pendencia = new PendenciaDocumento();
        pendencia.setEmpresa(empresa);
        pendencia.setClientePessoaFisica(clientePf);
        pendencia.setTemplateDocumento(template);
        pendencia.setCompetencia(competencia);
        pendencia.setStatus(PendenciaStatus.PENDENTE);
        pendencia.setVencimento(vencimento);
        pendenciaDocumentoRepository.save(pendencia);
        return 1;
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
            // Cliente PF pode carregar vínculo legado de empresa; no fluxo PF deve prevalecer o filtro por cliente.
            if (usuarioAtual.getClientePessoaFisica() != null) {
                clientePfFiltrado = usuarioAtual.getClientePessoaFisica().getId();
                empresaFiltrada = null;
                if (clientePessoaFisicaId != null && !clientePessoaFisicaId.equals(clientePfFiltrado)) {
                    throw new IllegalArgumentException("Cadastro PF inválido para este usuário.");
                }
                if (empresaId != null) {
                    throw new IllegalArgumentException("Empresa não aplicável para cliente pessoa física.");
                }
            } else if (usuarioAtual.getEmpresa() != null) {
                empresaFiltrada = usuarioAtual.getEmpresa().getId();
                clientePfFiltrado = null;
                if (empresaId != null && !empresaId.equals(empresaFiltrada)) {
                    throw new IllegalArgumentException("Empresa inválida para este usuário.");
                }
            } else {
                Optional<ClientePessoaFisica> clientePfFallback = localizarClientePfPorUsuario(usuarioAtual);
                if (clientePfFallback.isPresent()) {
                    clientePfFiltrado = clientePfFallback.get().getId();
                    empresaFiltrada = null;
                } else {
                    return List.of();
                }
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
            if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE && pendencias.isEmpty()) {
                pendencias = carregarPendenciasPfFallbackPorCpfResponsavel(usuarioAtual, ano, mes);
            }
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

    private List<PendenciaDocumento> carregarPendenciasPfFallbackPorCpfResponsavel(
            Usuario usuarioAtual,
            Integer ano,
            Integer mes
    ) {
        Optional<ClientePessoaFisica> clientePf = localizarClientePfPorUsuario(usuarioAtual);
        if (clientePf.isEmpty()) {
            return List.of();
        }
        return pendenciaDocumentoRepository.findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
                ano,
                mes,
                clientePf.get().getId()
        );
    }

    private Optional<ClientePessoaFisica> localizarClientePfPorUsuario(Usuario usuarioAtual) {
        Optional<ClientePessoaFisica> clientePf = Optional.empty();
        if (usuarioAtual.getEmpresa() != null) {
            String cpfResponsavel = usuarioAtual.getEmpresa().getCpfResponsavel();
            if (cpfResponsavel != null && !cpfResponsavel.isBlank()) {
                clientePf = clientePessoaFisicaRepository.findByCpf(cpfResponsavel);
            }
        }
        if (clientePf.isEmpty() && usuarioAtual.getNome() != null && !usuarioAtual.getNome().isBlank()) {
            clientePf = clientePessoaFisicaRepository.findFirstByNomeCompletoIgnoreCase(usuarioAtual.getNome().trim());
        }
        return clientePf;
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
