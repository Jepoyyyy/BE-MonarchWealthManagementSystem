package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.AdminDashboardDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/admin-dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDTO>> getAdminDashboard() {
        AdminDashboardDTO result = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(ApiResponse.<AdminDashboardDTO>builder()
                .restApiResponseHttpCode(ApiSuccess.DASHBOARD_FETCHED.getCode())
                .restApiResponseMessage(ApiSuccess.DASHBOARD_FETCHED.getMessage())
                .restApiResponseResult(result)
                .restApiResponseError(null)
                .build());
    }

    @GetMapping("/api/v1/me/dashboard")
    public void getUserDashboard() {
    }
}
