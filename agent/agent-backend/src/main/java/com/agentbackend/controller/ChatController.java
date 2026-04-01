package com.agentbackend.controller;

import com.agentbackend.dto.ApiResponse;
import com.agentbackend.dto.MessageDTO;
import com.agentbackend.dto.SendMessageRequest;
import com.agentbackend.dto.SendMessageResponse;
import com.agentbackend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @PostMapping("/send")
    public ApiResponse<SendMessageResponse> sendMessage(@RequestBody SendMessageRequest request) {
        SendMessageResponse response = chatService.sendMessage(request);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/history")
    public ApiResponse<List<MessageDTO>> getChatHistory(@RequestParam String conversationId) {
        List<MessageDTO> messages = chatService.getChatHistory(conversationId);
        return ApiResponse.success(messages);
    }
}
