import { FormEvent, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { applyDocTabsTone, DocTabsTone, getDocTabsTone, persistDocTabsTone } from "../../lib/docTabsTone";
import { Sessao } from "../../lib/session";

const PERFIL_LABEL: Record<string, string> = { ADM: "Administrador", CONTADOR: "Contador", CLIENTE: "Cliente" };

export function MinhaContaPage({
  sessao,
  onNomeAtualizado
}: {
  sessao: Sessao;
  onNomeAtualizado: (novoNome: string) => void;
}) {
  const [nome, setNome] = useState(sessao.usuarioNome ?? "");
  const [erroNome, setErroNome] = useState("");
  const [okNome, setOkNome] = useState("");
  const [loadingNome, setLoadingNome] = useState(false);

  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [erroSenha, setErroSenha] = useState("");
  const [okSenha, setOkSenha] = useState("");
  const [loadingSenha, setLoadingSenha] = useState(false);
  const [docTabsTone, setDocTabsTone] = useState<DocTabsTone>(() => getDocTabsTone());
  const [okTema, setOkTema] = useState("");

  async function salvarNome(ev: FormEvent) {
    ev.preventDefault();
    setErroNome("");
    setOkNome("");
    setLoadingNome(true);
    try {
      await apiFetchJson("/api/auth/minha-conta", {
        method: "PUT",
        body: JSON.stringify({ nome }),
        sessao
      });
      setOkNome("Nome atualizado com sucesso.");
      onNomeAtualizado(nome);
    } catch (e) {
      setErroNome(e instanceof Error ? e.message : "Erro ao salvar nome.");
    } finally {
      setLoadingNome(false);
    }
  }

  async function trocarSenha(ev: FormEvent) {
    ev.preventDefault();
    setErroSenha("");
    setOkSenha("");
    if (novaSenha !== confirmar) {
      setErroSenha("A nova senha e a confirmação não coincidem.");
      return;
    }
    if (novaSenha.length < 6) {
      setErroSenha("A nova senha deve ter pelo menos 6 caracteres.");
      return;
    }
    setLoadingSenha(true);
    try {
      await apiFetchJson("/api/auth/senha", {
        method: "PUT",
        body: JSON.stringify({ senhaAtual, novaSenha }),
        sessao
      });
      setOkSenha("Senha alterada com sucesso.");
      setSenhaAtual("");
      setNovaSenha("");
      setConfirmar("");
    } catch (e) {
      setErroSenha(e instanceof Error ? e.message : "Erro ao trocar senha.");
    } finally {
      setLoadingSenha(false);
    }
  }

  function atualizarTemaCarrossel(tone: DocTabsTone) {
    setDocTabsTone(tone);
    applyDocTabsTone(tone);
    persistDocTabsTone(tone);
    setOkTema("Preferência visual salva.");
  }

  return (
    <section className="page page--modern">
      <h2>Minha conta</h2>
      <p className="muted-react">
        {sessao.usuarioEmail} · <span className="perfil-badge">{PERFIL_LABEL[sessao.perfil] ?? sessao.perfil}</span>
      </p>

      <div className="minha-conta-grid">
        <div className="minha-conta-card">
          <h3>Dados pessoais</h3>
          <form onSubmit={salvarNome}>
            <label>
              Nome
              <input value={nome} onChange={e => setNome(e.target.value)} required />
            </label>
            <label>
              E-mail
              <input value={sessao.usuarioEmail} disabled />
            </label>
            <button type="submit" disabled={loadingNome}>
              {loadingNome ? "Salvando..." : "Salvar nome"}
            </button>
            {erroNome ? <p className="erro">{erroNome}</p> : null}
            {okNome ? <p className="ok">{okNome}</p> : null}
          </form>
        </div>

        <div className="minha-conta-card">
          <h3>Alterar senha</h3>
          <form onSubmit={trocarSenha}>
            <label>
              Senha atual
              <input type="password" value={senhaAtual} onChange={e => setSenhaAtual(e.target.value)} required autoComplete="current-password" />
            </label>
            <label>
              Nova senha
              <input type="password" value={novaSenha} onChange={e => setNovaSenha(e.target.value)} required autoComplete="new-password" minLength={6} />
            </label>
            <label>
              Confirmar nova senha
              <input type="password" value={confirmar} onChange={e => setConfirmar(e.target.value)} required autoComplete="new-password" />
            </label>
            <button type="submit" disabled={loadingSenha}>
              {loadingSenha ? "Salvando..." : "Alterar senha"}
            </button>
            {erroSenha ? <p className="erro">{erroSenha}</p> : null}
            {okSenha ? <p className="ok">{okSenha}</p> : null}
          </form>
        </div>
        <div className="minha-conta-card">
          <h3>Preferências visuais</h3>
          <p className="muted-react">Tema de cores das categorias (IA).</p>
          <div className="docs-validados-tone-switch-react" role="group" aria-label="Intensidade das cores do carrossel">
            <span className="docs-validados-tone-switch-react__label">Tema das categorias</span>
            <button
              type="button"
              className={`docs-validados-tone-switch-react__btn ${docTabsTone === "suave" ? "is-active" : ""}`}
              aria-pressed={docTabsTone === "suave"}
              onClick={() => atualizarTemaCarrossel("suave")}
            >
              Suave
            </button>
            <button
              type="button"
              className={`docs-validados-tone-switch-react__btn ${docTabsTone === "vibrante" ? "is-active" : ""}`}
              aria-pressed={docTabsTone === "vibrante"}
              onClick={() => atualizarTemaCarrossel("vibrante")}
            >
              Vibrante
            </button>
          </div>
          {okTema ? <p className="ok">{okTema}</p> : null}
        </div>
      </div>
    </section>
  );
}
