import React from 'react'
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider, useAuth } from './auth/AuthProvider'
import { ChatPage } from './pages/ChatPage'
import { DocumentsPage } from './pages/DocumentsPage'
import { LlmDemoPage } from './pages/LlmDemoPage'

const queryClient = new QueryClient()

const NAV_LINKS = [
  { to: '/',        label: 'RAG Chat' },
  { to: '/documents', label: 'Documents' },
  { to: '/llm-demo',  label: 'LLM Demo' },
]

const AppNav: React.FC = () => {
  const { userEmail, tenantId, logout } = useAuth()
  return (
    <nav style={{
      display: 'flex', alignItems: 'center', gap: 24,
      padding: '0 24px', height: 56,
      background: '#161b22', color: '#e6edf3',
      borderBottom: '1px solid #30363d',
    }}>
      <span style={{ fontWeight: 700, fontSize: 16, marginRight: 16 }}>Enterprise RAG</span>
      {NAV_LINKS.map(l => (
        <NavLink key={l.to} to={l.to} style={({ isActive }) => ({
          color: isActive ? '#58a6ff' : '#e6edf3', textDecoration: 'none', fontSize: 14,
        })}>
          {l.label}
        </NavLink>
      ))}
      <span style={{ marginLeft: 'auto', fontSize: 13, color: '#8b949e' }}>
        {userEmail} · <em>{tenantId}</em>
      </span>
      <button onClick={logout}
        style={{ padding: '4px 14px', background: '#30363d', color: '#e6edf3', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
        Logout
      </button>
    </nav>
  )
}

const App: React.FC = () => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <BrowserRouter>
        <AppNav />
        <Routes>
          <Route path="/"          element={<ChatPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/llm-demo"  element={<LlmDemoPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </QueryClientProvider>
)

export default App
