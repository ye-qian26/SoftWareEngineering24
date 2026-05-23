package com.yupi.yuaicodemother.ai.tools;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    private static final String VALIDATION_FAILED_PREFIX = "SYNTAX_VALIDATION_FAILED";
    private static final String PROTECTED_FILE_PREFIX = "PROTECTED_FILE_REJECTED";
    private static final String MODIFY_FAILED_PREFIX = "MODIFY_FILE_FAILED";

    private final VueProjectPathResolver pathResolver;
    private final VueProjectScaffolder vueProjectScaffolder;
    private final VueProjectFileValidator vueProjectFileValidator;

    public FileModifyTool(
            VueProjectPathResolver pathResolver,
            VueProjectScaffolder vueProjectScaffolder,
            VueProjectFileValidator vueProjectFileValidator
    ) {
        this.pathResolver = pathResolver;
        this.vueProjectScaffolder = vueProjectScaffolder;
        this.vueProjectFileValidator = vueProjectFileValidator;
    }

    @Tool("Modify part of an existing file inside the generated Vue project.")
    public String modifyFile(
            @P("Relative file path inside the Vue project.")
            String relativeFilePath,
            @P("Exact existing content to replace.")
            String oldContent,
            @P("New content that replaces the old content.")
            String newContent,
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
            if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
                return MODIFY_FAILED_PREFIX + ": file does not exist -> " + normalizedPath;
            }

            String originalContent = Files.readString(targetPath, StandardCharsets.UTF_8);
            if (!originalContent.contains(oldContent)) {
                return MODIFY_FAILED_PREFIX + ": target snippet not found -> " + normalizedPath;
            }

            String modifiedContent = originalContent.replaceFirst(
                    Pattern.quote(oldContent),
                    Matcher.quoteReplacement(newContent)
            );
            if (originalContent.equals(modifiedContent)) {
                return MODIFY_FAILED_PREFIX + ": no effective change -> " + normalizedPath;
            }

            Files.writeString(
                    targetPath,
                    modifiedContent,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            VueProjectFileValidationResult validationResult = vueProjectFileValidator.validate(targetPath);
            if (!validationResult.valid()) {
                rollback(targetPath, originalContent);
                return buildValidationFailureMessage(normalizedPath, validationResult.message(), "modifyFile");
            }

            log.info("Modified Vue project file: {}", targetPath.toAbsolutePath());
            return "MODIFY_FILE_SUCCESS: " + normalizedPath;
        } catch (Exception e) {
            String errorMessage = MODIFY_FAILED_PREFIX + ": " + relativeFilePath + " -> " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    private void rollback(Path targetPath, String originalContent) throws IOException {
        Files.writeString(
                targetPath,
                originalContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private String buildValidationFailureMessage(String relativeFilePath, String validationMessage, String retryTool) {
        return VALIDATION_FAILED_PREFIX + ": " + relativeFilePath + " -> " + validationMessage
                + ". The file has been rolled back. Fix the syntax and retry " + retryTool
                + " for the same file path.";
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "Modify File";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        return String.format("""
                [Tool] %s %s

                Old content
                ```
                %s
                ```

                New content
                ```
                %s
                ```
                """, getDisplayName(), relativeFilePath, oldContent, newContent);
    }
}
