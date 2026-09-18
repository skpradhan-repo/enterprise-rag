import React, { useState } from 'react'
import { llmDemo } from '../api/chat'
import type { LlmDemoResponse } from '../types/api'

export const LlmDemoPage: React.FC = () => {
  const [prompt, setPrompt]         = useState('')
  const [response, setResponse]     = useState<LlmDemoResponse | null>(null)
  const [loading, setLoading]       = useState(false)
  const [error, setError]           = useState<string | null>(null)

  const run = async () => {
    if (!prompt.trim()) return
    setLoading(true); setError(null); setResponse(null)
    try {
      const res = await llmDemo(prompt)
      setResponse(res)
    } catch {
      setError('LLM call failed. Is Ollama running?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 24 }}>
      <h2>LLM Client / Server Demo</h2>
      <p style={{ color: '#57606a', marginBottom: 24 }}>
        Demonstrates direct <strong>Spring AI ChatClient → Ollama LLM Server</strong> communication
        without RAG. No vector search, no document context.
      </p>

      <div style={{ background: '#f7f8fa', border: '1px solid #e5e7eb', borderRadius: 8, padding: 16, marginBottom: 24, fontSize: 13 }}>
        <strong>Architecture:</strong>
        <pre style={{ margin: '8px 0 0', fontFamily: 'monospace', fontSize: 12 }}>
{`React → Spring Boot → Spring AI ChatClient → Ollama → Local LLM → Response`}
        </pre>
      </div>

      <textarea value={prompt} onChange={e => setPrompt(e.target.value)}
        rows={4} placeholder="Enter any prompt (e.g. 'Explain RAG in 3 sentences')"
        style={{ width: '100%', padding: '10px 12px', border: '1px solid #d0d7de', borderRadius: 6, fontSize: 14, resize: 'vertical', boxSizing: 'border-box' }} />
      <button onClick={run} disabled={loading || !prompt.trim()}
        style={{ marginTop: 12, padding: '8px 24px', background: '#0050e6', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
        {loading ? 'Calling LLM…' : 'Send to Ollama'}
      </button>

      {error && <p style={{ color: '#cf222e', marginTop: 16 }}>{error}</p>}

      {response && (
        <div style={{ marginTop: 24, border: '1px solid #e5e7eb', borderRadius: 8, padding: 20 }}>
          <div style={{ display: 'flex', gap: 16, marginBottom: 12, fontSize: 13, color: '#57606a' }}>
            <span>Model: <strong>{response.model}</strong></span>
            <span>Provider: <strong>{response.provider}</strong></span>
            <span>Latency: <strong>{response.latencyMs}ms</strong></span>
          </div>
          <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{response.response}</div>
        </div>
      )}
    </div>
  )
}
