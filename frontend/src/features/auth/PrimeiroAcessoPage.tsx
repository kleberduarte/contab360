import { FormEvent, useId, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

export function PrimeiroAcessoPage({ sessao, onSenhaCriada }: { sessao: Sessao; onSenhaCriada: () => void }) {
  const novaSenhaId = useId();
  const confirmarId = useId();
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(ev: FormEvent) {
    ev.preventDefault();
    if (novaSenha.length < 6) {
      setErro("A senha deve ter pelo menos 6 caracteres.");
      return;
    }
    if (novaSenha !== confirmar) {
      setErro("As senhas não conferem.");
      return;
    }
    setErro("");
    setLoading(true);
    try {
      await apiFetchJson("/api/auth/definir-nova-senha", {
        method: "POST",
        body: JSON.stringify({ novaSenha }),
        sessao
      });
      onSenhaCriada();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível definir a senha.");
    } finally {
      setLoading(false);
    }
  }

  const iniciais = (sessao.usuarioNome || sessao.usuarioEmail)
    .split(" ")
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");

  return (
    <main className="primeiro-acesso-screen">
      <aside className="primeiro-acesso-sidebar" aria-hidden="true">
        <div className="primeiro-acesso-sidebar__inner">
          <h2 className="primeiro-acesso-sidebar__brand">
            <span className="pa-brand-mark">
              <svg width="38" height="38" viewBox="0 0 44 44" fill="none">
                <defs>
                  <linearGradient id="pa-brand-grad" x1="4" y1="4" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#b8f3cf" />
                    <stop offset="1" stopColor="#7fe6b1" />
                  </linearGradient>
                </defs>
                <rect x="2" y="2" width="40" height="40" rx="11" fill="#fff" fillOpacity="0.16" />
                <circle cx="22" cy="22" r="14" stroke="url(#pa-brand-grad)" strokeWidth="2.35" fill="none" />
                <circle cx="22" cy="22" r="3.25" fill="url(#pa-brand-grad)" />
              </svg>
            </span>
            <span className="pa-brand-text">
              <span className="pa-brand-contab">Contab</span>
              <span className="pa-brand-360">360</span>
            </span>
          </h2>

          <div className="primeiro-acesso-sidebar__content">
            <div className="pa-avatar" aria-hidden="true">{iniciais || "?"}</div>
            <p className="pa-sidebar-greeting">Bem-vindo(a),</p>
            <p className="pa-sidebar-name">{sessao.usuarioNome || sessao.usuarioEmail}</p>
            <p className="pa-sidebar-desc">
              Este é o seu primeiro acesso. Defina uma senha pessoal para proteger sua conta.
            </p>
            <ul className="pa-sidebar-steps">
              <li className="pa-step pa-step--done">
                <span className="pa-step__dot" />
                Autenticação confirmada
              </li>
              <li className="pa-step pa-step--active">
                <span className="pa-step__dot" />
                Defina sua senha
              </li>
              <li className="pa-step">
                <span className="pa-step__dot" />
                Acesse o sistema
              </li>
            </ul>
          </div>
        </div>
      </aside>

      <section className="primeiro-acesso-main">
        <div className="primeiro-acesso-card">
          <div className="primeiro-acesso-card__header">
            <div className="primeiro-acesso-icon" aria-hidden="true">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <rect x="4.75" y="10.25" width="14.5" height="8.75" rx="2" />
                <path d="M8.5 10.25V8.75C8.5 6.82 10.07 5.25 12 5.25C13.93 5.25 15.5 6.82 15.5 8.75V10.25" />
                <circle cx="12" cy="14.75" r="1" fill="currentColor" stroke="none" />
                <path d="M12 15.75V17.25" />
              </svg>
            </div>
            <div>
              <h1 className="primeiro-acesso-title">Crie sua senha</h1>
              <p className="primeiro-acesso-lead">Escolha uma senha segura e que só você conheça.</p>
            </div>
          </div>

          <form onSubmit={submit} className="primeiro-acesso-form">
            <label htmlFor={novaSenhaId} className="primeiro-acesso-label">
              Nova senha
              <div className="login-password-row-react login-input-wrap-react">
                <span className="login-input-icon-react" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none">
                    <rect x="4.75" y="10.25" width="14.5" height="8.75" rx="2" stroke="currentColor" strokeWidth="1.6" />
                    <path d="M8.5 10.25V8.75C8.5 6.82 10.07 5.25 12 5.25C13.93 5.25 15.5 6.82 15.5 8.75V10.25" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                  </svg>
                </span>
                <input
                  id={novaSenhaId}
                  type={mostrarSenha ? "text" : "password"}
                  value={novaSenha}
                  onChange={(e) => setNovaSenha(e.target.value)}
                  autoComplete="new-password"
                  minLength={6}
                  required
                />
                <button
                  type="button"
                  className="login-eye-react"
                  onClick={() => setMostrarSenha((s) => !s)}
                  aria-label={mostrarSenha ? "Ocultar senha" : "Mostrar senha"}
                >
                  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M2.75 12C2.75 12 6.25 6.75 12 6.75C17.75 6.75 21.25 12 21.25 12C21.25 12 17.75 17.25 12 17.25C6.25 17.25 2.75 12 2.75 12Z" stroke="currentColor" strokeWidth="1.6" />
                    <circle cx="12" cy="12" r="2.4" stroke="currentColor" strokeWidth="1.6" />
                  </svg>
                </button>
              </div>
            </label>

            <label htmlFor={confirmarId} className="primeiro-acesso-label">
              Confirmar senha
              <div className="login-input-wrap-react">
                <span className="login-input-icon-react" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none">
                    <rect x="4.75" y="10.25" width="14.5" height="8.75" rx="2" stroke="currentColor" strokeWidth="1.6" />
                    <path d="M8.5 10.25V8.75C8.5 6.82 10.07 5.25 12 5.25C13.93 5.25 15.5 6.82 15.5 8.75V10.25" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                  </svg>
                </span>
                <input
                  id={confirmarId}
                  type={mostrarSenha ? "text" : "password"}
                  value={confirmar}
                  onChange={(e) => setConfirmar(e.target.value)}
                  autoComplete="new-password"
                  required
                />
              </div>
            </label>

            {novaSenha.length > 0 && (
              <div className="pa-strength">
                <div className="pa-strength__bar">
                  <div
                    className={`pa-strength__fill pa-strength__fill--${novaSenha.length < 6 ? "fraca" : novaSenha.length < 10 ? "media" : "forte"}`}
                    style={{ width: `${Math.min(100, (novaSenha.length / 12) * 100)}%` }}
                  />
                </div>
                <span className="pa-strength__label">
                  {novaSenha.length < 6 ? "Fraca" : novaSenha.length < 10 ? "Média" : "Forte"}
                </span>
              </div>
            )}

            <button type="submit" className="primeiro-acesso-submit" disabled={loading}>
              {loading ? (
                <>
                  <span className="shell-spinner-react" aria-hidden="true" />
                  Salvando...
                </>
              ) : (
                <>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M9 12l2 2 4-4" />
                    <path d="M12 3C7.03 3 3 7.03 3 12s4.03 9 9 9 9-4.03 9-9-4.03-9-9-9z" />
                  </svg>
                  Definir senha e entrar
                </>
              )}
            </button>

            {erro ? <small className="erro">{erro}</small> : null}
          </form>

          <p className="primeiro-acesso-hint">
            Mínimo de 6 caracteres. Use letras, números e símbolos para uma senha mais forte.
          </p>
        </div>
      </section>
    </main>
  );
}
