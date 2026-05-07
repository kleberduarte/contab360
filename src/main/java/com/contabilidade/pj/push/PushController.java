package com.contabilidade.pj.push;

import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.service.AuthContext;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushNotificationService pushNotificationService;

    public PushController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidPublicKey() {
        return ResponseEntity.ok(Map.of(
                "publicKey", pushNotificationService.getVapidPublicKey(),
                "enabled", String.valueOf(pushNotificationService.isEnabled())
        ));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody SubscribeRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        pushNotificationService.salvarSubscription(usuario, req.endpoint(), req.p256dh(), req.auth());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody UnsubscribeRequest req) {
        pushNotificationService.removerSubscription(req.endpoint());
        return ResponseEntity.ok().build();
    }

    /**
     * Diagnóstico: envia push de teste para o usuário logado.
     * Resposta sempre 200 com corpo JSON (evita 400 no navegador); ver {@code ok}, {@code sent} e {@code message}.
     */
    @PostMapping("/test-notify")
    public ResponseEntity<Map<String, Object>> testNotify() {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            int sent = pushNotificationService.enviarTesteParaUsuario(usuario);
            if (sent <= 0) {
                return ResponseEntity.ok(Map.of(
                        "ok", false,
                        "sent", 0,
                        "message",
                        "Nenhum envio aceito pelo gateway (FCM). Veja o log do servidor ou tente ativar as notificações de novo."));
            }
            return ResponseEntity.ok(Map.of("ok", true, "sent", sent));
        } catch (IllegalStateException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Push indisponível para teste.";
            return ResponseEntity.ok(Map.of("ok", false, "sent", 0, "message", msg));
        }
    }

    public record SubscribeRequest(String endpoint, String p256dh, String auth) {}
    public record UnsubscribeRequest(String endpoint) {}
}
