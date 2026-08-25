/** 统一 API Client：携带 JWT，处理统一错误模型。 */

const TOKEN_KEY = 'rap_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  constructor(public code: string, message: string) {
    super(message)
  }
}

interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  const resp = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  if (!resp.ok && resp.status === 401) {
    clearToken()
    throw new ApiError('AUTH_ERROR', '请重新登录')
  }
  const json = (await resp.json()) as ApiResponse<T>
  if (!resp.ok || json.code !== 'OK') {
    throw new ApiError(json.code ?? 'SYSTEM_ERROR', json.message ?? 'request failed')
  }
  return json.data
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body)
}
