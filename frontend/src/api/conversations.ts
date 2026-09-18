import apiClient from './client';
import type { ConversationResponse, PageResponse } from '../types/api';

export const createConversation = async (title?: string): Promise<ConversationResponse> => {
  const { data } = await apiClient.post<ConversationResponse>('/api/v1/conversations', { title });
  return data;
};

export const listConversations = async (page = 0): Promise<PageResponse<ConversationResponse>> => {
  const { data } = await apiClient.get<PageResponse<ConversationResponse>>(
    `/api/v1/conversations?page=${page}&size=30`
  );
  return data;
};

export const getConversation = async (id: string): Promise<ConversationResponse> => {
  const { data } = await apiClient.get<ConversationResponse>(`/api/v1/conversations/${id}`);
  return data;
};

export const deleteConversation = async (id: string): Promise<void> => {
  await apiClient.delete(`/api/v1/conversations/${id}`);
};
