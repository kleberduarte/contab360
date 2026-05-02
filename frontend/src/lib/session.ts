export type PerfilUsuario = "ADM" | "CONTADOR" | "CLIENTE";

export type Sessao = {
  token: string;
  usuarioNome: string;
  usuarioEmail: string;
  perfil: PerfilUsuario;
  /** ID do usuário logado (login). Usado para não oferecer ações contra a própria conta. */
  usuarioId?: number;
  senhaTempAtiva?: boolean;
  /** Presente quando o cliente está vinculado a uma empresa (PJ). */
  empresaId?: number | null;
  /** Presente quando o cliente está vinculado a um cadastro PF (IRPF etc.). */
  clientePessoaFisicaId?: number | null;
};

const STORAGE_KEY = "contabpj_session";

/** Aceita JSON salvo no formato da UI (`usuarioNome`) ou do backend (`nome`). */
export function normalizeSessao(raw: unknown): Sessao | null {
  if (!raw || typeof raw !== "object") return null;
  const o = raw as Record<string, unknown>;
  if (typeof o.token !== "string" || !o.token) return null;
  const nome =
    typeof o.usuarioNome === "string"
      ? o.usuarioNome
      : typeof o.nome === "string"
        ? o.nome
        : "";
  const email =
    typeof o.usuarioEmail === "string"
      ? o.usuarioEmail
      : typeof o.email === "string"
        ? o.email
        : "";
  const p = o.perfil;
  const perfil = p === "ADM" || p === "CONTADOR" || p === "CLIENTE" ? p : null;
  if (!perfil) return null;
  const empresaId =
    typeof o.empresaId === "number"
      ? o.empresaId
      : typeof o.empresaId === "string" && o.empresaId !== ""
        ? Number(o.empresaId)
        : null;
  const clientePessoaFisicaId =
    typeof o.clientePessoaFisicaId === "number"
      ? o.clientePessoaFisicaId
      : typeof o.clientePessoaFisicaId === "string" && o.clientePessoaFisicaId !== ""
        ? Number(o.clientePessoaFisicaId)
        : null;
  const usuarioIdRaw = o.usuarioId;
  const usuarioId =
    typeof usuarioIdRaw === "number" && Number.isFinite(usuarioIdRaw)
      ? usuarioIdRaw
      : typeof usuarioIdRaw === "string" && usuarioIdRaw !== ""
        ? Number(usuarioIdRaw)
        : undefined;
  return {
    token: o.token,
    usuarioNome: nome,
    usuarioEmail: email,
    perfil,
    usuarioId: Number.isFinite(usuarioId as number) ? (usuarioId as number) : undefined,
    senhaTempAtiva: o.senhaTempAtiva === true,
    empresaId: Number.isFinite(empresaId as number) ? (empresaId as number) : null,
    clientePessoaFisicaId: Number.isFinite(clientePessoaFisicaId as number)
      ? (clientePessoaFisicaId as number)
      : null
  };
}

export function getSessao(): Sessao | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return normalizeSessao(JSON.parse(raw) as unknown);
  } catch {
    return null;
  }
}

export function setSessao(sessao: Sessao): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessao));
}

export function clearSessao(): void {
  localStorage.removeItem(STORAGE_KEY);
}
