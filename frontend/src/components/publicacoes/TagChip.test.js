import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TagChip from './TagChip.vue'
import { tagHue } from '@/lib/tagColor'

describe('TagChip', () => {
  it('mostra o nome e usa a cor estável da tag', () => {
    const w = mount(TagChip, { props: { name: 'Projeto X' } })
    expect(w.text()).toContain('Projeto X')
    expect(w.attributes('style')).toContain(`--h: ${tagHue('Projeto X')}`)
  })

  it('só mostra o botão de remover quando removível e emite remove', async () => {
    expect(
      mount(TagChip, { props: { name: 'A' } })
        .find('button')
        .exists(),
    ).toBe(false)
    const w = mount(TagChip, { props: { name: 'A', removable: true } })
    await w.find('button').trigger('click')
    expect(w.emitted('remove')).toHaveLength(1)
  })

  it('interativo: expõe o estado selecionado', async () => {
    const w = mount(TagChip, { props: { name: 'A', interactive: true, selected: true } })
    expect(w.element.tagName).toBe('BUTTON')
    expect(w.attributes('aria-pressed')).toBe('true')
    await w.trigger('click')
    expect(w.emitted('click')).toHaveLength(1)
  })
})
