import { describe, expect, it } from 'vitest'
import { plainText, renderResumo } from '../markdownLite'

describe('renderResumo', () => {
  it('formata negrito, títulos e listas', () => {
    const html = renderResumo('## Objetivo\nEstudar **redes** profundas.\n\n- um\n- dois')
    expect(html).toContain('<h3>Objetivo</h3>')
    expect(html).toContain('<strong>redes</strong>')
    expect(html).toContain('<ul><li>um</li><li>dois</li></ul>')
  })

  it('escapa HTML (seguro para v-html)', () => {
    const html = renderResumo('<img src=x onerror=alert(1)> e <script>x</script>')
    expect(html).not.toContain('<img')
    expect(html).not.toContain('<script')
    expect(html).toContain('&lt;img')
  })

  it('não confunde negrito no início da linha com lista', () => {
    expect(renderResumo('**Objetivo:** estudar')).toBe('<p><strong>Objetivo:</strong> estudar</p>')
  })

  it('devolve vazio para texto vazio', () => {
    expect(renderResumo('')).toBe('')
    expect(renderResumo(null)).toBe('')
  })

  it('plainText remove a formatação para pré-visualizações', () => {
    expect(plainText('## Objetivo\nEstudar **redes** profundas.\n- um\n- dois')).toBe(
      'Objetivo: Estudar redes profundas. um dois',
    )
    expect(plainText(null)).toBe('')
  })
})
