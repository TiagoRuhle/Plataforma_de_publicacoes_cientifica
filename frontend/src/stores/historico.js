import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAPIStore } from './api'

export const useHistoricoStore = defineStore('historico', () => {
  const api = useAPIStore()

  const historico = ref([])
  const loading = ref(false)
  let pedido = 0

  /** Evita que uma resposta lenta de um filtro anterior substitua a do filtro atual. */
  async function carregar(fn) {
    const meu = ++pedido
    loading.value = true
    try {
      const response = await fn()
      if (meu === pedido) historico.value = response.data
    } finally {
      if (meu === pedido) loading.value = false
    }
  }

  const getMyHistorico = () => carregar(() => api.getHistorico())
  const getUserHistorico = (username) => carregar(() => api.getUserHistorico(username))
  const getAllHistorico = () => carregar(() => api.getAllHistorico())

  return { historico, loading, getMyHistorico, getUserHistorico, getAllHistorico }
})
