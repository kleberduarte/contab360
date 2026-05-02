/** Ícones do carrossel de categorias — paridade com app.js (docTabIconSvg). */
import type { ReactNode } from "react";

const svgProps = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.5,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true as const
};

function Svg({ children }: { children: ReactNode }) {
  return (
    <svg {...svgProps} xmlns="http://www.w3.org/2000/svg">
      {children}
    </svg>
  );
}

/** DARF / guias federais — mesmo pictograma para todas as abas de tributo RFB. */
const guiaReceitaFederalIcon = (
  <Svg>
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
    <path d="M14 2v6h6" />
    <path d="m9 15 2 2 4-4" />
  </Svg>
);

const icons: Record<string, ReactNode> = {
  NOTA_FISCAL: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  NFE: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  NFSE: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  NFCE: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  CTE: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  MDFE: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  CTE_OS: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M12 18v-6" />
      <path d="M9 15h6" />
      <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
    </Svg>
  ),
  FOLHA_PAGAMENTO: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M9 13h6" />
      <path d="M9 17h4" />
      <circle cx="12" cy="9" r="2" />
    </Svg>
  ),
  EXTRATO_BANCARIO: (
    <Svg>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M3 9h18" />
      <path d="M9 21V9" />
    </Svg>
  ),
  RECIBO_DESPESA: (
    <Svg>
      <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1-2-1z" />
      <path d="M8 10h8" />
      <path d="M8 14h5" />
    </Svg>
  ),
  GUIA_IRPF: guiaReceitaFederalIcon,
  GUIA_IRPJ: guiaReceitaFederalIcon,
  GUIA_CSLL: guiaReceitaFederalIcon,
  GUIA_PIS: guiaReceitaFederalIcon,
  GUIA_COFINS: guiaReceitaFederalIcon,
  GUIA_IPI: guiaReceitaFederalIcon,
  GUIA_IOF: guiaReceitaFederalIcon,
  GUIA_IRRF: guiaReceitaFederalIcon,
  GUIA_CIDE: guiaReceitaFederalIcon,
  GUIA_II: guiaReceitaFederalIcon,
  GUIA_ITR: guiaReceitaFederalIcon,
  GUIA_FUNRURAL: guiaReceitaFederalIcon,
  GUIA_IMPOSTO: guiaReceitaFederalIcon,
  DECLARACAO_OBRIGACAO: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M8 13h8" />
      <path d="M8 17h6" />
    </Svg>
  ),
  SPED_FISCAL_CONTRIBUICOES: (
    <Svg>
      <path d="M3 3v18h18" />
      <path d="M7 12l4-4 4 4 4-4" />
      <path d="M7 18h10" />
    </Svg>
  ),
  SPED_CONTABIL_FISCAL: (
    <Svg>
      <path d="M3 3v18h18" />
      <path d="M7 12l4-4 4 4 4-4" />
      <path d="M7 18h10" />
    </Svg>
  ),
  BALANCETES_DEMONSTRACOES: (
    <Svg>
      <path d="M3 3v18h18" />
      <path d="M7 12l4-4 4 4 4-4" />
      <path d="M7 18h10" />
    </Svg>
  ),
  CONTRATO_SOCIAL: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="m9 15 2 2 4-4" />
      <path d="M9 12h.01" />
    </Svg>
  ),
  ATA_REUNIAO: (
    <Svg>
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </Svg>
  ),
  DECLARACAO_ACESSORIA: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M8 13h8" />
      <path d="M8 17h6" />
    </Svg>
  ),
  CONCILIACAO_FINANCEIRA: (
    <Svg>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M3 9h18" />
      <path d="M9 21V9" />
    </Svg>
  ),
  CONTAS_PAGAR_RECEBER: (
    <Svg>
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M2 10h20" />
      <path d="M6 15h.01M12 15h.01M18 15h.01" />
    </Svg>
  ),
  CONTRATOS_SOCIOS: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="m9 15 2 2 4-4" />
      <path d="M9 12h.01" />
    </Svg>
  ),
  ATAS_E_REGISTROS: (
    <Svg>
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </Svg>
  ),
  CERTIDOES_LICENCAS: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="m9 15 2 2 4-4" />
    </Svg>
  ),
  PROCESSOS_FISCAIS_JURIDICOS: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M9 13h6" />
      <path d="M9 17h4" />
      <circle cx="12" cy="9" r="2" />
    </Svg>
  ),
  PATRIMONIO_IMOBILIZADO: (
    <Svg>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M3 9h18" />
      <path d="M9 21V9" />
    </Svg>
  ),
  ESTOQUE_CUSTO: (
    <Svg>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M3 9h18" />
      <path d="M9 21V9" />
    </Svg>
  ),
  COMERCIO_EXTERIOR_CAMBIO: (
    <Svg>
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M2 10h20" />
      <path d="M6 15h.01M12 15h.01M18 15h.01" />
    </Svg>
  ),
  CERTIFICADO_DIGITAL: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="m9 15 2 2 4-4" />
    </Svg>
  ),
  EMPRESTIMO_FINANCIAMENTO: (
    <Svg>
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M2 10h20" />
      <path d="M6 15h.01M12 15h.01M18 15h.01" />
    </Svg>
  ),
  FLUXO_CAIXA: (
    <Svg>
      <path d="M3 3v18h18" />
      <path d="M7 12l4-4 4 4 4-4" />
      <path d="M7 18h10" />
    </Svg>
  ),
  SEGUROS_APOLICES: (
    <Svg>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="m9 15 2 2 4-4" />
    </Svg>
  ),
  RELATORIOS_GERENCIAIS: (
    <Svg>
      <path d="M3 3v18h18" />
      <path d="M7 12l4-4 4 4 4-4" />
      <path d="M7 18h10" />
    </Svg>
  ),
  OUTROS: (
    <Svg>
      <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      <path d="M12 11v6" />
      <path d="M9 14h6" />
    </Svg>
  )
};

export function DocTabIconCarousel({ idAba }: { idAba: string }) {
  const id = (idAba || "OUTROS").toUpperCase();
  return <>{icons[id] ?? icons.OUTROS}</>;
}
