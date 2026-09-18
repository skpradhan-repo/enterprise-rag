import React, { useState, useRef, useEffect } from 'react'
import { ragQuery } from '../api/chat'
import type { ChatResponse, SourceReference } from '../types/api'

interface Message {
  role: 'user' | 'assistant'
  content: string
  sources?: SourceReference[]
  latencyMs?: number
}

interface Props {
  conversationId?: string
}

export const ChatWindow: React.FC<Props> = ({ conversationId }) => {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput]       = useState('')
  const [loading, setLoading]   = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const send = async () => {
    if (!input.trim() || loading) return
    const question = input.trim()
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: question }])
    setLoading(true)

    try {
      const res: ChatResponse = await ragQuery(question, conversationId)
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: res.answer,
        sources: res.sources,
        latencyMs: res.latencyMs,
      }])
    } catch {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'An error occurred. Please try again.',
      }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
        {messages.map((msg, i) => (
          <div key={i} style={{ marginBottom: 16, textAlign: msg.role === 'user' ? 'right' : 'left' }}>
            <div style={{
              display: 'inline-block',
              maxWidth: '80%',
              padding: '10px 14px',
              borderRadius: 8,
              background: msg.role === 'user' ? '#0050e6' : '#f1f3f5',
              color: msg.role === 'user' ? '#fff' : '#1f2328',
            }}>
              {msg.content}
            </div>
            {msg.sources && msg.sources.length > 0 && (
              <div style={{ fontSize: 12, color: '#57606a', marginTop: 6 }}>
                Sources: {msg.sources.map(s => s.documentName).join(', ')}
                {msg.latencyMs && ` · ${msg.latencyMs}ms`}
              </div>
            )}
          </div>
        ))}
        {loading && <div style={{ color: '#57606a', fontStyle: 'italic' }}>Thinking…</div>}
        <div ref={bottomRef} />
      </div>
      <div style={{ display: 'flex', gap: 8, padding: 12, borderTop: '1px solid #e5e7eb' }}>
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && !e.shiftKey && send()}
          placeholder="Ask a question about your documents…"
          disabled={loading}
          style={{ flex: 1, padding: '8px 12px', borderRadius: 6, border: '1px solid #d0d7de', fontSize: 14 }}
        />
        <button onClick={send} disabled={loading || !input.trim()}
          style={{ padding: '8px 20px', background: '#0050e6', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
          Send
        </button>
      </div>
    </div>
  )
}
