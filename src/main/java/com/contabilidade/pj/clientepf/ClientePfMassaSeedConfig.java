package com.contabilidade.pj.clientepf;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.repository.UsuarioRepository;
import com.contabilidade.pj.auth.service.AuthService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            TemplateDocumentoRepository templateDocumentoRepository,
            CompetenciaMensalRepository competenciaMensalRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository
    ) {
        return args -> {

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
                u.setSenhaHash(authService.passwordEncoder().encode(SENHA_MASSA));
                u.setAtivo(true);
                usuarioRepository.save(u);
                log.info("Usuário PF demo criado: {}", EMAIL_MASSA);
                return u;
            });

            // 3. Templates (se ainda não existirem para este PF)
            List<TemplateDocumento> templates = templateDocumentoRepository
                    .findByClientePessoaFisicaIdOrderByNomeAsc(pf.getId());
            if (templates.isEmpty()) {
                templates = List.of(
                        criarTemplate(templateDocumentoRepository, pf, "Informe de Rendimentos"),
                        criarTemplate(templateDocumentoRepository, pf, "Recibo de Pró-Labore"),
                        criarTemplate(templateDocumentoRepository, pf, "Comprovante de Pagamento INSS")
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
                        p.setClientePessoaFisica(pf);
                        p.setTemplateDocumento(template);
                        p.setCompetencia(competencia);
                        p.setStatus(PendenciaStatus.PENDENTE);
                        p.setVencimento(vencimento);
                        pendenciaDocumentoRepository.save(p);
                    }
                }
            }
            log.info("Pendências PF demo garantidas para CPF {} (3 meses x {} templates)",
                    CPF_MASSA, templates.size());
        };
    }

    private TemplateDocumento criarTemplate(
            TemplateDocumentoRepository repo,
            ClientePessoaFisica pf,
            String nome
    ) {
        TemplateDocumento t = new TemplateDocumento();
        t.setClientePessoaFisica(pf);
        t.setNome(nome);
        t.setObrigatorio(true);
        return repo.save(t);
    }
}
