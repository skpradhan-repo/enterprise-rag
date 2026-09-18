import apiClient from './client'
import type { DocumentResponse, PageResponse } from '../types/api'

export const listDocuments = (page = 0, size = 20) =>
  apiClient.get<PageResponse<DocumentResponse>>('/api/v1/documents', { params: { page, size } })
    .then(r => r.data)

export const uploadDocument = (file: File, title: string, tags: string[]) => {
  const form = new FormData()
  form.append('file', file)
  form.append('title', title)
  tags.forEach(t => form.append('tags', t))
  return apiClient.post<DocumentResponse>('/api/v1/documents', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data)
}

export const deleteDocument = (id: string) =>
  apiClient.delete(`/api/v1/documents/${id}`)
