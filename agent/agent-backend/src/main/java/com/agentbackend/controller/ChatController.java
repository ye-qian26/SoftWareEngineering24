package com.agentbackend.controller;

import com.agentbackend.dto.ApiResponse;
import com.agentbackend.dto.MessageDTO;
import com.agentbackend.dto.SendMessageRequest;
import com.agentbackend.dto.SendMessageResponse;
import com.agentbackend.service.ChatService;
import com.agentbackend.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ConversationService conversationService;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
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
    
    @PostMapping("/send")
    public ApiResponse<SendMessageResponse> sendMessage(
            @RequestBody SendMessageRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = getAuthenticatedUserId();
        }
        if (userId == null) {
            return ApiResponse.error("未登录或登录已过期");
        }
        conversationService.validateConversationOwnership(request.getConversationId(), userId);
        request.setUserId(userId);
        SendMessageResponse response = chatService.sendMessage(request);
        return ApiResponse.success(response);
    }
    
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @RequestBody SendMessageRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = getAuthenticatedUserId();
        }
        
        if (userId == null) {
            SseEmitter emitter = new SseEmitter();
            executorService.execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}")
                            .build());
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            });
            return emitter;
        }
        
        conversationService.validateConversationOwnership(request.getConversationId(), userId);
        request.setUserId(userId);
        
        SseEmitter emitter = new SseEmitter(180_000L);
        
        executorService.execute(() -> {
            try {
                StringBuilder fullContent = new StringBuilder();
                
                chatService.sendMessageStream(request)
                        .doOnNext(chunk -> {
                            try {
                                fullContent.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .data(chunk)
                                        .build());
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                chatService.saveAssistantMessage(request.getConversationId(), fullContent.toString());
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(emitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        emitter.onCompletion(() -> {
        });
        
        emitter.onTimeout(() -> {
            emitter.complete();
        });
        
        return emitter;
    }
    
    @GetMapping("/history")
    public ApiResponse<List<MessageDTO>> getChatHistory(
            @RequestParam String conversationId,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = getAuthenticatedUserId();
        }
        if (userId == null) {
            return ApiResponse.error("未登录或登录已过期");
        }
        conversationService.validateConversationOwnership(conversationId, userId);
        List<MessageDTO> messages = chatService.getChatHistory(conversationId);
        return ApiResponse.success(messages);
    }
}
