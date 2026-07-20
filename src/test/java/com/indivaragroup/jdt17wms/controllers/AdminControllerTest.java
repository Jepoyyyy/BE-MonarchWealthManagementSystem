package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.services.AdminProductManagementService;
import com.indivaragroup.jdt17wms.services.AuditTrailManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditTrailManagementService auditTrailManagementService;

    @MockBean
    private AdminProductManagementService adminProductManagementService;

    @Test
    void getAuditLogs_shouldReturnOk() throws Exception {
        when(auditTrailManagementService.getAuditLogs(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuditLogs_withHeadViewTrue_shouldReturnOk() throws Exception {
        when(auditTrailManagementService.getAuditLogs(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/audit").param("headView", "true"))
                .andExpect(status().isOk());
    }
}
