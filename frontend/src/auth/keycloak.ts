import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url:      import.meta.env.VITE_KEYCLOAK_URL      ?? 'http://localhost:8180',
  realm:    import.meta.env.VITE_KEYCLOAK_REALM    ?? 'enterprise-rag',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'rag-frontend',
})

export default keycloak
