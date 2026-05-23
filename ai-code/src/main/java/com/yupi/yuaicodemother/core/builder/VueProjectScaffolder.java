package com.yupi.yuaicodemother.core.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class VueProjectScaffolder {

    private static final String TEMPLATE_ROOT = "vue-template/";
    private static final List<String> BASE_TEMPLATE_FILES = List.of(
            "index.html",
            "package.json",
            "vite.config.js",
            "src/main.js",
            "src/router/index.js"
    );
    private static final List<String> OPTIONAL_TEMPLATE_FILES = List.of(
            "src/App.vue",
            "src/pages/Home.vue",
            "src/pages/Upload.vue"
    );
    private static final Set<String> PROTECTED_FILES = Set.of(
            "index.html",
            "package.json",
            "vite.config.js",
            "src/main.js"
    );

    private final VueProjectPathResolver pathResolver;
    private final VueProjectBuilder vueProjectBuilder;

    public VueProjectScaffolder(VueProjectPathResolver pathResolver, VueProjectBuilder vueProjectBuilder) {
        this.pathResolver = pathResolver;
        this.vueProjectBuilder = vueProjectBuilder;
    }

    public Path prepareProject(Long appId) {
        Path projectRoot = pathResolver.resolveProjectRoot(appId);
        try {
            Files.createDirectories(projectRoot);
            for (String template : BASE_TEMPLATE_FILES) {
                writeTemplate(projectRoot, template, true);
            }
            for (String template : OPTIONAL_TEMPLATE_FILES) {
                writeTemplate(projectRoot, template, false);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare Vue project scaffold for appId=" + appId, e);
        }

        if (!vueProjectBuilder.ensureDependenciesInstalled(projectRoot.toFile())) {
            throw new IllegalStateException("Failed to install Vue project dependencies for " + projectRoot);
        }
        return projectRoot;
    }

    public boolean isProtectedPath(String relativeFilePath) {
        return PROTECTED_FILES.contains(pathResolver.normalizeRelativePath(relativeFilePath));
    }

    private void writeTemplate(Path projectRoot, String relativePath, boolean overwrite) throws IOException {
        Path targetPath = projectRoot.resolve(relativePath);
        if (!overwrite && Files.exists(targetPath)) {
            return;
        }
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                targetPath,
                readTemplate(relativePath),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private String readTemplate(String relativePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_ROOT + relativePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
