param(
  [string]$OutputDir = "samples/massa-docs-validados",
  [string]$CnpjEmpresa = "12345678000195",
  [string]$CnpjTomador = "11222333000181",
  [string]$NomeRazaoSocial = "ACME SERVICOS CONTABEIS LTDA",
  [string]$NomeRazaoTomador = "BETA INDUSTRIA E COMERCIO LTDA",
  [string]$NomePessoaFisica = "Maria Fernanda Costa Oliveira",
  [string]$CpfPessoa = "52998224725"
)

$ErrorActionPreference = "Stop"

function Format-CnpjMascara {
  param([string]$Digits)
  $d = ($Digits -replace '\D', '').PadLeft(14, '0').Substring(0, 14)
  return $d -replace '^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$', '$1.$2.$3/$4-$5'
}

function Format-CpfMascara {
  param([string]$Digits)
  $d = ($Digits -replace '\D', '').PadLeft(11, '0').Substring(0, 11)
  return $d -replace '^(\d{3})(\d{3})(\d{3})(\d{2})$', '$1.$2.$3-$4'
}

function Write-Utf8File {
  param(
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][string]$Content
  )
  $dir = Split-Path -Parent $Path
  if ($dir -and -not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir | Out-Null
  }
  $utf8Bom = New-Object System.Text.UTF8Encoding $true
  [System.IO.File]::WriteAllText($Path, $Content, $utf8Bom)
}

function Build-Header {
  param(
    [string]$TipoDetectado,
    [string]$AbaCarrossel,
    [string]$FonteLeitura,
    [string[]]$ChavesCamposExtraidos,
    [ValidateSet("PJ", "PF")]
    [string]$EscopoPendencia,
    [string]$Notas = ""
  )
  $cnpjM = Format-CnpjMascara $CnpjEmpresa
  $cpfM = Format-CpfMascara $CpfPessoa
  $linhas = @(
    "# === Metadados massa Contab360 (gerado por scripts/generate_doc_test_mass.ps1) ===",
    "# escopo_pendencia (massa): $EscopoPendencia",
    "#   PJ -> nome empresarial de referencia: $NomeRazaoSocial",
    "#   PF -> nome da pessoa fisica de referencia: $NomePessoaFisica | CPF $cpfM ($CpfPessoa)",
    "# Ajuste as pendencias para competencia 2026-04 e CNPJ da empresa = $cnpjM ($CnpjEmpresa) quando aplicavel.",
    "# tipo_documento_detectado_esperado: $TipoDetectado",
    "# aba_carrossel_esperada: $AbaCarrossel",
    "# fonte_leitura: $FonteLeitura",
    "#",
    "# Estrutura JSON gravada em dadosExtraidosJson (resumo):",
    "#   fonte, tipoDocumento, status, confianca, camposObrigatorios[], camposExtraidos{}, motivosRevisao[]",
    "#   + detalhamentoDocumento? + capturaPerfil? (holerite / guia IRPF completos)",
    "#",
    "# Chaves tipicas em camposExtraidos para este cenario:"
  )
  foreach ($c in $ChavesCamposExtraidos) {
    $linhas += "#   - $c"
  }
  if ($Notas) {
    $linhas += "#"
    $linhas += "# Notas: $Notas"
  }
  $linhas += "# === Fim metadados / inicio do texto simulado ==="
  return ($linhas -join "`n")
}

$root = (Resolve-Path ".").Path
$target = Join-Path $root $OutputDir
if (-not (Test-Path $target)) {
  New-Item -ItemType Directory -Path $target | Out-Null
}

$cnpjMaskEmp = Format-CnpjMascara $CnpjEmpresa
$cnpjMaskTom = Format-CnpjMascara $CnpjTomador
$cpfMaskPf = Format-CpfMascara $CpfPessoa

$nfeXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<nfeProc>
  <NFe>
    <infNFe>
      <emit><CNPJ>$CnpjEmpresa</CNPJ><xNome>$NomeRazaoSocial</xNome></emit>
      <dest><CNPJ>$CnpjTomador</CNPJ><xNome>$NomeRazaoTomador</xNome></dest>
      <ide><dhEmi>2026-04-30T10:00:00-03:00</dhEmi></ide>
      <Competencia>04/2026</Competencia>
      <dVenc>15/05/2026</dVenc>
      <total><ICMSTot><vNF>1500.00</vNF></ICMSTot></total>
    </infNFe>
  </NFe>
