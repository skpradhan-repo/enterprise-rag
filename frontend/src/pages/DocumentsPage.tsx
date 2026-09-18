import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listDocuments, uploadDocument, deleteDocument } from '../api/documents'
import type { DocumentResponse } from '../types/api'

const STATUS_COLORS: Record<string, string> = {
  INDEXED:    '#1a7f37',
  PROCESSING: '#9a6700',
  UPLOADED:   '#0550ae',
  FAILED:     '#cf222e',
  DELETED:    '#57606a',
}

export const DocumentsPage: React.FC = () => {
  const queryClient = useQueryClient()
  const [file, setFile]   = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [tags, setTags]   = useState('')
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['documents'],
    queryFn:  () => listDocuments(),
  })

  const uploadMutation = useMutation({
    mutationFn: () => uploadDocument(file!, title, tags.split(',').map(t => t.trim()).filter(Boolean)),
    onSuccess:  () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      setFile(null); setTitle(''); setTags(''); setError(null)
    },
    onError: () => setError('Upload failed. Please check the file and try again.'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteDocument(id),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['documents'] }),
  })

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 24 }}>
      <h2 style={{ marginBottom: 24 }}>Documents</h2>

      {/* Upload form */}
      <div style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 20, marginBottom: 32, background: '#f7f8fa' }}>
        <h3 style={{ marginBottom: 16 }}>Upload Document</h3>
        {error && <div style={{ color: '#cf222e', marginBottom: 12 }}>{error}</div>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <input type="file" accept=".pdf,.docx,.txt,.md"
            onChange={e => setFile(e.target.files?.[0] ?? null)} />
          <input value={title} onChange={e => setTitle(e.target.value)}
            placeholder="Document title" style={{ padding: '6px 10px', border: '1px solid #d0d7de', borderRadius: 4 }} />
          <input value={tags} onChange={e => setTags(e.target.value)}
            placeholder="Tags (comma-separated)" style={{ padding: '6px 10px', border: '1px solid #d0d7de', borderRadius: 4 }} />
          <button disabled={!file || !title.trim() || uploadMutation.isPending}
            onClick={() => uploadMutation.mutate()}
            style={{ padding: '8px 20px', background: '#0050e6', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', width: 'fit-content' }}>
            {uploadMutation.isPending ? 'Uploading…' : 'Upload'}
          </button>
        </div>
      </div>

      {/* Document list */}
      {isLoading ? <p>Loading…</p> : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
              <th style={{ padding: '8px 12px' }}>Title</th>
              <th style={{ padding: '8px 12px' }}>Type</th>
              <th style={{ padding: '8px 12px' }}>Status</th>
              <th style={{ padding: '8px 12px' }}>Pages</th>
              <th style={{ padding: '8px 12px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((doc: DocumentResponse) => (
              <tr key={doc.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                <td style={{ padding: '8px 12px' }}>{doc.title}</td>
                <td style={{ padding: '8px 12px' }}>{doc.docType}</td>
                <td style={{ padding: '8px 12px' }}>
                  <span style={{ color: STATUS_COLORS[doc.status] ?? '#1f2328', fontWeight: 600 }}>
                    {doc.status}
                  </span>
                </td>
                <td style={{ padding: '8px 12px' }}>{doc.pageCount ?? '—'}</td>
                <td style={{ padding: '8px 12px' }}>
                  <button onClick={() => deleteMutation.mutate(doc.id)}
                    style={{ color: '#cf222e', background: 'none', border: 'none', cursor: 'pointer' }}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
