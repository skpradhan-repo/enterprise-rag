package com.enterprise.rag.rag.ingestion;

import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.domain.DocumentChunk;
import com.enterprise.rag.document.domain.DocumentStatus;
import com.enterprise.rag.document.repository.DocumentChunkRepository;
import com.enterprise.rag.document.repository.DocumentRepository;
import com.enterprise.rag.rag.chunking.ChunkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Asynchronous document ingestion pipeline.
 *
 * <p>Pipeline steps:
 * <ol>
 *   <li>Mark document PROCESSING</li>
 *   <li>Extract text via Apache Tika</li>
 *   <li>Normalize text</li>
 *   <li>Chunk text using sliding-window strategy</li>
 *   <li>Build Spring AI {@code Document} objects with security metadata</li>
 *   <li>Embed and store in PGVector via {@link VectorStore}</li>
 *   <li>Persist {@link DocumentChunk} records in PostgreSQL</li>
 *   <li>Mark document INDEXED</li>
 * </ol>
 *
 * <p>On any failure: marks document FAILED and records a safe error message.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionPipeline {

    private final DocumentExtractor      extractor;
    private final TextNormalizer         normalizer;
    private final ChunkingService        chunker;
    private final VectorStore            vectorStore;
    private final DocumentRepository     documentRepo;
    private final DocumentChunkRepository chunkRepo;

    // REQUIRES_NEW opens a fresh transaction independent of the caller's transaction.
    // This ensures the document row is already committed before we read it,
    // avoiding a race between the @Async thread and the upload transaction commit.
    @Async("ingestionExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ingest(UUID documentId, byte[] content, String fileName) {
        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        try {
            // Step 1 – mark processing
            doc.setStatus(DocumentStatus.PROCESSING);
            documentRepo.save(doc);

            // Step 2 – extract
            var extraction = extractor.extract(
                    new java.io.ByteArrayInputStream(content), fileName);

            // Step 3 – normalize
            String normalized = normalizer.normalize(extraction.text());

            if (normalized.isBlank()) {
                throw new IllegalStateException("No extractable text found in document.");
            }

            // Step 4 – chunk
            List<ChunkingService.Chunk> chunks = chunker.chunk(normalized, extraction.estimatedPageCount());

            // Step 5 – build Spring AI documents with metadata
            List<org.springframework.ai.document.Document> aiDocs = new ArrayList<>();
            for (ChunkingService.Chunk chunk : chunks) {
                Map<String, Object> metadata = Map.of(
                        "tenant_id",    doc.getTenantId().toString(),
                        "document_id",  doc.getId().toString(),
                        "document_name",doc.getFileName(),
                        "chunk_index",  chunk.index(),
                        "page_number",  chunk.estimatedPage(),
                        "status",       "INDEXED"
                );
                aiDocs.add(new org.springframework.ai.document.Document(chunk.content(), metadata));
            }

            // Step 6 – embed and store in PGVector
            vectorStore.add(aiDocs);

            // Step 7 – persist chunk records
            List<DocumentChunk> domainChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkingService.Chunk chunk = chunks.get(i);
                String vectorStoreId = aiDocs.get(i).getId();
                domainChunks.add(DocumentChunk.builder()
                        .document(doc)
                        .tenantId(doc.getTenantId())
                        .chunkIndex(chunk.index())
                        .pageNumber(chunk.estimatedPage())
                        .content(chunk.content())
                        .tokenCount(chunk.tokenCount())
                        .vectorStoreId(vectorStoreId)
                        .build());
            }
            chunkRepo.saveAll(domainChunks);

            // Step 8 – mark INDEXED
            doc.setStatus(DocumentStatus.INDEXED);
            doc.setPageCount(extraction.estimatedPageCount());
            documentRepo.save(doc);

            log.info("Ingestion complete: documentId={}, chunks={}", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Ingestion failed for documentId={}: {}", documentId, e.getMessage(), e);
            doc.setStatus(DocumentStatus.FAILED);
            // Store safe error info — never store the full exception message which may contain file paths
            doc.setErrorMessage("Ingestion failed. Check application logs for details.");
            documentRepo.save(doc);
        }
    }
}
