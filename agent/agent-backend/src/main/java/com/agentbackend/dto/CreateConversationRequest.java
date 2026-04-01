package com.agentbackend.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    
    private String conversationId;
    
    private Long userId;
}
