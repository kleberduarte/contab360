import { FormEvent, useCallback, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { formatApiError } from "../../lib/errors";
import { Sessao } from "../../lib/session";
import { ConfirmDialog } from "../../components/ConfirmDialog";

type Empresa = {
  id: number;
  cnpj: string;
  razaoSocial: string;
  ativo?: boolean;
};

type ClientePf = {
  id: number;
  cpf: string;
  nomeCompleto: string;
  ativo?: boolean;
};

type TemplateRow = {
  id: number;
  empresaId: number | null;
  clientePessoaFisicaId: number | null;
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

function formatarCpf(value: string): string {
  const d = (value || "").replace(/\D/g, "").slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function empresaAtiva(e: Empresa): boolean {
  return e.ativo !== false;
}

function pfAtivo(c: ClientePf): boolean {
  return c.ativo !== false;
}

export function TemplatesPage({ sessao }: { sessao: Sessao }) {
  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [clientesPf, setClientesPf] = useState<ClientePf[]>([]);
  const [tomadorTipo, setTomadorTipo] = useState<"PJ" | "PF">("PJ");
  const [tomadorId, setTomadorId] = useState("");
  const [nome, setNome] = useState("");
  const [sugestoesTemplates, setSugestoesTemplates] = useState<string[]>([]);
  const [obrigatorio, setObrigatorio] = useState(true);
  const [lista, setLista] = useState<TemplateRow[]>([]);
  const [loadingEmpresas, setLoadingEmpresas] = useState(true);
  const [loadingLista, setLoadingLista] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [editNome, setEditNome] = useState("");
  const [editObrigatorio, setEditObrigatorio] = useState(true);
  const [confirmExcluirId, setConfirmExcluirId] = useState<number | null>(null);

  async function carregarEmpresas() {
    setLoadingEmpresas(true);
    setErro("");
    try {
      const data = await apiFetchJson<Empresa[]>("/api/empresas", { sessao });
      setEmpresas(data.filter(empresaAtiva));
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar empresas.");
      setEmpresas([]);
    } finally {
      setLoadingEmpresas(false);
    }
  }

  async function carregarClientesPf() {
    try {
      const data = await apiFetchJson<ClientePf[]>("/api/clientes-pessoa-fisica", { sessao });
      setClientesPf(data.filter(pfAtivo));
    } catch {
      setClientesPf([]);
    }
  }

  async function carregarSugestoesTemplates() {
    try {
      const data = await apiFetchJson<string[]>("/api/templates-documentos/sugestoes", { sessao });
      const opcoes = (data || []).map((item) => (item || "").trim()).filter(Boolean);
      setSugestoesTemplates(opcoes);
      setNome((prev) => {
        if (prev && opcoes.includes(prev)) return prev;
        return opcoes[0] ?? "";
      });
    } catch {
      setSugestoesTemplates([]);
      setNome("");
    }
  }

  async function carregarTemplates(tipo: "PJ" | "PF", id: string) {
    if (!id) {
      setLista([]);
      return;
    }
    setLoadingLista(true);
    try {
      const qs =
        tipo === "PJ"
          ? `empresaId=${encodeURIComponent(id)}`
          : `clientePessoaFisicaId=${encodeURIComponent(id)}`;
      const data = await apiFetchJson<TemplateRow[]>(`/api/templates-documentos?${qs}`, { sessao });
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
    void carregarClientesPf();
    void carregarSugestoesTemplates();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const ativosEj = empresas.filter(empresaAtiva);
    const ativosPf = clientesPf.filter(pfAtivo);
    if (tomadorTipo === "PJ" && ativosEj.length && !tomadorId) {
      setTomadorId(String(ativosEj[0].id));
    }
    if (tomadorTipo === "PF" && ativosPf.length && !tomadorId) {
      setTomadorId(String(ativosPf[0].id));
    }
  }, [empresas, clientesPf, tomadorTipo, tomadorId]);

  useEffect(() => {
    if (tomadorId) void carregarTemplates(tomadorTipo, tomadorId);
  }, [tomadorId, tomadorTipo, sessao]);

  function onTomadorTipoChange(t: "PJ" | "PF") {
    setTomadorTipo(t);
    setTomadorId("");
    setLista([]);
  }

  async function onSubmit(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setOk("");
    if (!tomadorId) {
      setErro("Selecione o tomador (empresa ou pessoa física).");
      return;
    }
    if (!nome.trim()) {
      setErro("Selecione um template da lista.");
      return;
    }
    try {
      const body =
        tomadorTipo === "PJ"
          ? {
              empresaId: Number(tomadorId),
              clientePessoaFisicaId: null,
              nome: nome.trim(),
              obrigatorio
            }
          : {
              empresaId: null,
              clientePessoaFisicaId: Number(tomadorId),
              nome: nome.trim(),
              obrigatorio
            };
      await apiFetchJson("/api/templates-documentos", {
        method: "POST",
        body: JSON.stringify(body),
        sessao
      });
      setOk("Template adicionado.");
      setNome((prev) => (prev && sugestoesTemplates.includes(prev) ? prev : sugestoesTemplates[0] ?? ""));
      setObrigatorio(true);
      await carregarTemplates(tomadorTipo, tomadorId);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao adicionar template.";
      setErro(formatApiError(msg, "templates"));
    }
  }

  function iniciarEdicao(template: TemplateRow) {
    setErro("");
    setOk("");
    setEditandoId(template.id);
    setEditNome(template.nome);
    setEditObrigatorio(template.obrigatorio);
  }

  function cancelarEdicao() {
    setEditandoId(null);
    setEditNome("");
    setEditObrigatorio(true);
  }

  async function salvarEdicao() {
    if (editandoId == null) return;
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/templates-documentos/${editandoId}`, {
        method: "PUT",
        body: JSON.stringify({
          nome: editNome.trim(),
          obrigatorio: editObrigatorio
        }),
        sessao
      });
      setOk("Template atualizado.");
      cancelarEdicao();
      await carregarTemplates(tomadorTipo, tomadorId);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao atualizar template.";
      setErro(formatApiError(msg, "templates"));
    }
  }

  const confirmarExclusaoTemplate = useCallback(async () => {
    const id = confirmExcluirId;
    setConfirmExcluirId(null);
    if (id == null) return;
    setErro("");
    setOk("");
    try {
      await apiFetchJson(`/api/templates-documentos/${id}`, {
        method: "DELETE",
        sessao
      });
      if (editandoId === id) cancelarEdicao();
      setOk("Template excluído.");
      await carregarTemplates(tomadorTipo, tomadorId);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao excluir template.";
      setErro(formatApiError(msg, "templates"));
    }
  }, [confirmExcluirId, editandoId, tomadorTipo, tomadorId, sessao]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
    <ConfirmDialog
      open={confirmExcluirId != null}
      title="Excluir template"
      message="Deseja excluir este template?"
      confirmLabel="Excluir"
      cancelLabel="Cancelar"
      danger
      onConfirm={() => void confirmarExclusaoTemplate()}
      onCancel={() => setConfirmExcluirId(null)}
    />
    <section className="page">
      <h2>Template de documentos</h2>
      <p className="muted-react">Defina documentos por empresa (CNPJ) ou por cliente pessoa física (CPF).</p>

      <form className="templates-react-form" onSubmit={onSubmit}>
        <label>
          Tipo de tomador
          <select
            value={tomadorTipo}
            onChange={(e) => onTomadorTipoChange(e.target.value === "PF" ? "PF" : "PJ")}
            disabled={loadingEmpresas}
          >
            <option value="PJ">Empresa (CNPJ)</option>
            <option value="PF">Pessoa física (CPF)</option>
          </select>
        </label>
        {tomadorTipo === "PJ" ? (
          <label>
            Empresa
            <select
              value={tomadorId}
              onChange={(e) => setTomadorId(e.target.value)}
              required
              disabled={loadingEmpresas}
            >
              <option value="">— Selecione —</option>
              {empresas.filter(empresaAtiva).map((e) => (
                <option key={e.id} value={e.id}>
                  {e.razaoSocial} ({formatarCnpj(e.cnpj)})
                </option>
              ))}
            </select>
          </label>
        ) : (
          <label>
            Pessoa física
            <select
              value={tomadorId}
              onChange={(e) => setTomadorId(e.target.value)}
              required
              disabled={loadingEmpresas}
            >
              <option value="">— Selecione —</option>
              {clientesPf.filter(pfAtivo).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nomeCompleto} ({formatarCpf(c.cpf)})
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          Template de documento (carrossel IA)
          <select
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            required
          >
            <option value="">— Selecione —</option>
            {sugestoesTemplates.map((opcao) => (
              <option key={opcao} value={opcao}>
                {opcao}
              </option>
            ))}
          </select>
        </label>
        <label className="check-react">
          <input type="checkbox" checked={obrigatorio} onChange={(e) => setObrigatorio(e.target.checked)} />
          Obrigatório
        </label>
        <button type="submit">Adicionar template</button>
        {erro ? (
          <p className="erro" role="alert" aria-live="assertive">
            {erro}
          </p>
        ) : null}
        {ok ? <p className="ok">{ok}</p> : null}
      </form>

      <div className="templates-react-lista">
        <h3>Templates do tomador selecionado</h3>
        {loadingLista ? <p className="muted-react">Carregando lista...</p> : null}
        {!loadingLista && tomadorId && lista.length === 0 ? (
          <p className="muted-react">Nenhum template para este tomador ainda.</p>
        ) : null}
        {lista.length > 0 ? (
          <table className="templates-react-table">
            <thead>
              <tr>
                <th>Documento</th>
                <th>Obrigatório</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((t) => (
                <tr key={t.id}>
                  <td>
                    {editandoId === t.id ? (
                      <input
                        type="text"
                        value={editNome}
                        onChange={(e) => setEditNome(e.target.value)}
                        aria-label="Nome do template"
                      />
                    ) : (
                      t.nome
                    )}
                  </td>
                  <td>
                    {editandoId === t.id ? (
                      <label className="check-react">
                        <input
                          type="checkbox"
                          checked={editObrigatorio}
                          onChange={(e) => setEditObrigatorio(e.target.checked)}
                        />
                        Obrigatório
                      </label>
                    ) : t.obrigatorio ? (
                      "Sim"
                    ) : (
                      "Não"
                    )}
                  </td>
                  <td>
                    {editandoId === t.id ? (
                      <>
                        <button type="button" className="small" onClick={() => void salvarEdicao()}>
                          Salvar
                        </button>
                        <button type="button" className="small ghost" onClick={cancelarEdicao}>
                          Cancelar
                        </button>
                      </>
                    ) : (
                      <>
                        <button type="button" className="small" onClick={() => iniciarEdicao(t)}>
                          Editar
                        </button>
                        <button type="button" className="small danger" onClick={() => setConfirmExcluirId(t.id)}>
                          Excluir
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
    </>
  );
}
