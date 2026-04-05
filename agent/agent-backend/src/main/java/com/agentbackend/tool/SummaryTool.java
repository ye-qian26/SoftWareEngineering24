package com.agentbackend.tool;

import com.agentbackend.entity.Message;
import com.agentbackend.mapper.MessageMapper;
import com.agentbackend.service.ToolCallLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SummaryTool {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private ToolCallLogService toolCallLogService;

    @Tool(name = "getConversationSummary", description = "用于获取对话内容以便生成总结，当用户要求总结对话时使用")
    public String getConversationSummary(
            @ToolParam(description = "会话ID") String conversationId) {
        
        if (conversationId == null || conversationId.isEmpty()) {
            return "{\"success\": false, \"message\": \"会话ID不能为空\"}";
        }
        
        try {
            LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt);
            
            List<Message> messages = messageMapper.selectList(queryWrapper);
            
            if (messages.isEmpty()) {
                String result = "{\"success\": true, \"message\": \"暂无对话内容\", \"conversationContent\": \"\"}";
                toolCallLogService.logToolCall(conversationId, "getConversationSummary", 
                    java.util.Map.of("conversationId", conversationId), result);
                return result;
            }
            
            StringBuilder content = new StringBuilder();
            for (Message msg : messages) {
                String role = "user".equals(msg.getRole()) ? "用户" : "AI";
                content.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            
            String result = "{\"success\": true, \"message\": \"成功获取对话内容\", \"conversationContent\": \"" 
                + escapeJson(content.toString()) + "\"}";
            
            toolCallLogService.logToolCall(conversationId, "getConversationSummary", 
                java.util.Map.of("conversationId", conversationId), result);
            
            return result;
        } catch (Exception e) {
            String errorResult = "{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            toolCallLogService.logToolCallError(conversationId, "getConversationSummary", 
                java.util.Map.of("conversationId", conversationId), e.getMessage());
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
