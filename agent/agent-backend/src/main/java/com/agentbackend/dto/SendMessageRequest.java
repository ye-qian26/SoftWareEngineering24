package com.agentbackend.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    
    private String conversationId;
    
    private Long userId;
    
    private String message;
}
