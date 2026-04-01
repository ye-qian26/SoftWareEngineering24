package com.agentbackend.service;

import com.agentbackend.dto.MessageDTO;
import com.agentbackend.dto.SendMessageRequest;
import com.agentbackend.dto.SendMessageResponse;
import com.agentbackend.entity.Message;
import com.agentbackend.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private ChatClient.Builder chatClientBuilder;
    
    @Transactional
    public SendMessageResponse sendMessage(SendMessageRequest request) {
        saveUserMessage(request.getConversationId(), request.getMessage());
        
        List<Message> historyMessages = getHistoryMessages(request.getConversationId());
        
        String aiReply = callAI(historyMessages, request.getMessage());
        
        saveAssistantMessage(request.getConversationId(), aiReply);
        
        conversationService.updateConversationTime(request.getConversationId());
        
        if (historyMessages.isEmpty()) {
            String title = generateTitle(request.getMessage());
            conversationService.updateConversationTitle(request.getConversationId(), title);
        }
        
        SendMessageResponse response = new SendMessageResponse();
        response.setReply(aiReply);
        return response;
    }
    
    public List<MessageDTO> getChatHistory(String conversationId) {
        List<Message> messages = getHistoryMessages(conversationId);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private void saveUserMessage(String conversationId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }
    
    private void saveAssistantMessage(String conversationId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("assistant");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }
    
    private List<Message> getHistoryMessages(String conversationId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getConversationId, conversationId)
                    .orderByAsc(Message::getCreatedAt);
        return messageMapper.selectList(queryWrapper);
    }
    
    private String callAI(List<Message> historyMessages, String currentMessage) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        
        messages.add(new SystemMessage("你是一个友好的AI助手，请用中文回答用户的问题。"));
        
        for (Message msg : historyMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        
        messages.add(new UserMessage(currentMessage));
        
        Prompt prompt = new Prompt(messages);
        
        ChatClient chatClient = chatClientBuilder.build();
        
        return chatClient.prompt(prompt).call().content();
    }
    
    private String generateTitle(String firstMessage) {
        if (firstMessage.length() <= 20) {
            return firstMessage;
        }
        return firstMessage.substring(0, 20) + "...";
    }
    
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }
}
