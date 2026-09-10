import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 统一封装:token 注入(买家/管理员双 token)、统一错误提示、401 跳转
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const isAdmin = (config.url || '').startsWith('/admin')
  const token = localStorage.getItem(isAdmin ? 'admin_token' : 'mall_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      const isAdmin = (response.config.url || '').startsWith('/admin')
      localStorage.removeItem(isAdmin ? 'admin_token' : 'mall_token')
      ElMessage.error(res.message || '请先登录')
      setTimeout(() => {
        window.location.href = isAdmin ? '/admin/login' : '/login'
      }, 600)
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    const res = error.response?.data
    ElMessage.error(res?.message || '网络异常,请稍后重试')
    return Promise.reject(error)
  }
)

export default request
