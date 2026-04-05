import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const register = async (username, password) => {
  const response = await api.post('/user/register', { username, password });
  return response.data;
};

export const login = async (username, password) => {
  const response = await api.post('/user/login', { username, password });
  return response.data;
};

export const getUserInfo = async () => {
  const response = await api.get('/user/info');
  return response.data;
};

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

export const sendMessage = async (conversationId, userId, message) => {
  const response = await api.post('/chat/send', {
    conversationId,
    userId,
    message,
  });
  return response.data;
};

export const sendMessageStream = async (conversationId, userId, message, onChunk, onComplete, onError) => {
  try {
    const response = await fetch(`${API_BASE_URL}/chat/send/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({
        conversationId,
        userId,
        message,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullContent = '';
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      
      let dataLines = [];
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        
        if (line.startsWith('data:')) {
          const data = line.slice(5);
          
          if (data === '[DONE]') {
            continue;
          }
          
          dataLines.push(data);
        } else if (line === '' && dataLines.length > 0) {
          const message = dataLines.join('\n');
          fullContent += message;
          if (onChunk) {
            onChunk(message);
          }
          dataLines = [];
        }
      }
      
      if (dataLines.length > 0) {
        const message = dataLines.join('\n');
        fullContent += message;
        if (onChunk) {
          onChunk(message);
        }
      }
    }

    if (onComplete) {
      onComplete(fullContent);
    }

    return fullContent;
  } catch (error) {
    if (onError) {
      onError(error);
    }
    throw error;
  }
};

export const getChatHistory = async (conversationId) => {
  const response = await api.get('/chat/history', {
    params: { conversationId },
  });
  return response.data;
};
