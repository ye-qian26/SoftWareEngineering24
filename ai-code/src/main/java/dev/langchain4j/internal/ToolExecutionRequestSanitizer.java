package dev.langchain4j.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.Internal;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

@Internal
public final class ToolExecutionRequestSanitizer {

    private static final Logger LOG = LoggerFactory.getLogger(ToolExecutionRequestSanitizer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ToolExecutionRequestSanitizer() {
    }

    public static ToolExecutionRequest sanitize(ToolExecutionRequest request) {
        if (request == null || isNullOrBlank(request.name())) {
            return null;
        }

        String sanitizedArguments = sanitizeArguments(request.name(), request.arguments());
        if (sanitizedArguments == null) {
            LOG.warn("Skipping malformed tool execution request. toolName={}", request.name());
            return null;
        }

        if (sanitizedArguments.equals(request.arguments())) {
            return request;
        }

        LOG.warn("Sanitized malformed tool execution request. toolName={}", request.name());
        return ToolExecutionRequest.builder()
                .id(request.id())
                .name(request.name())
                .arguments(sanitizedArguments)
                .build();
    }

    public static String sanitizeArguments(String toolName, String rawArguments) {
        String normalizedArguments = normalizeArguments(rawArguments);
        if (isValidJsonObject(normalizedArguments)) {
            return normalizedArguments;
        }

        if ("writeFile".equals(toolName)) {
            return repairWriteFileArguments(normalizedArguments);
        }

        return null;
    }

    public static String normalizeArguments(String rawArguments) {
        return isNullOrBlank(rawArguments) ? "{}" : rawArguments;
    }

    private static boolean isValidJsonObject(String rawArguments) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(rawArguments);
            return jsonNode != null && jsonNode.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    private static String repairWriteFileArguments(String rawArguments) {
        try {
            String relativeFilePath = extractJsonStringValue(rawArguments, "relativeFilePath");
            String content = extractTrailingJsonStringValue(rawArguments, "content");
            if (relativeFilePath == null || content == null) {
                return null;
            }

            Map<String, String> arguments = new LinkedHashMap<>();
            arguments.put("relativeFilePath", decodeJsonString(relativeFilePath));
            arguments.put("content", decodeJsonString(content));
            return OBJECT_MAPPER.writeValueAsString(arguments);
        } catch (Exception e) {
            LOG.warn("Failed to repair malformed writeFile arguments", e);
            return null;
        }
    }

    private static String extractJsonStringValue(String rawArguments, String fieldName) {
        int keyIndex = rawArguments.indexOf("\"" + fieldName + "\"");
        if (keyIndex < 0) {
            return null;
        }
        int startQuote = findValueStartQuote(rawArguments, keyIndex);
        if (startQuote < 0) {
            return null;
        }
        int endQuote = findClosingQuote(rawArguments, startQuote);
        if (endQuote < 0) {
            return null;
        }
        return rawArguments.substring(startQuote + 1, endQuote);
    }

    private static String extractTrailingJsonStringValue(String rawArguments, String fieldName) {
        int keyIndex = rawArguments.indexOf("\"" + fieldName + "\"");
        if (keyIndex < 0) {
            return null;
        }

        int startQuote = findValueStartQuote(rawArguments, keyIndex);
        if (startQuote < 0) {
            return null;
        }

        int endQuote = findTrailingValueEndQuote(rawArguments);
        if (endQuote <= startQuote) {
            return null;
        }
        return rawArguments.substring(startQuote + 1, endQuote);
    }

    private static int findValueStartQuote(String rawArguments, int keyIndex) {
        int colonIndex = rawArguments.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            return -1;
        }

        for (int i = colonIndex + 1; i < rawArguments.length(); i++) {
            char ch = rawArguments.charAt(i);
            if (!Character.isWhitespace(ch)) {
                return ch == '"' ? i : -1;
            }
        }
        return -1;
    }

    private static int findClosingQuote(String rawArguments, int startQuote) {
        for (int i = startQuote + 1; i < rawArguments.length(); i++) {
            if (rawArguments.charAt(i) == '"' && !isEscaped(rawArguments, i)) {
                return i;
            }
        }
        return -1;
    }

    private static int findTrailingValueEndQuote(String rawArguments) {
        int i = rawArguments.length() - 1;
        while (i >= 0 && Character.isWhitespace(rawArguments.charAt(i))) {
            i--;
        }
        while (i >= 0 && rawArguments.charAt(i) == '}') {
            i--;
            while (i >= 0 && Character.isWhitespace(rawArguments.charAt(i))) {
                i--;
            }
        }

        for (; i >= 0; i--) {
            if (rawArguments.charAt(i) == '"' && !isEscaped(rawArguments, i)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isEscaped(String value, int index) {
        int backslashCount = 0;
        for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
            backslashCount++;
        }
        return backslashCount % 2 != 0;
    }

    private static String decodeJsonString(String rawValue) {
        StringBuilder decoded = new StringBuilder(rawValue.length());
        for (int i = 0; i < rawValue.length(); i++) {
            char current = rawValue.charAt(i);
            if (current != '\\' || i == rawValue.length() - 1) {
                decoded.append(current);
                continue;
            }

            char escaped = rawValue.charAt(++i);
            switch (escaped) {
                case '"':
                    decoded.append('"');
                    break;
                case '\\':
                    decoded.append('\\');
                    break;
                case '/':
                    decoded.append('/');
                    break;
                case 'b':
                    decoded.append('\b');
                    break;
                case 'f':
                    decoded.append('\f');
                    break;
                case 'n':
                    decoded.append('\n');
                    break;
                case 'r':
                    decoded.append('\r');
                    break;
                case 't':
                    decoded.append('\t');
                    break;
                case 'u':
                    if (i + 4 < rawValue.length()) {
                        String hex = rawValue.substring(i + 1, i + 5);
                        decoded.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    } else {
                        decoded.append("\\u");
                    }
                    break;
                default:
                    decoded.append(escaped);
                    break;
            }
        }
        return decoded.toString();
    }
}
