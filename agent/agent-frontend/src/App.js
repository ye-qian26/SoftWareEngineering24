import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import './App.css';
import ConversationList from './components/ConversationList';
import ChatWindow from './components/ChatWindow';
import Login from './pages/Login';
import Register from './pages/Register';
import { createConversation, getConversationList } from './api/user';

function ChatApp() {
  const [conversations, setConversations] = useState([]);
  const [currentConversation, setCurrentConversation] = useState(null);
  const [userId, setUserId] = useState(() => parseInt(localStorage.getItem('userId') || '0'));
  const [username, setUsername] = useState(localStorage.getItem('username') || '');
  const navigate = useNavigate();

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login');
    } else {
      loadConversations();
      setUserId(parseInt(localStorage.getItem('userId') || '0'));
      setUsername(localStorage.getItem('username') || '');
    }
  }, [navigate]);

  const loadConversations = async () => {
    try {
      const storedUserId = parseInt(localStorage.getItem('userId') || '0');
      if (storedUserId) {
        const response = await getConversationList(storedUserId);
        if (response.code === 200) {
          setConversations(response.data);
        }
      }
    } catch (error) {
      console.error('加载会话列表失败:', error);
    }
  };

  const handleCreateConversation = async () => {
    try {
      const storedUserId = parseInt(localStorage.getItem('userId') || '0');
      const conversationId = generateUUID();
      const response = await createConversation(conversationId, storedUserId);
      if (response.code === 200) {
        setConversations([response.data, ...conversations]);
        setCurrentConversation(response.data);
      }
    } catch (error) {
      console.error('创建会话失败:', error);
    }
  };

  const handleSelectConversation = (conversation) => {
    setCurrentConversation(conversation);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    navigate('/login');
  };

  const generateUUID = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  };

  return (
    <div className="app">
      <div className="sidebar">
        <div className="sidebar-header">
          <h2>AI 对话助手</h2>
          <div className="user-info">
            <span>欢迎, {username}</span>
            <button className="logout-btn" onClick={handleLogout}>退出</button>
          </div>
          <button className="new-chat-btn" onClick={handleCreateConversation}>
            + 新建会话
          </button>
        </div>
        <ConversationList
          conversations={conversations}
          currentConversation={currentConversation}
          onSelectConversation={handleSelectConversation}
        />
      </div>
      <div className="main-content">
        {currentConversation ? (
          <ChatWindow
            conversation={currentConversation}
            userId={userId}
          />
        ) : (
          <div className="empty-state">
            <h2>欢迎使用 AI 对话助手</h2>
            <p>请点击左侧"新建会话"开始对话</p>
          </div>
        )}
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/" element={<ChatApp />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
