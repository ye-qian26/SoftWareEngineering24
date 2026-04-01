import React, { useState, useEffect, useRef } from 'react';
import './App.css';
import ConversationList from './components/ConversationList';
import ChatWindow from './components/ChatWindow';
import { createConversation, getConversationList } from './api/conversation';

function App() {
  const [conversations, setConversations] = useState([]);
  const [currentConversation, setCurrentConversation] = useState(null);
  const [userId] = useState(1);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    loadConversations();
  }, []);

  const loadConversations = async () => {
    try {
      const response = await getConversationList(userId);
      if (response.code === 200) {
        setConversations(response.data);
      }
    } catch (error) {
      console.error('加载会话列表失败:', error);
    }
  };

  const handleCreateConversation = async () => {
    try {
      const conversationId = generateUUID();
      const response = await createConversation(conversationId, userId);
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
            messagesEndRef={messagesEndRef}
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

export default App;
