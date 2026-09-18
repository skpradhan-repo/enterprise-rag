package com.enterprise.rag.document.repository;

import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByTenantIdAndStatusNot(UUID tenantId, DocumentStatus status, Pageable pageable);

    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndChecksum(UUID tenantId, String checksum);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status")
    Page<Document> findByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") DocumentStatus status,
            Pageable pageable);
}
