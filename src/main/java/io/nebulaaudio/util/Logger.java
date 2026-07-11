package io.nebulaaudio.util;

import org.slf4j.LoggerFactory;

/**
 * Thin factory helper around SLF4J. Named Logger per the project spec, but
 * deliberately does NOT implement org.slf4j.Logger to avoid ambiguity/shadowing
 * everywhere else in the codebase calls LoggerFactory.getLogger(Class) directly.
 * This class just centralizes that call with a project-consistent name.
 */
public final class Logger {
    private Logger() {
    }

    public static org.slf4j.Logger get(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
