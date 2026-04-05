package com.agentbackend.config;

import com.agentbackend.tool.CalculatorTool;
import com.agentbackend.tool.HistoryTool;
import com.agentbackend.tool.SummaryTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {
    
    @Autowired
    private HistoryTool historyTool;
    
    @Autowired
    private SummaryTool summaryTool;
    
    @Autowired
    private CalculatorTool calculatorTool;
    
    @Value("${agent.tools.enabled:true}")
    private boolean toolsEnabled;
    
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(historyTool, summaryTool, calculatorTool)
                .build();
    }
    
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        
        if (toolsEnabled) {
            builder.defaultToolCallbacks(toolCallbackProvider);
        }
        
        return builder.build();
    }
}
