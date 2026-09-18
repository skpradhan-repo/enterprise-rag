package com.enterprise.rag.rag.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Extracts plain text from documents using Apache Tika's AutoDetectParser.
 * Supports PDF, DOCX, TXT, Markdown, and any other format Tika can handle.
 */
@Slf4j
@Component
public class DocumentExtractor {

    private final AutoDetectParser parser = new AutoDetectParser();

    public record ExtractionResult(String text, int estimatedPageCount, String detectedMimeType) {}

    /**
     * Extracts plain text from the given input stream.
     *
     * @param inputStream the document binary
     * @param fileName    used for MIME type hint
     * @return extracted text and metadata
     */
    public ExtractionResult extract(InputStream inputStream, String fileName) {
        try {
            Metadata metadata = new Metadata();
            metadata.set("resourceName", fileName);

            BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            String text = handler.toString();
            String mimeType = metadata.get(Metadata.CONTENT_TYPE);

            // Tika doesn't always know page count; approximate from content size
            int estimatedPages = Math.max(1, text.length() / 3000);

            log.debug("Extracted {} chars from '{}' (MIME: {})", text.length(), fileName, mimeType);
            return new ExtractionResult(text, estimatedPages, mimeType);

        } catch (Exception e) {
            log.error("Failed to extract text from document: {}", fileName, e);
            throw new DocumentExtractionException("Text extraction failed for: " + fileName, e);
        }
    }
}