</nfeProc>
"@

$nfseXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<CompNfse>
  <Nfse>
    <InfNfse>
      <PrestadorServico>
        <RazaoSocial>$NomeRazaoSocial</RazaoSocial>
        <IdentificacaoPrestador><Cnpj>$CnpjEmpresa</Cnpj></IdentificacaoPrestador>
      </PrestadorServico>
      <TomadorServico>
        <RazaoSocial>$NomeRazaoTomador</RazaoSocial>
        <IdentificacaoTomador><CpfCnpj><Cnpj>$CnpjTomador</Cnpj></CpfCnpj></IdentificacaoTomador>
      </TomadorServico>
      <Competencia>04/2026</Competencia>
      <dVenc>20/05/2026</dVenc>
      <Servico>
        <Valores>
          <ValorServicos>2500.00</ValorServicos>
          <ValorLiquidoNfse>2375.00</ValorLiquidoNfse>
        </Valores>
      </Servico>
    </InfNfse>
  </Nfse>
</CompNfse>
"@

$commonTxtCampos = @(
  "cnpj",
  "cnpjPrestador",
  "cnpjTomador",
  "valor",
  "competencia",
  "vencimento",
  "tributo",
  "tipoDocumento"
)

$docs = @(
  @{
    File     = "01-NFE-xml.xml"
    Escopo   = "PJ"
    Template = "Nota fiscal produto XML"
    Aba      = "NFE"
    Tipo     = "NFE_XML"
    Fonte    = "xml"
    Campos   = @("cnpjEmitente", "cnpjDestinatario", "valor", "competencia", "vencimento", "tributo", "tipoDocumento")
    Content  = $nfeXml
    Notas    = "processarXml: tributo vem de heuristica sobre o codigo do tipo (nao do XML)."
  },
  @{
    File     = "02-NFSE-xml.xml"
    Escopo   = "PJ"
    Template = "Nota fiscal servico XML"
    Aba      = "NFSE"
    Tipo     = "NFSE_XML"
    Fonte    = "xml"
    Campos   = @("cnpjEmitente", "cnpjDestinatario", "valor", "competencia", "vencimento", "tributo", "tipoDocumento")
    Content  = $nfseXml
    Notas    = ""
  },
  @{
    File     = "03-NFCE.txt"
    Escopo   = "PJ"
    Template = "Nota fiscal consumidor NFC-e"
    Aba      = "NFCE"
    Tipo     = "NFCE"
    Fonte    = "texto"
    Campos   = $commonTxtCampos + @("valorServicos", "valorIss", "pis", "cofins", "valorLiquido")
    Content  = @"
NFC-e Nota Fiscal do Consumidor Eletronica
Emitente  -  Razao Social: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Inscricao Estadual: 123.456.789.110
Endereco: Av. Exemplo, 1000  -  Sao Paulo/SP
Competencia de referencia: 04/2026
Valor total da nota = R$ 85,40
Valor dos Servicos: R$ 85,40
Aliquota ISS: 5,00%
Valor ISS: R$ 4,27
PIS: R$ 0,55
COFINS: R$ 2,56
Valor Liquido: R$ 78,02
"@
    Notas    = "Inclui quadro ISS opcional (mesmos campos da NFS-e em texto)."
  },
  @{
    File     = "04-CTE.txt"
    Escopo   = "PJ"
    Template = "Nota fiscal CT-e transporte"
    Aba      = "CTE"
    Tipo     = "CTE"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
CT-e Conhecimento de Transporte Eletronico  -  modelo rodoviario
Emitente  -  Transportador: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Remetente: $NomeRazaoTomador | CNPJ $cnpjMaskTom
Valor do frete: R$ 950,00
"@
    Notas    = ""
  },
  @{
    File     = "05-MDFE.txt"
    Escopo   = "PJ"
    Template = "Nota fiscal MDF-e manifesto"
    Aba      = "MDFE"
    Tipo     = "MDFE"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
MDF-e Manifesto Eletronico de Documentos Fiscais
Emitente  -  Razao Social: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Placa veiculo tracao: ABC1D23 | UF: SP
Valor da carga: R$ 45.000,00
"@
    Notas    = ""
  },
  @{
    File     = "06-CTE-OS.txt"
    Escopo   = "PJ"
    Template = "Nota fiscal CT-e OS"
    Aba      = "CTE_OS"
    Tipo     = "CTE_OS"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
CT-e OS  -  Conhecimento de Transporte de Outros Servicos
Prestador do servico de transporte: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Tomador do servico: $NomeRazaoTomador | CNPJ $cnpjMaskTom
Valor do servico: R$ 1.120,00
"@
    Notas    = ""
  },
  @{
    File     = "07-NOTA-FISCAL.txt"
    Escopo   = "PJ"
    Template = "Nota fiscal servico PDF simulado"
    Aba      = "NOTA_FISCAL"
    Tipo     = "NOTA_FISCAL"
    Fonte    = "texto"
    Campos   = $commonTxtCampos + @("valorServicos", "aliquotaIssPercentual", "valorIss", "valorLiquido")
    Content  = @"
Nota Fiscal de Servico Eletronica  -  NFS-e (layout municipal simplificado)
PRESTADOR DE SERVICOS
Razao Social: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Inscricao Municipal: 9.87.654-3
TOMADOR DE SERVICOS
Razao Social: $NomeRazaoTomador
CNPJ: $cnpjMaskTom
Competencia: 04/2026
Valor total da nota = R$ 1.250,00
Valor dos Servicos: R$ 1.250,00
Aliquota ISS: 5,00%
Valor ISS: R$ 62,50
Valor Liquido: R$ 1.187,50
"@
    Notas    = ""
  },
  @{
    File     = "08-FOLHA-PAGAMENTO.txt"
    Escopo   = "PF"
    Template = "Holerite folha pagamento"
    Aba      = "FOLHA_PAGAMENTO"
    Tipo     = "FOLHA_PAGAMENTO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos + @("nomeTrabalhador", "cpf", "detalhamentoDocumento (holerite)", "capturaPerfil=HOLERITE_ESCRITORIO_COMPLETO")
    Content  = @"
RECIBO DE PAGAMENTO DE SALARIO  -  Holerite
EMPREGADOR (contratante)
Razao Social: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
EMPREGADO (contratado)
Nome: $NomePessoaFisica
CPF: $cpfMaskPf
Matricula: 8844 | Departamento: Contabilidade | Cargo: Analista
Competencia: 04/2026
Proventos: Salario base R$ 4.200,00 | Horas extras R$ 350,00
Descontos: INSS R$ 514,20 | IRRF R$ 185,60
Liquido a receber: R$ 3.250,00
Base FGTS: R$ 4.550,00
"@
    Notas    = "Se o parser de holerite preencher minimo, campos tecnicos podem ir para holerite.* no JSON."
  },
  @{
    File     = "09-GUIA-IMPOSTO.txt"
    Escopo   = "PJ"
    Template = "Guia imposto DARF"
    Aba      = "GUIA_IMPOSTO"
    Tipo     = "GUIA_IMPOSTO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos + @("detalhamentoDocumento? (IRPF)", "capturaPerfil? GUIA_IMPOSTO_IRPF_COMPLETO")
    Content  = @"
DARF  -  Documento de Arrecadacao de Receitas Federais
Codigo de receita: 2089  -  IRPJ  -  PJ
Nome empresarial / contribuinte: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Periodo de apuracao: 04/2026
Valor principal: R$ 2.150,00
Vencimento: 10/05/2026
Autenticacao bancaria (simulada): A1B2-C3D4-E5F6
"@
    Notas    = "tributo inferido por palavras-chave (ex.: IRPJ, DARF). Alinhar vencimento da pendencia com 2026-05-10 se quiser evitar motivo de revisao."
  },
  @{
    File     = "10-DECLARACAO-OBRIGACAO.txt"
    Escopo   = "PJ"
    Template = "Declaracao DCTFWeb"
    Aba      = "DECLARACAO_OBRIGACAO"
    Tipo     = "DECLARACAO_ACESSORIA"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
Declaracao acessoria  -  DCTFWeb (Receita Federal)
Contribuinte: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Numero do recibo de transmissao (simulado): 26.04.12345678-9
"@
    Notas    = "Tipo interno DECLARACAO_ACESSORIA; aba do carrossel DECLARACAO_OBRIGACAO."
  },
  @{
    File     = "11-SPED-FISCAL-CONTRIBUICOES.txt"
    Escopo   = "PJ"
    Template = "SPED fiscal EFD contribuicoes"
    Aba      = "SPED_FISCAL_CONTRIBUICOES"
    Tipo     = "SPED_FISCAL_CONTRIBUICOES"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
SPED Fiscal  -  EFD Contribuicoes (Leiaute versao 1.0  -  trecho simulado)
Nome empresarial: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Periodo de apuracao: 04/2026
Registro |0000| ABERTURA|1|
Registro |0001| DADOS|1|
Texto de validacao: EFD contribuicoes  -  arquivo aceito (simulado)
"@
    Notas    = ""
  },
  @{
    File     = "12-SPED-CONTABIL-FISCAL.txt"
    Escopo   = "PJ"
    Template = "SPED contabil ECD ECF"
    Aba      = "SPED_CONTABIL_FISCAL"
    Tipo     = "SPED_CONTABIL_FISCAL"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
ECD  -  Escrituracao Contabil Digital / ECF  -  Escrituracao Contabil Fiscal (trecho)
Empresa: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Periodo: 04/2026
Livro Razao digital  -  hash referencia (simulado): 3F2A9B1C...
"@
    Notas    = ""
  },
  @{
    File     = "13-BALANCETES-DEMONSTRACOES.txt"
    Escopo   = "PJ"
    Template = "Balancete patrimonial"
    Aba      = "BALANCETES_DEMONSTRACOES"
    Tipo     = "BALANCETES_DEMONSTRACOES"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
Balancete patrimonial e demonstracoes financeiras
Empresa: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Ativo circulante: R$ 32.000,00 | Ativo nao circulante: R$ 18.000,00
Total ativo: R$ 50.000,00
Passivo circulante: R$ 12.000,00 | Patrimonio liquido: R$ 38.000,00
"@
    Notas    = ""
  },
  @{
    File     = "14-EXTRATO-BANCARIO.txt"
    Escopo   = "PJ"
    Template = "Extrato bancario conta corrente"
    Aba      = "EXTRATO_BANCARIO"
    Tipo     = "EXTRATO_BANCARIO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
Extrato de conta corrente  -  Pessoa Juridica
Titular: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Agencia: 1234-5 | Conta: 98765-4 | Periodo: 01/04/2026 a 30/04/2026
Saldo inicial: R$ 8.200,00 | Saldo final: R$ 10.000,00
"@
    Notas    = ""
  },
  @{
    File     = "15-CONCILIACAO-FINANCEIRA.txt"
    Escopo   = "PJ"
    Template = "Conciliacao financeira mensal"
    Aba      = "CONCILIACAO_FINANCEIRA"
    Tipo     = "CONCILIACAO_FINANCEIRA"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
Relatorio de conciliacao financeira mensal
Empresa: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Saldo contabil: R$ 10.000,00 | Saldo bancario: R$ 10.000,00
Diferenca apurada: R$ 0,00
"@
    Notas    = ""
  },
  @{
    File     = "16-CONTAS-PAGAR-RECEBER.txt"
    Escopo   = "PJ"
    Template = "Contas a pagar e receber"
    Aba      = "CONTAS_PAGAR_RECEBER"
    Tipo     = "CONTAS_PAGAR_RECEBER"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
Posicao de contas a pagar e contas a receber  -  04/2026
Empresa: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Arquivo remessa/retorno de boletos  -  CNAB 240 (referencia)
Total em aberto a pagar: R$ 4.100,00 | Total em aberto a receber: R$ 3.700,00
"@
    Notas    = ""
  },
  @{
    File     = "17-RECIBO-DESPESA.txt"
    Escopo   = "PF"
    Template = "Recibo despesa operacional"
    Aba      = "RECIBO_DESPESA"
    Tipo     = "RECIBO_DESPESA"
    Fonte    = "texto"
    Campos   = @("cpf", "nomeCompleto", "cnpj (opcional)", "valor", "competencia", "tipoDocumento")
    Content  = @"
RECIBO DE PRESTACAO DE SERVICOS  -  autonomo / PF
Recebi de $NomeRazaoSocial, inscrita no CNPJ sob o n. $cnpjMaskEmp,
a importancia de R$ 320,50 (trezentos e vinte reais e cinquenta centavos),
referente a servicos de consultoria em 04/2026.
Prestador do servico (beneficiario):
Nome completo: $NomePessoaFisica
CPF: $cpfMaskPf
Data: 30/04/2026 | Local: Sao Paulo/SP
Assinatura: ___________________________
"@
    Notas    = "Cenario PF: destaca nome e CPF do prestador/recebedor."
  },
  @{
    File     = "18-CONTRATOS-SOCIOS.txt"
    Escopo   = "PJ"
    Template = "Contrato social empresa"
    Aba      = "CONTRATOS_SOCIOS"
    Tipo     = "CONTRATO_SOCIAL"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
CONTRATO SOCIAL  -  3o ADITIVO (simulado)
Sociedade empresaria limitada denominada $NomeRazaoSocial,
com sede nesta capital, inscrita no CNPJ sob o n. $cnpjMaskEmp.
Clausula 1  -  Objeto social: prestacao de servicos de contabilidade e consultoria.
Quadro societario atualizado em 30/04/2026.
"@
    Notas    = "CONTRATO_SOCIAL mapeia para aba CONTRATOS_SOCIOS."
  },
  @{
    File     = "19-ATAS-REGISTROS.txt"
    Escopo   = "PJ"
    Template = "Ata reuniao socios"
    Aba      = "ATAS_E_REGISTROS"
    Tipo     = "ATA_REUNIAO"
    Fonte    = "texto"
    Campos   = @("tipoDocumento", "cnpj (opcional)", "competencia", "vencimento", "tributo")
    Content  = @"
ATA DA ASSEMBLEIA GERAL ORDINARIA  -  30/04/2026
Empresa: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Presidente da mesa: Fulano de Tal | Secretario: Beltrano Silva
Deliberacao: aprovacao das demonstracoes financeiras encerradas em 31/03/2026.
Arquivamento: Junta Comercial do Estado de Sao Paulo (simulado).
"@
    Notas    = "Campos obrigatorios minimos: tipoDocumento."
  },
  @{
    File     = "20-CERTIDOES-LICENCAS.txt"
    Escopo   = "PJ"
    Template = "Certidao e licenca funcionamento"
    Aba      = "CERTIDOES_LICENCAS"
    Tipo     = "CERTIDOES_LICENCAS"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
CERTIDAO NEGATIVA DE DEBITOS  -  Tributos Federais (simulado)
Contribuinte: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Emissao: 30/04/2026 | Validade: 31/12/2026
Codigo de controle: CN-2026-04-ACME-001

LICENCA DE FUNCIONAMENTO  -  Prefeitura Municipal (simulado)
Razao social: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Atividade: servicos contabeis | Endereco comercial conforme cadastro municipal
"@
    Notas    = ""
  },
  @{
    File     = "21-PROCESSOS-FISCAIS-JURIDICOS.txt"
    Escopo   = "PJ"
    Template = "Notificacao fiscal"
    Aba      = "PROCESSOS_FISCAIS_JURIDICOS"
    Tipo     = "PROCESSOS_FISCAIS_JURIDICOS"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
NOTIFICACAO FISCAL  -  auto de infracao em elaboracao (simulado)
Contribuinte: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Processo administrativo fiscal n. 2026.000123
Assunto: conferencia de obrigacoes acessorias  -  competencia 04/2026
Prazo para manifestacao: 30 dias
"@
    Notas    = ""
  },
  @{
    File     = "22-PATRIMONIO-IMOBILIZADO.txt"
    Escopo   = "PJ"
    Template = "Patrimonio imobilizado"
    Aba      = "PATRIMONIO_IMOBILIZADO"
    Tipo     = "PATRIMONIO_IMOBILIZADO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
RELATORIO DE CONTROLE PATRIMONIAL  -  ativo imobilizado
Empresa: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Item: veiculo utilitario  -  aquisicao 2022 | Vida util remanescente: 36 meses
Depreciacao do mes: R$ 1.500,00
"@
    Notas    = ""
  },
  @{
    File     = "23-ESTOQUE-CUSTO.txt"
    Escopo   = "PJ"
    Template = "Inventario estoque"
    Aba      = "ESTOQUE_CUSTO"
    Tipo     = "ESTOQUE_CUSTO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
INVENTARIO DE ESTOQUE  -  custo medio ponderado
Estabelecimento: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Posicao de estoque em 30/04/2026
Produto A  -  qtd 120 | custo unit. R$ 85,00 | total R$ 10.200,00
Produto B  -  qtd 45 | custo unit. R$ 295,00 | total R$ 13.275,00
Valor total do estoque: R$ 23.475,00 (arredondamentos podem diferir)
"@
    Notas    = ""
  },
  @{
    File     = "24-COMERCIO-EXTERIOR-CAMBIO.txt"
    Escopo   = "PJ"
    Template = "Declaracao importacao invoice"
    Aba      = "COMERCIO_EXTERIOR_CAMBIO"
    Tipo     = "COMERCIO_EXTERIOR_CAMBIO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
DECLARACAO DE IMPORTACAO (DI)  -  trecho simulado
Importador: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Invoice comercial: INV-2026-US-8844 | Valor FOB USD: 1.000,00
Despacho aduaneiro: Porto de Santos/SP
"@
    Notas    = "Valor USD pode normalizar diferente de BRL; confira confianca e revisao."
  },
  @{
    File     = "25-CERTIFICADO-DIGITAL.txt"
    Escopo   = "PJ"
    Template = "Certificado digital A1"
    Aba      = "CERTIFICADO_DIGITAL"
    Tipo     = "CERTIFICADO_DIGITAL"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
CERTIFICADO DIGITAL ICP-Brasil  -  modelo A1 (e-CNPJ)  -  dados simulados
Titular: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Emissor AC: AC Certificadora Exemplo | Validade: 30/04/2027
Uso: assinatura de obrigacoes e NF-e
"@
    Notas    = ""
  },
  @{
    File     = "26-EMPRESTIMO-FINANCIAMENTO.txt"
    Escopo   = "PJ"
    Template = "Contrato emprestimo bancario"
    Aba      = "EMPRESTIMO_FINANCIAMENTO"
    Tipo     = "EMPRESTIMO_FINANCIAMENTO"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
CONTRATO DE FINANCIAMENTO BANCARIO  -  investimento em capital de giro
Mutuario: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Credor: Banco Exemplo S.A.
Valor financiado: R$ 150.000,00 | Taxa: 1,2% a.m. | Prazo: 48 meses
Garantia: aval dos socios e penhor de recebiveis (clausulas em anexo)
"@
    Notas    = ""
  },
  @{
    File     = "27-SEGUROS-APOLICES.txt"
    Escopo   = "PJ"
    Template = "Apolice seguro empresarial"
    Aba      = "SEGUROS_APOLICES"
    Tipo     = "SEGUROS_APOLICES"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
APOLICE DE SEGURO EMPRESARIAL  -  cobertura patrimonial
Segurado: $NomeRazaoSocial
CNPJ: $cnpjMaskEmp
Apolice n. 9988776655 | Vigencia: 04/2026
Premio: R$ 950,00 | Seguradora: Seguros Exemplo S.A.
"@
    Notas    = ""
  },
  @{
    File     = "28-RELATORIOS-GERENCIAIS.txt"
    Escopo   = "PJ"
    Template = "Relatorio gerencial KPI"
    Aba      = "RELATORIOS_GERENCIAIS"
    Tipo     = "RELATORIOS_GERENCIAIS"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
RELATORIO GERENCIAL DE DESEMPENHO  -  KPIs
Empresa: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Receita liquida: R$ 88.000,00 | Margem EBITDA: 18% | Inadimplencia: 2,1%
"@
    Notas    = ""
  },
  @{
    File     = "29-FLUXO-CAIXA.txt"
    Escopo   = "PJ"
    Template = "Fluxo de caixa"
    Aba      = "FLUXO_CAIXA"
    Tipo     = "FLUXO_CAIXA"
    Fonte    = "texto"
    Campos   = $commonTxtCampos
    Content  = @"
FLUXO DE CAIXA PROJETADO  -  consolidado
Empresa: $NomeRazaoSocial | CNPJ: $cnpjMaskEmp
Competencia: 04/2026
Entradas operacionais: R$ 10.000,00
Saidas operacionais: R$ 7.200,00
Saldo projetado fim de periodo: R$ 12.800,00
"@
    Notas    = ""
  },
  @{
    File     = "30-OUTROS.txt"
    Escopo   = "PF"
    Template = "Documento diverso sem classe"
    Aba      = "OUTROS"
    Tipo     = "DESCONHECIDO"
    Fonte    = "texto"
    Campos   = @("tipoDocumento", "cpf", "nomeCompleto", "valor?", "competencia?", "motivosRevisao (provavel)")
    Content  = @"
DEMONSTRATIVO DE PAGAMENTO  -  Carnê-Leao / rendimentos tributaveis PF (simulado)
Contribuinte: $NomePessoaFisica
CPF: $cpfMaskPf
Competencia: 04/2026
Rendimento tributavel recebido de pessoa juridica: R$ 4.800,00
Imposto devido (estimativa): R$ 192,00 | Codigo DARF: 0190 (exemplo)
Observacao: documento generico para teste  -  pode classificar como DESCONHECIDO se faltar contexto.
"@
    Notas    = "Tende a DESCONHECIDO / baixa confianca; perfil CLIENTE pode nao listar se status nao for PROCESSADO."
  }
)

