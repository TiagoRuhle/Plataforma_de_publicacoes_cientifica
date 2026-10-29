/**
 * Formatação mínima para os resumos gerados por IA (negrito, itálico, títulos, listas).
 * O texto é escapado ANTES de aplicar a formatação, por isso o resultado é seguro para v-html.
 */
const escapar = (s) =>
  s
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')

function emLinha(texto) {
  return escapar(texto)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[\s(])\*(?!\s)(.+?)\*(?=[\s).,;:]|$)/g, '$1<em>$2</em>')
}

export function renderResumo(texto) {
  if (!texto) return ''
  let html = ''
  let emLista = false
  const fecharLista = () => {
    if (emLista) {
      html += '</ul>'
      emLista = false
    }
  }
  for (const bruta of texto.replace(/\r\n/g, '\n').split('\n')) {
    const linha = bruta.trim()
    if (!linha) {
      fecharLista()
      continue
    }
    const item = /^[-*•]\s+(.*)$/.exec(linha)
    if (item) {
      if (!emLista) {
        html += '<ul>'
        emLista = true
      }
      html += `<li>${emLinha(item[1])}</li>`
      continue
    }
    fecharLista()
    const titulo = /^#{1,4}\s+(.*)$/.exec(linha)
    html += titulo ? `<h3>${emLinha(titulo[1])}</h3>` : `<p>${emLinha(linha)}</p>`
  }
  fecharLista()
  return html
}

/** Versão em texto simples (sem formatação) para pré-visualizações, ex.: na lista de publicações. */
export function plainText(texto) {
  if (!texto) return ''
  return texto
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map((linha) => linha.trim())
    .filter(Boolean)
    .map((linha) => {
      const titulo = /^#{1,4}\s+(.*)$/.exec(linha)
      const limpa = (titulo ? titulo[1] : linha.replace(/^[-*•]\s+/, ''))
        .replaceAll('**', '')
        .replace(/(^|\s)\*(\S)/g, '$1$2')
        .replace(/(\S)\*(\s|$)/g, '$1$2')
      return titulo ? `${limpa}:` : limpa
    })
    .join(' ')
}
