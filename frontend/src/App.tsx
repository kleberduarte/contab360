import { ReactNode, Suspense, useEffect, useMemo, useState } from "react";
import { Navigate, NavLink, Route, Routes, useLocation } from "react-router-dom";
import { AppRouteFallback } from "./components/AppRouteFallback";
import { LoginPage } from "./features/auth/LoginPage";
import { PrimeiroAcessoPage } from "./features/auth/PrimeiroAcessoPage";
import {
  ClientePendenciasPage,
  ClienteUploadPage,
  DashboardPage,
  DocsValidadosPage,
  EmpresasPage,
  ClientesPfPage,
  FiscalAlertasPage,
  FiscalCadastrosPage,
  FiscalCertificadosPage,
  FiscalCobrancasPage,
  FiscalNotasPage,
  IaObservadoraPage,
  IaRevisaoPage,
  MinhaContaPage,
  PendenciasPage,
  TemplatesPage,
  UsuariosPage
} from "./routes/lazyPages";
import { clearSessao, getSessao, PerfilUsuario, Sessao, setSessao } from "./lib/session";
import { setOnUnauthorized } from "./lib/api";
import { HeaderUserMenu } from "./components/HeaderUserMenu";
import { useNavigate } from "react-router-dom";
import { PrivacyPolicyModal } from "./features/lgpd/PrivacyPolicyModal";
import { checkConsentimentoPendente } from "./features/lgpd/lgpdApi";

function GuardContador({ sessao, children }: { sessao: Sessao; children: ReactNode }) {
  if (sessao.perfil !== "CONTADOR" && sessao.perfil !== "ADM") {
    return <Navigate to="/cliente-portal" replace />;
  }
  return <>{children}</>;
}

function GuardAdmin({ sessao, children }: { sessao: Sessao; children: ReactNode }) {
  if (sessao.perfil !== "ADM") {
    return <Navigate to={sessao.perfil === "CLIENTE" ? "/cliente-portal" : "/dashboard"} replace />;
  }
  return <>{children}</>;
}

