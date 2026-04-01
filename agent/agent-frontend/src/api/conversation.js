import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const createConversation = async (conversationId, userId) => {
  const response = await api.post('/conversation/create', {
    conversationId,
    userId,
  });
  return response.data;
};

export const getConversationList = async (userId) => {
  const response = await api.get('/conversation/list', {
    params: { userId },
  });
  return response.data;
};
