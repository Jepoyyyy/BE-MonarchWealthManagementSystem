package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.HealthDTO;
import com.indivaragroup.jdt17wms.dto.response.RecommendationDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.services.ActionRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActionRecommendationService actionRecommendationService;

    // ==========================================
    //  getHealth Tests
    // ==========================================

    @Test
    void getHealth_shouldReturnHealthScore() throws Exception {
        HealthDTO health = HealthDTO.builder()
                .status("Excellent")
                .totalScore(85)
                .portfolioValue(BigDecimal.valueOf(1000000))
                .availableSurplus(BigDecimal.valueOf(500000))
                .build();

        when(actionRecommendationService.getHealthScore()).thenReturn(health);

        mockMvc.perform(get("/api/v1/me/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("Excellent"))
                .andExpect(jsonPath("$.result.totalScore").value(85))
                .andExpect(jsonPath("$.result.portfolio-value").value(1000000))
                .andExpect(jsonPath("$.result.available-surplus").value(500000));
    }

    @Test
    void getHealth_shouldReturn403_whenQuestionnaireNotCompleted() throws Exception {
        when(actionRecommendationService.getHealthScore())
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER, "Risk Profiler Assessment Required"));

        mockMvc.perform(get("/api/v1/me/health"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getHealth_shouldReturn404_whenUserNotFound() throws Exception {
        when(actionRecommendationService.getHealthScore())
                .thenThrow(new CoreThrowHandler(ApiError.USER_NOT_FOUND, "User Not Found"));

        mockMvc.perform(get("/api/v1/me/health"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"))
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==========================================
    //  getRecommendations Tests
    // ==========================================

    @Test
    void getRecommendations_shouldReturnRecommendationList() throws Exception {
        List<RecommendationDTO> recs = List.of(
                RecommendationDTO.builder()
                        .id(UUID.fromString(UUID.randomUUID().toString()))
                        .category("emergency")
                        .priority("high")
                        .title("Build emergency fund")
                        .build()
        );

        when(actionRecommendationService.generateRecommendations()).thenReturn(recs);

        mockMvc.perform(post("/api/v1/me/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].category").value("emergency"))
                .andExpect(jsonPath("$.result[0].priority").value("high"))
                .andExpect(jsonPath("$.result[0].title").value("Build emergency fund"));
    }

    @Test
    void getRecommendations_shouldReturn403_whenQuestionnaireNotCompleted() throws Exception {
        when(actionRecommendationService.generateRecommendations())
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER, "Risk Profiler Assessment Required"));

        mockMvc.perform(post("/api/v1/me/recommendations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getRecommendations_shouldReturn404_whenUserNotFound() throws Exception {
        when(actionRecommendationService.generateRecommendations())
                .thenThrow(new CoreThrowHandler(ApiError.USER_NOT_FOUND, "User Not Found"));

        mockMvc.perform(post("/api/v1/me/recommendations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"))
                .andExpect(jsonPath("$.code").value(404));
    }
}