function AppShell({ sessao, onLogout, onNomeAtualizado }: { sessao: Sessao; onLogout: () => void; onNomeAtualizado: (nome: string) => void }) {
  const location = useLocation();
  const navigate = useNavigate();
  const linkGroups = useMemo(() => getLinkGroupsByPerfil(sessao.perfil), [sessao.perfil]);
  const links = useMemo(() => linkGroups.flatMap((group) => group.links), [linkGroups]);
  const [navOpen, setNavOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [navCollapsed, setNavCollapsed] = useState(false);

  useEffect(() => {
    const mq = window.matchMedia("(max-width: 959px)");
    const sync = () => setIsMobile(mq.matches);
    sync();
    mq.addEventListener("change", sync);
    return () => mq.removeEventListener("change", sync);
  }, []);

  useEffect(() => {
    if (!isMobile) setNavOpen(false);
  }, [isMobile]);

  useEffect(() => {
    if (isMobile) setNavOpen(false);
  }, [location.pathname, isMobile]);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <div
      className={`app-shell-react${isMobile && navOpen ? " app-shell-react--nav-open" : ""}${
        !isMobile && navCollapsed ? " app-shell-react--nav-collapsed" : ""
      }`}
    >
      {isMobile && navOpen ? (
        <div className="shell-backdrop-react" aria-hidden="true" onClick={() => setNavOpen(false)} />
      ) : null}
      <aside className="sidebar-react" aria-label="Navegação principal">
        <div className="sidebar-brand-react">
          <span className="sidebar-brand-mark-react" aria-hidden="true">
            <svg width="30" height="30" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="brand-grad-sidebar-react" x1="4" y1="4" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                  <stop stopColor="#21c25e" />
                  <stop offset="1" stopColor="#0fa96a" />
                </linearGradient>
              </defs>
              <rect x="2" y="2" width="40" height="40" rx="11" fill="url(#brand-grad-sidebar-react)" opacity="0.11" />
              <circle cx="22" cy="22" r="14" stroke="url(#brand-grad-sidebar-react)" strokeWidth="2.35" fill="none" />
              <circle cx="22" cy="22" r="3.25" fill="url(#brand-grad-sidebar-react)" />
            </svg>
          </span>
          <span className="sidebar-brand-text-react">
            <span>Contab</span>
            <strong>360</strong>
          </span>
        </div>
        {!isMobile ? (
          <button
            type="button"
            className="sidebar-collapse-btn-react"
            onClick={() => setNavCollapsed((s) => !s)}
            aria-label={navCollapsed ? "Expandir menu lateral" : "Recolher menu lateral"}
            aria-pressed={navCollapsed}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
              {navCollapsed ? <path d="M9 6L15 12L9 18" /> : <path d="M15 6L9 12L15 18" />}
            </svg>
          </button>
        ) : null}
        <h2 className="sidebar-title-react">Menu</h2>
        {linkGroups.map((group) => (
          <section key={group.title} className="sidebar-group-react" aria-label={group.title}>
            <h3 className="sidebar-group-title-react">{group.title}</h3>
            {group.links.map((link) => (
              <NavLink
                key={link.path}
                to={link.path}
                className={({ isActive }) => (isActive ? "active" : "")}
                onClick={() => {
                  if (isMobile) setNavOpen(false);
                }}
                title={navCollapsed ? link.label : undefined}
                end
              >
                <span className="sidebar-link-icon-react">{renderMenuIcon(link.icon)}</span>
                <span className="sidebar-link-label-react">{link.label}</span>
              </NavLink>
            ))}
          </section>
        ))}
      </aside>
      <div className="content-react">
        <header className="shell-header-react">
          <button
            type="button"
            className="shell-header-react__menu-btn"
            aria-label={navOpen ? "Fechar menu" : "Abrir menu"}
            aria-expanded={navOpen}
            onClick={() => setNavOpen((o) => !o)}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              {navOpen ? <path d="M18 6L6 18M6 6l12 12" /> : <path d="M4 6h16M4 12h16M4 18h16" />}
            </svg>
          </button>
          <div className="shell-header-react__brand">
            <h1 className="shell-header-react__title">
              <span className="shell-header-react__mark" aria-hidden="true">
                <svg width="36" height="36" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <linearGradient id="shell-brand-grad" x1="4" y1="4" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                      <stop stopColor="#21c25e" />
                      <stop offset="1" stopColor="#0fa96a" />
                    </linearGradient>
                  </defs>
                  <rect x="2" y="2" width="40" height="40" rx="11" fill="url(#shell-brand-grad)" opacity="0.14" />
                  <circle cx="22" cy="22" r="14" stroke="url(#shell-brand-grad)" strokeWidth="2.35" fill="none" />
                  <circle cx="22" cy="22" r="3.25" fill="url(#shell-brand-grad)" />
                </svg>
              </span>
              <span className="shell-header-react__wordmark">
                <span className="shell-header-react__w1">Contab</span>
                <span className="shell-header-react__w2">360</span>
              </span>
            </h1>
            <p className="shell-header-react__subtitle">
              Cobrança de documentos, pendências e entregas em um só lugar.
            </p>
          </div>
          <div className="shell-header-react__session">
            <HeaderUserMenu sessao={sessao} onLogout={onLogout} onMinhaContaClick={() => navigate("/minha-conta")} />
          </div>
        </header>
        <Suspense fallback={<AppRouteFallback />}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                sessao.perfil !== "CLIENTE" ? (
                  <DashboardPage sessao={sessao} />
                ) : (
                  <Navigate to="/cliente-portal" replace />
                )
              }
            />
            <Route
              path="/empresas"
              element={
                <GuardContador sessao={sessao}>
                  <EmpresasPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/clientes-pf"
              element={
                <GuardContador sessao={sessao}>
                  <ClientesPfPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/templates"
              element={
                sessao.perfil !== "CLIENTE" ? (
                  <TemplatesPage sessao={sessao} />
                ) : (
                  <Navigate to="/cliente-portal" replace />
                )
              }
            />
            <Route
              path="/pendencias"
              element={
                <GuardContador sessao={sessao}>
                  <PendenciasPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/ia-revisao"
              element={
                <GuardContador sessao={sessao}>
                  <IaRevisaoPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route path="/docs-validados" element={<DocsValidadosPage sessao={sessao} />} />
            <Route
              path="/ia-observadora"
              element={
                <GuardAdmin sessao={sessao}>
                  <IaObservadoraPage sessao={sessao} />
                </GuardAdmin>
              }
            />
            <Route
              path="/fiscal-notas"
              element={
                <GuardContador sessao={sessao}>
                  <FiscalNotasPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/fiscal-cadastros"
              element={
                <GuardContador sessao={sessao}>
                  <FiscalCadastrosPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/fiscal-cobrancas"
              element={
                <GuardContador sessao={sessao}>
                  <FiscalCobrancasPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/fiscal-certificados"
              element={
                <GuardContador sessao={sessao}>
                  <FiscalCertificadosPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/fiscal-alertas"
              element={
                <GuardContador sessao={sessao}>
                  <FiscalAlertasPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/usuarios"
              element={
                <GuardContador sessao={sessao}>
                  <UsuariosPage sessao={sessao} />
                </GuardContador>
              }
            />
            <Route
              path="/minha-conta"
              element={<MinhaContaPage sessao={sessao} onNomeAtualizado={onNomeAtualizado} />}
            />
            <Route path="/cliente-portal" element={<DashboardPage sessao={sessao} />} />
            <Route
              path="/cliente-pendencias"
              element={
                sessao.perfil === "CLIENTE" ? (
                  <ClientePendenciasPage sessao={sessao} />
                ) : (
                  <Navigate to="/dashboard" replace />
                )
              }
            />
            <Route
              path="/cliente-upload"
              element={
                sessao.perfil === "CLIENTE" ? <ClienteUploadPage sessao={sessao} /> : <Navigate to="/dashboard" replace />
              }
            />
            <Route path="*" element={<Navigate to={links[0].path} replace />} />
          </Routes>
        </Suspense>
      </div>
    </div>
  );
}

type LinkItem = { path: string; label: string; icon: LinkIcon };
type LinkGroup = { title: string; links: LinkItem[] };
type LinkIcon =
  | "home"
  | "building"
  | "template"
  | "tasks"
  | "sparkles"
  | "files"
  | "radar"
  | "invoice"
  | "id"
  | "money"
  | "shield"
  | "alert"
  | "upload"
  | "users";

function renderMenuIcon(icon: LinkIcon) {
  const common = { fill: "none", stroke: "currentColor", strokeWidth: "1.8", strokeLinecap: "round" as const, strokeLinejoin: "round" as const };
  switch (icon) {
    case "home":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M3.5 10.25L12 3.75L20.5 10.25V19.25H3.5V10.25Z" />
          <path {...common} d="M9.5 19.25V13H14.5V19.25" />
        </svg>
      );
    case "building":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M5 20.25V5.75H14.75V20.25" />
          <path {...common} d="M14.75 9H19V20.25H14.75" />
          <path {...common} d="M8 9.25H10.25M8 12.5H10.25M8 15.75H10.25" />
        </svg>
      );
    case "template":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect {...common} x="3.75" y="4" width="16.5" height="16" rx="2.4" />
          <path {...common} d="M7.5 9H16.5M7.5 12.5H16.5M7.5 16H13" />
        </svg>
      );
    case "tasks":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M8.5 7.5H19.5M8.5 12H19.5M8.5 16.5H19.5" />
          <path {...common} d="M4.5 7.5L5.6 8.6L7.4 6.8M4.5 12L5.6 13.1L7.4 11.3M4.5 16.5L5.6 17.6L7.4 15.8" />
        </svg>
      );
    case "sparkles":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M12 3.75L13.35 7.15L16.75 8.5L13.35 9.85L12 13.25L10.65 9.85L7.25 8.5L10.65 7.15L12 3.75Z" />
          <path {...common} d="M18.25 13.75L19 15.5L20.75 16.25L19 17L18.25 18.75L17.5 17L15.75 16.25L17.5 15.5L18.25 13.75Z" />
        </svg>
      );
    case "files":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M6 3.75H14.5L18 7.25V20.25H6V3.75Z" />
          <path {...common} d="M14.5 3.75V7.25H18M8.5 11H15.5M8.5 14.25H15.5M8.5 17.5H13" />
        </svg>
      );
    case "radar":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle {...common} cx="12" cy="12" r="7.5" />
          <circle {...common} cx="12" cy="12" r="3.25" />
          <path {...common} d="M12 12L18.5 8.75" />
        </svg>
      );
    case "invoice":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M6 3.75H18V20.25L15 18.6L12 20.25L9 18.6L6 20.25V3.75Z" />
          <path {...common} d="M9 8.5H15M9 12H15M9 15.5H13.5" />
        </svg>
      );
    case "id":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect {...common} x="3.75" y="5.5" width="16.5" height="13" rx="2.5" />
          <circle {...common} cx="8.75" cy="12" r="1.75" />
          <path {...common} d="M12.25 10H16.5M12.25 13H16.5" />
        </svg>
      );
    case "money":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect {...common} x="3.75" y="6.25" width="16.5" height="11.5" rx="2.3" />
          <circle {...common} cx="12" cy="12" r="2.25" />
          <path {...common} d="M6.25 9.25H6.3M17.75 14.75H17.8" />
        </svg>
      );
    case "shield":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M12 3.75L19.25 6.75V11.75C19.25 16.25 15.8 19.2 12 20.25C8.2 19.2 4.75 16.25 4.75 11.75V6.75L12 3.75Z" />
          <path {...common} d="M9 12L11 14L15 10" />
        </svg>
      );
    case "alert":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M12 3.75L21 19.25H3L12 3.75Z" />
          <path {...common} d="M12 9V13.25M12 16.25H12.01" />
        </svg>
      );
    case "upload":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path {...common} d="M12 15.75V6.25M8.75 9.5L12 6.25L15.25 9.5" />
          <path {...common} d="M4 15.75V18.25H20V15.75" />
        </svg>
      );
    case "users":
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle {...common} cx="9" cy="8" r="3" />
          <path {...common} d="M3 20c0-3.31 2.69-6 6-6s6 2.69 6 6" />
          <path {...common} d="M16 3.13a4 4 0 0 1 0 7.75M21 20c0-2.76-2.24-5-5-5" />
        </svg>
      );
  }
}

