export type PerfilUsuario = "ADM" | "CONTADOR" | "CLIENTE";

export type Sessao = {
  token: string;
  usuarioNome: string;
  usuarioEmail: string;
  perfil: PerfilUsuario;
  senhaTempAtiva?: boolean;
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
  return { token: o.token, usuarioNome: nome, usuarioEmail: email, perfil, senhaTempAtiva: o.senhaTempAtiva === true };
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
