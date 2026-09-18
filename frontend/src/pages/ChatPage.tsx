import React, { useState } from 'react'
import { ChatWindow } from '../components/ChatWindow'

export const ChatPage: React.FC = () => {
  const [conversationId] = useState<string | undefined>(undefined)

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 64px)' }}>
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '16px 24px', borderBottom: '1px solid #e5e7eb' }}>
          <h2 style={{ margin: 0 }}>RAG Chat</h2>
          <p style={{ margin: '4px 0 0', color: '#57606a', fontSize: 13 }}>
            Ask questions about your uploaded documents. Answers are grounded in your knowledge base.
          </p>
        </div>
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <ChatWindow conversationId={conversationId} />
        </div>
      </main>
    </div>
  )
}
