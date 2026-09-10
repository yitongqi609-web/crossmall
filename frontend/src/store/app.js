import { defineStore } from 'pinia'

/**
 * 全局偏好:语言 + 展示币种(localStorage 持久化)
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    locale: localStorage.getItem('cm_locale') || 'zh',
    currency: localStorage.getItem('cm_currency') || 'USD',
    symbol: localStorage.getItem('cm_symbol') || '$'
  }),
  actions: {
    setLocale(locale) {
      this.locale = locale
      localStorage.setItem('cm_locale', locale)
    },
    setCurrency(rate) {
      // rate: { currency, symbol }
      this.currency = rate.currency
      this.symbol = rate.symbol
      localStorage.setItem('cm_currency', rate.currency)
      localStorage.setItem('cm_symbol', rate.symbol)
    }
  }
})
