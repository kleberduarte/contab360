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

  return (
    <main className="primeiro-acesso-screen">
      <div className="primeiro-acesso-card">
        <div className="primeiro-acesso-icon" aria-hidden="true">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <rect x="4.75" y="10.25" width="14.5" height="8.75" rx="2" />
            <path d="M8.5 10.25V8.75C8.5 6.82 10.07 5.25 12 5.25C13.93 5.25 15.5 6.82 15.5 8.75V10.25" />
            <circle cx="12" cy="14.75" r="1" fill="currentColor" stroke="none" />
            <path d="M12 15.75V17.25" />
          </svg>
        </div>

        <h1 className="primeiro-acesso-title">Defina sua senha</h1>
        <p className="primeiro-acesso-lead">
          Olá, <strong>{sessao.usuarioNome || sessao.usuarioEmail}</strong>! Para continuar, crie uma senha pessoal. Você usará ela em todos os próximos acessos.
        </p>

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

          <button type="submit" className="primeiro-acesso-submit" disabled={loading}>
            {loading ? "Salvando..." : "Definir senha e entrar"}
          </button>

          {erro ? <small className="erro">{erro}</small> : null}
        </form>

        <p className="primeiro-acesso-hint">
          Mínimo de 6 caracteres. Escolha uma senha que só você saiba.
        </p>
      </div>
    </main>
  );
}
