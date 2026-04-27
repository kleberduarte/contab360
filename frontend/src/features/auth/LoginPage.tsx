import { FormEvent, useId, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type LoginPayload = {
  email: string;
  senha: string;
};

type LoginResponse = {
  token: string;
  nome: string;
  email: string;
  perfil: string;
  empresaId?: number | null;
  clientePessoaFisicaId?: number | null;
};

function loginResponseToSessao(data: LoginResponse): Sessao {
  const perfil = data.perfil === "ADM" || data.perfil === "CONTADOR" || data.perfil === "CLIENTE" ? data.perfil : null;
  if (!perfil) {
    throw new Error("Perfil inválido na resposta do servidor.");
  }
  return {
    token: data.token,
    usuarioNome: data.nome ?? "",
    usuarioEmail: data.email ?? "",
    perfil,
    empresaId: data.empresaId ?? null,
    clientePessoaFisicaId: data.clientePessoaFisicaId ?? null
  };
}

export function LoginPage({ onLogin }: { onLogin: (sessao: Sessao) => void }) {
  const emailId = useId();
  const senhaId = useId();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    setErro("");
    setLoading(true);
    try {
      const data = await apiFetchJson<LoginResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, senha } satisfies LoginPayload)
      });
      onLogin(loginResponseToSessao(data));
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha no login.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-screen-react">
      <aside className="login-sidebar-react" aria-hidden="true">
        <h1 className="login-brand-react">
          <span className="brand-mark-react" aria-hidden="true">
            <svg width="40" height="40" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="brand-grad-login-react" x1="4" y1="4" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                  <stop stopColor="#b8f3cf" />
                  <stop offset="1" stopColor="#7fe6b1" />
                </linearGradient>
              </defs>
              <rect x="2" y="2" width="40" height="40" rx="11" fill="#fff" fillOpacity="0.16" />
              <circle cx="22" cy="22" r="14" stroke="url(#brand-grad-login-react)" strokeWidth="2.35" fill="none" />
              <circle cx="22" cy="22" r="3.25" fill="url(#brand-grad-login-react)" />
            </svg>
          </span>
          <span className="brand-text-react">
            <span className="brand-text-contab-react">Contab</span>
            <span className="brand-text-360-react">360</span>
          </span>
        </h1>
        <p className="login-sidebar-tag-react">No Contab360 você pode</p>
        <ul className="login-sidebar-list-react">
          <li>Centralizar pendências e documentos</li>
          <li>Acompanhar entregas e fiscal em um só lugar</li>
        </ul>
      </aside>
      <section className="login-main-react">
        <div className="login-card-react">
          <h2>Acessar o sistema</h2>
          <form onSubmit={submit} className="login-form-react">
            <label className="login-field-react" htmlFor={emailId}>
              <span className="login-field-label-react">E-mail</span>
              <span className="login-input-wrap-react">
                <span className="login-input-icon-react" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="3.75" y="5.75" width="16.5" height="12.5" rx="2" stroke="currentColor" strokeWidth="1.6" />
                    <path d="M4.75 7L12 12.5L19.25 7" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </span>
                <input
                  id={emailId}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  type="email"
                  autoComplete="email"
                  required
                />
              </span>
            </label>
            <label className="login-field-react" htmlFor={senhaId}>
              <span className="login-field-label-react">Senha</span>
              <div className="login-password-row-react login-input-wrap-react">
                <span className="login-input-icon-react" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="4.75" y="10.25" width="14.5" height="8.75" rx="2" stroke="currentColor" strokeWidth="1.6" />
                    <path d="M8.5 10.25V8.75C8.5 6.82 10.07 5.25 12 5.25C13.93 5.25 15.5 6.82 15.5 8.75V10.25" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                  </svg>
                </span>
                <input
                  id={senhaId}
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  type={mostrarSenha ? "text" : "password"}
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="login-eye-react"
                  onClick={() => setMostrarSenha((s) => !s)}
                  aria-label={mostrarSenha ? "Ocultar senha" : "Mostrar senha"}
                >
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <path d="M2.75 12C2.75 12 6.25 6.75 12 6.75C17.75 6.75 21.25 12 21.25 12C21.25 12 17.75 17.25 12 17.25C6.25 17.25 2.75 12 2.75 12Z" stroke="currentColor" strokeWidth="1.6" />
                    <circle cx="12" cy="12" r="2.4" stroke="currentColor" strokeWidth="1.6" />
                  </svg>
                </button>
              </div>
            </label>
            <button type="submit" disabled={loading}>
              {loading ? "Entrando..." : "Entrar"}
            </button>
            {erro ? <small className="erro">{erro}</small> : null}
          </form>
        </div>
      </section>
    </main>
  );
}
