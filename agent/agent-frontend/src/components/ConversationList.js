import React from 'react';

function ConversationList({ conversations, currentConversation, onSelectConversation }) {
  return (
    <div className="conversation-list">
      {conversations.length === 0 ? (
        <div className="empty-list">
          <p>暂无会话</p>
        </div>
      ) : (
        conversations.map((conversation) => (
          <div
            key={conversation.id}
            className={`conversation-item ${
              currentConversation && currentConversation.id === conversation.id
                ? 'active'
                : ''
            }`}
            onClick={() => onSelectConversation(conversation)}
          >
            <div className="conversation-title">{conversation.title}</div>
            <div className="conversation-time">
              {new Date(conversation.updatedAt).toLocaleString('zh-CN', {
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </div>
          </div>
        ))
      )}
    </div>
  );
}

export default ConversationList;
