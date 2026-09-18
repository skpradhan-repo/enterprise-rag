import React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listConversations, createConversation, deleteConversation } from '../api/conversations'
import type { ConversationResponse } from '../types/api'
import { useNavigate, useParams } from 'react-router-dom'

const ConversationSidebar: React.FC = () => {
  const navigate = useNavigate()
  const { conversationId } = useParams()
  const qc = useQueryClient()

  const { data } = useQuery({
    queryKey: ['conversations'],
    queryFn: () => listConversations(),
  })

  const createMutation = useMutation({
    mutationFn: () => createConversation('New conversation'),
    onSuccess: (conv) => {
      qc.invalidateQueries({ queryKey: ['conversations'] })
      navigate(`/chat/${conv.id}`)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteConversation(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['conversations'] })
      navigate('/chat')
    },
  })

  const sidebarStyle: React.CSSProperties = {
    width: 260, background: '#1e293b', color: '#e2e8f0', display: 'flex',
    flexDirection: 'column', padding: '16px 0',
  }

  return (
    <div style={sidebarStyle}>
      <div style={{ padding: '0 16px 12px', borderBottom: '1px solid #334155' }}>
        <button
          onClick={() => createMutation.mutate()}
          style={{ width: '100%', padding: '8px', background: '#3b82f6', color: '#fff',
            border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14 }}>
          + New conversation
        </button>
      </div>
      <div style={{ overflowY: 'auto', flex: 1, padding: '8px 0' }}>
        {data?.content.map((conv: ConversationResponse) => (
          <div
            key={conv.id}
            onClick={() => navigate(`/chat/${conv.id}`)}
            style={{
              padding: '10px 16px', cursor: 'pointer',
              background: conversationId === conv.id ? '#334155' : 'transparent',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            }}>
            <span style={{ fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {conv.title || 'Conversation'}
            </span>
            <button
              onClick={e => { e.stopPropagation(); deleteMutation.mutate(conv.id) }}
              style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: 16 }}>
              ×
            </button>
          </div>
        ))}
      </div>
      <div style={{ padding: '12px 16px', borderTop: '1px solid #334155' }}>
        <a href="/documents" style={{ color: '#94a3b8', fontSize: 13, textDecoration: 'none' }}>
          📄 Manage Documents
        </a>
      </div>
    </div>
  )
}

export default ConversationSidebar
