package com.enterprise.rag.rag.orchestration;

import com.enterprise.rag.api.dto.response.SourceReference;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts source citations from the LLM answer and maps them to SourceReference objects.
 * The LLM is instructed to cite sources as [Source N] where N is 1-based index.
 */
public final class SourceAttributor {

    private static final Pattern SOURCE_PATTERN = Pattern.compile("\\[Source (\\d+)\\]");
    private static final int EXCERPT_LENGTH = 200;

    private SourceAttributor() {}

    public static List<SourceReference> extract(String answer, List<Document> chunks) {
        if (answer == null || chunks == null || chunks.isEmpty()) return List.of();

        List<SourceReference> result = new ArrayList<>();
        Matcher matcher = SOURCE_PATTERN.matcher(answer);

        while (matcher.find()) {
            int sourceNum = Integer.parseInt(matcher.group(1));
            int chunkIndex = sourceNum - 1; // 1-based to 0-based

            if (chunkIndex >= 0 && chunkIndex < chunks.size()) {
                Document doc = chunks.get(chunkIndex);
                Map<String, Object> meta = doc.getMetadata();

                SourceReference ref = new SourceReference();
                ref.setChunkId(parseUuid(meta.get("id")));
                ref.setDocumentId(parseUuid(meta.get("document_id")));
                ref.setDocumentTitle((String) meta.getOrDefault("document_title", "Unknown"));
                ref.setPageNumber(parseInteger(meta.get("page_number")));
                ref.setScore(parseDouble(meta.get("distance")));
                ref.setExcerpt(truncate(doc.getText(), EXCERPT_LENGTH));

                // Avoid duplicate sources
                if (result.stream().noneMatch(r -> sourceNum == result.indexOf(r) + 1)) {
                    result.add(ref);
                }
            }
        }
        return result;
    }

    private static UUID parseUuid(Object val) {
        if (val == null) return null;
        try { return UUID.fromString(val.toString()); } catch (Exception e) { return null; }
    }

    private static Integer parseInteger(Object val) {
        if (val == null) return null;
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private static Double parseDouble(Object val) {
        if (val == null) return null;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
