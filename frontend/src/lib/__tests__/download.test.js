import { describe, expect, it } from 'vitest'
import { filenameFromDisposition } from '../download'

describe('filenameFromDisposition', () => {
  it('prefere filename* em UTF-8', () => {
    const h = `attachment; filename="relatorio.pdf"; filename*=UTF-8''relat%C3%B3rio%20final.pdf`
    expect(filenameFromDisposition(h)).toBe('relatório final.pdf')
  })

  it('lê o filename simples', () => {
    expect(filenameFromDisposition('attachment; filename="dados.zip"')).toBe('dados.zip')
  })

  it('usa o fallback quando o cabeçalho falta', () => {
    expect(filenameFromDisposition(undefined, 'x.pdf')).toBe('x.pdf')
  })
})
