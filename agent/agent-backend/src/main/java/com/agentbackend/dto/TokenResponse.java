package com.agentbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    private String token;
    
    private Long userId;
    
    private String username;
    
    private Long expiresIn;
}
