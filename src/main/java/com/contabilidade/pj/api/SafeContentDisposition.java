package com.contabilidade.pj.api;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Monta o header {@code Content-Disposition} de download sem permitir CRLF injection
 * nem caracteres que quebrem o parsing do header (RFC 6266 / 5987 via Spring).
 */
public final class SafeContentDisposition {

    private static final int MAX_FILENAME_LENGTH = 180;
    private static final String FALLBACK = "download";

    private SafeContentDisposition() {
    }

    /**
     * Valor completo do header, ex.: {@code attachment; filename="..."; filename*=UTF-8''...}
     */
    public static String attachment(@Nullable String filename) {
        String safe = sanitizeFilename(filename);
        return ContentDisposition.attachment()
                .filename(safe, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    static String sanitizeFilename(@Nullable String raw) {
        if (!StringUtils.hasText(raw)) {
            return FALLBACK;
        }
        String s = raw.replace('\r', '_').replace('\n', '_').trim();
        int lastSlash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            s = s.substring(lastSlash + 1);
        }
        // Remove aspas, ponto-e-vírgula (quebram o header), controles e backslash
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c == 0x7f || c == '"' || c == ';' || c == '\\') {
                out.append('_');
            } else {
                out.append(c);
            }
        }
        s = out.toString().trim();
        if (!StringUtils.hasText(s) || ".".equals(s) || "..".equals(s)) {
            return FALLBACK;
        }
        if (s.length() > MAX_FILENAME_LENGTH) {
            int dot = s.lastIndexOf('.');
            if (dot > 0 && s.length() - dot <= 12) {
                String ext = s.substring(dot);
                String base = s.substring(0, dot);
                int keepBase = Math.max(1, MAX_FILENAME_LENGTH - ext.length());
                s = base.substring(0, Math.min(base.length(), keepBase)) + ext;
            } else {
                s = s.substring(0, MAX_FILENAME_LENGTH);
            }
        }
        return s;
    }
}
