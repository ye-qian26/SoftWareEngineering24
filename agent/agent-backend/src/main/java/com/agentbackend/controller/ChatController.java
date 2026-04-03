package com.agentbackend.controller;

import com.agentbackend.dto.ApiResponse;
import com.agentbackend.dto.MessageDTO;
import com.agentbackend.dto.SendMessageRequest;
import com.agentbackend.dto.SendMessageResponse;
import com.agentbackend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @PostMapping("/send")
    public ApiResponse<SendMessageResponse> sendMessage(@RequestBody SendMessageRequest request) {
        SendMessageResponse response = chatService.sendMessage(request);
        return ApiResponse.success(response);
    }
    
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@RequestBody SendMessageRequest request) {
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
    public ApiResponse<List<MessageDTO>> getChatHistory(@RequestParam String conversationId) {
        List<MessageDTO> messages = chatService.getChatHistory(conversationId);
        return ApiResponse.success(messages);
    }
}
