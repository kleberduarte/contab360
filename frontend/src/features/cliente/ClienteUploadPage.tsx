import { FormEvent, useEffect, useRef, useState } from "react";
import { apiFetchFormData, apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type PendenciaOpt = { id: number; templateDocumentoNome: string; status: string };

const LOTE_TIMEOUT_MS = 120_000;
const MAX_ARQUIVOS_LOTE = 25;

type LoteErro = { nomeArquivoOriginal: string; mensagem: string };
type LoteResponse = {
  entregas: { id: number; pendenciaId: number; nomeArquivoOriginal: string; enviadoEm: string; observacao: string | null }[];
  erros: LoteErro[];
  message: string | null;
};

function hojeCompetencia() {
  const d = new Date();
  return { ano: d.getFullYear(), mes: d.getMonth() + 1 };
}

function competenciaAnterior(ano: number, mes: number) {
  if (mes > 1) return { ano, mes: mes - 1 };
  return { ano: ano - 1, mes: 12 };
}

export function ClienteUploadPage({ sessao }: { sessao: Sessao }) {
  const comp = hojeCompetencia();
  const [ano, setAno] = useState(comp.ano);
  const [mes, setMes] = useState(comp.mes);
  const [pendencias, setPendencias] = useState<PendenciaOpt[]>([]);
  const [pendenciaId, setPendenciaId] = useState("");
  const [arquivos, setArquivos] = useState<File[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [observacao, setObservacao] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingPendencias, setLoadingPendencias] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [infoCompetencia, setInfoCompetencia] = useState("");

  async function carregarPendencias(opts?: { sincronizarCompetencia?: boolean }) {
    const sincronizarCompetencia = opts?.sincronizarCompetencia ?? false;
    setLoadingPendencias(true);
    setInfoCompetencia("");
    try {
      let anoBusca = ano;
      let mesBusca = mes;
      let data: PendenciaOpt[] = [];
      for (let tentativa = 0; tentativa < 6; tentativa++) {
        data = await apiFetchJson<PendenciaOpt[]>(`/api/pendencias?ano=${anoBusca}&mes=${mesBusca}`, { sessao });
        if (data.length > 0) {
          break;
        }
        const anterior = competenciaAnterior(anoBusca, mesBusca);
        anoBusca = anterior.ano;
        mesBusca = anterior.mes;
      }
      setPendencias(data);
      if (data.length > 0 && (anoBusca !== ano || mesBusca !== mes)) {
        setInfoCompetencia(`Mostrando pendências de ${String(mesBusca).padStart(2, "0")}/${anoBusca}.`);
        if (sincronizarCompetencia) {
          setAno(anoBusca);
          setMes(mesBusca);
        }
      }
      if (data.length) {
        const atualExiste = data.some((p) => String(p.id) === pendenciaId);
        if (!atualExiste) {
          setPendenciaId(String(data[0].id));
        }
      } else {
        setPendenciaId("");
      }
    } catch {
      setPendencias([]);
      setPendenciaId("");
    } finally {
      setLoadingPendencias(false);
    }
  }

  useEffect(() => {
    void carregarPendencias();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ano, mes]);

  async function onSubmit(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setOk("");
    if (!arquivos.length) {
      setErro("Selecione pelo menos um arquivo.");
      return;
    }
    if (arquivos.length > MAX_ARQUIVOS_LOTE) {
      setErro(`No máximo ${MAX_ARQUIVOS_LOTE} arquivos por envio.`);
      return;
    }
    if (!pendenciaId) {
      setErro("Selecione uma pendência.");
      return;
    }
    setLoading(true);
    try {
      if (arquivos.length === 1) {
        const fd = new FormData();
        fd.append("arquivo", arquivos[0]);
        if (observacao.trim()) fd.append("observacao", observacao.trim());
        await apiFetchFormData(`/api/pendencias/${pendenciaId}/entregas`, fd, sessao);
        setOk("Documento enviado e pendência marcada como ENVIADO.");
      } else {
        const fd = new FormData();
        arquivos.forEach((f) => fd.append("arquivos", f));
        if (observacao.trim()) fd.append("observacao", observacao.trim());
        const data = await apiFetchFormData<LoteResponse>(
          `/api/pendencias/${pendenciaId}/entregas/lote`,
          fd,
          sessao,
          LOTE_TIMEOUT_MS
        );
        if (data.erros.length === 0) {
          setOk(
            `${data.entregas.length} documento(s) enviado(s) e pendência marcada como ENVIADO.`
          );
        } else {
          const detalhes = data.erros
            .slice(0, 8)
            .map((e) => `${e.nomeArquivoOriginal}: ${e.mensagem}`)
            .join(" · ");
          const mais =
            data.erros.length > 8 ? ` (+${data.erros.length - 8} outro(s))` : "";
          setOk([data.message, detalhes ? `${detalhes}${mais}` : ""].filter(Boolean).join(" "));
        }
      }
      setArquivos([]);
      if (fileInputRef.current) fileInputRef.current.value = "";
      setObservacao("");
      await carregarPendencias();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro no upload.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page">
      <h2>Enviar documento</h2>
      <form className="cliente-upload-form" onSubmit={onSubmit}>
        <div className="cliente-filtro-react">
          <label>
            Ano
            <input
              type="number"
              min={2000}
              max={2100}
              value={ano}
              onChange={(e) => setAno(Number(e.target.value))}
            />
          </label>
          <label>
            Mês
            <input
              type="number"
              min={1}
              max={12}
              value={mes}
              onChange={(e) => setMes(Number(e.target.value))}
            />
          </label>
          <button
            type="button"
            className="ghost"
            onClick={() => void carregarPendencias({ sincronizarCompetencia: true })}
            disabled={loading || loadingPendencias}
          >
            {loadingPendencias ? "Atualizando..." : "Atualizar pendências"}
          </button>
        </div>
        {infoCompetencia ? <p className="muted-react small">{infoCompetencia}</p> : null}
        <label>
          Pendência
          <select value={pendenciaId} onChange={(e) => setPendenciaId(e.target.value)} required>
            <option value="">— Selecione —</option>
            {pendencias.map((p) => (
              <option key={p.id} value={p.id}>
                #{p.id} {p.templateDocumentoNome} ({p.status})
              </option>
            ))}
          </select>
        </label>
        <label>
          Arquivo(s)
          <input
            ref={fileInputRef}
            type="file"
            multiple
            onChange={(e) => setArquivos(Array.from(e.target.files ?? []))}
            required
          />
        </label>
        <p className="muted-react small">
          Qualquer tipo de arquivo — até {MAX_ARQUIVOS_LOTE} arquivos por envio. Um arquivo usa o fluxo único; vários usam
          envio em lote (cada arquivo gravado de forma independente).
        </p>
        {arquivos.length > 0 ? (
          <p className="muted-react small">
            {arquivos.length} arquivo(s) selecionado(s): {arquivos.map((f) => f.name).join(", ")}
          </p>
        ) : null}
        <label>
          Observação
          <input
            type="text"
            value={observacao}
            onChange={(e) => setObservacao(e.target.value)}
            placeholder="Opcional (vale para todos no lote)"
          />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? "Enviando..." : arquivos.length > 1 ? "Enviar lote" : "Enviar documento"}
        </button>
        {erro ? <p className="erro">{erro}</p> : null}
        {ok ? <p className="ok">{ok}</p> : null}
      </form>
    </section>
  );
}
