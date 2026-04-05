package com.agentbackend.controller;

import com.agentbackend.dto.ApiResponse;
import com.agentbackend.dto.ToolCallLogDTO;
import com.agentbackend.entity.ToolCallLog;
import com.agentbackend.mapper.ToolCallLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tool")
public class ToolCallLogController {
    
    @Autowired
    private ToolCallLogMapper toolCallLogMapper;
    
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
    
    @GetMapping("/logs")
    public ApiResponse<List<ToolCallLogDTO>> getToolLogs(
            @RequestParam String conversationId,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = getAuthenticatedUserId();
        }
        if (userId == null) {
            return ApiResponse.error("未登录或登录已过期");
        }
        
        LambdaQueryWrapper<ToolCallLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ToolCallLog::getConversationId, conversationId)
                    .orderByDesc(ToolCallLog::getCreatedAt);
        
        List<ToolCallLog> logs = toolCallLogMapper.selectList(queryWrapper);
        
        List<ToolCallLogDTO> dtos = logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ApiResponse.success(dtos);
    }
    
    private ToolCallLogDTO convertToDTO(ToolCallLog log) {
        ToolCallLogDTO dto = new ToolCallLogDTO();
        BeanUtils.copyProperties(log, dto);
        return dto;
    }
}
