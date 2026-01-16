package com.splitwise.util;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import org.slf4j.MDC;

import jakarta.validation.constraints.NotNull;
import net.logstash.logback.argument.StructuredArgument;

/**
 * Utility for structured logging.
 */
public class StructuredLogging {
    public static final String ALPINE_NULL = "<alpine-null>";

    public static StructuredArgument getKV(String key, Object value) {
        return keyValue(key != null ? key : ALPINE_NULL, value != null ? value : ALPINE_NULL);
    }

    public static void putMDC(String key, Object value) {
        MDC.put(key != null ? key : ALPINE_NULL, value != null ? value.toString() : ALPINE_NULL);
    }

    /**
     * getMDCCloseable is used to call putCloseable with automatic value to string testing for null.
     * This is helpful to set an MDC value in a try-with-resources block so that it is only valid for a period of time.
     *
     * @param keyName name of the key to set
     * @param value object value to set
     * @return MDCCloseable to use in try-with-resources block
     */
    @NotNull
    public static MDC.MDCCloseable getMDCCloseable(String keyName, Object value) {
        return MDC.putCloseable(keyName, value != null ? value.toString() : ALPINE_NULL);
    }

    public static void clearMDC() {
        MDC.clear();
    }

    private StructuredLogging() {
        // Prevent instantiation
    }
}
