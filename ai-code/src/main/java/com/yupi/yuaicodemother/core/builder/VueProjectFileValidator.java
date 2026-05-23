package com.yupi.yuaicodemother.core.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectFileValidator {

    private static final long VALIDATION_TIMEOUT_SECONDS = 30;
    private static final String NODE_SCRIPT_JS = "validator/validate-module.cjs";
    private static final String NODE_SCRIPT_VUE = "validator/validate-vue-sfc.cjs";

    private final Map<String, Path> extractedScripts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VueProjectFileValidationResult validate(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".vue")) {
                return runNodeValidator(NODE_SCRIPT_VUE, filePath);
            }
            if (fileName.endsWith(".js") || fileName.endsWith(".mjs") || fileName.endsWith(".cjs")
                    || fileName.endsWith(".ts") || fileName.endsWith(".tsx")
                    || fileName.endsWith(".jsx") || fileName.endsWith(".css")) {
                return runNodeValidator(NODE_SCRIPT_JS, filePath);
            }
            if (fileName.endsWith(".json")) {
                objectMapper.readTree(Files.readString(filePath, StandardCharsets.UTF_8));
                return VueProjectFileValidationResult.success("JSON syntax valid");
            }
            return VueProjectFileValidationResult.success("Validation skipped for file type: " + fileName);
        } catch (Exception e) {
            return VueProjectFileValidationResult.failure(e.getMessage());
        }
    }

    private VueProjectFileValidationResult runNodeValidator(String resourcePath, Path filePath) {
        Path projectRoot = findProjectRoot(filePath);
        if (projectRoot == null) {
            return VueProjectFileValidationResult.failure("Cannot locate project root for " + filePath);
        }

        Path scriptPath = extractScript(resourcePath);
        ProcessBuilder processBuilder = new ProcessBuilder(List.of(
                "node",
                scriptPath.toString(),
                projectRoot.toString(),
                filePath.toAbsolutePath().toString()
        ));
        processBuilder.directory(projectRoot.toFile());
        processBuilder.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        try {
            Process process = processBuilder.start();
            Thread outputReader = Thread.ofVirtual()
                    .name("vue-file-validator-" + System.currentTimeMillis())
                    .start(() -> readProcessOutput(process, output));

            boolean finished = process.waitFor(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                return VueProjectFileValidationResult.failure("Syntax validation timed out for " + filePath.getFileName());
            }

            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            String message = readCapturedOutput(output);
            if (process.exitValue() == 0) {
                return VueProjectFileValidationResult.success(message.isBlank() ? "Syntax valid" : message);
            }
            return VueProjectFileValidationResult.failure(message.isBlank() ? "Unknown syntax validation failure" : message);
        } catch (Exception e) {
            log.warn("Failed to validate Vue project file {}", filePath, e);
            return VueProjectFileValidationResult.failure("Syntax validation failed to execute: " + e.getMessage());
        }
    }

    private Path findProjectRoot(Path filePath) {
        Path current = filePath.toAbsolutePath().getParent();
        while (current != null) {
            if (Files.exists(current.resolve("package.json"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private Path extractScript(String resourcePath) {
        return extractedScripts.computeIfAbsent(resourcePath, key -> {
            try (InputStream inputStream = new ClassPathResource(key).getInputStream()) {
                String suffix = key.substring(key.lastIndexOf('.'));
                Path tempFile = Files.createTempFile("ai-code-validator-", suffix);
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                return tempFile;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to extract validator script: " + key, e);
            }
        });
    }

    private void readProcessOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read syntax validator output", e);
        }
    }

    private String readCapturedOutput(StringBuilder output) {
        synchronized (output) {
            return output.toString().trim();
        }
    }
}
