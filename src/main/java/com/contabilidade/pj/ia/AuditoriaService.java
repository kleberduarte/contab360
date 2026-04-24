package com.contabilidade.pj.ia;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaEventoRepository auditoriaEventoRepository;

    public AuditoriaService(AuditoriaEventoRepository auditoriaEventoRepository) {
        this.auditoriaEventoRepository = auditoriaEventoRepository;
    }

    @Transactional
    public void registrarHttp(String metodo, String path, int statusHttp) {
        if (path == null || path.startsWith("/api/health")) {
            return;
        }
        Usuario u = AuthContext.get();
        AuditoriaEvento ev = new AuditoriaEvento();
        ev.setMetodoHttp(metodo == null ? "?" : metodo);
        ev.setPath(truncar(path, 500));
        ev.setCategoria(categorizar(path));
        ev.setStatusHttp(statusHttp);
        ev.setCriadoEm(Instant.now());
        if (u != null) {
            ev.setUsuarioEmail(truncar(u.getEmail(), 118));
            ev.setPerfil(u.getPerfil() != null ? u.getPerfil().name() : null);
        } else {
            ev.setUsuarioEmail(null);
            ev.setPerfil(null);
        }
        auditoriaEventoRepository.save(ev);
    }

    private static String categorizar(String path) {
        if (path.startsWith("/api/fiscal")) {
            return "FISCAL";
        }
        if (path.startsWith("/api/pendencias")) {
            return "PENDENCIAS";
        }
        if (path.startsWith("/api/inteligencia")) {
            return "IA_DOCUMENTOS";
        }
        if (path.startsWith("/api/empresas")) {
            return "EMPRESAS";
        }
        if (path.startsWith("/api/templates")) {
            return "TEMPLATES";
        }
        if (path.startsWith("/api/auth")) {
            return "AUTH";
        }
        if (path.startsWith("/api/ia-observadora")) {
            return "IA_OBSERVADORA";
        }
        return "OUTROS";
    }

    private static String truncar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
