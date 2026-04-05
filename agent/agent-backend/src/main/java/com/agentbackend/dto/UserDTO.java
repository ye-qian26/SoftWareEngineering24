package com.agentbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    
    private Long id;
    
    private String username;
    
    private LocalDateTime createdAt;
}
