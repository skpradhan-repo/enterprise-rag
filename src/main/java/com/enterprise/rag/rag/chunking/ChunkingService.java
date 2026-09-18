package com.enterprise.rag.rag.chunking;

import com.enterprise.rag.configuration.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Token-based sliding-window text chunker.
 *
 * <p>Splits text into overlapping chunks of approximately {@code chunkSize} tokens
 * (approximated as whitespace-separated words). The overlap ensures that context
 * spanning chunk boundaries is preserved.
 *
 * <p>An extension point — more sophisticated semantic chunking strategies can be
 * swapped in by implementing a {@code ChunkingStrategy} interface in future phases.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkingService {

    private final AppProperties props;

    public record Chunk(int index, String content, int tokenCount, Integer estimatedPage) {}

    /**
     * Splits normalized text into overlapping chunks.
     *
     * @param text          normalized document text
     * @param totalPages    estimated page count (for page-number attribution)
     * @return ordered list of chunks
     */
    public List<Chunk> chunk(String text, int totalPages) {
        int chunkSize    = props.getRag().getChunkSize();
        int chunkOverlap = props.getRag().getChunkOverlap();

        String[] words = text.split("\\s+");
        if (words.length == 0) return List.of();

        List<Chunk> chunks  = new ArrayList<>();
        int index           = 0;
        int start           = 0;

        while (start < words.length) {
            int end = Math.min(start + chunkSize, words.length);
            String chunkText = String.join(" ", List.of(words).subList(start, end));

            // Estimate which page this chunk falls on (linear approximation)
            int estimatedPage = totalPages <= 1 ? 1 :
                    (int) Math.ceil(((double) start / words.length) * totalPages);

            chunks.add(new Chunk(index++, chunkText, end - start, estimatedPage));

            if (end == words.length) break;
            start += (chunkSize - chunkOverlap);
        }

        log.debug("Chunked {} words into {} chunks (size={}, overlap={})",
                words.length, chunks.size(), chunkSize, chunkOverlap);
        return chunks;
    }
}
