package com.enterprise.rag.rag.ingestion;

import org.springframework.stereotype.Component;

/**
 * Normalizes extracted text before chunking.
 * Removes null bytes, normalizes Unicode whitespace, and trims.
 */
@Component
public class TextNormalizer {

    public String normalize(String rawText) {
        if (rawText == null) return "";

        return rawText
                .replace("\u0000", "")           // remove null bytes (Tika artifact)
                .replaceAll("\\r\\n|\\r", "\n")  // normalize line endings
                .replaceAll("[ \\t]+", " ")       // collapse horizontal whitespace
                .replaceAll("\\n{3,}", "\n\n")    // collapse excessive blank lines
                .trim();
    }
}
