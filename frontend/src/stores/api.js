import { defineStore } from 'pinia'
import { http } from '@/lib/http'

const enc = encodeURIComponent
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } }

/** Todas as chamadas à API REST num só sítio. Cada função devolve a promessa do axios. */
export const useAPIStore = defineStore('api', () => {
  // ---- autenticação
  const postLogin = (credentials) =>
    http.post('/auth/login', credentials, { skipAuthRedirect: true })
  const forgotPassword = (email) =>
    http.post('/auth/forgot-password', { email }, { skipAuthRedirect: true })
  const resetPassword = (token, newPassword) =>
    http.post('/auth/reset-password', { token, newPassword }, { skipAuthRedirect: true })

  // ---- utilizadores
  const getAuthUser = () => http.get('/users/me')
  const getAllUsers = () => http.get('/users')
  const getUser = (username) => http.get(`/users/view/${enc(username)}`)
  const createUser = (user) => http.post('/users', user)
  const putUser = (username, { name, email }) =>
    http.put(`/users/${enc(username)}`, { name, email })
  const updateRole = (username, role) => http.patch(`/users/${enc(username)}/updateRole`, { role })
  const statusUser = (username, active) =>
    http.patch(`/users/${enc(username)}/updateStatus`, { active })
  const deleteUser = (username) => http.delete(`/users/${enc(username)}`)
  const changePassword = (username, oldPassword, newPassword) =>
    http.patch(`/users/${enc(username)}/updatePassword`, { oldPassword, newPassword })

  // ---- tags e subscrições
  const getAllTags = () => http.get('/tags/view') // responsável/admin: inclui ocultas
  const getVisibleTags = () => http.get('/tags')
  const createTag = (tag) => http.post('/tags', tag)
  const deleteTag = (tag) => http.delete(`/tags/${enc(tag)}`)
  const getSubscribedTags = () => http.get('/users/view/subscricoes')
  const subscribeTag = (nome) => http.post('/subscricao', { nome })
  const unsubscribeTag = (nome) => http.delete('/subscricao', { data: { nome } })

  // ---- publicações
  const searchPublicacoes = (params) => http.get('/publicacoes/search', { params })
  const getPublicacao = (id) => http.get(`/publicacoes/${id}`)
  const createPublicacao = (formData, onUploadProgress) =>
    http.post('/publicacoes/create', formData, { onUploadProgress })
  const updatePublicacao = (id, dados) => http.put(`/publicacoes/${id}`, dados)
  const regenerarResumo = (id) => http.post(`/publicacoes/${id}/resumo/regenerar`)
  const ocultarPublicacao = (id) => http.put(`/publicacoes/${id}/ocultar`)
  const mostrarPublicacao = (id) => http.put(`/publicacoes/${id}/visivel`)
  const downloadPublicacao = (id) =>
    http.get(`/publicacoes/${id}/download`, { responseType: 'blob' })
  const addTagToPublicacao = (id, tag) =>
    http.post(`/publicacoes/${id}/tags`, JSON.stringify(tag), JSON_HEADERS)
  const removeTagFromPublicacao = (id, tag) =>
    http.delete(`/publicacoes/${id}/tags`, { data: JSON.stringify(tag), ...JSON_HEADERS })
  const addRating = (id, rating) => http.post(`/publicacoes/${id}/rating`, { rating })

  // ---- comentários
  const addComentario = (publicacaoId, texto) => http.post('/comentarios', { publicacaoId, texto })
  const deleteComentario = (id) => http.delete(`/comentarios/${id}`)
  const ocultarComentario = (id) => http.put(`/comentarios/${id}/ocultar`)
  const mostrarComentario = (id) => http.put(`/comentarios/${id}/visivel`)

  // ---- histórico
  const getHistorico = () => http.get('/historico/meu')
  const getUserHistorico = (username) => http.get(`/historico/user/${enc(username)}`)
  const getAllHistorico = () => http.get('/historico')
  const getHistoricoPublicacao = (id) => http.get(`/historico/publicacao/${id}`)

  return {
    postLogin,
    forgotPassword,
    resetPassword,
    getAuthUser,
    getAllUsers,
    getUser,
    createUser,
    putUser,
    updateRole,
    statusUser,
    deleteUser,
    changePassword,
    getAllTags,
    getVisibleTags,
    createTag,
    deleteTag,
    getSubscribedTags,
    subscribeTag,
    unsubscribeTag,
    searchPublicacoes,
    getPublicacao,
    createPublicacao,
    updatePublicacao,
    regenerarResumo,
    ocultarPublicacao,
    mostrarPublicacao,
    downloadPublicacao,
    addTagToPublicacao,
    removeTagFromPublicacao,
    addRating,
    addComentario,
    deleteComentario,
    ocultarComentario,
    mostrarComentario,
    getHistorico,
    getUserHistorico,
    getAllHistorico,
    getHistoricoPublicacao,
  }
})
