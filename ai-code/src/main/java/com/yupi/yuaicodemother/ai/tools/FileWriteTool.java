package com.yupi.yuaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yupi.yuaicodemother.core.builder.VueProjectFileValidationResult;
import com.yupi.yuaicodemother.core.builder.VueProjectFileValidator;
import com.yupi.yuaicodemother.core.builder.VueProjectPathResolver;
import com.yupi.yuaicodemother.core.builder.VueProjectScaffolder;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    private static final String VALIDATION_FAILED_PREFIX = "SYNTAX_VALIDATION_FAILED";
    private static final String PROTECTED_FILE_PREFIX = "PROTECTED_FILE_REJECTED";
    private static final String WRITE_FAILED_PREFIX = "WRITE_FILE_FAILED";

    private final VueProjectPathResolver pathResolver;
    private final VueProjectScaffolder vueProjectScaffolder;
    private final VueProjectFileValidator vueProjectFileValidator;

    public FileWriteTool(
            VueProjectPathResolver pathResolver,
            VueProjectScaffolder vueProjectScaffolder,
            VueProjectFileValidator vueProjectFileValidator
    ) {
        this.pathResolver = pathResolver;
        this.vueProjectScaffolder = vueProjectScaffolder;
        this.vueProjectFileValidator = vueProjectFileValidator;
    }

    @Tool("Create or overwrite a file inside the generated Vue project.")
    public String writeFile(
            @P("Relative file path inside the Vue project.")
            String relativeFilePath,
            @P("Full file content to write.")
            String content,
            @ToolMemoryId Long appId
    ) {
        String normalizedPath;
        try {
            normalizedPath = pathResolver.normalizeRelativePath(relativeFilePath);
            if (vueProjectScaffolder.isProtectedPath(normalizedPath)) {
                return PROTECTED_FILE_PREFIX + ": " + normalizedPath
                        + ". This scaffold file is managed by the system. Edit business files instead.";
            }

            Path targetPath = pathResolver.resolvePath(appId, normalizedPath);
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            byte[] backup = Files.exists(targetPath) ? Files.readAllBytes(targetPath) : null;
            Files.writeString(
                    targetPath,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            VueProjectFileValidationResult validationResult = vueProjectFileValidator.validate(targetPath);
            if (!validationResult.valid()) {
                rollback(targetPath, backup);
                return buildValidationFailureMessage(normalizedPath, validationResult.message(), "writeFile");
            }

            log.info("Wrote Vue project file: {}", targetPath.toAbsolutePath());
            return "WRITE_FILE_SUCCESS: " + normalizedPath;
        } catch (Exception e) {
            String errorMessage = WRITE_FAILED_PREFIX + ": " + relativeFilePath + " -> " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    private void rollback(Path targetPath, byte[] backup) throws IOException {
        if (backup == null) {
            Files.deleteIfExists(targetPath);
            return;
        }
        Files.write(targetPath, backup, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String buildValidationFailureMessage(String relativeFilePath, String validationMessage, String retryTool) {
        return VALIDATION_FAILED_PREFIX + ": " + relativeFilePath + " -> " + validationMessage
                + ". The file has been rolled back. Fix the syntax and retry " + retryTool
                + " for the same file path.";
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "Write File";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [Tool] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
