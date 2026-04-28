import { FormEvent, useCallback, useEffect, useDeferredValue, useMemo, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";
import { ConfirmDialog } from "../../components/ConfirmDialog";

type Empresa = { id: number; razaoSocial: string };
type Usuario = {
  id: number;
  nome: string;
  email: string;
  perfil: string;
  empresaId: number | null;
  empresaNome: string | null;
  ativo: boolean;
};
type UsuarioResponse = Usuario & { senhaTempRevelada?: string | null };
type FormState = { nome: string; email: string; perfil: string; empresaId: string };

const PERFIS_ADM = ["ADM", "CONTADOR", "CLIENTE"];
const PERFIS_CONTADOR = ["CLIENTE"];
const PERFIL_LABEL: Record<string, string> = { ADM: "Administrador", CONTADOR: "Contador", CLIENTE: "Cliente" };

const initialForm: FormState = { nome: "", email: "", perfil: "CLIENTE", empresaId: "" };

export function UsuariosPage({ sessao }: { sessao: Sessao }) {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [busca, setBusca] = useState("");
  const [mostrarInativos, setMostrarInativos] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [senhaTempRevelada, setSenhaTempRevelada] = useState<string | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [resetSenhaId, setResetSenhaId] = useState<number | null>(null);

  const isContador = sessao.perfil === "CONTADOR";
  const perfisDisponiveis = sessao.perfil === "ADM" ? PERFIS_ADM : PERFIS_CONTADOR;

  async function carregar() {
    setCarregando(true);
    setErro("");
    try {
      const [us, es] = await Promise.all([
        apiFetchJson<Usuario[]>("/api/usuarios", { sessao }),
        apiFetchJson<Empresa[]>("/api/empresas", { sessao })
      ]);
      setUsuarios(us);
      setEmpresas(es);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar.");
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => { void carregar(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const buscaDef = useDeferredValue(busca);
  const filtrados = useMemo(() => {
    const t = buscaDef.trim().toLowerCase();
    return usuarios.filter(u => {
      if (isContador && u.perfil !== "CLIENTE") return false;
      if (!mostrarInativos && !u.ativo) return false;
      if (!t) return true;
      return u.nome.toLowerCase().includes(t) || u.email.toLowerCase().includes(t);
    });
  }, [usuarios, buscaDef, mostrarInativos, isContador]);

  function limparEdicao() {
    setEditandoId(null);
    setForm(initialForm);
    setSenhaTempRevelada(null);
    setErro("");
  }

  function iniciarEdicao(u: Usuario) {
    if (isContador && u.perfil !== "CLIENTE") {
      setErro("Contador só pode visualizar/editar usuários do perfil Cliente.");
      return;
    }
    setErro("");
    setOk("");
    setSenhaTempRevelada(null);
    setEditandoId(u.id);
    setForm({
      nome: u.nome,
      email: u.email,
      perfil: isContador ? "CLIENTE" : u.perfil,
      empresaId: u.empresaId != null ? String(u.empresaId) : ""
    });
  }

  async function onSubmit(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setOk("");
    setSenhaTempRevelada(null);
    const payload = {
      nome: form.nome,
      email: form.email,
      perfil: form.perfil,
      empresaId: form.empresaId ? Number(form.empresaId) : null
    };
    try {
      if (editandoId != null) {
        await apiFetchJson(`/api/usuarios/${editandoId}`, { method: "PUT", body: JSON.stringify(payload), sessao });
        setOk("Usuário atualizado.");
        limparEdicao();
      } else {
        const resp = await apiFetchJson<UsuarioResponse>("/api/usuarios", { method: "POST", body: JSON.stringify(payload), sessao });
        if (resp.senhaTempRevelada) setSenhaTempRevelada(resp.senhaTempRevelada);
        setForm(initialForm);
        setOk("Usuário criado com sucesso.");
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar.");
    }
  }

  function desativar(id: number) {
    setConfirmId(id);
  }

  const confirmarResetSenha = useCallback(async () => {
    const id = resetSenhaId;
    setResetSenhaId(null);
    if (id == null) return;
    setErro("");
    setOk("");
    try {
      const resp = await apiFetchJson<UsuarioResponse>(`/api/usuarios/${id}/redefinir-senha`, { method: "POST", sessao });
      if (resp.senhaTempRevelada) setSenhaTempRevelada(resp.senhaTempRevelada);
      setOk("Senha redefinida com sucesso.");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível redefinir a senha.");
    }
  }, [resetSenhaId, sessao]); // eslint-disable-line react-hooks/exhaustive-deps

  const confirmarDesativar = useCallback(async () => {
    const id = confirmId;
    setConfirmId(null);
    if (id == null) return;
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/usuarios/${id}`, { method: "DELETE", sessao });
      if (editandoId === id) limparEdicao();
      setOk("Usuário desativado.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível desativar.");
    }
  }, [confirmId, editandoId, sessao]); // eslint-disable-line react-hooks/exhaustive-deps

  async function reativar(id: number) {
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/usuarios/${id}/reativar`, { method: "POST", sessao });
      setOk("Usuário reativado.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível reativar.");
    }
  }

  return (
    <>
    <ConfirmDialog
      open={resetSenhaId != null}
      title="Redefinir senha"
      message="Uma nova senha temporária será gerada. O usuário precisará alterá-la em 'Minha conta' após o próximo acesso."
      confirmLabel="Redefinir"
      cancelLabel="Cancelar"
      onConfirm={() => void confirmarResetSenha()}
      onCancel={() => setResetSenhaId(null)}
    />
    <ConfirmDialog
      open={confirmId != null}
      title="Desativar usuário"
      message="O usuário será desativado e não poderá mais acessar o sistema. Você pode reativá-lo depois."
      confirmLabel="Desativar"
      cancelLabel="Cancelar"
      danger
      onConfirm={() => void confirmarDesativar()}
      onCancel={() => setConfirmId(null)}
    />
    <section className="page page--modern">
      <h2>Usuários</h2>
      <p className="muted-react">Gerencie os acessos ao sistema.</p>

      {senhaTempRevelada ? (
        <div className="senha-temp-box">
          <strong>{editandoId ? "Senha redefinida!" : "Usuário criado!"}</strong> Compartilhe a senha temporária com o usuário:
          <code className="senha-temp-code">{senhaTempRevelada}</code>
          <p className="muted-react small">Esta senha não será exibida novamente. O usuário deve alterá-la em "Minha conta".</p>
          <button type="button" className="ghost small" onClick={() => setSenhaTempRevelada(null)}>Fechar</button>
        </div>
      ) : null}

      <div className="usuarios-react-layout">
        <form className="usuarios-react-form" onSubmit={onSubmit}>
          <h3>{editandoId ? "Editar usuário" : "Novo usuário"}</h3>

          <label>
            Nome
            <input value={form.nome} onChange={e => setForm(s => ({ ...s, nome: e.target.value }))} required />
          </label>
          <label>
            E-mail
            <input type="email" value={form.email} onChange={e => setForm(s => ({ ...s, email: e.target.value }))} required />
          </label>
          <label>
            Perfil
            <select value={form.perfil} onChange={e => setForm(s => ({ ...s, perfil: e.target.value }))} required>
              {perfisDisponiveis.map(p => (
                <option key={p} value={p}>{PERFIL_LABEL[p] ?? p}</option>
              ))}
            </select>
          </label>
          <label>
            Empresa (opcional)
            <select value={form.empresaId} onChange={e => setForm(s => ({ ...s, empresaId: e.target.value }))}>
              <option value="">— Nenhuma —</option>
              {empresas.map(e => (
                <option key={e.id} value={e.id}>{e.razaoSocial}</option>
              ))}
            </select>
          </label>

          <div className="usuarios-react-actions">
            <button type="submit">{editandoId ? "Salvar alterações" : "Criar usuário"}</button>
            {editandoId != null ? (
              <>
                <button type="button" className="ghost" onClick={limparEdicao}>Cancelar</button>
                <button type="button" className="ghost" onClick={() => setResetSenhaId(editandoId)}>Redefinir senha</button>
                <button type="button" className="danger" onClick={() => desativar(editandoId)}>Desativar</button>
              </>
            ) : null}
          </div>
          {erro ? <p className="erro">{erro}</p> : null}
          {ok && !senhaTempRevelada ? <p className="ok">{ok}</p> : null}
        </form>

        <div className="usuarios-react-list">
          <div className="usuarios-react-list-top">
            <h3>Usuários cadastrados</h3>
            <button type="button" className="ghost" onClick={() => void carregar()}>Atualizar</button>
          </div>
          <label>
            Buscar
            <input value={busca} onChange={e => setBusca(e.target.value)} placeholder="Nome ou e-mail" />
          </label>
          <label className="check-react">
            <input type="checkbox" checked={mostrarInativos} onChange={e => setMostrarInativos(e.target.checked)} />
            Mostrar inativos
          </label>
          {carregando ? <p className="muted-react">Carregando...</p> : null}
          {!carregando && filtrados.length === 0 ? <p className="muted-react">Nenhum usuário encontrado.</p> : null}
          {filtrados.length > 0 ? (
            <table className="usuarios-react-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Perfil</th>
                  <th>Empresa</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtrados.map(u => (
                  <tr key={u.id} className={!u.ativo ? "row-inativo" : ""}>
                    <td>
                      <span>{u.nome}</span>
                      <span className="usuarios-email">{u.email}</span>
                      {!u.ativo ? <span className="tag-inativa">Inativo</span> : null}
                    </td>
                    <td>{PERFIL_LABEL[u.perfil] ?? u.perfil}</td>
                    <td>{u.empresaNome ?? <span className="muted-react">—</span>}</td>
                    <td>
                      {u.ativo ? (
                        <>
                          <button type="button" className="small" onClick={() => iniciarEdicao(u)}>Editar</button>
                          {u.id !== Number(sessao.usuarioEmail) ? (
                            <button type="button" className="small danger" onClick={() => desativar(u.id)}>Desativar</button>
                          ) : null}
                        </>
                      ) : (
                        <button type="button" className="small" onClick={() => void reativar(u.id)}>Reativar</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}
        </div>
      </div>
    </section>
    </>
  );
}
