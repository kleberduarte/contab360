import { FormEvent, useCallback, useDeferredValue, useEffect, useMemo, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";
import { ConfirmDialog } from "../../components/ConfirmDialog";

type ClientePf = {
  id: number;
  cpf: string;
  nomeCompleto: string;
  ativo?: boolean;
};

type FormState = {
  cpf: string;
  nomeCompleto: string;
};

const initialForm: FormState = { cpf: "", nomeCompleto: "" };

function somenteDigitos(value: string): string {
  return value.replace(/\D/g, "");
}

function formatarCpf(value: string): string {
  const digits = somenteDigitos(value).slice(0, 11);
  return digits
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function mascararCpfLista(value: string): string {
  const digits = somenteDigitos(value);
  if (digits.length !== 11) return formatarCpf(digits);
  return `***.***.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

function ativo(c: ClientePf): boolean {
  return c.ativo !== false;
}

export function ClientesPfPage({ sessao }: { sessao: Sessao }) {
  const [lista, setLista] = useState<ClientePf[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [busca, setBusca] = useState("");
  const [incluirInativas, setIncluirInativas] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro("");
    try {
      const qs = incluirInativas ? "?incluirInativas=true" : "";
      const data = await apiFetchJson<ClientePf[]>(`/api/clientes-pessoa-fisica${qs}`, { sessao });
      setLista(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao carregar cadastros PF.");
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    void carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [incluirInativas]);

  const buscaDeferida = useDeferredValue(busca);
  const filtradas = useMemo(() => {
    const termo = buscaDeferida.trim().toLowerCase();
    if (!termo) return lista;
    return lista.filter((c) => {
      const nome = (c.nomeCompleto || "").toLowerCase();
      const cpf = somenteDigitos(c.cpf || "");
      return nome.includes(termo) || cpf.includes(somenteDigitos(termo));
    });
  }, [buscaDeferida, lista]);

  function limparEdicao() {
    setEditandoId(null);
    setForm(initialForm);
  }

  function iniciarEdicao(c: ClientePf) {
    if (!ativo(c)) {
      setErro("Este cadastro está inativo. Reative antes de editar.");
      return;
    }
    setErro("");
    setEditandoId(c.id);
    setForm({
      cpf: formatarCpf(c.cpf || ""),
      nomeCompleto: c.nomeCompleto || ""
    });
  }

  async function onSubmit(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    setErro("");
    setOk("");
    const cpf = somenteDigitos(form.cpf);
    if (cpf.length !== 11) {
      setErro("CPF deve ter 11 dígitos.");
      return;
    }
    try {
      const body = { cpf, nomeCompleto: form.nomeCompleto.trim() };
      if (editandoId != null) {
        await apiFetchJson(`/api/clientes-pessoa-fisica/${editandoId}`, {
          method: "PUT",
          body: JSON.stringify(body),
          sessao
        });
        setOk("Cadastro atualizado.");
        limparEdicao();
      } else {
        await apiFetchJson("/api/clientes-pessoa-fisica", {
          method: "POST",
          body: JSON.stringify(body),
          sessao
        });
        setOk("Cadastro criado.");
        setForm(initialForm);
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar.");
    }
  }

  function desativar(id: number) {
    setConfirmId(id);
  }

  const confirmarDesativar = useCallback(async () => {
    const id = confirmId;
    setConfirmId(null);
    if (id == null) return;
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/clientes-pessoa-fisica/${id}`, { method: "DELETE", sessao });
      if (editandoId === id) limparEdicao();
      setOk("Cadastro desativado.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível desativar.");
    }
  }, [confirmId, editandoId, sessao]); // eslint-disable-line react-hooks/exhaustive-deps

  async function reativar(id: number) {
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/clientes-pessoa-fisica/${id}/reativar`, { method: "POST", sessao });
      setOk("Cadastro reativado.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível reativar.");
    }
  }

  return (
    <>
      <ConfirmDialog
        open={confirmId != null}
        title="Desativar cadastro"
        message="O cadastro será desativado e ficará oculto da listagem padrão. Você pode reativá-lo depois."
        confirmLabel="Desativar"
        cancelLabel="Cancelar"
        danger
        onConfirm={() => void confirmarDesativar()}
        onCancel={() => setConfirmId(null)}
      />
    <section className="page page--modern">
      <h2>Clientes pessoa física</h2>
      <p className="muted-react">Cadastro para IRPF e documentos sem CNPJ (guias em CPF, declarações etc.).</p>

      <div className="empresas-react-layout">
        <form className="empresas-react-form" onSubmit={onSubmit}>
          <h3>{editandoId ? "Editar cadastro" : "Novo cadastro"}</h3>
          <label>
            CPF
            <input
              value={form.cpf}
              onChange={(e) => setForm((s) => ({ ...s, cpf: formatarCpf(e.target.value) }))}
              required
            />
          </label>
          <label>
            Nome completo
            <input
              value={form.nomeCompleto}
              onChange={(e) => setForm((s) => ({ ...s, nomeCompleto: e.target.value }))}
              required
            />
          </label>
          <div className="empresas-react-actions">
            <button type="submit">{editandoId ? "Salvar alterações" : "Cadastrar"}</button>
            {editandoId ? (
              <button type="button" className="ghost" onClick={limparEdicao}>
                Cancelar edição
              </button>
            ) : null}
          </div>
        </form>

        <div className="empresas-react-list">
          <div className="empresas-react-list-top">
            <label className="empresas-react-busca">
              <span className="muted-react">Buscar</span>
              <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome ou CPF" />
            </label>
            <label className="check-react">
              <input
                type="checkbox"
                checked={incluirInativas}
                onChange={(e) => setIncluirInativas(e.target.checked)}
              />
              Mostrar inativos
            </label>
          </div>
          {ok ? <p className="ok">{ok}</p> : null}
          {erro ? <p className="erro">{erro}</p> : null}
          {carregando ? <p className="muted-react">Carregando…</p> : null}
          {!carregando && filtradas.length === 0 ? <p className="muted-react">Nenhum cadastro encontrado.</p> : null}
          <table className="empresas-react-table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>CPF</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filtradas.map((c) => {
                const a = ativo(c);
                return (
                  <tr key={c.id}>
                    <td>
                      {c.nomeCompleto} {!a ? <span className="tag-inativa">Inativo</span> : null}
                    </td>
                    <td>{mascararCpfLista(c.cpf)}</td>
                    <td>{a ? "Ativo" : "Inativo"}</td>
                    <td className="empresas-react-table-actions">
                      {a ? (
                        <>
                          <button type="button" className="small" onClick={() => iniciarEdicao(c)}>
                            Editar
                          </button>
                          <button type="button" className="small danger" onClick={() => void desativar(c.id)}>
                            Desativar
                          </button>
                        </>
                      ) : (
                        <button type="button" className="small" onClick={() => void reativar(c.id)}>
                          Reativar
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </section>
    </>
  );
}
