import { describe, expect, it } from 'vitest'
import { errorMessage } from '../errors'

const erro = (status, data) => ({ response: { status, data } })

describe('errorMessage', () => {
  it('usa o texto devolvido pelo backend nos erros de regra de negócio', () => {
    expect(errorMessage(erro(400, 'A password atual está incorreta.'))).toBe(
      'A password atual está incorreta.',
    )
  })

  it('ignora respostas HTML e usa a mensagem do estado', () => {
    expect(errorMessage(erro(500, '<html>Erro</html>'))).toMatch(/Erro no servidor/)
  })

  it('traduz estados sem corpo', () => {
    expect(errorMessage(erro(403))).toMatch(/permissão/)
    expect(errorMessage(erro(404))).toMatch(/encontrado/)
    expect(errorMessage(erro(409))).toMatch(/Já existe/)
    expect(errorMessage(erro(429))).toMatch(/Demasiadas tentativas/)
  })

  it('distingue falta de ligação ao servidor', () => {
    expect(errorMessage({ request: {} })).toMatch(/Sem ligação/)
  })

  it('usa o fallback quando não há informação', () => {
    expect(errorMessage(erro(418), undefined)).toBe('Algo correu mal. Tente novamente.')
    expect(errorMessage(null, 'x')).toBe('x')
  })
})
