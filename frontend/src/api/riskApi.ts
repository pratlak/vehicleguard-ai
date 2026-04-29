import axios from 'axios';
import { AssessRequest, AssessResponse, ChatRequest, ChatResponse } from '../types';

const BASE_URL = '/api/v1';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

export const assessRisk = async (request: AssessRequest): Promise<AssessResponse> => {
  const { data } = await api.post<AssessResponse>('/risk/assess', request);
  return data;
};

export const getQuote = async (quoteId: string): Promise<AssessResponse> => {
  const { data } = await api.get<AssessResponse>(`/risk/quote/${quoteId}`);
  return data;
};

export const sendChatMessage = async (request: ChatRequest): Promise<ChatResponse> => {
  const { data } = await api.post<ChatResponse>('/chat/message', request);
  return data;
};
