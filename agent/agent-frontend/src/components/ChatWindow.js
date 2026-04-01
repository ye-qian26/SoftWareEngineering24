import React, { useState, useEffect, useRef } from 'react';
import { sendMessage, getChatHistory } from '../api/chat';

function ChatWindow({ conversation, userId, messagesEndRef }) {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const chatContainerRef = useRef(null);

  useEffect(() => {
    if (conversation) {
      loadChatHistory();
    }
  }, [conversation]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadChatHistory = async () => {
    try {
      const response = await getChatHistory(conversation.id);
      if (response.code === 200) {
        setMessages(response.data);
      }
    } catch (error) {
      console.error('加载聊天记录失败:', error);
    }
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || loading) return;

    const userMessage = inputMessage.trim();
    setInputMessage('');
    setLoading(true);

    const newUserMessage = {
      id: Date.now(),
      conversationId: conversation.id,
      role: 'user',
      content: userMessage,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, newUserMessage]);

    try {
      const response = await sendMessage(conversation.id, userId, userMessage);
      if (response.code === 200) {
        const assistantMessage = {
          id: Date.now() + 1,
          conversationId: conversation.id,
          role: 'assistant',
          content: response.data.reply,
          createdAt: new Date().toISOString(),
        };
        setMessages((prev) => [...prev, assistantMessage]);
      }
    } catch (error) {
      console.error('发送消息失败:', error);
      alert('发送消息失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const scrollToBottom = () => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  };

  return (
    <div className="chat-window">
      <div className="chat-header">
        <h3>{conversation.title}</h3>
      </div>
      <div className="chat-messages" ref={chatContainerRef}>
        {messages.length === 0 ? (
          <div className="empty-messages">
            <p>开始新的对话吧！</p>
          </div>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={`message ${
                message.role === 'user' ? 'user-message' : 'assistant-message'
              }`}
            >
              <div className="message-role">
                {message.role === 'user' ? '我' : 'AI'}
              </div>
              <div className="message-content">{message.content}</div>
            </div>
          ))
        )}
        {loading && (
          <div className="message assistant-message">
            <div className="message-role">AI</div>
            <div className="message-content loading">正在思考中...</div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>
      <div className="chat-input">
        <textarea
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="输入消息... (Enter发送)"
          disabled={loading}
        />
        <button onClick={handleSendMessage} disabled={loading || !inputMessage.trim()}>
          {loading ? '发送中...' : '发送'}
        </button>
      </div>
    </div>
  );
}

export default ChatWindow;
