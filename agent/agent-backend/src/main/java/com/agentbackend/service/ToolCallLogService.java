package com.agentbackend.service;

import com.agentbackend.entity.ToolCallLog;
import com.agentbackend.mapper.ToolCallLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ToolCallLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolCallLogService.class);
    
    @Autowired
    private ToolCallLogMapper toolCallLogMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void logToolCall(String conversationId, String toolName, Map<String, Object> args, String result, boolean success, String errorMessage) {
        ToolCallLog log = new ToolCallLog();
        log.setConversationId(conversationId);
        log.setToolName(toolName);
        log.setToolArgs(toJsonString(args));
        log.setResult(result);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());
        
        toolCallLogMapper.insert(log);
        
        logger.info("工具调用日志: conversationId={}, tool={}, success={}", conversationId, toolName, success);
    }
    
    public void logToolCall(String conversationId, String toolName, Map<String, Object> args, String result) {
        logToolCall(conversationId, toolName, args, result, true, null);
    }
    
    public void logToolCallError(String conversationId, String toolName, Map<String, Object> args, String errorMessage) {
        logToolCall(conversationId, toolName, args, null, false, errorMessage);
    }
    
    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("JSON序列化失败", e);
            return obj.toString();
        }
    }
}
