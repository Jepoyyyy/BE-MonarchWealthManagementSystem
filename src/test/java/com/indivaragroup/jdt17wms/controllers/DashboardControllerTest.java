package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.services.DashboardService;
import com.indivaragroup.jdt17wms.services.JwtService;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getAdminDashboard_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin-dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserDashboard_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/me/dashboard"))
                .andExpect(status().isOk());
    }
}
