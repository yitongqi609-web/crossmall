import { defineStore } from 'pinia'
import { cartCount } from '../api/mall'

export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0
  }),
  actions: {
    async refresh() {
      const token = localStorage.getItem('mall_token')
      if (!token) {
        this.count = 0
        return
      }
      try {
        this.count = await cartCount()
      } catch (e) {
        this.count = 0
      }
    },
    set(count) {
      this.count = count
    }
  }
})
