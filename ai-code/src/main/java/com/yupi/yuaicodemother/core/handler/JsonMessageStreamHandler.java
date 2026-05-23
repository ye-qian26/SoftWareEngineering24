package com.yupi.yuaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.yuaicodemother.ai.model.message.AiResponseMessage;
import com.yupi.yuaicodemother.ai.model.message.StreamErrorMessage;
import com.yupi.yuaicodemother.ai.model.message.StreamMessage;
import com.yupi.yuaicodemother.ai.model.message.StreamMessageTypeEnum;
import com.yupi.yuaicodemother.ai.model.message.ToolExecutedMessage;
import com.yupi.yuaicodemother.ai.model.message.ToolRequestMessage;
import com.yupi.yuaicodemother.ai.tools.BaseTool;
import com.yupi.yuaicodemother.ai.tools.ToolManager;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class JsonMessageStreamHandler {

    public static final String STREAM_ERROR_CHUNK_PREFIX = "__STREAM_ERROR__";

    @Resource
    private ToolManager toolManager;

    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds))
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    String aiResponse = chatHistoryStringBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(
                                appId,
                                aiResponse,
                                ChatHistoryMessageTypeEnum.AI.getValue(),
                                loginUser.getId()
                        );
                    }
                })
                .doOnError(error -> chatHistoryService.addChatMessage(
                        appId,
                        "AI reply failed: " + error.getMessage(),
                        ChatHistoryMessageTypeEnum.AI.getValue(),
                        loginUser.getId()
                ));
    }

    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.warn("Unsupported stream message type payload: {}", chunk);
            return "";
        }
        return switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                chatHistoryStringBuilder.append(data);
                yield data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);
                    BaseTool tool = toolManager.getTool(toolRequestMessage.getName());
                    yield tool == null ? "" : tool.generateToolRequestResponse();
                }
                yield "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                BaseTool tool = toolManager.getTool(toolExecutedMessage.getName());
                String result = tool == null ? "" : tool.generateToolExecutedResult(jsonObject);
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                yield output;
            }
            case STREAM_ERROR -> {
                StreamErrorMessage streamErrorMessage = JSONUtil.toBean(chunk, StreamErrorMessage.class);
                chatHistoryStringBuilder.append("\n\n[系统提示] ")
                        .append(streamErrorMessage.getMessage())
                        .append("\n\n");
                yield STREAM_ERROR_CHUNK_PREFIX + JSONUtil.toJsonStr(streamErrorMessage);
            }
        };
    }
}
