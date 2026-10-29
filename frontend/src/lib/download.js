/** Extrai o nome do ficheiro do cabeçalho Content-Disposition (suporta filename* em UTF-8). */
export function filenameFromDisposition(cabecalho, fallback = 'ficheiro') {
  if (!cabecalho) return fallback
  const utf8 = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(cabecalho)
  if (utf8) {
    try {
      return decodeURIComponent(utf8[1].trim())
    } catch {
      /* continua para o filename simples */
    }
  }
  const simples = /filename\s*=\s*"?([^";]+)"?/i.exec(cabecalho)
  return simples ? simples[1].trim() : fallback
}

/** Guarda um Blob no computador do utilizador. */
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
