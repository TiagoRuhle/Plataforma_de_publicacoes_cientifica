const POR_ESTADO = {
  400: 'Os dados enviados não são válidos.',
  401: 'A sessão terminou. Inicie sessão novamente.',
  403: 'Não tem permissão para fazer isto.',
  404: 'Não foi encontrado.',
  409: 'Já existe um registo com estes dados.',
  413: 'O ficheiro é demasiado grande.',
  415: 'Tipo de ficheiro não suportado.',
  429: 'Demasiadas tentativas. Aguarde uns minutos e tente de novo.',
  500: 'Erro no servidor. Tente novamente dentro de instantes.',
  502: 'O servidor está indisponível. Tente novamente dentro de instantes.',
  503: 'O servidor está indisponível. Tente novamente dentro de instantes.',
}

/**
 * Converte um erro do axios numa mensagem para o utilizador.
 * O backend devolve texto simples nos erros de regra de negócio (400), por isso usa-se esse texto.
 */
export function errorMessage(error, fallback = 'Algo correu mal. Tente novamente.') {
  if (!error) return fallback
  const resposta = error.response
  if (!resposta) {
    return error.request
      ? 'Sem ligação ao servidor. Confirme que o backend está a correr.'
      : error.message || fallback
  }
  const dados = resposta.data
  if (typeof dados === 'string') {
    const texto = dados.trim()
    if (texto && texto.length < 300 && !texto.startsWith('<')) return texto
  }
  if (dados && typeof dados === 'object' && typeof dados.message === 'string' && dados.message) {
    return dados.message
  }
  return POR_ESTADO[resposta.status] || fallback
}
