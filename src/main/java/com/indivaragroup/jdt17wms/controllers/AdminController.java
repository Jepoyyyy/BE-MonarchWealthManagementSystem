package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.constants.AuditConstants;
import com.indivaragroup.jdt17wms.dto.request.AdminProductCreateDTO;
import com.indivaragroup.jdt17wms.dto.request.AdminProductUpdateDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.AuditLogDTO;
import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.AdminProductManagementService;
import com.indivaragroup.jdt17wms.services.AuditTrailManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_ADMIN_ROUTE)
public class AdminController {

    private final AuditTrailManagementService auditTrailManagementService;
    private final AdminProductManagementService adminProductManagementService;

    public AdminController(
            AuditTrailManagementService auditTrailManagementService,
            AdminProductManagementService adminProductManagementService) {
        this.auditTrailManagementService = auditTrailManagementService;
        this.adminProductManagementService = adminProductManagementService;
    }


    @GetMapping(ApiPath.AUDIT_ROUTE)
    public ApiResponse<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false, defaultValue = "false") Boolean headView,
            Pageable pageable) {
        return ApiResponse.success(ApiSuccess.AUDIT_LOGS_FETCHED,
                auditTrailManagementService.getAuditLogs(headView, pageable));
    }

    @GetMapping(ApiPath.AUDIT_SEARCH_ROUTE)
    public ApiResponse<Page<AuditLogDTO>> searchAuditLogs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {
        return ApiResponse.success(ApiSuccess.AUDIT_LOGS_FETCHED,
                auditTrailManagementService.getFilteredAuditLogs(category, search, from, to, pageable));
    }

    @GetMapping(ApiPath.PRODUCTS_ROUTE)
    public ApiResponse<Page<ProductResponseDTO>> listAdminProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        return ApiResponse.success(ApiSuccess.ADMIN_PRODUCTS_FETCHED,
                adminProductManagementService.listProducts(search, type, pageable));
    }


    @PostMapping(ApiPath.PRODUCTS_ROUTE)
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLogged(action = AuditConstants.Action.CREATE_PRODUCT, category = AuditConstants.PRODUCT_CATEGORY)
    public ApiResponse<ProductResponseDTO> createProduct(@Valid @RequestBody AdminProductCreateDTO dto) {
        return ApiResponse.success(ApiSuccess.ADMIN_PRODUCT_CREATED,
                adminProductManagementService.createProduct(dto));
    }


    @PutMapping(ApiPath.PRODUCTS_ID_ROUTE)
    @AuditLogged(action = AuditConstants.Action.UPDATE_PRODUCT, category = AuditConstants.PRODUCT_CATEGORY)
    public ApiResponse<ProductResponseDTO> updateProduct(
            @PathVariable UUID id,
            @RequestBody AdminProductUpdateDTO dto) {
        return ApiResponse.success(ApiSuccess.ADMIN_PRODUCT_UPDATED,
                adminProductManagementService.updateProduct(id, dto));
    }
}
