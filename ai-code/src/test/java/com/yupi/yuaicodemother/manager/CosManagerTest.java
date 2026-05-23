package com.yupi.yuaicodemother.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosManagerTest {

    @Test
    void normalizeKeyShouldRemoveLeadingSlash() {
        assertEquals("screenshots/2026/04/16/demo.jpg", CosManager.normalizeKey("/screenshots/2026/04/16/demo.jpg"));
    }

    @Test
    void joinHostAndKeyShouldAvoidDuplicateSlash() {
        String url = CosManager.joinHostAndKey("https://cdn.example.com/", "/screenshots/demo.jpg");

        assertEquals("https://cdn.example.com/screenshots/demo.jpg", url);
    }
}
