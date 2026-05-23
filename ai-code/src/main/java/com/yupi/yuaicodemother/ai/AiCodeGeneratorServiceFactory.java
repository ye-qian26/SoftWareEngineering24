package com.yupi.yuaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yuaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.yupi.yuaicodemother.ai.tools.ToolManager;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import com.yupi.yuaicodemother.utils.SpringContextUtil;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    private static final int CHAT_HISTORY_LIMIT = 20;
    private static final int MAX_SEQUENTIAL_TOOL_INVOCATIONS = 40;

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private ChatMemoryStore chatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    private final Cache<String, CachedAiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.debug("Removed AI service from cache, key={}, cause={}", key, cause))
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return getOrCreateCachedAiCodeGeneratorService(appId, codeGenType).service();
    }

    public AiCodeGeneratorService getAiCodeGeneratorServiceForRequest(long appId, CodeGenTypeEnum codeGenType) {
        CachedAiCodeGeneratorService cachedService = getOrCreateCachedAiCodeGeneratorService(appId, codeGenType);
        prepareChatMemoryForRequest(appId, codeGenType, cachedService.chatMemory());
        return cachedService.service();
    }

    private CachedAiCodeGeneratorService getOrCreateCachedAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    private CachedAiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("Create AI code generator service for appId={}, type={}", appId, codeGenType);

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(CHAT_HISTORY_LIMIT)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, CHAT_HISTORY_LIMIT, codeGenType);

        AiCodeGeneratorService aiCodeGeneratorService = switch (codeGenType) {
            case VUE_PROJECT -> {
                StreamingChatModel reasoningStreamingChatModel =
                        SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools((Object[]) toolManager.getAllTools())
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(
                                        toolExecutionRequest,
                                        "Error: there is no tool called " + toolExecutionRequest.name()
                                ))
                        .maxSequentialToolsInvocations(MAX_SEQUENTIAL_TOOL_INVOCATIONS)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            case HTML, MULTI_FILE -> {
                StreamingChatModel openAiStreamingChatModel =
                        SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "Unsupported code generation type: " + codeGenType.getValue()
            );
        };
        return new CachedAiCodeGeneratorService(aiCodeGeneratorService, chatMemory);
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    private void prepareChatMemoryForRequest(long appId,
                                             CodeGenTypeEnum codeGenType,
                                             MessageWindowChatMemory chatMemory) {
        if (codeGenType != CodeGenTypeEnum.VUE_PROJECT) {
            return;
        }
        // Vue project edits should use the current project files as source of truth.
        // Reusing previous edit prompts and tool traces across requests causes drift quickly.
        chatMemory.clear();
        log.info("Reset Vue project chat memory before new request, appId={}", appId);
    }

    private record CachedAiCodeGeneratorService(
            AiCodeGeneratorService service,
            MessageWindowChatMemory chatMemory
    ) {
    }
}
