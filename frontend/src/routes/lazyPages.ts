import { lazy } from "react";

export const DashboardPage = lazy(() =>
  import("../features/dashboard/DashboardPage").then((m) => ({ default: m.DashboardPage }))
);

export const EmpresasPage = lazy(() =>
  import("../features/empresas/EmpresasPage").then((m) => ({ default: m.EmpresasPage }))
);

export const PendenciasPage = lazy(() =>
  import("../features/pendencias/PendenciasPage").then((m) => ({ default: m.PendenciasPage }))
);

export const TemplatesPage = lazy(() =>
  import("../features/templates/TemplatesPage").then((m) => ({ default: m.TemplatesPage }))
);

export const DocsValidadosPage = lazy(() =>
  import("../features/ia/DocsValidadosPage").then((m) => ({ default: m.DocsValidadosPage }))
);

export const IaObservadoraPage = lazy(() =>
  import("../features/ia/IaObservadoraPage").then((m) => ({ default: m.IaObservadoraPage }))
);

export const IaRevisaoPage = lazy(() =>
  import("../features/ia/IaRevisaoPage").then((m) => ({ default: m.IaRevisaoPage }))
);

export const FiscalNotasPage = lazy(() =>
  import("../features/fiscal/FiscalNotasPage").then((m) => ({ default: m.FiscalNotasPage }))
);

export const FiscalCadastrosPage = lazy(() =>
  import("../features/fiscal/FiscalCadastrosPage").then((m) => ({ default: m.FiscalCadastrosPage }))
);

export const FiscalCobrancasPage = lazy(() =>
  import("../features/fiscal/FiscalCobrancasPage").then((m) => ({ default: m.FiscalCobrancasPage }))
);

export const FiscalCertificadosPage = lazy(() =>
  import("../features/fiscal/FiscalCertificadosPage").then((m) => ({ default: m.FiscalCertificadosPage }))
);

export const FiscalAlertasPage = lazy(() =>
  import("../features/fiscal/FiscalAlertasPage").then((m) => ({ default: m.FiscalAlertasPage }))
);

export const ClientePendenciasPage = lazy(() =>
  import("../features/cliente/ClientePendenciasPage").then((m) => ({ default: m.ClientePendenciasPage }))
);

export const ClienteUploadPage = lazy(() =>
  import("../features/cliente/ClienteUploadPage").then((m) => ({ default: m.ClienteUploadPage }))
);
