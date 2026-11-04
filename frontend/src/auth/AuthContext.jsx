import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { authApi, setUnauthorizedHandler, tokenStore } from '../api.js'

const AuthContext = createContext(null)

// Reads the "exp" claim so an expired token logs the user out straight away
function isExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    const t = tokenStore.get()
    if (t && isExpired(t)) {
      tokenStore.clear()
      return null
    }
    return t
  })
  const [user, setUser] = useState(null)

  const logout = useCallback(() => {
    tokenStore.clear()
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  // Load the profile whenever we have a token
  useEffect(() => {
    if (!token) return
    authApi.me().then(setUser).catch(() => {})
  }, [token])

  const login = async (email, password) => {
    const { access_token } = await authApi.login(email, password)
    tokenStore.set(access_token)
    setToken(access_token)
  }

  const register = async (name, email, password) => {
    await authApi.register(name, email, password)
    await login(email, password) // sign straight in after registering
  }

  return (
    <AuthContext.Provider value={{ token, user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
