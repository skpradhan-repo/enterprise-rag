import apiClient from './client'
import type { ChatResponse, LlmDemoResponse } from '../types/api'

export const ragQuery = (question: string, conversationId?: string) =>
  apiClient.post<ChatResponse>('/api/v1/rag/query', { question, conversationId })
    .then(r => r.data)

export const llmDemo = (prompt: string) =>
  apiClient.post<LlmDemoResponse>('/api/v1/llm/demo', { prompt })
    .then(r => r.data)
