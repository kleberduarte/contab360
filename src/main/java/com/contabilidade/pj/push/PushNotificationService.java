package com.contabilidade.pj.push;

import com.contabilidade.pj.auth.entity.Usuario;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationService {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptionRepository;

    @Value("${contab360.push.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${contab360.push.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${contab360.push.vapid.subject:mailto:contato@contab360.com.br}")
    private String vapidSubject;

    private PushService pushService;
    private boolean enabled = false;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() {
        if (vapidPublicKey == null || vapidPublicKey.isBlank()
                || vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            log.warn("Push notifications desabilitadas: VAPID_PUBLIC_KEY ou VAPID_PRIVATE_KEY não configuradas.");
            return;
        }
        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            enabled = true;
            log.info("Push notifications habilitadas (VAPID).");
        } catch (Exception e) {
            log.error("Erro ao inicializar PushService: {}", e.getMessage(), e);
        }
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Transactional
    public void salvarSubscription(Usuario usuario, String endpoint, String p256dh, String auth) {
        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint)
                .orElseGet(PushSubscription::new);
        sub.setUsuario(usuario);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void removerSubscription(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    /**
     * Envia um push de teste para todas as subscriptions do usuário (diagnóstico).
     *
     * @return quantas subscriptions receberam HTTP 2xx do gateway (ex.: FCM)
     */
    public int enviarTesteParaUsuario(Usuario usuario) {
        if (!enabled) {
            throw new IllegalStateException("Push não está habilitado (configure VAPID).");
        }
        List<PushSubscription> subs = subscriptionRepository.findByUsuarioId(usuario.getId());
        if (subs.isEmpty()) {
            throw new IllegalStateException("Nenhuma subscription salva para este usuário.");
        }
        String payload =
                "{\"title\":\"Contab360 (teste)\",\"body\":\"Se apareceu, o envio e o service worker estão ok.\",\"url\":\"/cliente-pendencias\"}";
        int ok = 0;
        for (PushSubscription sub : subs) {
            if (enviar(sub, payload)) {
                ok++;
            }
        }
        return ok;
    }

    /**
     * Envia push para os clientes afetados pela geração de pendências.
     * empresaId e clientePfId são mutuamente exclusivos; se ambos nulos, notifica todos os clientes.
     */
    @Async
    @Transactional(readOnly = true)
    public void notificarPendenciasGeradas(Long empresaId, Long clientePfId, int totalCriadas) {
        if (!enabled || totalCriadas <= 0) {
            return;
        }
        List<PushSubscription> subscriptions;
        if (empresaId != null) {
            subscriptions = subscriptionRepository.findForEmpresaTomador(empresaId);
        } else if (clientePfId != null) {
            subscriptions = subscriptionRepository.findByClientePessoaFisicaId(clientePfId);
        } else {
            subscriptions = subscriptionRepository.findAllClientes();
        }
        if (subscriptions.isEmpty()) {
            log.info(
                    "Push pendências: nenhuma subscription para empresaId={} clientePfId={} (totalCriadas={})",
                    empresaId,
                    clientePfId,
                    totalCriadas);
            return;
        }
        log.info(
                "Push pendências: enviando para {} subscription(ões), empresaId={} clientePfId={}",
                subscriptions.size(),
                empresaId,
                clientePfId);
        String payload = buildPayload(totalCriadas);
        int ok = 0;
        for (PushSubscription sub : subscriptions) {
            if (enviar(sub, payload)) {
                ok++;
            }
        }
        log.info("Push pendências: concluído — {} de {} envio(s) com HTTP 2xx", ok, subscriptions.size());
    }

    private boolean enviar(PushSubscription sub, String payload) {
        try {
            Subscription subscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
            );
            Notification notification = new Notification(subscription, payload);
            HttpResponse response = pushService.send(notification);
            int code = response.getStatusLine().getStatusCode();
            String body = "";
            if (response.getEntity() != null) {
                body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (code >= 200 && code < 300) {
                log.info(
                        "Push aceito pelo gateway (HTTP {}) subscriptionId={}",
                        code,
                        sub.getId());
                return true;
            }
            log.warn(
                    "Push rejeitado HTTP {} subscriptionId={} corpo={}",
                    code,
                    sub.getId(),
                    truncateForLog(body, 300));
            if (code == 410 || code == 404) {
                subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                log.info("Subscription removida (endpoint inválido): {}", sub.getEndpoint());
            }
            return false;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.warn("Falha ao enviar push para subscriptionId={}: {}", sub.getId(), msg, e);
            if (msg.contains("410") || msg.contains("404")) {
                subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                log.info("Subscription removida (endpoint expirado): {}", sub.getEndpoint());
            }
            return false;
        }
    }

    private static String truncateForLog(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }

    private String buildPayload(int totalCriadas) {
        String titulo = "Contab360 — Novas pendências";
        String corpo = totalCriadas == 1
                ? "1 novo documento pendente aguarda seu envio."
                : totalCriadas + " novos documentos pendentes aguardam seu envio.";
        return "{\"title\":\"" + titulo + "\",\"body\":\"" + corpo + "\",\"url\":\"/cliente-pendencias\"}";
    }
}
