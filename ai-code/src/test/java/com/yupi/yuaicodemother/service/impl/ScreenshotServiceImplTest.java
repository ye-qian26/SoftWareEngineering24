package com.yupi.yuaicodemother.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotServiceImplTest {

    private final ScreenshotServiceImpl screenshotService = new ScreenshotServiceImpl();

    @Test
    void generateScreenshotKeyShouldUseRelativePath() {
        String key = screenshotService.generateScreenshotKey("/demo_compressed.jpg");

        assertFalse(key.startsWith("/"));
        assertTrue(key.startsWith("screenshots/"));
        assertTrue(key.endsWith("/demo_compressed.jpg"));
    }
}
