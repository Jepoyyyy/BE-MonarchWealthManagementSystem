package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.services.AuditTrailManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AdminController {

    private final AuditTrailManagementService auditTrailManagementService;

    public AdminController(AuditTrailManagementService auditTrailManagementService) {
        this.auditTrailManagementService = auditTrailManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false, defaultValue = "false") Boolean headView,
            Pageable pageable) {
        Page<AuditLog> result = auditTrailManagementService.getAuditLogs(headView, pageable);
        return ResponseEntity.ok(ApiResponse.<Page<AuditLog>>builder()
                .restApiResponseHttpCode(ApiSuccess.AUDIT_LOGS_FETCHED.getCode())
                .restApiResponseMessage(ApiSuccess.AUDIT_LOGS_FETCHED.getMessage())
                .restApiResponseResult(result)
                .restApiResponseError(null)
                .build());
    }
}
