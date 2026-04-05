package com.agentbackend.controller;

import com.agentbackend.dto.ApiResponse;
import com.agentbackend.dto.ConversationDTO;
import com.agentbackend.dto.CreateConversationRequest;
import com.agentbackend.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;
    
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }
    
    @PostMapping("/create")
    public ApiResponse<ConversationDTO> createConversation(
            @RequestBody CreateConversationRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = getAuthenticatedUserId();
        }
        if (userId == null) {
            return ApiResponse.error("未登录或登录已过期");
        }
        request.setUserId(userId);
        ConversationDTO conversation = conversationService.createConversation(request);
        return ApiResponse.success(conversation);
    }
    
    @GetMapping("/list")
    public ApiResponse<List<ConversationDTO>> getConversationList(
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = getAuthenticatedUserId();
        }
        if (userId == null) {
            return ApiResponse.error("未登录或登录已过期");
        }
        List<ConversationDTO> conversations = conversationService.getConversationList(userId);
        return ApiResponse.success(conversations);
    }
}
