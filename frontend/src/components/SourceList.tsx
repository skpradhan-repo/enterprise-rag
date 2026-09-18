import React from 'react'
import type { SourceReference } from '../types/api'

interface Props {
  sources: SourceReference[]
}

const SourceList: React.FC<Props> = ({ sources }) => {
  if (!sources || sources.length === 0) return null

  return (
    <div style={{ marginTop: 12, borderTop: '1px solid #e2e8f0', paddingTop: 10 }}>
      <p style={{ fontSize: 12, color: '#64748b', fontWeight: 600, marginBottom: 6 }}>
        SOURCES ({sources.length})
      </p>
      {sources.map((src, i) => (
        <div key={src.chunkId ?? i} style={{
          background: '#f1f5f9', borderRadius: 6, padding: '8px 12px',
          marginBottom: 6, fontSize: 13,
        }}>
          <div style={{ fontWeight: 600, color: '#334155' }}>
            [{i + 1}] {src.documentName}
            {src.pageNumber && <span style={{ color: '#64748b', fontWeight: 400 }}> — Page {src.pageNumber}</span>}
            {src.similarity != null && (
              <span style={{ float: 'right', color: '#94a3b8', fontSize: 11 }}>
                {(src.similarity * 100).toFixed(0)}% match
              </span>
            )}
          </div>
          {src.excerpt && (
            <p style={{ color: '#475569', marginTop: 4, lineHeight: 1.5 }}>
              {src.excerpt}
            </p>
          )}
        </div>
      ))}
    </div>
  )
}

export default SourceList
