package com.agentbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDTO {
    
    private String id;
    
    private Long userId;
    
    private String title;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
