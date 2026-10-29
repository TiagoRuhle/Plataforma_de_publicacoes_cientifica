import axios from 'axios'
import { API_BASE_URL } from './config'

const TOKEN_KEY = 'dae.token'

/** Persistência do JWT (sobrevive a recarregar a página). */
export const tokenStorage = {
  get() {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token) {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* armazenamento indisponível: a sessão dura só até recarregar */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* ignorar */
    }
  },
}

export const http = axios.create({ baseURL: API_BASE_URL })

http.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let onUnauthorized = () => {}

/** Chamado quando a API responde 401 a um pedido autenticado (sessão expirada ou conta suspensa). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.skipAuthRedirect) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)
