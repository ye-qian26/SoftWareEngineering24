import React, { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { sendMessageStream, getChatHistory } from '../api/chat';

function ChatWindow({ conversation, userId, messagesEndRef }) {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [streamingContent, setStreamingContent] = useState('');
  const [streamingMessageId, setStreamingMessageId] = useState(null);
  const [isWaiting, setIsWaiting] = useState(false);
  const chatContainerRef = useRef(null);

  useEffect(() => {
    if (conversation) {
      loadChatHistory();
    }
  }, [conversation]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingContent, isWaiting]);

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
    setStreamingContent('');
    setIsWaiting(true);

    const newUserMessage = {
      id: Date.now(),
      conversationId: conversation.id,
      role: 'user',
      content: userMessage,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, newUserMessage]);

    const tempAssistantMessage = {
      id: Date.now() + 1,
      conversationId: conversation.id,
      role: 'assistant',
      content: '',
      createdAt: new Date().toISOString(),
    };

    let hasReceivedContent = false;

    try {
      await sendMessageStream(
        conversation.id,
        userId,
        userMessage,
        (chunk) => {
          if (!hasReceivedContent) {
            hasReceivedContent = true;
            setIsWaiting(false);
            setMessages((prev) => [...prev, tempAssistantMessage]);
            setStreamingMessageId(tempAssistantMessage.id);
          }
          setStreamingContent((prev) => prev + chunk);
        },
        (fullContent) => {
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === tempAssistantMessage.id
                ? { ...msg, content: fullContent }
                : msg
            )
          );
          setStreamingContent('');
          setStreamingMessageId(null);
          setIsWaiting(false);
        },
        (error) => {
          console.error('发送消息失败:', error);
          setIsWaiting(false);
          if (!hasReceivedContent) {
            setMessages((prev) => [
              ...prev,
              { ...tempAssistantMessage, content: '抱歉，发生了错误，请重试。' },
            ]);
          } else {
            setMessages((prev) =>
              prev.map((msg) =>
                msg.id === tempAssistantMessage.id
                  ? { ...msg, content: '抱歉，发生了错误，请重试。' }
                  : msg
              )
            );
          }
          setStreamingMessageId(null);
        }
      );
    } catch (error) {
      console.error('发送消息失败:', error);
      setIsWaiting(false);
      setStreamingMessageId(null);
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

  const getMessageContent = (message) => {
    if (message.role === 'assistant' && message.id === streamingMessageId) {
      return streamingContent || message.content;
    }
    return message.content;
  };

  const MarkdownRenderer = ({ content }) => {
    return (
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code({ node, inline, className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || '');
            return !inline && match ? (
              <SyntaxHighlighter
                style={oneDark}
                language={match[1]}
                PreTag="div"
                {...props}
              >
                {String(children).replace(/\n$/, '')}
              </SyntaxHighlighter>
            ) : (
              <code className={className} {...props}>
                {children}
              </code>
            );
          },
        }}
      >
        {content}
      </ReactMarkdown>
    );
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
              <div className="message-content markdown-body">
                {message.role === 'user' ? (
                  getMessageContent(message)
                ) : (
                  <>
                    <MarkdownRenderer content={getMessageContent(message)} />
                    {message.id === streamingMessageId && streamingContent && (
                      <span className="cursor-blink">▊</span>
                    )}
                  </>
                )}
              </div>
            </div>
          ))
        )}
        
        {isWaiting && (
          <div className="message assistant-message">
            <div className="message-role">AI</div>
            <div className="message-content loading-dots">
              <span className="dot"></span>
              <span className="dot"></span>
              <span className="dot"></span>
            </div>
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
