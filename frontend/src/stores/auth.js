import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useAPIStore } from './api'
import { tokenStorage } from '@/lib/http'

export const useAuthStore = defineStore('auth', () => {
  const api = useAPIStore()

  const currentUser = ref(undefined)
  let inicializacao = null

  const isLoggedIn = computed(() => currentUser.value !== undefined)
  const currentUserRole = computed(() => currentUser.value?.role)
  const isGestor = computed(() =>
    ['RESPONSAVEL', 'ADMINISTRADOR'].includes(currentUser.value?.role),
  )
  const isAdmin = computed(() => currentUser.value?.role === 'ADMINISTRADOR')

  /** Restaura a sessão a partir do token guardado. Só corre uma vez; o router espera por ela. */
  function init() {
    if (!inicializacao) {
      inicializacao = (async () => {
        if (tokenStorage.get()) {
          try {
            await getUser()
          } catch {
            tokenStorage.clear()
            currentUser.value = undefined
          }
        }
      })()
    }
    return inicializacao
  }

  async function getUser() {
    const response = await api.getAuthUser()
    currentUser.value = response.data
    return currentUser.value
  }

  async function login(credentials) {
    const response = await api.postLogin(credentials)
    // o backend devolve o JWT em texto simples; remove aspas por segurança
    tokenStorage.set(String(response.data).replaceAll('"', '').trim())
    try {
      return await getUser()
    } catch (e) {
      tokenStorage.clear()
      throw e
    }
  }

  function logout() {
    tokenStorage.clear()
    currentUser.value = undefined
  }

  return {
    currentUser,
    isLoggedIn,
    currentUserRole,
    isGestor,
    isAdmin,
    init,
    login,
    logout,
    getUser,
  }
})
