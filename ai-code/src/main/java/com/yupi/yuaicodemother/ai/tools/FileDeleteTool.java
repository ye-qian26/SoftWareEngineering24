package com.yupi.yuaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import com.yupi.yuaicodemother.core.builder.VueProjectPathResolver;
import com.yupi.yuaicodemother.core.builder.VueProjectScaffolder;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Slf4j
@Component
public class FileDeleteTool extends BaseTool {

    private static final Set<String> EXTRA_PROTECTED_FILES = Set.of(
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml"
    );

    private final VueProjectPathResolver pathResolver;
    private final VueProjectScaffolder vueProjectScaffolder;

    public FileDeleteTool(VueProjectPathResolver pathResolver, VueProjectScaffolder vueProjectScaffolder) {
        this.pathResolver = pathResolver;
        this.vueProjectScaffolder = vueProjectScaffolder;
    }

    @Tool("Delete a file inside the generated Vue project.")
    public String deleteFile(
            @P("Relative file path inside the Vue project.")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            String normalizedPath = pathResolver.normalizeRelativePath(relativeFilePath);
            if (vueProjectScaffolder.isProtectedPath(normalizedPath) || EXTRA_PROTECTED_FILES.contains(normalizedPath)) {
                return "DELETE_FILE_REJECTED: " + normalizedPath + ". This file is protected.";
            }

            Path targetPath = pathResolver.resolvePath(appId, normalizedPath);
            if (!Files.exists(targetPath)) {
                return "DELETE_FILE_SKIPPED: file does not exist -> " + normalizedPath;
            }
            if (!Files.isRegularFile(targetPath)) {
                return "DELETE_FILE_FAILED: target is not a file -> " + normalizedPath;
            }

            Files.delete(targetPath);
            log.info("Deleted Vue project file: {}", targetPath.toAbsolutePath());
            return "DELETE_FILE_SUCCESS: " + normalizedPath;
        } catch (IOException | IllegalArgumentException e) {
            String errorMessage = "DELETE_FILE_FAILED: " + relativeFilePath + " -> " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "Delete File";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[Tool] %s %s", getDisplayName(), relativeFilePath);
    }
}
