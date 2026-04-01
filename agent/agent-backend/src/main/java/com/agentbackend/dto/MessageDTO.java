package com.agentbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {
    
    private Long id;
    
    private String conversationId;
    
    private String role;
    
    private String content;
    
    private LocalDateTime createdAt;
}