function getLinkGroupsByPerfil(perfil: PerfilUsuario): LinkGroup[] {
  if (perfil === "CLIENTE") {
    return [
      {
        title: "Area do cliente",
        links: [
          { path: "/cliente-portal", label: "Portal", icon: "home" },
          { path: "/cliente-pendencias", label: "Minhas pendencias", icon: "tasks" },
          { path: "/cliente-upload", label: "Enviar documento", icon: "upload" }
        ]
      },
      {
        title: "IA",
        links: [{ path: "/docs-validados", label: "Documentos validados (IA)", icon: "files" }]
      }
    ];
  }
  return [
    {
      title: "Operacao",
      links: [
        { path: "/dashboard", label: "Dashboard", icon: "home" },
        { path: "/empresas", label: "Empresas", icon: "building" },
        { path: "/usuarios", label: "Usuários", icon: "users" },
        { path: "/clientes-pf", label: "Pessoa fisica", icon: "id" },
        { path: "/templates", label: "Templates", icon: "template" },
        { path: "/pendencias", label: "Pendencias", icon: "tasks" }
      ]
    },
    {
      title: "IA",
      links: [
        { path: "/ia-revisao", label: "Revisao IA", icon: "sparkles" },
        { path: "/docs-validados", label: "Documentos validados (IA)", icon: "files" },
        ...(perfil === "ADM" ? [{ path: "/ia-observadora", label: "IA Observadora", icon: "radar" as const }] : [])
      ]
    },
    {
      title: "Fiscal",
      links: [
        { path: "/fiscal-notas", label: "Notas fiscais", icon: "invoice" },
        { path: "/fiscal-cadastros", label: "CPF/CNPJ", icon: "id" },
        { path: "/fiscal-cobrancas", label: "Cobrancas", icon: "money" },
        { path: "/fiscal-certificados", label: "Certificados", icon: "shield" },
        { path: "/fiscal-alertas", label: "Alertas e relatorios", icon: "alert" }
      ]
    }
  ];
}

