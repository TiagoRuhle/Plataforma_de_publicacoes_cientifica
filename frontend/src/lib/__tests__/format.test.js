import { describe, expect, it } from 'vitest'
import {
  acaoInfo,
  dayKey,
  formatDayLabel,
  formatRating,
  initials,
  plural,
  roleLabel,
} from '../format'

describe('formatação', () => {
  it('formata ratings com vírgula decimal', () => {
    expect(formatRating(4.25)).toMatch(/^4[,.][2-3]/)
    expect(formatRating(0)).toMatch(/^0[,.]0$/)
    expect(formatRating(undefined)).toBe('0,0')
  })

  it('pluraliza', () => {
    expect(plural(1, 'comentário', 'comentários')).toBe('1 comentário')
    expect(plural(0, 'comentário', 'comentários')).toBe('0 comentários')
    expect(plural(3, 'comentário', 'comentários')).toBe('3 comentários')
  })

  it('calcula iniciais', () => {
    expect(initials('Maria Santos')).toBe('MS')
    expect(initials('Ana Maria de Sousa')).toBe('AS')
    expect(initials('administrador')).toBe('A')
    expect(initials('')).toBe('?')
  })

  it('agrupa os dias do histórico', () => {
    const agora = new Date(2026, 9, 26, 15, 0)
    expect(formatDayLabel(new Date(2026, 9, 26, 9, 0), agora)).toBe('Hoje')
    expect(formatDayLabel(new Date(2026, 9, 25, 23, 59), agora)).toBe('Ontem')
    expect(formatDayLabel(new Date(2026, 8, 1), agora)).toMatch(/2026/)
    expect(dayKey(new Date(2026, 9, 26, 1))).toBe(dayKey(new Date(2026, 9, 26, 23)))
  })

  it('traduz roles e ações, mesmo as desconhecidas', () => {
    expect(roleLabel('RESPONSAVEL')).toBe('Responsável')
    expect(acaoInfo('RESUMO_EDITADO_MANUALMENTE').label).toBe('Resumo corrigido')
    expect(acaoInfo('ALGO_NOVO').label).toBe('algo novo')
  })
})
