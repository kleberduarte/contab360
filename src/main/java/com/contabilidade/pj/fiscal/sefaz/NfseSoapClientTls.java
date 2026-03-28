package com.contabilidade.pj.fiscal.sefaz;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.time.Duration;
import java.io.FileInputStream;

final class NfseSoapClientTls {

    private NfseSoapClientTls() {
    }

    static String post(String endpointUrl, String soapBody, String pkcs12Path, String senha, NfseProperties.SaoPaulo sp)
            throws Exception {
        char[] pwd = senha == null ? new char[0] : senha.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(pkcs12Path)) {
            ks.load(fis, pwd);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pwd);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        HttpClient client = HttpClient.newBuilder()
                .sslContext(ssl)
                .connectTimeout(Duration.ofSeconds(45))
                .build();

        String proto = sp.getSoapProtocolo() == null ? "1.2" : sp.getSoapProtocolo().trim();
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .timeout(Duration.ofMinutes(3))
                .POST(HttpRequest.BodyPublishers.ofString(soapBody, StandardCharsets.UTF_8));

        if ("1.1".equals(proto)) {
            b.header("Content-Type", "text/xml; charset=utf-8");
            String action = sp.getSoapAction();
            if (action != null && !action.isBlank()) {
                b.header("SOAPAction", "\"" + action.replace("\"", "") + "\"");
            }
        } else {
            String action = sp.getSoapAction();
            String ct = "application/soap+xml; charset=utf-8";
            if (action != null && !action.isBlank()) {
                ct += "; action=\"" + action.replace("\"", "") + "\"";
            }
            b.header("Content-Type", ct);
        }

        HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = resp.statusCode();
        if (code >= 400) {
            String body = resp.body();
            throw new IllegalStateException("HTTP " + code + " em " + endpointUrl + ": "
                    + (body == null || body.length() < 800 ? body : body.substring(0, 800)));
        }
        return resp.body();
    }
}
