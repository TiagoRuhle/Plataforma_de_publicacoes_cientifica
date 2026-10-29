import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StarRating from './StarRating.vue'

describe('StarRating', () => {
  it('só leitura: expõe a média ao leitor de ecrã', () => {
    const w = mount(StarRating, { props: { modelValue: 4.3, readonly: true } })
    expect(w.attributes('role')).toBe('img')
    expect(w.attributes('aria-label')).toMatch(/4[,.]3 em 5/)
  })

  it('interativo: emite a nota escolhida', async () => {
    const w = mount(StarRating, { props: { modelValue: 0 } })
    const estrelas = w.findAll('[role="radio"]')
    expect(estrelas).toHaveLength(5)
    await estrelas[3].trigger('click')
    expect(w.emitted('rate')[0]).toEqual([4])
    expect(w.emitted('update:modelValue')[0]).toEqual([4])
  })

  it('marca a estrela atual e respeita disabled', async () => {
    const w = mount(StarRating, { props: { modelValue: 2, disabled: true } })
    const estrelas = w.findAll('[role="radio"]')
    expect(estrelas[1].attributes('aria-checked')).toBe('true')
    await estrelas[4].trigger('click')
    expect(w.emitted('rate')).toBeUndefined()
  })
})
