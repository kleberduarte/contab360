package com.contabilidade.pj.pendencia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.pendencia.entity.PendenciaDocumento;
import com.contabilidade.pj.pendencia.entity.PendenciaStatus;
import com.contabilidade.pj.pendencia.repository.CompetenciaMensalRepository;
import com.contabilidade.pj.pendencia.repository.PendenciaDocumentoRepository;
import com.contabilidade.pj.pendencia.repository.TemplateDocumentoRepository;
import com.contabilidade.pj.push.PushNotificationService;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendenciaDocumentoServiceTest {

    @Mock
    private CompetenciaMensalRepository competenciaMensalRepository;
    @Mock
    private PendenciaDocumentoRepository pendenciaDocumentoRepository;
    @Mock
    private TemplateDocumentoRepository templateDocumentoRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    @Mock
    private CompetenciaArquivamentoService competenciaArquivamentoService;
    @Mock
    private PushNotificationService pushNotificationService;

    private PendenciaDocumentoService service;

    @BeforeEach
    void setUp() {
        service = new PendenciaDocumentoService(
                competenciaMensalRepository,
                pendenciaDocumentoRepository,
                templateDocumentoRepository,
                empresaRepository,
                clientePessoaFisicaRepository,
                competenciaArquivamentoService,
                pushNotificationService
        );
    }

    @Test
    void listarClienteComPfEEmpresaLegadoDeveUsarFiltroPf() throws Exception {
        ClientePessoaFisica clientePf = new ClientePessoaFisica();
        setId(clientePf, 55L);
        Empresa empresaLegado = new Empresa();
        empresaLegado.setId(99L);

        Usuario usuario = new Usuario();
        usuario.setPerfil(PerfilUsuario.CLIENTE);
        usuario.setClientePessoaFisica(clientePf);
        usuario.setEmpresa(empresaLegado);

        List<PendenciaDocumento> esperado = List.of(new PendenciaDocumento());
        when(pendenciaDocumentoRepository.findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
                2026, 4, 55L
        )).thenReturn(esperado);

        List<PendenciaDocumento> resultado = service.listar(
                2026, 4, null, null, null, usuario, false
        );

        assertEquals(esperado, resultado);
        verify(pendenciaDocumentoRepository)
                .findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(2026, 4, 55L);
        verify(pendenciaDocumentoRepository, never())
                .findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(2026, 4, 99L);
    }

    @Test
    void listarClientePfComEmpresaIdInformadoDeveFalhar() throws Exception {
        ClientePessoaFisica clientePf = new ClientePessoaFisica();
        setId(clientePf, 55L);

        Usuario usuario = new Usuario();
        usuario.setPerfil(PerfilUsuario.CLIENTE);
        usuario.setClientePessoaFisica(clientePf);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.listar(2026, 4, 10L, null, PendenciaStatus.PENDENTE, usuario, false)
        );

        assertEquals("Empresa não aplicável para cliente pessoa física.", ex.getMessage());
    }

    @Test
    void listarClienteSomenteEmpresaDeveUsarFallbackPfQuandoEmpresaNaoTemPendencias() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(99L);
        empresa.setCpfResponsavel("12345678901");

        ClientePessoaFisica clientePf = new ClientePessoaFisica();
        setId(clientePf, 77L);

        Usuario usuario = new Usuario();
        usuario.setPerfil(PerfilUsuario.CLIENTE);
        usuario.setEmpresa(empresa);
        usuario.setClientePessoaFisica(null);

        when(pendenciaDocumentoRepository.findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
                2026, 4, 99L
        )).thenReturn(List.of());
        when(clientePessoaFisicaRepository.findByCpf("12345678901")).thenReturn(java.util.Optional.of(clientePf));
        List<PendenciaDocumento> esperado = List.of(new PendenciaDocumento());
        when(pendenciaDocumentoRepository.findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
                2026, 4, 77L
        )).thenReturn(esperado);

        List<PendenciaDocumento> resultado = service.listar(
                2026, 4, null, null, null, usuario, false
        );

        assertEquals(esperado, resultado);
        verify(clientePessoaFisicaRepository).findByCpf("12345678901");
        verify(pendenciaDocumentoRepository)
                .findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(2026, 4, 77L);
    }

    @Test
    void listarClienteSomenteEmpresaDeveUsarFallbackPorNomeQuandoCpfNaoEncontrarPf() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(88L);
        empresa.setCpfResponsavel("00000000000");

        ClientePessoaFisica clientePf = new ClientePessoaFisica();
        setId(clientePf, 66L);
        clientePf.setNomeCompleto("Kleber Duarte");

        Usuario usuario = new Usuario();
        usuario.setPerfil(PerfilUsuario.CLIENTE);
        usuario.setEmpresa(empresa);
        usuario.setNome("Kleber Duarte");

        when(pendenciaDocumentoRepository.findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
                2026, 4, 88L
        )).thenReturn(List.of());
        when(clientePessoaFisicaRepository.findByCpf("00000000000")).thenReturn(java.util.Optional.empty());
        when(clientePessoaFisicaRepository.findFirstByNomeCompletoIgnoreCase("Kleber Duarte"))
                .thenReturn(java.util.Optional.of(clientePf));
        List<PendenciaDocumento> esperado = List.of(new PendenciaDocumento());
        when(pendenciaDocumentoRepository.findByCompetenciaAnoAndCompetenciaMesAndClientePfIdOrderByTemplateDocumentoNomeAsc(
                2026, 4, 66L
        )).thenReturn(esperado);

        List<PendenciaDocumento> resultado = service.listar(
                2026, 4, null, null, null, usuario, false
        );

        assertEquals(esperado, resultado);
        verify(clientePessoaFisicaRepository).findFirstByNomeCompletoIgnoreCase("Kleber Duarte");
    }

    @SuppressWarnings("SameParameterValue")
    private static void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
