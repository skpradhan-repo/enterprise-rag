package com.enterprise.rag.document.service;

import com.enterprise.rag.api.dto.DocumentResponse;
import com.enterprise.rag.api.error.BusinessException;
import com.enterprise.rag.api.error.ResourceNotFoundException;
import com.enterprise.rag.configuration.AppProperties;
import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.domain.DocumentStatus;
import com.enterprise.rag.document.domain.DocumentType;
import com.enterprise.rag.document.repository.DocumentRepository;
import com.enterprise.rag.rag.ingestion.IngestionPipeline;
import com.enterprise.rag.security.authorization.RagAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Document management service.
 * Handles upload, listing, retrieval, and soft deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository      documentRepo;
    private final IngestionPipeline       ingestionPipeline;
    private final RagAuthorizationService authService;
    private final AppProperties           props;

    @Transactional
    public Document upload(MultipartFile file, String title, List<String> tags) throws IOException {
        UUID tenantId = UUID.fromString(authService.getCurrentTenantId());
        UUID userId   = UUID.fromString(authService.getCurrentUserId());

        // Compute checksum for deduplication
        byte[] bytes    = file.getBytes();
        String checksum = sha256(bytes);

        if (documentRepo.existsByTenantIdAndChecksum(tenantId, checksum)) {
            throw new BusinessException("A document with identical content already exists in this tenant.");
        }

        // Persist to storage
        Path uploadDir = Path.of(props.getStorage().getUploadDir());
        Files.createDirectories(uploadDir);
        String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path storagePath  = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);

        Document doc = Document.builder()
                .tenantId(tenantId)
                .uploadedBy(userId)
                .title(title)
                .fileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .docType(detectType(file.getContentType()))
                .status(DocumentStatus.UPLOADED)
                .storagePath(storagePath.toString())
                .checksum(checksum)
                .tags(tags != null ? tags : List.of())
                .build();

        doc = documentRepo.save(doc);

        // Schedule ingestion AFTER the current transaction commits to avoid a
        // race condition where the async thread reads the document before it is visible.
        final UUID   docId    = doc.getId();
        final String fileName = file.getOriginalFilename();
        final byte[] content  = bytes;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestionPipeline.ingest(docId, content, fileName);
            }
        });

        log.info("Document uploaded: id={}, tenant={}", docId, tenantId);
        return doc;
    }

    @Transactional(readOnly = true)
    public Page<Document> list(Pageable pageable) {
        UUID tenantId = UUID.fromString(authService.getCurrentTenantId());
        return documentRepo.findByTenantIdAndStatusNot(tenantId, DocumentStatus.DELETED, pageable);
    }

    @Transactional(readOnly = true)
    public Document getById(UUID id) {
        UUID tenantId = UUID.fromString(authService.getCurrentTenantId());
        return documentRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        Document doc = getById(id);
        doc.setStatus(DocumentStatus.DELETED);
        documentRepo.save(doc);
        log.info("Document soft-deleted: id={}", id);
    }

    private DocumentType detectType(String mimeType) {
        if (mimeType == null) return DocumentType.UNKNOWN;
        return switch (mimeType) {
            case "application/pdf"                                                       -> DocumentType.PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocumentType.DOCX;
            case "text/plain"                                                            -> DocumentType.TXT;
            case "text/markdown"                                                         -> DocumentType.MARKDOWN;
            default                                                                      -> DocumentType.UNKNOWN;
        };
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }
}
