import axios from 'axios'
import keycloak from '../auth/keycloak'

// Empty baseURL means all requests use relative paths (e.g. /api/v1/...).
// In the browser the frontend nginx proxy forwards /api/ → http://app:8090.
// In local dev (npm run dev with proxy in vite.config.ts) the Vite proxy handles it.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
})

// Attach Bearer token to every request
apiClient.interceptors.request.use(config => {
  if (keycloak.token) {
    config.headers['Authorization'] = `Bearer ${keycloak.token}`
  }
  return config
})

// On 401, try token refresh then retry once
apiClient.interceptors.response.use(
  res => res,
  async error => {
    if (error.response?.status === 401) {
      try {
        await keycloak.updateToken(30)
        error.config.headers['Authorization'] = `Bearer ${keycloak.token}`
        return axios(error.config)
      } catch {
        keycloak.logout()
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
