type ErrorContext = "templates" | "empresas" | "clientesPf" | "usuarios";

const DUPLICIDADE_MESSAGES: Record<ErrorContext, string> = {
  templates: "Já existe um template com esse nome para este tomador. Use outro nome ou edite o template existente.",
  empresas: "Já existe um cadastro de empresa com os mesmos dados. Revise CNPJ e razão social.",
  clientesPf: "Já existe um cadastro de pessoa física com os mesmos dados. Revise CPF e nome.",
  usuarios: "Já existe um usuário com esses dados. Revise principalmente o e-mail informado."
};

export function formatApiError(message: string, context?: ErrorContext): string {
  const original = message || "";
  const texto = original.toLowerCase();

  if (
    texto.includes("erro http 401") ||
    texto.includes("usuário não autenticado") ||
    texto.includes("usuario nao autenticado")
  ) {
    return "Sua sessão expirou. Faça login novamente.";
  }

  if (
    texto.includes("erro http 403") ||
    texto.includes("apenas contador") ||
    texto.includes("sem permissão") ||
    texto.includes("sem permissao")
  ) {
    return "Você não tem permissão para realizar esta ação.";
  }

  if (
    texto.includes("informe o nome do documento") ||
    texto.includes("nome: must not be blank") ||
    texto.includes("must not be blank") ||
    texto.includes("deve conter")
  ) {
    return "Verifique os campos obrigatórios e tente novamente.";
  }

  if (
    texto.includes("já existe um template") ||
    texto.includes("ja existe um template")
  ) {
    if (context) return DUPLICIDADE_MESSAGES[context];
    return "Não foi possível salvar porque já existe um cadastro equivalente.";
  }

  if (texto.includes("registro duplicado")) {
    if (context) return DUPLICIDADE_MESSAGES[context];
    return "Não foi possível salvar porque já existe um cadastro equivalente.";
  }

  if (texto.includes("dados duplicados ou invalidos") || texto.includes("dados duplicados ou inválidos")) {
    if (context) return DUPLICIDADE_MESSAGES[context];
    return "Não foi possível salvar porque já existe um cadastro equivalente.";
  }

  return original;
}
