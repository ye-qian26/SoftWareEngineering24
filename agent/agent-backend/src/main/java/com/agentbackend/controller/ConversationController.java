package com.agentbackend.controller;

import com.agentbackend.dto.ApiResponse;
import com.agentbackend.dto.ConversationDTO;
import com.agentbackend.dto.CreateConversationRequest;
import com.agentbackend.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;
    
    @PostMapping("/create")
    public ApiResponse<ConversationDTO> createConversation(@RequestBody CreateConversationRequest request) {
        ConversationDTO conversation = conversationService.createConversation(request);
        return ApiResponse.success(conversation);
    }
    
    @GetMapping("/list")
    public ApiResponse<List<ConversationDTO>> getConversationList(@RequestParam Long userId) {
        List<ConversationDTO> conversations = conversationService.getConversationList(userId);
        return ApiResponse.success(conversations);
    }
}
