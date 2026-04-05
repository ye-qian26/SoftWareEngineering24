package com.agentbackend.tool;

import com.agentbackend.entity.Message;
import com.agentbackend.mapper.MessageMapper;
import com.agentbackend.service.ToolCallLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HistoryTool {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private ToolCallLogService toolCallLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final int DEFAULT_LIMIT = 10;
    
    @Tool(name = "getConversationHistory", description = "用于获取当前会话的历史聊天记录，当用户询问历史消息、之前说了什么等问题时使用")
    public String getConversationHistory(
            @ToolParam(description = "会话ID") String conversationId) {
        
        if (conversationId == null || conversationId.isEmpty()) {
            return "{\"success\": false, \"message\": \"会话ID不能为空\"}";
        }
        
        try {
            LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Message::getConversationId, conversationId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT " + DEFAULT_LIMIT);
            
            List<Message> messages = messageMapper.selectList(queryWrapper);
            
            if (messages.isEmpty()) {
                String result = "{\"success\": true, \"message\": \"暂无历史消息\", \"history\": []}";
                toolCallLogService.logToolCall(conversationId, "getConversationHistory", 
                    Map.of("conversationId", conversationId), result);
                return result;
            }
            
            List<Map<String, String>> history = messages.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .map(msg -> Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent(),
                        "createdAt", msg.getCreatedAt().toString()
                    ))
                    .collect(Collectors.toList());
            
            String result = objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "成功获取历史消息",
                "history", history
            ));
            
            toolCallLogService.logToolCall(conversationId, "getConversationHistory", 
                Map.of("conversationId", conversationId), result);
            
            return result;
        } catch (Exception e) {
            String errorResult = "{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            toolCallLogService.logToolCallError(conversationId, "getConversationHistory", 
                Map.of("conversationId", conversationId), e.getMessage());
            return errorResult;
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
