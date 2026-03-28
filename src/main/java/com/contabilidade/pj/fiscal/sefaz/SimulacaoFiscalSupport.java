package com.contabilidade.pj.fiscal.sefaz;

import java.security.SecureRandom;

final class SimulacaoFiscalSupport {

    private static final SecureRandom RND = new SecureRandom();

    private SimulacaoFiscalSupport() {
    }

    static String gerarChave44Digitos() {
        StringBuilder sb = new StringBuilder(44);
        for (int i = 0; i < 44; i++) {
            sb.append(RND.nextInt(10));
        }
        return sb.toString();
    }
}
