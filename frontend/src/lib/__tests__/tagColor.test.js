import { describe, expect, it } from 'vitest'
import { tagHue, tagStyle } from '../tagColor'

describe('tagHue', () => {
  it('é estável: a mesma tag tem sempre a mesma cor', () => {
    expect(tagHue('Projeto X')).toBe(tagHue('Projeto X'))
    expect(tagStyle('Projeto X')).toEqual({ '--h': tagHue('Projeto X') })
  })

  it('usa apenas matizes da lista curada', () => {
    const permitidos = [25, 70, 125, 170, 215, 255, 295, 335]
    for (const nome of ['a', 'Machine Learning', 'Ciência de Dados', '', 'tag-interna']) {
      expect(permitidos).toContain(tagHue(nome))
    }
  })

  it('distribui tags diferentes por várias cores', () => {
    const matizes = new Set(
      ['Projeto X', 'Projeto Y', 'IA', 'ML', 'Dados', 'Redes', 'Materiais', 'Física'].map(tagHue),
    )
    expect(matizes.size).toBeGreaterThan(3)
  })
})
