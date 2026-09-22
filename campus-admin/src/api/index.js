import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 8000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    if (body.code === 200) {
      return body.data
    }

    ElMessage.error(body.message || '请求失败')

    const error = new Error(body.message || '请求失败')
    error.code = body.code
    error.response = body
    return Promise.reject(error)
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_info')
      location.href = '/login'
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '网络请求失败')
    return Promise.reject(error)
  }
)

export const get = (url, params) => http.get(url, { params })
export const post = (url, data) => http.post(url, data)
export const put = (url, data) => http.put(url, data)
export const del = url => http.delete(url)

export default http
