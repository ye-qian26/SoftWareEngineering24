import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const sendMessage = async (conversationId, userId, message) => {
  const response = await api.post('/chat/send', {
    conversationId,
    userId,
    message,
  });
  return response.data;
};

export const getChatHistory = async (conversationId) => {
  const response = await api.get('/chat/history', {
    params: { conversationId },
  });
  return response.data;
};
