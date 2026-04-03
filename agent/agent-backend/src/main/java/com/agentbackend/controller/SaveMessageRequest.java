package com.agentbackend.controller;

import lombok.Data;

@Data
class SaveMessageRequest {
    private String conversationId;
    private String content;
}
