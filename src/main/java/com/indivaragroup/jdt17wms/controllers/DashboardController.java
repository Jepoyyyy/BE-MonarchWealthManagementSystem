package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.AdminDashboardDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.UserDashboardDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping(ApiPath.BASE_ADMIN_ROUTE + ApiPath.DASHBOARD_SLUG)
    public ApiResponse<AdminDashboardDTO> getAdminDashboard() {
        return ApiResponse.success(ApiSuccess.DASHBOARD_FETCHED,
                dashboardService.getAdminDashboard());
    }

    @GetMapping(ApiPath.BASE_USER_ROUTE + ApiPath.DASHBOARD_SLUG)
    public ApiResponse<UserDashboardDTO> getUserDashboard() {
        return ApiResponse.success(ApiSuccess.DASHBOARD_FETCHED,
                dashboardService.getUserDashboard());
    }
}
