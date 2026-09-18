package com.enterprise.rag.rag.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Sanitizes user input before it reaches the LLM.
 *
 * <p>Defense against prompt injection: detects common injection patterns and
 * strips control characters. Note: the primary defense is the system prompt itself
 * (treating context as untrusted data). This sanitizer provides an additional layer.
 */
@Slf4j
@Component
public class PromptSanitizer {

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore (previous|prior|all)? ?(instructions?|prompts?|rules?)|" +
            "you are now|act as|pretend to|forget (everything|your instructions?)|" +
            "system prompt|</?(s|system|user|assistant)>|\\[INST\\]|\\[/INST\\])",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_LENGTH = 4096;

    /**
     * Sanitizes a user question.
     * - Strips null bytes and control characters
     * - Truncates to max length
     * - Logs a warning if injection patterns are detected (does not block the request)
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String cleaned = input
                .replace("\u0000", "")
                .replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "")
                .trim();

        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH);
        }

        if (INJECTION_PATTERN.matcher(cleaned).find()) {
            log.warn("Potential prompt injection pattern detected in user input (sanitized and forwarded)");
        }

        return cleaned;
    }
}
