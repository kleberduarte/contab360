import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type Empresa = {
  id: number;
  cnpj: string;
  razaoSocial: string;
  ativo?: boolean;
};

type TemplateRow = {
  id: number;
  empresaId: number;
  nome: string;
  obrigatorio: boolean;
};

function formatarCnpj(value: string): string {
  const d = (value || "").replace(/\D/g, "").slice(0, 14);
  return d
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function empresaAtiva(e: Empresa): boolean {
  return e.ativo !== false;
}

export function TemplatesPage({ sessao }: { sessao: Sessao }) {
  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [empresaId, setEmpresaId] = useState("");
  const [nome, setNome] = useState("");
  const [obrigatorio, setObrigatorio] = useState(true);
  const [lista, setLista] = useState<TemplateRow[]>([]);
  const [loadingEmpresas, setLoadingEmpresas] = useState(true);
  const [loadingLista, setLoadingLista] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");

  async function carregarEmpresas() {
    setLoadingEmpresas(true);
    setErro("");
    try {
      const data = await apiFetchJson<Empresa[]>("/api/empresas", { sessao });
      setEmpresas(data.filter(empresaAtiva));
      if (data.filter(empresaAtiva).length && !empresaId) {
        setEmpresaId(String(data.filter(empresaAtiva)[0].id));
      }
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar empresas.");
      setEmpresas([]);
    } finally {
      setLoadingEmpresas(false);
    }
  }

  async function carregarTemplates(empId: string) {
    if (!empId) {
      setLista([]);
      return;
    }
    setLoadingLista(true);
    try {
      const data = await apiFetchJson<TemplateRow[]>(`/api/templates-documentos?empresaId=${empId}`, { sessao });
      setLista(data);
    } catch (e) {
      setLista([]);
      setErro(e instanceof Error ? e.message : "Erro ao listar templates.");
    } finally {
      setLoadingLista(false);
    }
  }

  useEffect(() => {
    void carregarEmpresas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (empresaId) void carregarTemplates(empresaId);
  }, [empresaId, sessao]);

  async function onSubmit(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setOk("");
    try {
      await apiFetchJson("/api/templates-documentos", {
        method: "POST",
        body: JSON.stringify({
          empresaId: Number(empresaId),
          nome: nome.trim(),
          obrigatorio
        }),
        sessao
      });
      setOk("Template adicionado.");
      setNome("");
      setObrigatorio(true);
      await carregarTemplates(empresaId);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao adicionar template.");
    }
  }

  return (
    <section className="page">
      <h2>Template de documentos</h2>
      <p className="muted-react">Defina documentos obrigatórios ou opcionais por empresa.</p>

      <form className="templates-react-form" onSubmit={onSubmit}>
        <label>
          Empresa
          <select
            value={empresaId}
            onChange={(e) => setEmpresaId(e.target.value)}
            required
            disabled={loadingEmpresas}
          >
            <option value="">— Selecione —</option>
            {empresas.map((e) => (
              <option key={e.id} value={e.id}>
                {e.razaoSocial} ({formatarCnpj(e.cnpj)})
              </option>
            ))}
          </select>
        </label>
        <label>
          Documento
          <input
            type="text"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            placeholder="Ex.: Nota fiscal"
            required
          />
        </label>
        <label className="check-react">
          <input type="checkbox" checked={obrigatorio} onChange={(e) => setObrigatorio(e.target.checked)} />
          Obrigatório
        </label>
        <button type="submit">Adicionar template</button>
        {erro ? <p className="erro">{erro}</p> : null}
        {ok ? <p className="ok">{ok}</p> : null}
      </form>

      <div className="templates-react-lista">
        <h3>Templates da empresa selecionada</h3>
        {loadingLista ? <p className="muted-react">Carregando lista...</p> : null}
        {!loadingLista && empresaId && lista.length === 0 ? (
          <p className="muted-react">Nenhum template para esta empresa ainda.</p>
        ) : null}
        {lista.length > 0 ? (
          <table className="templates-react-table">
            <thead>
              <tr>
                <th>Documento</th>
                <th>Obrigatório</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((t) => (
                <tr key={t.id}>
                  <td>{t.nome}</td>
                  <td>{t.obrigatorio ? "Sim" : "Não"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}
