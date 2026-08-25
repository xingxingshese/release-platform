import { defineStore } from 'pinia'
import { api, clearToken, getToken, setToken } from '../../api/client'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken() ?? '',
    username: ''
  }),
  getters: {
    isLoggedIn: (state) => state.token.length > 0
  },
  actions: {
    async login(username: string, password: string) {
      const result = await api.post<{ token: string; username: string }>('/api/auth/login', {
        username,
        password
      })
      this.token = result.token
      this.username = result.username
      setToken(result.token)
    },
    logout() {
      this.token = ''
      this.username = ''
      clearToken()
    }
  }
})
