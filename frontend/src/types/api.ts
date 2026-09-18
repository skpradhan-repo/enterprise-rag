// API type definitions matching the Spring Boot response DTOs

export interface DocumentResponse {
  id:            string
  title:         string
  fileName:      string
  mimeType:      string
  fileSizeBytes: number
  docType:       string
  status:        'UPLOADED' | 'PROCESSING' | 'INDEXED' | 'FAILED' | 'DELETED'
  pageCount:     number | null
  tags:          string[]
  createdAt:     string
  updatedAt:     string
}

export interface SourceReference {
  documentId:   string
  documentName: string
  pageNumber:   number | null
  chunkId:      string
  similarity:   number
  excerpt:      string
}

export interface ChatResponse {
  answer:         string
  conversationId: string
  correlationId:  string
  model:          string
  sources:        SourceReference[]
  latencyMs:      number
  respondedAt:    string
}

export interface ConversationResponse {
  id:        string
  title:     string | null
  createdAt: string
  updatedAt: string
}

export interface LlmDemoResponse {
  response:  string
  model:     string
  provider:  string
  latencyMs: number
}

export interface ApiError {
  status:       number
  title:        string
  detail:       string
  correlationId: string
  violations:   { field: string; message: string }[]
  timestamp:    string
}

export interface PageResponse<T> {
  content:          T[]
  totalElements:    number
  totalPages:       number
  number:           number
  size:             number
}
