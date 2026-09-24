package com.slotsim.config;

/**
 * Thrown when a math model file cannot be read, parsed or fails validation.
 */
public class MathModelLoadException extends RuntimeException {

    public MathModelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