foreach ($d in $docs) {
  $hdr = Build-Header -TipoDetectado $d.Tipo -AbaCarrossel $d.Aba -FonteLeitura $d.Fonte -ChavesCamposExtraidos $d.Campos -EscopoPendencia $d.Escopo -Notas $d.Notas
  Write-Utf8File -Path (Join-Path $target $d.File) -Content ($hdr + "`n`n" + $d.Content.TrimEnd() + "`n")
}

$manifestHeader = "arquivo;escopo_pendencia;aba_carrossel;tipo_detectado;template_sugerido;fonte;campos_extraidos"
$manifestRows = $docs | ForEach-Object {
  $camposJoin = ($_.Campos -join "|")
  '"{0}";"{1}";"{2}";"{3}";"{4}";"{5}";"{6}"' -f $_.File, $_.Escopo, $_.Aba, $_.Tipo, $_.Template, $_.Fonte, $camposJoin
}
$manifest = @($manifestHeader) + $manifestRows
Write-Utf8File -Path (Join-Path $target "manifesto-massa-docs.csv") -Content ($manifest -join "`r`n")

$readme = @"
Massa de teste  -  Docs Validados (carrossel)

Pasta: $OutputDir
CNPJ empresa (padrao): $CnpjEmpresa
CNPJ tomador XML (padrao): $CnpjTomador
Nome razao social (PJ na massa): $NomeRazaoSocial
Nome pessoa fisica (PF na massa): $NomePessoaFisica | CPF $cpfMaskPf

Cada arquivo comeca com comentarios '#' listando:
- escopo_pendencia: PJ (corpo com nome da empresa) ou PF (corpo com nome da pessoa / CPF)
- tipo de documento esperado apos a leitura automatica
- aba do carrossel esperada (mapeamento contab360.doc-tabs)
- chaves tipicas em camposExtraidos (JSON)

CSV: manifesto-massa-docs.csv

Parametros do gerador (exemplo):
  ./scripts/generate_doc_test_mass.ps1 -CnpjEmpresa 12345678000195 -CnpjTomador 11222333000181 `
    -NomeRazaoSocial 'MINHA EMPRESA LTDA' -NomePessoaFisica 'Joao da Silva' -CpfPessoa 12345678909

Regras de teste:
- Use pendencias na competencia 2026-04 alinhada ao texto (quando houver competencia).
- Empresa do cadastro deve usar o mesmo CNPJ do cabecalho (evita rejeicao por validacao de pendencia).
"@
Write-Utf8File -Path (Join-Path $target "README.txt") -Content $readme

Write-Host "Massa gerada em: $target"
Write-Host "Manifesto: $(Join-Path $target 'manifesto-massa-docs.csv')"
