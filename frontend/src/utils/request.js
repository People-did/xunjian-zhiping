import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 🔥 终极绝杀：如果是文件下载（二进制流 Blob 或 Excel），直接一路绿灯放行！不要去判断 code！
    if (
      response.config.responseType === 'blob' || 
      (response.headers['content-type'] && (
        response.headers['content-type'].includes('application/octet-stream') ||
        response.headers['content-type'].includes('application/vnd.ms-excel') ||
        response.headers['content-type'].includes('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
      ))
    ) {
      return response.data
    }

    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.clear()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.clear()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request