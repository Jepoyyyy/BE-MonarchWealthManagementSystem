package com.indivaragroup.jdt17wms.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.request.AdminProductCreateDTO;
import com.indivaragroup.jdt17wms.dto.request.AdminProductUpdateDTO;
import com.indivaragroup.jdt17wms.dto.response.AuditLogDTO;
import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.exceptions.ExceptionHandlingAdvice;
import com.indivaragroup.jdt17wms.services.AdminProductManagementService;
import com.indivaragroup.jdt17wms.services.AuditTrailManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ExceptionHandlingAdvice.class)
class AdminControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditTrailManagementService auditTrailManagementService;

    @MockBean
    private AdminProductManagementService adminProductManagementService;

    // --- GET /api/v1/admin/audit ---

    @Test
    @DisplayName("getAuditLogs - when headView is false, return 200 OK")
    void getAuditLogs_shouldReturnOk() throws Exception {
        when(auditTrailManagementService.getAuditLogs(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Audit logs retrieved successfully"));
    }

    @Test
    @DisplayName("getAuditLogs - when headView is true, return 200 OK")
    void getAuditLogs_withHeadViewTrue_shouldReturnOk() throws Exception {
        when(auditTrailManagementService.getAuditLogs(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/audit").param("headView", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Audit logs retrieved successfully"));
    }

    // --- GET /api/v1/admin/audit/search ---

    @Test
    @DisplayName("searchAuditLogs - when query params provided, return 200 OK")
    void searchAuditLogs_withFilters_shouldReturnOk() throws Exception {
        AuditLogDTO log = AuditLogDTO.builder()
                .id(UUID.randomUUID())
                .category("SECURITY")
                .action("LOGIN")
                .build();
        when(auditTrailManagementService.getFilteredAuditLogs(eq("SECURITY"), eq("LOGIN"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/v1/admin/audit/search")
                        .param("category", "SECURITY")
                        .param("search", "LOGIN")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Audit logs retrieved successfully"));
    }

    @Test
    @DisplayName("searchAuditLogs - when no query params provided, return 200 OK")
    void searchAuditLogs_withoutFilters_shouldReturnOk() throws Exception {
        when(auditTrailManagementService.getFilteredAuditLogs(eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/audit/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Audit logs retrieved successfully"));
    }

    @Test
    @DisplayName("searchAuditLogs - when date format is invalid, return 500 Internal Server Error")
    void searchAuditLogs_withInvalidDateFormat_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/search")
                        .param("from", "invalid-date-format"))
                .andExpect(status().isInternalServerError());
    }

    // --- GET /api/v1/admin/products ---

    @Test
    @DisplayName("listAdminProducts - with search and type filters, return 200 OK")
    void listAdminProducts_withFilters_shouldReturnOk() throws Exception {
        ProductResponseDTO product = ProductResponseDTO.builder()
                .id(UUID.randomUUID())
                .name("Gold Investment")
                .type("GOLD")
                .build();
        when(adminProductManagementService.listProducts(eq("gold"), eq("GOLD"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        mockMvc.perform(get("/api/v1/admin/products")
                        .param("search", "gold")
                        .param("type", "GOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin products retrieved successfully"));
    }

    @Test
    @DisplayName("listAdminProducts - without filters, return 200 OK")
    void listAdminProducts_withoutFilters_shouldReturnOk() throws Exception {
        when(adminProductManagementService.listProducts(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin products retrieved successfully"));
    }

    // --- POST /api/v1/admin/products ---

    @Test
    @DisplayName("createProduct - with valid request DTO, return 201 Created")
    void createProduct_withValidRequest_shouldReturnCreated() throws Exception {
        AdminProductCreateDTO dto = AdminProductCreateDTO.builder()
                .code("PRD-001")
                .name("Gold Product")
                .issuer("Monarch")
                .type("GOLD")
                .riskLevel(2)
                .annualReturn(new BigDecimal("0.0800"))
                .minInvestment(new BigDecimal("100000.0000"))
                .currentPrice(new BigDecimal("105000.0000"))
                .description("Gold asset")
                .lotSize(1)
                .isFractionalAllowed(true)
                .visible(true)
                .build();

        ProductResponseDTO createdProduct = ProductResponseDTO.builder()
                .id(UUID.randomUUID())
                .code("PRD-001")
                .name("Gold Product")
                .build();

        when(adminProductManagementService.createProduct(any(AdminProductCreateDTO.class)))
                .thenReturn(createdProduct);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Admin product created successfully"));
    }

    @Test
    @DisplayName("createProduct - with invalid/blank request body, return 400 Bad Request")
    void createProduct_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/v1/admin/products/{id} ---

    @Test
    @DisplayName("updateProduct - with valid request, return 200 OK")
    void updateProduct_withValidRequest_shouldReturnOk() throws Exception {
        UUID productId = UUID.randomUUID();
        AdminProductUpdateDTO dto = AdminProductUpdateDTO.builder()
                .name("Updated Product Name")
                .build();

        ProductResponseDTO updatedProduct = ProductResponseDTO.builder()
                .id(productId)
                .name("Updated Product Name")
                .build();

        when(adminProductManagementService.updateProduct(eq(productId), any(AdminProductUpdateDTO.class)))
                .thenReturn(updatedProduct);

        mockMvc.perform(put("/api/v1/admin/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin product updated successfully"));
    }

    @Test
    @DisplayName("updateProduct - when product not found, return 404 Not Found")
    void updateProduct_whenNotFound_shouldReturnNotFound() throws Exception {
        UUID productId = UUID.randomUUID();
        AdminProductUpdateDTO dto = AdminProductUpdateDTO.builder()
                .name("Updated Product Name")
                .build();

        when(adminProductManagementService.updateProduct(eq(productId), any(AdminProductUpdateDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND, "Product not found"));

        mockMvc.perform(put("/api/v1/admin/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updateProduct - when product ID is invalid UUID, return 500 Internal Server Error")
    void updateProduct_withInvalidUuid_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(put("/api/v1/admin/products/invalid-uuid-string")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }
}
