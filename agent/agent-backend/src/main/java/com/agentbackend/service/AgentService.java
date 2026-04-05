package com.agentbackend.service;

import com.agentbackend.entity.Message;
import com.agentbackend.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentService {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private MessageMapper messageMapper;
    
    public String chatWithTools(String conversationId, String userMessage) {
        List<Message> historyMessages = getHistoryMessages(conversationId);
        List<org.springframework.ai.chat.messages.Message> messages = buildMessages(historyMessages, userMessage, conversationId);
        
        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }
    
    public Flux<String> chatWithToolsStream(String conversationId, String userMessage) {
        List<Message> historyMessages = getHistoryMessages(conversationId);
        List<org.springframework.ai.chat.messages.Message> messages = buildMessages(historyMessages, userMessage, conversationId);
        
        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content();
    }
    
    private List<Message> getHistoryMessages(String conversationId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getConversationId, conversationId)
                    .orderByAsc(Message::getCreatedAt);
        return messageMapper.selectList(queryWrapper);
    }
    
    private List<org.springframework.ai.chat.messages.Message> buildMessages(List<Message> historyMessages, String currentMessage, String conversationId) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        
        messages.add(new SystemMessage(buildSystemPrompt(conversationId)));
        
        for (Message msg : historyMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        
        messages.add(new UserMessage(currentMessage));
        return messages;
    }
    
    private String buildSystemPrompt(String conversationId) {
        return """
            你是一个友好的AI助手，请用中文回答用户的问题。
            
            当前会话ID: %s
            
            你可以使用以下工具来帮助回答问题：
            1. getConversationHistory - 当用户询问历史消息、之前说了什么等问题时使用，需要传入会话ID
            2. getConversationSummary - 当用户要求总结对话时使用，需要传入会话ID
            3. calculate - 当用户请求计算数学表达式时使用
            
            重要说明:
            - 调用 getConversationHistory 或 getConversationSummary 时，必须使用上面提供的当前会话ID
            - 如果用户的消息不包含明确的计算表达式，不要调用 calculate 工具
            - 如果用户没有明确询问历史消息，不要调用 getConversationHistory 工具
            - 如果用户没有明确要求总结，不要调用 getConversationSummary 工具
            
            请根据用户的问题智能选择是否需要调用工具。
            如果用户的问题不需要工具，请直接回答。
            """.formatted(conversationId);
    }
}
