// All calls use relative URLs. Locally nginx (or the Vite dev proxy) routes
// them; on AWS CloudFront routes /api/* to the load balancer.
const TOKEN_KEY = 'taskapp_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

// Called when any request comes back 401 (expired/invalid token)
let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

// FastAPI returns { detail: "..." } or { detail: [{loc, msg}] };
// Spring returns RFC 7807 { detail, errors: { field: msg } }.
function parseError(body, status) {
  if (!body) return new ApiError(`Request failed (${status})`, status)
  if (Array.isArray(body.detail)) {
    const fieldErrors = {}
    for (const e of body.detail) {
      const field = e.loc?.[e.loc.length - 1]
      if (field && !fieldErrors[field]) fieldErrors[field] = e.msg.replace(/^Value error, /, '')
    }
    return new ApiError(Object.values(fieldErrors)[0] || 'Invalid input', status, fieldErrors)
  }
  return new ApiError(body.detail || body.message || `Request failed (${status})`, status, body.errors || {})
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError('Cannot reach the server. Please try again.', 0)
  }

  if (res.status === 204) return null
  const data = await res.json().catch(() => null)

  if (!res.ok) {
    if (res.status === 401 && auth) onUnauthorized()
    throw parseError(data, res.status)
  }
  return data
}

export const authApi = {
  register: (name, email, password) =>
    request('/api/auth/register', { method: 'POST', body: { name, email, password }, auth: false }),
  login: (email, password) =>
    request('/api/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  me: () => request('/api/auth/me'),
}

export const taskApi = {
  list: () => request('/api/tasks'),
  create: (task) => request('/api/tasks', { method: 'POST', body: task }),
  update: (id, task) => request(`/api/tasks/${id}`, { method: 'PUT', body: task }),
  updateStatus: (id, status) => request(`/api/tasks/${id}/status`, { method: 'PATCH', body: { status } }),
  remove: (id) => request(`/api/tasks/${id}`, { method: 'DELETE' }),
}

export const notificationApi = {
  list: () => request('/api/notifications'),
  unreadCount: () => request('/api/notifications/unread-count'),
  markAllRead: () => request('/api/notifications/read-all', { method: 'PATCH' }),
}
