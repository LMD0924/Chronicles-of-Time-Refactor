import axios from 'axios'

export const TOKEN_KEY = 'cot-token'

const http = axios.create({ baseURL: '', timeout: 20000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('cot-user')
  window.dispatchEvent(new Event('cot:unauthorized'))
}

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      const err = new Error(body.message || '请求失败')
      err.code = body.code
      return Promise.reject(err)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    // 登录接口自身的 401 属于「密码错误」，不能清掉本地会话
    if ((status === 401 || status === 403) && !url.includes('/auth/login') && !url.includes('/auth/register')) {
      clearSession()
    }
    const body = error.response?.data
    const err = new Error(body?.message || error.message || '网络异常')
    err.code = body?.code || status
    return Promise.reject(err)
  }
)

export default http