export function App() {
  const [sessao, setSessaoState] = useState<Sessao | null>(() => getSessao());
  const [consentimentoPendente, setConsentimentoPendente] = useState(false);

  useEffect(() => {
    setOnUnauthorized(() => {
      clearSessao();
      setSessaoState(null);
      setConsentimentoPendente(false);
    });
  }, []);

  useEffect(() => {
    if (!sessao || sessao.senhaTempAtiva) {
      setConsentimentoPendente(false);
      return;
    }
    checkConsentimentoPendente(sessao)
      .then((r) => setConsentimentoPendente(r.pendente))
      .catch(() => setConsentimentoPendente(false));
  }, [sessao]);

  function onLogin(next: Sessao) {
    setSessao(next);
    setSessaoState(next);
    if (window.location.pathname === "/login") {
      window.history.replaceState(null, "", "/");
    }
  }

  function onLogout() {
    clearSessao();
    setSessaoState(null);
    setConsentimentoPendente(false);
    window.history.replaceState(null, "", "/login");
  }

  function onNomeAtualizado(novoNome: string) {
    if (!sessao) return;
    const atualizada = { ...sessao, usuarioNome: novoNome };
    setSessao(atualizada);
    setSessaoState(atualizada);
  }

  function onSenhaCriada() {
    if (!sessao) return;
    const atualizada = { ...sessao, senhaTempAtiva: false };
    setSessao(atualizada);
    setSessaoState(atualizada);
  }

  if (!sessao) return <LoginPage onLogin={onLogin} />;
  if (sessao.senhaTempAtiva) return <PrimeiroAcessoPage sessao={sessao} onSenhaCriada={onSenhaCriada} />;
  if (consentimentoPendente) {
    return <PrivacyPolicyModal sessao={sessao} onAceito={() => setConsentimentoPendente(false)} />;
  }
  return <AppShell sessao={sessao} onLogout={onLogout} onNomeAtualizado={onNomeAtualizado} />;
}
