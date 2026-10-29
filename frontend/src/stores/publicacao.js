import { defineStore } from 'pinia'
import { useAPIStore } from './api'

/** Operações sobre publicações. As listas vivem nas páginas (estado de pesquisa próprio de cada vista). */
export const usePublicacaoStore = defineStore('publicacao', () => {
  const api = useAPIStore()

  const search = async (params) => (await api.searchPublicacoes(params)).data
  const get = async (id) => (await api.getPublicacao(id)).data
  const create = async (formData, onProgress) =>
    (await api.createPublicacao(formData, onProgress)).data
  const update = async (id, dados) => (await api.updatePublicacao(id, dados)).data
  const regenerarResumo = async (id) => (await api.regenerarResumo(id)).data
  const ocultar = async (id) => (await api.ocultarPublicacao(id)).data
  const mostrar = async (id) => (await api.mostrarPublicacao(id)).data
  const addTag = async (id, tag) => (await api.addTagToPublicacao(id, tag)).data
  const removeTag = async (id, tag) => (await api.removeTagFromPublicacao(id, tag)).data
  const rate = async (id, rating) => (await api.addRating(id, rating)).data
  const addComentario = async (id, texto) => (await api.addComentario(id, texto)).data
  const deleteComentario = (id) => api.deleteComentario(id)
  const ocultarComentario = (id) => api.ocultarComentario(id)
  const mostrarComentario = (id) => api.mostrarComentario(id)
  const getHistorico = async (id) => (await api.getHistoricoPublicacao(id)).data

  return {
    search,
    get,
    create,
    update,
    regenerarResumo,
    ocultar,
    mostrar,
    addTag,
    removeTag,
    rate,
    addComentario,
    deleteComentario,
    ocultarComentario,
    mostrarComentario,
    getHistorico,
  }
})
