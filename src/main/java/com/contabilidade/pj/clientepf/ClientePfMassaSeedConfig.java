package com.contabilidade.pj.clientepf;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.repository.UsuarioRepository;
import com.contabilidade.pj.auth.service.AuthService;
import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.pendencia.entity.CompetenciaMensal;
import com.contabilidade.pj.pendencia.entity.PendenciaDocumento;
import com.contabilidade.pj.pendencia.PendenciaTomadorUids;
import com.contabilidade.pj.pendencia.entity.PendenciaStatus;
import com.contabilidade.pj.pendencia.entity.TemplateDocumento;
import com.contabilidade.pj.pendencia.repository.CompetenciaMensalRepository;
import com.contabilidade.pj.pendencia.repository.PendenciaDocumentoRepository;
import com.contabilidade.pj.pendencia.repository.TemplateDocumentoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Massa de teste para o CPF 617.499.330-20.
 * Cria: ClientePessoaFisica, usuário CLIENTE vinculado, templates e pendências dos últimos 3 meses.
 */
@Configuration
public class ClientePfMassaSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(ClientePfMassaSeedConfig.class);

    private static final String CPF_MASSA   = "61749933020";
    private static final String EMAIL_MASSA = "cliente.pf@demo.com";
    private static final String NOME_MASSA  = "Cliente PF Demo";
    private static final String SENHA_MASSA = "123456";

    @Bean
    CommandLineRunner seedClientePfMassa(
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            UsuarioRepository usuarioRepository,
            AuthService authService,
            EmpresaRepository empresaRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            CompetenciaMensalRepository competenciaMensalRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            garantirColunaCpfComTamanhoSuficiente(jdbcTemplate);
            garantirIndiceUnicoPendenciaPorTomador(jdbcTemplate);
            Empresa empresaBase = empresaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                    .stream()
                    .findFirst()
                    .orElse(null);

            // 1. ClientePessoaFisica
            ClientePessoaFisica pf = clientePessoaFisicaRepository.findByCpf(CPF_MASSA)
                    .orElseGet(() -> {
                        ClientePessoaFisica novo = new ClientePessoaFisica();
                        novo.setCpf(CPF_MASSA);
                        novo.setNomeCompleto(NOME_MASSA);
                        novo.setAtivo(true);
                        ClientePessoaFisica salvo = clientePessoaFisicaRepository.save(novo);
                        log.info("Massa PF criada: CPF {} id={}", CPF_MASSA, salvo.getId());
                        return salvo;
                    });

            // 2. Usuário CLIENTE vinculado ao PF
            usuarioRepository.findByEmail(EMAIL_MASSA).orElseGet(() -> {
                Usuario u = new Usuario();
                u.setNome(NOME_MASSA);
                u.setEmail(EMAIL_MASSA);
                u.setPerfil(PerfilUsuario.CLIENTE);
                u.setClientePessoaFisica(pf);
                u.setEmpresa(empresaBase);
                u.setSenhaHash(authService.passwordEncoder().encode(SENHA_MASSA));
                u.setAtivo(true);
                usuarioRepository.save(u);
                log.info("Usuário PF demo criado: {}", EMAIL_MASSA);
                return u;
            });

            if (empresaBase == null) {
                log.warn("Seed PF demo: nenhuma empresa encontrada; templates e pendências não serão criados.");
                return;
            }

            // 3. Templates (se ainda não existirem para este PF)
            List<TemplateDocumento> templates = templateDocumentoRepository
                    .findByClientePessoaFisicaIdOrderByNomeAsc(pf.getId());
            if (templates.isEmpty()) {
                templates = List.of(
                        criarTemplate(templateDocumentoRepository, empresaBase, pf, "Informe de Rendimentos"),
                        criarTemplate(templateDocumentoRepository, empresaBase, pf, "Recibo de Pró-Labore"),
                        criarTemplate(templateDocumentoRepository, empresaBase, pf, "Comprovante de Pagamento INSS")
                );
                log.info("Templates PF demo criados para CPF {}", CPF_MASSA);
            }

            // 4. Competências e pendências — últimos 3 meses
            LocalDate hoje = LocalDate.now();
            for (int i = 1; i <= 3; i++) {
                LocalDate ref = hoje.minusMonths(i);
                int ano = ref.getYear();
                int mes = ref.getMonthValue();

                CompetenciaMensal competencia = competenciaMensalRepository
                        .findByAnoAndMes(ano, mes)
                        .orElseGet(() -> {
                            CompetenciaMensal c = new CompetenciaMensal();
                            c.setAno(ano);
                            c.setMes(mes);
                            return competenciaMensalRepository.save(c);
                        });

                LocalDate vencimento = LocalDate.of(ano, mes, 1).plusMonths(1).withDayOfMonth(10);

                for (TemplateDocumento template : templates) {
                    String tomadorUid = PendenciaTomadorUids.clientePessoaFisica(pf.getId());
                    boolean existe = pendenciaDocumentoRepository
                            .findByTomadorUidAndTemplateDocumentoIdAndCompetenciaId(
                                    tomadorUid, template.getId(), competencia.getId())
                            .isPresent();
                    if (!existe) {
                        PendenciaDocumento p = new PendenciaDocumento();
                        p.setEmpresa(empresaBase);
                        p.setClientePessoaFisica(pf);
                        p.setTemplateDocumento(template);
                        p.setCompetencia(competencia);
                        p.setStatus(PendenciaStatus.PENDENTE);
                        p.setVencimento(vencimento);
                        try {
                            pendenciaDocumentoRepository.save(p);
                        } catch (DataIntegrityViolationException ex) {
                            // Ambiente legado pode ter índice único antigo e/ou corrida entre seeds.
                            log.debug("Pendência PF já existia (ignorado): {}", ex.getMostSpecificCause().getMessage());
                        }
                    }
                }
            }
            log.info("Pendências PF demo garantidas para CPF {} (3 meses x {} templates)",
                    CPF_MASSA, templates.size());
        };
    }

    private void garantirColunaCpfComTamanhoSuficiente(JdbcTemplate jdbcTemplate) {
        try {
            // Após criptografia LGPD, CPF deixa de caber em coluna legada VARCHAR(11).
            jdbcTemplate.execute(
                    "ALTER TABLE clientes_pessoa_fisica MODIFY COLUMN cpf VARCHAR(255) NOT NULL"
            );
            log.info("Migration: coluna clientes_pessoa_fisica.cpf ajustada para VARCHAR(255).");
        } catch (Exception ex) {
            log.debug("clientes_pessoa_fisica.cpf já ajustada ou não foi possível alterar: {}", ex.getMessage());
        }
    }

    private void garantirIndiceUnicoPendenciaPorTomador(JdbcTemplate jdbcTemplate) {
        try {
            // Nome aleatório comum do índice legado gerado pelo Hibernate.
            jdbcTemplate.execute("ALTER TABLE pendencias_documentos DROP INDEX `UKevo82bxyn6qgcu2we5cl19fwd`");
            log.warn("Migration pendências: índice legado UKevo82bxyn6qgcu2we5cl19fwd removido.");
        } catch (Exception ex) {
            log.debug("Índice legado UKevo82... não existe ou não pôde ser removido: {}", ex.getMessage());
        }

        try {
            List<Map<String, Object>> linhas = jdbcTemplate.queryForList(
                    """
                    SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
                    FROM INFORMATION_SCHEMA.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'pendencias_documentos'
                      AND NON_UNIQUE = 0
                    ORDER BY INDEX_NAME, SEQ_IN_INDEX
                    """
            );

            Map<String, List<String>> colunasPorIndice = linhas.stream()
                    .collect(Collectors.groupingBy(
                            l -> String.valueOf(l.get("INDEX_NAME")),
                            Collectors.mapping(l -> String.valueOf(l.get("COLUMN_NAME")), Collectors.toList())
                    ));

            for (Map.Entry<String, List<String>> entry : colunasPorIndice.entrySet()) {
                List<String> cols = entry.getValue();
                boolean contemEmpresa = cols.stream().anyMatch(c -> "empresa_id".equalsIgnoreCase(c));
                boolean contemTemplate = cols.stream().anyMatch(c -> "template_documento_id".equalsIgnoreCase(c));
                boolean contemCompetencia = cols.stream().anyMatch(c -> "competencia_id".equalsIgnoreCase(c));
                boolean contemTomador = cols.stream().anyMatch(c -> "tomador_uid".equalsIgnoreCase(c));
                boolean legadoEmpresaTemplateCompetencia = cols.size() == 3
                        && contemEmpresa
                        && contemTemplate
                        && contemCompetencia
                        && !contemTomador;
                if (legadoEmpresaTemplateCompetencia) {
                    String idx = entry.getKey();
                    jdbcTemplate.execute("ALTER TABLE pendencias_documentos DROP INDEX `" + idx + "`");
                    log.warn("Migration pendências: índice único legado removido ({})", idx);
                }
            }
        } catch (Exception ex) {
            log.debug("Não foi possível inspecionar/remover índice legado de pendências: {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE pendencias_documentos " +
                            "ADD CONSTRAINT uk_pendencias_tomador_template_competencia " +
                            "UNIQUE (tomador_uid, template_documento_id, competencia_id)"
            );
            log.info("Migration pendências: índice único por tomador criado.");
        } catch (Exception ex) {
            log.debug("Índice único por tomador já existe ou não foi possível criar: {}", ex.getMessage());
        }
    }

    private TemplateDocumento criarTemplate(
            TemplateDocumentoRepository repo,
            Empresa empresaBase,
            ClientePessoaFisica pf,
            String nome
    ) {
        TemplateDocumento t = new TemplateDocumento();
        t.setEmpresa(empresaBase);
        t.setClientePessoaFisica(pf);
        t.setNome(nome);
        t.setObrigatorio(true);
        return repo.save(t);
    }
}
