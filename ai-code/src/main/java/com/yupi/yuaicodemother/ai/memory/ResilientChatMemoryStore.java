package com.yupi.yuaicodemother.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ResilientChatMemoryStore implements ChatMemoryStore {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MILLIS = 200L;

    private final ChatMemoryStore delegate;
    private final Map<Object, List<ChatMessage>> localFallbackStore = new ConcurrentHashMap<>();

    public ResilientChatMemoryStore(ChatMemoryStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            return withRetry(() -> {
                List<ChatMessage> messages = delegate.getMessages(memoryId);
                List<ChatMessage> safeCopy = copyMessages(messages);
                localFallbackStore.put(memoryId, safeCopy);
                return safeCopy;
            }, "getMessages", memoryId);
        } catch (Exception e) {
            log.warn("Falling back to local chat memory for memoryId={} after get failure", memoryId, e);
            return copyMessages(localFallbackStore.get(memoryId));
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        List<ChatMessage> safeCopy = copyMessages(messages);
        try {
            withRetry(() -> {
                delegate.updateMessages(memoryId, safeCopy);
                return null;
            }, "updateMessages", memoryId);
        } catch (Exception e) {
            log.warn("Falling back to local chat memory for memoryId={} after update failure", memoryId, e);
        }
        localFallbackStore.put(memoryId, safeCopy);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            withRetry(() -> {
                delegate.deleteMessages(memoryId);
                return null;
            }, "deleteMessages", memoryId);
        } catch (Exception e) {
            log.warn("Ignoring chat memory delete failure for memoryId={}", memoryId, e);
        } finally {
            localFallbackStore.remove(memoryId);
        }
    }

    private <T> T withRetry(ChatMemoryOperation<T> operation, String action, Object memoryId) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                log.warn("Chat memory {} failed for memoryId={}, attempt={}/{}", action, memoryId, attempt, MAX_RETRIES);
                if (attempt < MAX_RETRIES) {
                    sleepQuietly();
                }
            }
        }
        throw lastException;
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<ChatMessage> copyMessages(List<ChatMessage> messages) {
        return messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    }

    @FunctionalInterface
    private interface ChatMemoryOperation<T> {

        T execute() throws Exception;
    }
}
