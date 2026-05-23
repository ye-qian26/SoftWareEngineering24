package com.yupi.yuaicodemother.core.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {

    private static final int INSTALL_TIMEOUT_SECONDS = 300;
    private static final int BUILD_TIMEOUT_SECONDS = 180;

    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual()
                .name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildProject(projectPath);
                    } catch (Exception e) {
                        log.error("Failed to build Vue project asynchronously: {}", projectPath, e);
                    }
                });
    }

    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("Vue project directory does not exist: {}", projectPath);
            return false;
        }

        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.error("package.json is missing in Vue project: {}", projectPath);
            return false;
        }

        log.info("Start building Vue project: {}", projectPath);
        if (!ensureDependenciesInstalled(projectDir)) {
            return false;
        }
        if (!executeNpmCommand(projectDir, BUILD_TIMEOUT_SECONDS, "run", "build")) {
            log.error("npm run build failed for Vue project: {}", projectPath);
            return false;
        }

        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            log.error("dist directory was not generated for Vue project: {}", projectPath);
            return false;
        }

        log.info("Vue project build succeeded: {}", projectPath);
        return true;
    }

    public boolean ensureDependenciesInstalled(File projectDir) {
        if (hasInstalledDependencies(projectDir)) {
            log.info("Skip npm install because dependencies already exist: {}", projectDir.getAbsolutePath());
            return true;
        }
        return executeNpmCommand(projectDir, INSTALL_TIMEOUT_SECONDS, "install");
    }

    private boolean hasInstalledDependencies(File projectDir) {
        Path nodeModulesPath = projectDir.toPath().resolve("node_modules");
        return Files.isDirectory(nodeModulesPath.resolve("@vue").resolve("compiler-sfc"))
                && Files.isDirectory(nodeModulesPath.resolve("esbuild"));
    }

    private boolean executeNpmCommand(File projectDir, int timeoutSeconds, String... args) {
        List<String> command = new ArrayList<>();
        command.add(isWindows() ? "npm.cmd" : "npm");
        command.addAll(List.of(args));

        String printableCommand = String.join(" ", command);
        log.info("Executing command in {}: {}", projectDir.getAbsolutePath(), printableCommand);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(projectDir);
        processBuilder.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        try {
            Process process = processBuilder.start();
            Thread outputReader = Thread.ofVirtual()
                    .name("vue-command-reader-" + System.currentTimeMillis())
                    .start(() -> readProcessOutput(process, output));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                log.error("Command timed out after {} seconds: {}", timeoutSeconds, printableCommand);
                logCommandOutput(output);
                return false;
            }

            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Command failed with exit code {}: {}", exitCode, printableCommand);
                logCommandOutput(output);
                return false;
            }

            logCommandOutput(output);
            return true;
        } catch (Exception e) {
            log.error("Failed to execute command: {}", printableCommand, e);
            logCommandOutput(output);
            return false;
        }
    }

    private void readProcessOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    output.append(line).append(System.lineSeparator());
                }
                log.info("[vue-project] {}", line);
            }
        } catch (IOException e) {
            log.warn("Failed to read Vue project command output", e);
        }
    }

    private void logCommandOutput(StringBuilder output) {
        String text;
        synchronized (output) {
            text = output.toString().trim();
        }
        if (!text.isEmpty()) {
            log.info("Command output:\n{}", text);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
