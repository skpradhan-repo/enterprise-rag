import React, { createContext, useContext, useEffect, useState } from 'react'
import keycloak from './keycloak'

interface AuthContextValue {
  isAuthenticated: boolean
  isLoading: boolean
  token: string | undefined
  userEmail: string | undefined
  tenantId: string | undefined
  roles: string[]
  logout: () => void
}

const AuthContext = createContext<AuthContextValue>({
  isAuthenticated: false,
  isLoading: true,
  token: undefined,
  userEmail: undefined,
  tenantId: undefined,
  roles: [],
  logout: () => {},
})

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading]             = useState(true)

  useEffect(() => {
    keycloak
      .init({ onLoad: 'login-required', pkceMethod: 'S256' })
      .then(auth => {
        setIsAuthenticated(auth)
        setIsLoading(false)

        // Refresh token 60 seconds before expiry
        setInterval(() => {
          keycloak.updateToken(60).catch(() => keycloak.logout())
        }, 30_000)
      })
      .catch(() => setIsLoading(false))
  }, [])

  const parsedToken = keycloak.tokenParsed as Record<string, unknown> | undefined

  return (
    <AuthContext.Provider value={{
      isAuthenticated,
      isLoading,
      token:     keycloak.token,
      userEmail: parsedToken?.['email'] as string | undefined,
      tenantId:  parsedToken?.['tenant_id'] as string | undefined,
      roles:     (keycloak.realmAccess?.roles ?? []),
      logout:    () => keycloak.logout(),
    }}>
      {isLoading ? <div style={{ padding: 32 }}>Authenticating…</div> : children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
