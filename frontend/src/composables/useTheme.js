import { computed, ref } from 'vue'

const KEY = 'dae.theme'
const MODOS = ['system', 'light', 'dark']

function ler() {
  try {
    const valor = localStorage.getItem(KEY)
    return MODOS.includes(valor) ? valor : 'system'
  } catch {
    return 'system'
  }
}

const modo = ref(ler())
let mql = null

function aplicar() {
  const escuro = modo.value === 'dark' || (modo.value === 'system' && !!mql?.matches)
  document.documentElement.classList.toggle('dark', escuro)
  document.documentElement.style.colorScheme = escuro ? 'dark' : 'light'
}

/** Chamar uma vez no arranque (main.js). Segue as preferências do sistema enquanto o modo for "system". */
export function initTheme() {
  mql = window.matchMedia('(prefers-color-scheme: dark)')
  mql.addEventListener('change', aplicar)
  aplicar()
}

export function useTheme() {
  function setModo(novo) {
    if (!MODOS.includes(novo)) return
    modo.value = novo
    try {
      localStorage.setItem(KEY, novo)
    } catch {
      /* ignorar */
    }
    aplicar()
  }
  return { modo: computed(() => modo.value), setModo }
}
