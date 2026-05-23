package com.yupi.yuaicodemother.core.builder;

public record VueProjectFileValidationResult(boolean valid, String message) {

    public static VueProjectFileValidationResult success(String message) {
        return new VueProjectFileValidationResult(true, message);
    }

    public static VueProjectFileValidationResult failure(String message) {
        return new VueProjectFileValidationResult(false, message);
    }
}
