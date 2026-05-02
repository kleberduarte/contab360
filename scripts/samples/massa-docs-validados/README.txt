Massa de teste  -  Docs Validados (carrossel)

Pasta: samples/massa-docs-validados
CNPJ empresa (padrao): 12345678000195
CNPJ tomador XML (padrao): 11222333000181
Nome razao social (PJ na massa): ACME SERVICOS CONTABEIS LTDA
Nome pessoa fisica (PF na massa): Maria Fernanda Costa Oliveira | CPF 529.982.247-25

Cada arquivo comeca com comentarios '#' listando:
- escopo_pendencia: PJ (corpo com nome da empresa) ou PF (corpo com nome da pessoa / CPF)
- tipo de documento esperado apos a leitura automatica
- aba do carrossel esperada (mapeamento contab360.doc-tabs)
- chaves tipicas em camposExtraidos (JSON)

CSV: manifesto-massa-docs.csv

Parametros do gerador (exemplo):
  ./scripts/generate_doc_test_mass.ps1 -CnpjEmpresa 12345678000195 -CnpjTomador 11222333000181 
    -NomeRazaoSocial 'MINHA EMPRESA LTDA' -NomePessoaFisica 'Joao da Silva' -CpfPessoa 12345678909

Regras de teste:
- Use pendencias na competencia 2026-04 alinhada ao texto (quando houver competencia).
- Empresa do cadastro deve usar o mesmo CNPJ do cabecalho (evita rejeicao por validacao de pendencia).