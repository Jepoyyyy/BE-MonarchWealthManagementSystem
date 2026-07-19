package com.indivaragroup.jdt17wms.repositories;

import com.indivaragroup.jdt17wms.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:category IS NULL OR LOWER(a.category) = LOWER(CAST(:category AS string)))
          AND (:search IS NULL OR
               LOWER(a.action) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               LOWER(a.userName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               LOWER(a.details) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:from AS string) IS NULL OR a.timestamp >= :from)
          AND (CAST(:to AS string) IS NULL OR a.timestamp <= :to)
    """)
    Page<AuditLog> findFiltered(
        @Param("category") String category,
        @Param("search") String search,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );
}
