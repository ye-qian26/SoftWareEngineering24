-- 工具调用日志表
CREATE TABLE IF NOT EXISTS tool_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
    tool_args TEXT COMMENT '工具参数(JSON格式)',
    result TEXT COMMENT '工具返回结果',
    success TINYINT(1) DEFAULT 1 COMMENT '是否执行成功',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_tool_name (tool_name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用日志表';
