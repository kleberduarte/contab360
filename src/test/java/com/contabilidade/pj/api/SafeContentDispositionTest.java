package com.contabilidade.pj.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeContentDispositionTest {

    @Test
    void stripsPathAndNewlines() {
        assertThat(SafeContentDisposition.sanitizeFilename("../../etc/passwd\r\n")).isEqualTo("passwd__");
    }

    @Test
    void attachmentHeaderContainsNoRawQuotesFromName() {
        String h = SafeContentDisposition.attachment("ok\"; ignored=\"1");
        assertThat(h).startsWith("attachment;");
        assertThat(h).doesNotContain("\"; ignored");
    }
}
