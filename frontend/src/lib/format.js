const LOCALE = 'pt-PT'

const dataCurta = new Intl.DateTimeFormat(LOCALE, {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})
const dataHora = new Intl.DateTimeFormat(LOCALE, {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})
const hora = new Intl.DateTimeFormat(LOCALE, { hour: '2-digit', minute: '2-digit' })

function paraData(valor) {
  const d = valor instanceof Date ? valor : new Date(valor)
  return Number.isNaN(d.getTime()) ? null : d
}

export function formatDate(valor) {
  const d = paraData(valor)
  return d ? dataCurta.format(d) : ''
}

export function formatDateTime(valor) {
  const d = paraData(valor)
  return d ? dataHora.format(d) : ''
}

export function formatTime(valor) {
  const d = paraData(valor)
  return d ? hora.format(d) : ''
}

/** "há 5 minutos", "ontem", ... */
export function formatRelative(valor, agora = new Date()) {
  const d = paraData(valor)
  if (!d) return ''
  const diffSeg = Math.round((d.getTime() - agora.getTime()) / 1000)
  const rtf = new Intl.RelativeTimeFormat(LOCALE, { numeric: 'auto' })
  const abs = Math.abs(diffSeg)
  if (abs < 60) return rtf.format(0, 'second')
  if (abs < 3600) return rtf.format(Math.round(diffSeg / 60), 'minute')
  if (abs < 86400) return rtf.format(Math.round(diffSeg / 3600), 'hour')
  if (abs < 86400 * 7) return rtf.format(Math.round(diffSeg / 86400), 'day')
  return formatDate(d)
}

/** Título do grupo no histórico: "Hoje", "Ontem" ou a data. */
export function formatDayLabel(valor, agora = new Date()) {
  const d = paraData(valor)
  if (!d) return ''
  const inicio = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const dias = Math.round((inicio(agora) - inicio(d)) / 86400000)
  if (dias === 0) return 'Hoje'
  if (dias === 1) return 'Ontem'
  return formatDate(d)
}

export function dayKey(valor) {
  const d = paraData(valor)
  return d ? `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}` : ''
}

export function formatRating(n) {
  const v = Number(n)
  if (!Number.isFinite(v)) return '0,0'
  return v.toLocaleString(LOCALE, { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}

export function plural(n, singular, pluralForm) {
  return `${n} ${n === 1 ? singular : pluralForm}`
}

export function initials(nome) {
  const partes = String(nome ?? '')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (partes.length === 0) return '?'
  if (partes.length === 1) return partes[0].charAt(0).toUpperCase()
  return (partes[0].charAt(0) + partes[partes.length - 1].charAt(0)).toUpperCase()
}

export const ROLES = {
  COLABORADOR: 'Colaborador',
  RESPONSAVEL: 'Responsável',
  ADMINISTRADOR: 'Administrador',
}

export function roleLabel(role) {
  return ROLES[role] || role || ''
}

/** Texto, ícone e grupo de cada tipo de ação do histórico. */
export const ACOES = {
  PUBLICACAO_CRIADA: { label: 'Publicação criada', grupo: 'publicacoes' },
  PUBLICACAO_ATUALIZADA: { label: 'Publicação atualizada', grupo: 'publicacoes' },
  PUBLICACAO_OCULTADA: { label: 'Publicação ocultada', grupo: 'publicacoes' },
  PUBLICACAO_VISIVEL: { label: 'Publicação visível', grupo: 'publicacoes' },
  PUBLICACAO_VISUALIZADA: { label: 'Publicação consultada', grupo: 'publicacoes' },
  RESUMO_GERADO: { label: 'Resumo gerado por IA', grupo: 'publicacoes' },
  RESUMO_EDITADO_MANUALMENTE: { label: 'Resumo corrigido', grupo: 'publicacoes' },
  TAG_ADICIONADA: { label: 'Tag associada', grupo: 'tags' },
  TAG_REMOVIDA: { label: 'Tag desassociada', grupo: 'tags' },
  TAG_CRIADA: { label: 'Tag criada', grupo: 'tags' },
  TAG_ATUALIZADA: { label: 'Tag atualizada', grupo: 'tags' },
  TAG_SUBSCRITA: { label: 'Tag subscrita', grupo: 'tags' },
  TAG_DESSUBSCRITA: { label: 'Subscrição cancelada', grupo: 'tags' },
  COMENTARIO_ADICIONADO: { label: 'Comentário', grupo: 'comentarios' },
  COMENTARIO_REMOVIDO: { label: 'Comentário apagado', grupo: 'comentarios' },
  COMENTARIO_EDITADO: { label: 'Comentário moderado', grupo: 'comentarios' },
  RATING_ADICIONADO: { label: 'Avaliação', grupo: 'avaliacoes' },
  RATING_ATUALIZADO: { label: 'Avaliação alterada', grupo: 'avaliacoes' },
  RATING_REMOVIDO: { label: 'Avaliação removida', grupo: 'avaliacoes' },
  LOGIN: { label: 'Sessão iniciada', grupo: 'conta' },
  LOGOUT: { label: 'Sessão terminada', grupo: 'conta' },
  PERFIL_ATUALIZADO: { label: 'Perfil atualizado', grupo: 'conta' },
  SENHA_ALTERADA: { label: 'Palavra-passe alterada', grupo: 'conta' },
  USER_CRIADO: { label: 'Conta criada', grupo: 'conta' },
}

export const GRUPOS_ACAO = {
  publicacoes: 'Publicações',
  comentarios: 'Comentários',
  tags: 'Tags',
  avaliacoes: 'Avaliações',
  conta: 'Conta',
}

export function acaoInfo(tipo) {
  return (
    ACOES[tipo] || {
      label: String(tipo ?? '')
        .replaceAll('_', ' ')
        .toLowerCase(),
      grupo: 'conta',
    }
  )
}
