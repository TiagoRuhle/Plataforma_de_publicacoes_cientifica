import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAPIStore } from './api'

export const useUserStore = defineStore('user', () => {
  const api = useAPIStore()
  const users = ref([])

  const load = async () => {
    users.value = (await api.getAllUsers()).data
  }
  const get = async (username) => (await api.getUser(username)).data
  const create = async (dados) => (await api.createUser(dados)).data
  const update = async (username, dados) => (await api.putUser(username, dados)).data
  const setRole = (username, role) => api.updateRole(username, role)
  const setStatus = (username, active) => api.statusUser(username, active)
  const remove = (username) => api.deleteUser(username)
  const changePassword = (username, oldPassword, newPassword) =>
    api.changePassword(username, oldPassword, newPassword)

  return { users, load, get, create, update, setRole, setStatus, remove, changePassword }
})
