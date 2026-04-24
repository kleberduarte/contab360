import { useEffect, useRef, useState } from "react";
import { Sessao } from "../lib/session";

function iniciais(sessao: Sessao): string {
  const base = (sessao.usuarioNome || sessao.usuarioEmail || "?").trim();
  const parts = base.split(/\s+/).filter(Boolean);
  const s = parts
    .map((p) => p[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
  return s || "?";
}

export function HeaderUserMenu({ sessao, onLogout }: { sessao: Sessao; onLogout: () => void }) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDocClick(ev: MouseEvent) {
      if (!wrapRef.current?.contains(ev.target as Node)) setOpen(false);
    }
    document.addEventListener("click", onDocClick);
    return () => document.removeEventListener("click", onDocClick);
  }, [open]);

  useEffect(() => {
    function onKey(ev: KeyboardEvent) {
      if (ev.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);

  const initials = iniciais(sessao);
  const nome = sessao.usuarioNome || sessao.usuarioEmail || "Usuário";
  const email = sessao.usuarioEmail;
  const perfilLabel = sessao.perfil === "CONTADOR" ? "Contador" : "Cliente";

  return (
    <div ref={wrapRef} className="header-user-menu-react">
      <button
        type="button"
        className="header-user-menu-react__trigger"
        aria-expanded={open}
        aria-haspopup="true"
        onClick={(e) => {
          e.stopPropagation();
          setOpen((o) => !o);
        }}
      >
        <span className="header-user-menu-react__avatar" aria-hidden="true">
          {initials}
        </span>
        <span className="header-user-menu-react__chev" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M6 9l6 6 6-6" />
          </svg>
        </span>
      </button>

      {open ? (
        <div className="header-user-menu-react__dropdown" role="menu" aria-label="Menu do usuário">
          <p className="header-user-menu-react__eyebrow">Perfil pessoal</p>
          <div className="header-user-menu-react__card">
            <span className="header-user-menu-react__avatar header-user-menu-react__avatar--card" aria-hidden="true">
              {initials}
            </span>
            <div className="header-user-menu-react__card-text">
              <span className="header-user-menu-react__name">{nome}</span>
              <span className="header-user-menu-react__meta">
                {email} · {perfilLabel}
              </span>
            </div>
            <span className="header-user-menu-react__check" title="Conta ativa" aria-hidden="true">
              ✓
            </span>
          </div>
          <button
            type="button"
            className="header-user-menu-react__link"
            role="menuitem"
            onClick={() => setOpen(false)}
          >
            Minha conta
          </button>
          <div className="header-user-menu-react__divider" role="separator" />
          <button
            type="button"
            className="header-user-menu-react__logout"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
          >
            Sair
          </button>
        </div>
      ) : null}
    </div>
  );
}
