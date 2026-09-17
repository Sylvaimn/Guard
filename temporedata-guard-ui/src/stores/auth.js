import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('zqy_token') || '',
    user: JSON.parse(localStorage.getItem('zqy_user') || 'null'),
    isLoggedIn: !!localStorage.getItem('zqy_token')
  }),

  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      this.isLoggedIn = true
      localStorage.setItem('zqy_token', token)
      localStorage.setItem('zqy_user', JSON.stringify(user))
    },

    logout() {
      this.token = ''
      this.user = null
      this.isLoggedIn = false
      localStorage.removeItem('zqy_token')
      localStorage.removeItem('zqy_user')
    }
  }
})