package com.contabilidade.pj.auth;

public final class AuthContext {

    private static final ThreadLocal<Usuario> USUARIO_ATUAL = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(Usuario usuario) {
        USUARIO_ATUAL.set(usuario);
    }

    public static Usuario get() {
        return USUARIO_ATUAL.get();
    }

    public static void clear() {
        USUARIO_ATUAL.remove();
    }
}
