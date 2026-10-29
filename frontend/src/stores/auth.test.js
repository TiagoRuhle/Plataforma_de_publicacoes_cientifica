import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/lib/http', () => {
  const guardado = { token: null }
  return {
    http: { get: vi.fn(), post: vi.fn() },
    tokenStorage: {
      get: () => guardado.token,
      set: (t) => (guardado.token = t),
      clear: () => (guardado.token = null),
    },
  }
})

import { http, tokenStorage } from '@/lib/http'
import { useAuthStore } from './auth'

beforeEach(() => {
  setActivePinia(createPinia())
  tokenStorage.clear()
  vi.clearAllMocks()
})

describe('auth store', () => {
  it('login guarda o token (sem aspas) e carrega o utilizador', async () => {
    http.post.mockResolvedValue({ data: '"abc.def.ghi"' })
    http.get.mockResolvedValue({ data: { username: 'resp', name: 'R', role: 'RESPONSAVEL' } })
    const auth = useAuthStore()

    await auth.login({ username: 'resp', password: 'x' })

    expect(tokenStorage.get()).toBe('abc.def.ghi')
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.isGestor).toBe(true)
    expect(auth.isAdmin).toBe(false)
  })

  it('login falhado não deixa token nem sessão', async () => {
    http.post.mockRejectedValue({ response: { status: 401 } })
    const auth = useAuthStore()
    await expect(auth.login({ username: 'a', password: 'b' })).rejects.toBeTruthy()
    expect(tokenStorage.get()).toBeNull()
    expect(auth.isLoggedIn).toBe(false)
  })

  it('se /users/me falhar depois do login, limpa o token', async () => {
    http.post.mockResolvedValue({ data: 'tok' })
    http.get.mockRejectedValue({ response: { status: 401 } })
    const auth = useAuthStore()
    await expect(auth.login({ username: 'a', password: 'b' })).rejects.toBeTruthy()
    expect(tokenStorage.get()).toBeNull()
  })

  it('init restaura a sessão a partir do token guardado (sobrevive ao F5)', async () => {
    tokenStorage.set('guardado')
    http.get.mockResolvedValue({ data: { username: 'col', role: 'COLABORADOR' } })
    const auth = useAuthStore()
    await auth.init()
    await auth.init() // só corre uma vez
    expect(http.get).toHaveBeenCalledTimes(1)
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.isGestor).toBe(false)
  })

  it('init sem token não chama a API; com token inválido limpa-o', async () => {
    const semToken = useAuthStore()
    await semToken.init()
    expect(http.get).not.toHaveBeenCalled()

    setActivePinia(createPinia())
    tokenStorage.set('expirado')
    http.get.mockRejectedValue({ response: { status: 401 } })
    const comToken = useAuthStore()
    await comToken.init()
    expect(comToken.isLoggedIn).toBe(false)
    expect(tokenStorage.get()).toBeNull()
  })

  it('logout limpa token e utilizador', async () => {
    http.post.mockResolvedValue({ data: 'tok' })
    http.get.mockResolvedValue({ data: { username: 'a', role: 'ADMINISTRADOR' } })
    const auth = useAuthStore()
    await auth.login({ username: 'a', password: 'b' })
    expect(auth.isAdmin).toBe(true)
    auth.logout()
    expect(auth.isLoggedIn).toBe(false)
    expect(tokenStorage.get()).toBeNull()
  })
})
