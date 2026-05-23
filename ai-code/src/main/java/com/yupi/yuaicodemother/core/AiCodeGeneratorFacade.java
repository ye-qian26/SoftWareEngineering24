package com.yupi.yuaicodemother.core;

import cn.hutool.json.JSONUtil;
import com.yupi.yuaicodemother.ai.AiCodeGeneratorService;
import com.yupi.yuaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.yuaicodemother.ai.model.message.AiResponseMessage;
import com.yupi.yuaicodemother.ai.model.message.StreamErrorMessage;
import com.yupi.yuaicodemother.ai.model.message.ToolExecutedMessage;
import com.yupi.yuaicodemother.ai.model.message.ToolRequestMessage;
import com.yupi.yuaicodemother.core.builder.VueProjectBuilder;
import com.yupi.yuaicodemother.core.builder.VueProjectPathResolver;
import com.yupi.yuaicodemother.core.builder.VueProjectScaffolder;
import com.yupi.yuaicodemother.core.parser.CodeParserExecutor;
import com.yupi.yuaicodemother.core.saver.CodeFileSaverExecutor;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class AiCodeGeneratorFacade {

    private static final int MAX_EDIT_RETRY_ATTEMPTS = 1;
    private static final String PROJECT_EDIT_RULES_MARKER = "[PROJECT_EDIT_RULES]";
    private static final Set<String> FILE_MUTATION_TOOL_NAMES = Set.of("modifyFile", "writeFile", "deleteFile");

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private VueProjectScaffolder vueProjectScaffolder;

    @Resource
    private VueProjectPathResolver vueProjectPathResolver;

    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Code generation type is required");
        }
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorServiceForRequest(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "Unsupported code generation type: " + codeGenTypeEnum.getValue()
            );
        };
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Code generation type is required");
        }
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorServiceForRequest(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> processCodeStream(
                    aiCodeGeneratorService.generateHtmlCodeStream(userMessage),
                    CodeGenTypeEnum.HTML,
                    appId
            );
            case MULTI_FILE -> processCodeStream(
                    aiCodeGeneratorService.generateMultiFileCodeStream(userMessage),
                    CodeGenTypeEnum.MULTI_FILE,
                    appId
            );
            case VUE_PROJECT -> Flux.defer(() -> {
                try {
                    return processVueProjectStream(aiCodeGeneratorService, userMessage, appId);
                } catch (Exception e) {
                    log.error("Failed to start Vue project generation for appId={}", appId, e);
                    return Flux.just(buildStreamErrorChunk("stream_start", buildUserFacingMessage(e), true));
                }
            });
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "Unsupported code generation type: " + codeGenTypeEnum.getValue()
            );
        };
    }

    private Flux<String> processVueProjectStream(AiCodeGeneratorService aiCodeGeneratorService,
                                                 String userMessage,
                                                 Long appId) {
        return Flux.create(sink -> {
            try {
                vueProjectScaffolder.prepareProject(appId);
            } catch (Exception e) {
                log.error("Failed to prepare Vue project scaffold for appId={}", appId, e);
                sink.next(buildStreamErrorChunk("scaffold", "Project scaffold initialization failed, please retry later.", true));
                sink.complete();
                return;
            }

            startVueProjectAttempt(sink, aiCodeGeneratorService, userMessage, appId, 0);
        });
    }

    private void startVueProjectAttempt(FluxSink<String> sink,
                                        AiCodeGeneratorService aiCodeGeneratorService,
                                        String userMessage,
                                        Long appId,
                                        int attempt) {
        if (sink.isCancelled()) {
            return;
        }

        AtomicBoolean fileMutationExecuted = new AtomicBoolean(false);
        TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
        tokenStream.onPartialResponse(partialResponse -> {
                    AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                    sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                })
                .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                    ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                    sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                })
                .onToolExecuted((ToolExecution toolExecution) -> {
                    ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                    sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    if (isSuccessfulFileMutation(toolExecution)) {
                        fileMutationExecuted.set(true);
                    }
                })
                .onCompleteResponse((ChatResponse response) -> {
                    if (requiresFileMutation(userMessage) && !fileMutationExecuted.get()) {
                        if (attempt < MAX_EDIT_RETRY_ATTEMPTS) {
                            log.warn("Vue project edit completed without file changes, retrying. appId={}, attempt={}", appId, attempt + 1);
                            startVueProjectAttempt(
                                    sink,
                                    aiCodeGeneratorService,
                                    buildMissingToolRetryPrompt(userMessage),
                                    appId,
                                    attempt + 1
                            );
                            return;
                        }
                        sink.next(buildStreamErrorChunk(
                                "missing_file_change",
                                "AI replied but did not modify any project files, so the preview was not updated. Please retry with a more specific edit request.",
                                true
                        ));
                        sink.complete();
                        return;
                    }

                    String projectPath = vueProjectPathResolver.resolveProjectRoot(appId).toString();
                    if (!vueProjectBuilder.buildProject(projectPath)) {
                        sink.next(buildStreamErrorChunk(
                                "build",
                                "Code changes were generated, but the Vue project build failed. Please continue fixing the project based on the errors.",
                                false
                        ));
                        sink.complete();
                        return;
                    }
                    sink.complete();
                })
                .onError(error -> {
                    log.error("Vue project generation failed for appId={}", appId, error);
                    sink.next(buildStreamErrorChunk("streaming", buildUserFacingMessage(error), isRetryable(error)));
                    sink.complete();
                })
                .start();
    }

    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String completeCode = codeBuilder.toString();
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("Code generated and saved successfully: {}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("Failed to save generated code", e);
                    }
                });
    }

    private String buildStreamErrorChunk(String stage, String message, boolean retryable) {
        StreamErrorMessage streamErrorMessage = new StreamErrorMessage(
                stage,
                ErrorCode.SYSTEM_ERROR.getCode(),
                message,
                retryable,
                true,
                true
        );
        return JSONUtil.toJsonStr(streamErrorMessage);
    }

    private boolean requiresFileMutation(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        return userMessage.contains(PROJECT_EDIT_RULES_MARKER);
    }

    private boolean isSuccessfulFileMutation(ToolExecution toolExecution) {
        if (toolExecution == null || toolExecution.request() == null) {
            return false;
        }
        String toolName = toolExecution.request().name();
        if (!FILE_MUTATION_TOOL_NAMES.contains(toolName)) {
            return false;
        }
        String result = toolExecution.result();
        return result != null
                && (result.startsWith("MODIFY_FILE_SUCCESS")
                || result.startsWith("WRITE_FILE_SUCCESS")
                || result.startsWith("DELETE_FILE_SUCCESS"));
    }

    private String buildMissingToolRetryPrompt(String userMessage) {
        return userMessage + "\n\n[MANDATORY_TOOL_RETRY]\n"
                + "Your previous response did not execute any successful file-changing tool.\n"
                + "You must inspect the existing project files if needed and then execute modifyFile, writeFile, or deleteFile to apply a real code change before finishing.\n"
                + "A plain text answer is not acceptable for this request.";
    }

    private String buildUserFacingMessage(Throwable error) {
        if (isRetryable(error)) {
            return "The AI session service is temporarily unavailable. This generation was interrupted. Please retry later.";
        }
        String errorMessage = error.getMessage();
        if (errorMessage == null || errorMessage.isBlank()) {
            return "A system error occurred during generation. Please retry later.";
        }
        return errorMessage;
    }

    private boolean isRetryable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getName();
            if (className.contains("JedisConnectionException")
                    || className.contains("SocketException")
                    || className.contains("ConnectException")
                    || className.contains("TimeoutException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
