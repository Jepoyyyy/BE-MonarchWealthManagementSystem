package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.AuditConstants;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditTrailManagementService {

    private final AuditLogRepository auditLogRepository;
    private static final String SORT_BY_TIMESTAMP = "timestamp";

    public AuditTrailManagementService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLog> getAuditLogs(Boolean headView, Pageable pageable) {
        if (Boolean.TRUE.equals(headView)) {
            return auditLogRepository.findAll(PageRequest.of(
              0, AuditConstants.ADMIN_SUMMARY_AUDIT_LOG_LIMIT, Sort.by(Sort.Direction.DESC, SORT_BY_TIMESTAMP)));
        }
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getFilteredAuditLogs(
            String category,
            String search,
            Instant from,
            Instant to,
            Pageable pageable) {
        String cat = (category != null && !category.isBlank()) ? category : null;
        String srch = (search != null && !search.isBlank()) ? search : null;
        return auditLogRepository.findFiltered(cat, srch, from, to, pageable);
    }
}
