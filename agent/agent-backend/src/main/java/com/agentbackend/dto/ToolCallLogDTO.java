package com.agentbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolCallLogDTO {
    
    private Long id;
    
    private String conversationId;
    
    private String toolName;
    
    private String toolArgs;
    
    private String result;
    
    private Boolean success;
    
    private String errorMessage;
    
    private LocalDateTime createdAt;
}
