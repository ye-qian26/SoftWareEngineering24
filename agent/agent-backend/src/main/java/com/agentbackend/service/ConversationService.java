package com.agentbackend.service;

import com.agentbackend.dto.ConversationDTO;
import com.agentbackend.dto.CreateConversationRequest;
import com.agentbackend.entity.Conversation;
import com.agentbackend.mapper.ConversationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    
    @Autowired
    private ConversationMapper conversationMapper;
    
    public ConversationDTO createConversation(CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        String conversationId = request.getConversationId();
        conversation.setId(conversationId);
        conversation.setUserId(request.getUserId());
        int len = conversationId.length();
        conversation.setTitle("会话 " + conversationId.substring(len - 6, len));
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        
        conversationMapper.insert(conversation);
        
        return convertToDTO(conversation);
    }
    
    public List<ConversationDTO> getConversationList(Long userId) {
        LambdaQueryWrapper<Conversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Conversation::getUserId, userId)
                    .orderByDesc(Conversation::getUpdatedAt);
        
        List<Conversation> conversations = conversationMapper.selectList(queryWrapper);
        
        return conversations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public void updateConversationTitle(String conversationId, String title) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setTitle(title);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }
    
    public void updateConversationTime(String conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }
    
    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        BeanUtils.copyProperties(conversation, dto);
        return dto;
    }
}
