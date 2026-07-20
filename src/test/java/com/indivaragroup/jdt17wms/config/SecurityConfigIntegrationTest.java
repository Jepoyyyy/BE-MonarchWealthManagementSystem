package com.indivaragroup.jdt17wms.config;

import com.indivaragroup.jdt17wms.repositories.*;
import com.indivaragroup.jdt17wms.services.AuthService;
import com.indivaragroup.jdt17wms.services.DashboardService;
import com.indivaragroup.jdt17wms.services.JwtService;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetRepository assetRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ExpenseRepository expenseRepository;

    @MockBean
    private FinancialProfileRepository financialProfileRepository;

    @MockBean
    private GoalRepository goalRepository;

    @MockBean
    private ProductPriceRepository productPriceRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private RecommendationRepository recommendationRepository;

    @MockBean
    private TransactionHistoryRepository transactionHistoryRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private AuthService authService;

    @Test
    void whenUnauthenticated_accessingAdminDashboard_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin-dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void whenUnauthenticated_accessingUserDashboard_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void whenUnauthenticated_accessingShorthandUserDashboard_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/me/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value(401));
    }



    @Test
    void whenAuthenticatedAsUser_accessingAdminDashboard_shouldReturn403Forbidden() throws Exception {
        String token = "valid-user-token";

        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.getEmailFromToken(token)).thenReturn("user@example.com");
        when(jwtService.getRoleFromToken(token)).thenReturn("USER");
        when(jwtService.getUserIdFromToken(token)).thenReturn(UUID.randomUUID());
        when(jwtService.getNameFromToken(token)).thenReturn("Test User");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void whenAuthenticatedAsUser_accessingUserDashboard_shouldReturnOk() throws Exception {
        String token = "valid-user-token";

        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.getEmailFromToken(token)).thenReturn("user@example.com");
        when(jwtService.getRoleFromToken(token)).thenReturn("USER");
        when(jwtService.getUserIdFromToken(token)).thenReturn(UUID.randomUUID());
        when(jwtService.getNameFromToken(token)).thenReturn("Test User");

        mockMvc.perform(get("/api/v1/me/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void whenAuthenticatedAsUser_accessingShorthandUserDashboard_shouldBypassFiltersAndReturn404NotFound() throws Exception {
        String token = "valid-user-token";

        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.getEmailFromToken(token)).thenReturn("user@example.com");
        when(jwtService.getRoleFromToken(token)).thenReturn("USER");
        when(jwtService.getUserIdFromToken(token)).thenReturn(UUID.randomUUID());
        when(jwtService.getNameFromToken(token)).thenReturn("Test User");

        mockMvc.perform(get("/me/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenUnauthenticated_loggingOut_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void whenAuthenticatedAsUser_loggingOut_shouldReturn200Ok() throws Exception {
        String token = "valid-user-token";
        UUID mockUserId = UUID.randomUUID();

        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.getEmailFromToken(token)).thenReturn("user@example.com");
        when(jwtService.getRoleFromToken(token)).thenReturn("USER");
        when(jwtService.getUserIdFromToken(token)).thenReturn(mockUserId);
        when(jwtService.getNameFromToken(token)).thenReturn("Test User");

        com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO mockResponse =
                com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO.builder()
                        .success(true)
                        .message("Logout successful")
                        .build();
        when(authService.logout(org.mockito.ArgumentMatchers.any(String.class), org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.message").value("Logout successful"));
    }
}
