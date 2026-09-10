import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, me } from '../api/mall'

/**
 * 买家会话:token 持久化 + 用户信息
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('mall_token') || '',
    userInfo: null
  }),
  getters: {
    isLogin: state => !!state.token
  },
  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      localStorage.setItem('mall_token', data.token)
    },
    async register(payload) {
      await registerApi(payload)
    },
    async fetchMe() {
      if (!this.token) return
      try {
        this.userInfo = await me()
      } catch (e) {
        this.logout()
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('mall_token')
    }
  }
})
