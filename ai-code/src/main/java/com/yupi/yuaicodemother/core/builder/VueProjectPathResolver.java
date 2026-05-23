package com.yupi.yuaicodemother.core.builder;

import com.yupi.yuaicodemother.constant.AppConstant;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class VueProjectPathResolver {

    public Path resolveProjectRoot(Long appId) {
        if (appId == null) {
            throw new IllegalArgumentException("appId must not be null");
        }
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId)
                .toAbsolutePath()
                .normalize();
    }

    public Path resolvePath(Long appId, String relativeFilePath) {
        String normalizedRelativePath = normalizeRelativePath(relativeFilePath);
        Path projectRoot = resolveProjectRoot(appId);
        Path resolvedPath = projectRoot.resolve(normalizedRelativePath).normalize();
        if (!resolvedPath.startsWith(projectRoot)) {
            throw new IllegalArgumentException("File path escapes project root: " + relativeFilePath);
        }
        return resolvedPath;
    }

    public String normalizeRelativePath(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            throw new IllegalArgumentException("relativeFilePath must not be blank");
        }
        String normalized = Paths.get(relativeFilePath)
                .normalize()
                .toString()
                .replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.isBlank() || ".".equals(normalized)) {
            throw new IllegalArgumentException("relativeFilePath must not be empty after normalization");
        }
        return normalized;
    }
}
