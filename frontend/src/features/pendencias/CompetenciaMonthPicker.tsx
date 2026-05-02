import { useEffect, useId, useRef, useState } from "react";
import { DayPicker } from "react-day-picker";
import { ptBR } from "react-day-picker/locale";
import "react-day-picker/style.css";

type Props = {
  ano: number;
  mes: number;
  onChange: (ano: number, mes: number) => void;
  ariaLabelledBy?: string;
};

function competenciaLabel(ano: number, mes: number): string {
  const raw = new Date(ano, mes - 1, 1).toLocaleDateString("pt-BR", { month: "long", year: "numeric" });
  return raw.charAt(0).toUpperCase() + raw.slice(1);
}

export function CompetenciaMonthPicker({ ano, mes, onChange, ariaLabelledBy }: Props) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  const btnId = useId();

  const monthDate = new Date(ano, mes - 1, 1);

  const anoCorrente = new Date().getFullYear();
  const anosNoCalendario = 10;
  const anoInicio = anoCorrente - (anosNoCalendario - 1);
  const startMonth = new Date(anoInicio, 0);
  const endMonth = new Date(anoCorrente, 11);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  return (
    <div className="pendencias-competencia-picker" ref={wrapRef}>
      <button
        type="button"
        id={btnId}
        className="pendencias-competencia-picker__trigger"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={`${btnId}-cal`}
        aria-labelledby={
          ariaLabelledBy ? `${ariaLabelledBy} ${btnId}-val` : `${btnId}-val`
        }
        onClick={() => setOpen((v) => !v)}
      >
        <span id={`${btnId}-val`} className="pendencias-competencia-picker__trigger-text">
          {competenciaLabel(ano, mes)}
        </span>
        <span className="pendencias-competencia-picker__chev" aria-hidden="true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="m6 9 6 6 6-6"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>
      </button>

      {open ? (
        <div
          id={`${btnId}-cal`}
          className="pendencias-rdp-pop"
          role="dialog"
          aria-modal="true"
          aria-label="Seleção de mês e ano da competência"
        >
          <DayPicker
            locale={ptBR}
            month={monthDate}
            onMonthChange={(d) => {
              onChange(d.getFullYear(), d.getMonth() + 1);
              setOpen(false);
            }}
            startMonth={startMonth}
            endMonth={endMonth}
            captionLayout="dropdown"
            animate
            className="pendencias-rdp pendencias-rdp--month-only"
          />
        </div>
      ) : null}
    </div>
  );
}
