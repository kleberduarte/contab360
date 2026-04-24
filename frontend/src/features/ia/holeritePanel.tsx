/** Painel visual do holerite a partir de `detalhamentoDocumento` (paridade com app.js). */

type Rubrica = { descricao?: string; referencia?: string; valorOriginalBr?: string; valorNumerico?: number };

function esc(v: unknown): string {
  if (v == null) return "";
  return String(v);
}

function nonEmpty(v: unknown): boolean {
  return v != null && String(v).trim() !== "";
}

function kvRows(obj: Record<string, unknown> | undefined, labels: Record<string, string>): JSX.Element | null {
  if (!obj || typeof obj !== "object") return null;
  const parts: JSX.Element[] = [];
  for (const [key, label] of Object.entries(labels)) {
    const val = obj[key];
    if (nonEmpty(val)) {
      parts.push(
        <div key={key} className="holerite-kv">
          <span className="holerite-k">{label}</span>
          <span className="holerite-v">{esc(val)}</span>
        </div>
      );
    }
  }
  if (!parts.length) return null;
  return <div className="holerite-kv-grid">{parts}</div>;
}

function rubricaTable(items: unknown, titulo: string): JSX.Element | null {
  if (!Array.isArray(items) || !items.length) return null;
  const rows = (items as Rubrica[]).map((r, i) => {
    const d = r.descricao != null ? r.descricao : "";
    const ref = r.referencia != null ? r.referencia : "";
    const br =
      r.valorOriginalBr != null && String(r.valorOriginalBr).trim() !== ""
        ? r.valorOriginalBr
        : r.valorNumerico != null
          ? String(r.valorNumerico)
          : "";
    return (
      <tr key={i}>
        <td>{esc(d)}</td>
        <td>{esc(ref)}</td>
        <td>{esc(br)}</td>
      </tr>
    );
  });
  return (
    <section className="holerite-secao">
      <h4 className="holerite-secao-titulo">{titulo}</h4>
      <table className="dados-tabela holerite-mini">
        <thead>
          <tr>
            <th>Descrição</th>
            <th>Ref.</th>
            <th>Valor</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    </section>
  );
}

export function HoleritePainelDetalhe({
  det,
  capturaPerfil
}: {
  det: Record<string, unknown> | null | undefined;
  capturaPerfil: string;
}) {
  if (!det || typeof det !== "object") return null;

  const emp = det.empresa as Record<string, unknown> | undefined;
  const fun = det.funcionario as Record<string, unknown> | undefined;
  const per = det.periodo as Record<string, unknown> | undefined;
  const tot = det.totais as Record<string, unknown> | undefined;
  const bas = det.bases as Record<string, unknown> | undefined;
  const extra = det.camposAdicionais as Record<string, unknown> | undefined;

  const empInner = kvRows(emp, {
    razaoSocial: "Razão social",
    cnpjFormatado: "CNPJ",
    cnpjDigitos: "CNPJ (só dígitos)",
    endereco: "Endereço"
  });
  const funInner = kvRows(fun, {
    nome: "Nome",
    cpf: "CPF",
    pisPasep: "PIS/PASEP",
    cargo: "Cargo",
    departamento: "Departamento",
    dataAdmissao: "Admissão"
  });
  const perInner = kvRows(per, {
    competencia: "Competência / referência",
    tipoFolha: "Tipo de folha",
    diasTrabalhados: "Dias trabalhados"
  });
  const totInner = kvRows(tot, {
    totalProventosBr: "Total proventos",
    totalDescontosBr: "Total descontos",
    valorLiquidoBr: "Valor líquido",
    valorLiquidoNumerico: "Líquido (número)"
  });
  const basInner = kvRows(bas, {
    salarioBaseESocialBr: "Salário base eSocial",
    baseCalculoFgtsBr: "Base FGTS",
    fgtsMesBr: "FGTS mês",
    baseCalculoInssBr: "Base INSS"
  });

  let extraInner: JSX.Element | null = null;
  if (extra && typeof extra === "object" && !Array.isArray(extra)) {
    const keys = Object.keys(extra);
    const parts: JSX.Element[] = [];
    for (const k of keys) {
      if (nonEmpty(extra[k])) {
        parts.push(
          <div key={k} className="holerite-kv">
            <span className="holerite-k">{k}</span>
            <span className="holerite-v">{esc(extra[k])}</span>
          </div>
        );
      }
    }
    if (parts.length) {
      extraInner = (
        <section className="holerite-secao">
          <h4 className="holerite-secao-titulo">Outros</h4>
          <div className="holerite-kv-grid">{parts}</div>
        </section>
      );
    }
  }

  return (
    <div className="holerite-painel-react">
      {capturaPerfil === "HOLERITE_ESCRITORIO_COMPLETO" ? (
        <p className="holerite-badge-react">Holerite · captura alinhada ao escritório</p>
      ) : null}
      {empInner ? (
        <section className="holerite-secao">
          <h4 className="holerite-secao-titulo">Empregador</h4>
          {empInner}
        </section>
      ) : null}
      {funInner ? (
        <section className="holerite-secao">
          <h4 className="holerite-secao-titulo">Trabalhador</h4>
          {funInner}
        </section>
      ) : null}
      {perInner ? (
        <section className="holerite-secao">
          <h4 className="holerite-secao-titulo">Período</h4>
          {perInner}
        </section>
      ) : null}
      {rubricaTable(det.proventos, "Proventos")}
      {rubricaTable(det.descontos, "Descontos")}
      {totInner ? (
        <section className="holerite-secao">
          <h4 className="holerite-secao-titulo">Totais</h4>
          {totInner}
        </section>
      ) : null}
      {basInner ? (
        <section className="holerite-secao">
          <h4 className="holerite-secao-titulo">Bases e encargos</h4>
          {basInner}
        </section>
      ) : null}
      {extraInner}
    </div>
  );
}
