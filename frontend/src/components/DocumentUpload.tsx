import React, { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { uploadDocument } from '../api/documents'

const DocumentUpload: React.FC = () => {
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [tags, setTags] = useState('')
  const [jobId, setJobId] = useState<string | null>(null)
  const qc = useQueryClient()

  const uploadMutation = useMutation({
    mutationFn: () => uploadDocument(
      file!,
      title || file!.name,
      tags ? tags.split(',').map(t => t.trim()).filter(Boolean) : []
    ),
    onSuccess: (doc) => {
      setJobId(doc.id ?? null)
      setFile(null); setTitle(''); setTags('')
      qc.invalidateQueries({ queryKey: ['documents'] })
    },
  })

  return (
    <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 10, padding: 20 }}>
      <h3 style={{ marginBottom: 16, fontSize: 16 }}>Upload Document</h3>

      <input
        type="file"
        accept=".pdf,.docx,.txt,.html,.md"
        onChange={e => setFile(e.target.files?.[0] ?? null)}
        style={{ marginBottom: 12, display: 'block' }}
      />
      <input
        value={title}
        onChange={e => setTitle(e.target.value)}
        placeholder="Title (optional)"
        style={{ width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0',
          borderRadius: 6, marginBottom: 10, fontSize: 14 }}
      />
      <input
        value={tags}
        onChange={e => setTags(e.target.value)}
        placeholder="Tags (comma-separated, e.g. policy,hr)"
        style={{ width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0',
          borderRadius: 6, marginBottom: 14, fontSize: 14 }}
      />
      <button
        onClick={() => uploadMutation.mutate()}
        disabled={!file || uploadMutation.isPending}
        style={{ padding: '9px 20px', background: '#3b82f6', color: '#fff',
          border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14,
          opacity: !file ? 0.5 : 1 }}>
        {uploadMutation.isPending ? 'Uploading...' : 'Upload'}
      </button>

      {jobId && (
        <p style={{ marginTop: 12, fontSize: 13, color: '#16a34a' }}>
          ✓ Ingestion queued — job ID: <code>{jobId}</code>
        </p>
      )}
      {uploadMutation.isError && (
        <p style={{ marginTop: 12, fontSize: 13, color: '#dc2626' }}>
          Upload failed. Please try again.
        </p>
      )}
    </div>
  )
}

export default DocumentUpload
