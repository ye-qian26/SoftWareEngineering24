package com.yupi.yuaicodemother.service.impl;

import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ChatHistoryServiceImplTest {

    @Test
    void loadChatHistoryToMemory_shouldSkipPersistedHistoryForVueProject() {
        ChatHistoryServiceImpl service = new ChatHistoryServiceImpl();
        MessageWindowChatMemory chatMemory = mock(MessageWindowChatMemory.class);

        int loadedCount = service.loadChatHistoryToMemory(1L, chatMemory, 20, CodeGenTypeEnum.VUE_PROJECT);

        Assertions.assertEquals(0, loadedCount);
        verify(chatMemory).clear();
    }

    @Test
    void loadChatHistoryToMemory_shouldDelegateForNonVueProject() {
        ChatHistoryServiceImpl service = spy(new ChatHistoryServiceImpl());
        MessageWindowChatMemory chatMemory = mock(MessageWindowChatMemory.class);
        doReturn(2).when(service).loadChatHistoryToMemory(1L, chatMemory, 20);

        int loadedCount = service.loadChatHistoryToMemory(1L, chatMemory, 20, CodeGenTypeEnum.HTML);

        Assertions.assertEquals(2, loadedCount);
        verify(service).loadChatHistoryToMemory(1L, chatMemory, 20);
        verify(chatMemory, never()).clear();
    }
}
