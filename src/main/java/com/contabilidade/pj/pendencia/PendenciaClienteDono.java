package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;

public final class PendenciaClienteDono {

    private PendenciaClienteDono() {
    }

    public static boolean clienteEhDonoDaPendencia(Usuario usuario, PendenciaDocumento pendencia) {
        if (usuario == null || usuario.getPerfil() != PerfilUsuario.CLIENTE) {
            return false;
        }
        if (usuario.getEmpresa() != null && pendencia.getEmpresa() != null) {
            return usuario.getEmpresa().getId().equals(pendencia.getEmpresa().getId());
        }
        if (usuario.getClientePessoaFisica() != null && pendencia.getClientePessoaFisica() != null) {
            return usuario.getClientePessoaFisica().getId().equals(pendencia.getClientePessoaFisica().getId());
        }
        return false;
    }
}
