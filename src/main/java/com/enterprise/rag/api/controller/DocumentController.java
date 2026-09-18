package com.enterprise.rag.api.controller;

import com.enterprise.rag.api.dto.DocumentResponse;
import com.enterprise.rag.api.dto.DocumentUploadRequest;
import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for document management.
 * All endpoints require authentication; upload/delete require UPLOADER or ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document upload, management and status")
@SecurityRequirement(name = "Bearer")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('UPLOADER','ADMIN')")
    @Operation(summary = "Upload a document for ingestion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart(value = "tags", required = false) List<String> tags) throws IOException {

        Document doc = documentService.upload(file, title, tags);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(doc));
    }

    @GetMapping
    @Operation(summary = "List all documents for the current tenant")
    public Page<DocumentResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return documentService.list(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    public DocumentResponse getById(@PathVariable UUID id) {
        return toResponse(documentService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('UPLOADER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a document")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }

    private DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
                doc.getId(), doc.getTitle(), doc.getFileName(), doc.getMimeType(),
                doc.getFileSizeBytes(), doc.getDocType(), doc.getStatus(),
                doc.getPageCount(), doc.getTags(), doc.getCreatedAt(), doc.getUpdatedAt());
    }
}
