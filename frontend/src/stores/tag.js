import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAPIStore } from './api'
import { useAuthStore } from './auth'

export const useTagStore = defineStore('tag', () => {
  const api = useAPIStore()
  const auth = useAuthStore()

  const tags = ref([])
  const subscricoes = ref([])

  /** Responsável/Admin recebem também as tags ocultas; os restantes só as visíveis. */
  async function load() {
    const response = auth.isGestor ? await api.getAllTags() : await api.getVisibleTags()
    tags.value = response.data
  }

  async function loadSubscricoes() {
    subscricoes.value = (await api.getSubscribedTags()).data
  }

  const create = async (tag) => {
    const response = await api.createTag(tag)
    await load()
    return response.data
  }

  const remove = async (nome) => {
    await api.deleteTag(nome)
    await Promise.all([load(), loadSubscricoes()])
  }

  const subscribe = async (nome) => {
    await api.subscribeTag(nome)
    await loadSubscricoes()
  }

  const unsubscribe = async (nome) => {
    await api.unsubscribeTag(nome)
    await loadSubscricoes()
  }

  return { tags, subscricoes, load, loadSubscricoes, create, remove, subscribe, unsubscribe }
})
