import { FormEvent, useDeferredValue, useEffect, useMemo, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type Empresa = {
  id: number;
  cnpj: string;
  razaoSocial: string;
  cpfResponsavel: string | null;
  mei: boolean;
  vencimentoDas: string | null;
  vencimentoCertificadoMei: string | null;
  ativo?: boolean;
};

type EmpresaPayload = {
  cnpj: string;
  razaoSocial: string;
  cpfResponsavel: string | null;
  mei: boolean;
  vencimentoDas: string | null;
  vencimentoCertificadoMei: string | null;
};

type EmpresaFormState = {
  cnpj: string;
  razaoSocial: string;
  cpfResponsavel: string;
  mei: boolean;
  vencimentoDas: string;
  vencimentoCertificadoMei: string;
};

const initialForm: EmpresaFormState = {
  cnpj: "",
  razaoSocial: "",
  cpfResponsavel: "",
  mei: false,
  vencimentoDas: "",
  vencimentoCertificadoMei: ""
};

function somenteDigitos(value: string): string {
  return value.replace(/\D/g, "");
}

function formatarCnpj(value: string): string {
  const digits = somenteDigitos(value).slice(0, 14);
  return digits
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function formatarCpf(value: string): string {
  const digits = somenteDigitos(value).slice(0, 11);
  return digits
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function mascararCnpjLista(value: string): string {
  const digits = somenteDigitos(value);
  if (digits.length !== 14) return formatarCnpj(digits);
  return `${digits.slice(0, 2)}.***.***/${digits.slice(8, 12)}-${digits.slice(12)}`;
}

function empresaAtiva(empresa: Empresa): boolean {
  return empresa.ativo !== false;
}

export function EmpresasPage({ sessao }: { sessao: Sessao }) {
  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [busca, setBusca] = useState("");
  const [incluirInativas, setIncluirInativas] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [form, setForm] = useState<EmpresaFormState>(initialForm);

  async function carregarEmpresas() {
    setCarregando(true);
    setErro("");
    try {
      const qs = incluirInativas ? "?incluirInativas=true" : "";
      const data = await apiFetchJson<Empresa[]>(`/api/empresas${qs}`, { sessao });
      setEmpresas(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao carregar empresas.");
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    void carregarEmpresas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [incluirInativas]);

  const buscaDeferida = useDeferredValue(busca);
  const filtradas = useMemo(() => {
    const termo = buscaDeferida.trim().toLowerCase();
    if (!termo) return empresas;
    return empresas.filter((empresa) => {
      const nome = (empresa.razaoSocial || "").toLowerCase();
      const cnpj = somenteDigitos(empresa.cnpj || "");
      return nome.includes(termo) || cnpj.includes(somenteDigitos(termo));
    });
  }, [buscaDeferida, empresas]);
  const buscaEmTransicao = busca !== buscaDeferida;

  function limparEdicao() {
    setEditandoId(null);
    setForm(initialForm);
  }

  function iniciarEdicao(empresa: Empresa) {
    if (!empresaAtiva(empresa)) {
      setErro("Esta empresa está inativa. Reative antes de editar.");
      return;
    }
    setErro("");
    setEditandoId(empresa.id);
    setForm({
      cnpj: formatarCnpj(empresa.cnpj || ""),
      razaoSocial: empresa.razaoSocial || "",
      cpfResponsavel: formatarCpf(empresa.cpfResponsavel || ""),
      mei: !!empresa.mei,
      vencimentoDas: empresa.vencimentoDas || "",
      vencimentoCertificadoMei: empresa.vencimentoCertificadoMei || ""
    });
  }

  function payloadFromForm(): EmpresaPayload {
    return {
      cnpj: somenteDigitos(form.cnpj),
      razaoSocial: form.razaoSocial.trim(),
      cpfResponsavel: somenteDigitos(form.cpfResponsavel) || null,
      mei: form.mei,
      vencimentoDas: form.vencimentoDas || null,
      vencimentoCertificadoMei: form.vencimentoCertificadoMei || null
    };
  }

  async function onSubmit(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    setErro("");
    setOk("");
    try {
      const payload = payloadFromForm();
      if (editandoId != null) {
        await apiFetchJson(`/api/empresas/${editandoId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
          sessao
        });
        setOk("Empresa atualizada.");
        limparEdicao();
      } else {
        await apiFetchJson("/api/empresas", {
          method: "POST",
          body: JSON.stringify(payload),
          sessao
        });
        setOk("Empresa cadastrada.");
        setForm(initialForm);
      }
      await carregarEmpresas();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar empresa.");
    }
  }

  async function desativarEmpresa(id: number) {
    if (!window.confirm("A empresa será desativada. Deseja continuar?")) return;
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/empresas/${id}`, { method: "DELETE", sessao });
      if (editandoId === id) limparEdicao();
      setOk("Empresa desativada.");
      await carregarEmpresas();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível desativar.");
    }
  }

  async function reativarEmpresa(id: number) {
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/empresas/${id}/reativar`, { method: "POST", sessao });
      setOk("Empresa reativada.");
      await carregarEmpresas();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível reativar.");
    }
  }

  return (
    <section className="page page--modern">
      <h2>Empresas</h2>
      <p className="muted-react">Cadastro e gestão das empresas do escritório.</p>

      <div className="empresas-react-layout">
        <form className="empresas-react-form" onSubmit={onSubmit}>
          <h3>{editandoId ? "Editar empresa" : "Novo cadastro"}</h3>
          <label>
            CNPJ
            <input
              value={form.cnpj}
              onChange={(e) => setForm((s) => ({ ...s, cnpj: formatarCnpj(e.target.value) }))}
              required
            />
          </label>
          <label>
            Razão social
            <input
              value={form.razaoSocial}
              onChange={(e) => setForm((s) => ({ ...s, razaoSocial: e.target.value }))}
              required
            />
          </label>
          <label>
            CPF responsável
            <input
              value={form.cpfResponsavel}
              onChange={(e) => setForm((s) => ({ ...s, cpfResponsavel: formatarCpf(e.target.value) }))}
            />
          </label>
          <label className="check-react">
            <input
              type="checkbox"
              checked={form.mei}
              onChange={(e) => setForm((s) => ({ ...s, mei: e.target.checked }))}
            />
            Empresa MEI
          </label>
          <label>
            Vencimento DAS
            <input
              type="date"
              value={form.vencimentoDas}
              onChange={(e) => setForm((s) => ({ ...s, vencimentoDas: e.target.value }))}
            />
          </label>
          <label>
            Vencimento certificado MEI
            <input
              type="date"
              value={form.vencimentoCertificadoMei}
              onChange={(e) => setForm((s) => ({ ...s, vencimentoCertificadoMei: e.target.value }))}
            />
          </label>

          <div className="empresas-react-actions">
            <button type="submit">{editandoId ? "Salvar alterações" : "Cadastrar empresa"}</button>
            {editandoId != null ? (
              <>
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    limparEdicao();
                    setErro("");
                  }}
                >
                  Cancelar
                </button>
                <button type="button" className="danger" onClick={() => desativarEmpresa(editandoId)}>
                  Desativar
                </button>
              </>
            ) : null}
          </div>
          {erro ? <p className="erro">{erro}</p> : null}
          {ok ? <p className="ok">{ok}</p> : null}
        </form>

        <div className="empresas-react-list">
          <div className="empresas-react-list-top">
            <h3>Empresas cadastradas</h3>
            <button type="button" className="ghost" onClick={() => void carregarEmpresas()}>
              Atualizar
            </button>
          </div>
          <label>
            Buscar
            <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Razão social ou CNPJ" />
          </label>
          {buscaEmTransicao ? (
            <p className="muted-react small" aria-live="polite">
              Ajustando resultados…
            </p>
          ) : null}
          <label className="check-react">
            <input
              type="checkbox"
              checked={incluirInativas}
              onChange={(e) => setIncluirInativas(e.target.checked)}
            />
            Mostrar empresas inativas
          </label>
          {carregando ? <p className="muted-react">Carregando...</p> : null}
          {!carregando && filtradas.length === 0 ? <p className="muted-react">Nenhuma empresa encontrada.</p> : null}

          {filtradas.length > 0 ? (
            <table className="empresas-react-table">
              <thead>
                <tr>
                  <th>Razão social</th>
                  <th>CNPJ</th>
                  <th>MEI</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtradas.map((empresa) => {
                  const ativa = empresaAtiva(empresa);
                  return (
                    <tr key={empresa.id}>
                      <td>
                        {empresa.razaoSocial} {!ativa ? <span className="tag-inativa">Inativa</span> : null}
                      </td>
                      <td title={formatarCnpj(empresa.cnpj)}>{mascararCnpjLista(empresa.cnpj)}</td>
                      <td>{empresa.mei ? "Sim" : "Não"}</td>
                      <td>
                        {ativa ? (
                          <>
                            <button type="button" className="small" onClick={() => iniciarEdicao(empresa)}>
                              Editar
                            </button>
                            <button type="button" className="small danger" onClick={() => desativarEmpresa(empresa.id)}>
                              Desativar
                            </button>
                          </>
                        ) : (
                          <button type="button" className="small" onClick={() => void reativarEmpresa(empresa.id)}>
                            Reativar
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          ) : null}
        </div>
      </div>
    </section>
  );
}
