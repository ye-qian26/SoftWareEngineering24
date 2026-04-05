package com.agentbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tool_call_log")
public class ToolCallLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String conversationId;
    
    private String toolName;
    
    private String toolArgs;
    
    private String result;
    
    private Boolean success;
    
    private String errorMessage;
    
    private LocalDateTime createdAt;
}
