/**
 * Cada tag tem sempre a mesma cor: o matiz vem de um hash do nome, escolhido numa lista curada
 * para que as cores combinem entre si (a luminosidade e a saturação são fixadas no CSS).
 */
const MATIZES = [25, 70, 125, 170, 215, 255, 295, 335]

export function tagHue(nome) {
  let hash = 2166136261 // FNV-1a
  const texto = String(nome ?? '')
  for (let i = 0; i < texto.length; i++) {
    hash ^= texto.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return MATIZES[(hash >>> 0) % MATIZES.length]
}

export function tagStyle(nome) {
  return { '--h': tagHue(nome) }
}
