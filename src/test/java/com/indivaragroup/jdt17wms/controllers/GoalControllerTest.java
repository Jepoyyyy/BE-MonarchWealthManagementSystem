package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.GoalRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalDTO;
import com.indivaragroup.jdt17wms.services.GoalsManagementService;
import com.indivaragroup.jdt17wms.services.GoalsProjectionService;
import com.indivaragroup.jdt17wms.services.GoalProgressService;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProjectionDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoalControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalsManagementService goalsManagementService;

    @MockBean
    private GoalsProjectionService goalsProjectionService;

    @MockBean
    private GoalProgressService goalProgressService;

    @Test
    void getGoals_shouldReturnOk() throws Exception {
        GoalDTO goalDto = GoalDTO.builder()
                .id(UUID.randomUUID())
                .name("Retirement Fund")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();
        when(goalsManagementService.getGoalsForUser()).thenReturn(java.util.List.of(goalDto));

        mockMvc.perform(get("/api/v1/me/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].name").value("Retirement Fund"))
                .andExpect(jsonPath("$.result[0].target_amount").value(500000.00))
                .andExpect(jsonPath("$.result[0].status").value("IN_PROGRESS"));
    }

    @Test
    void createGoal_shouldReturnOk() throws Exception {
        GoalDTO goalDto = GoalDTO.builder()
                .id(UUID.randomUUID())
                .name("Retirement Fund")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();
        when(goalsManagementService.createGoalForUser(any(GoalRegistrationDTO.class))).thenReturn(goalDto);

        mockMvc.perform(post("/api/v1/me/goals")
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"type\":\"retirement\",\"target_amount\":500000.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Retirement Fund"))
                .andExpect(jsonPath("$.result.target_amount").value(500000.00))
                .andExpect(jsonPath("$.result.status").value("IN_PROGRESS"));
    }

    @Test
    void createGoal_shouldReturn400WhenFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/me/goals")
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"type\":\"retirement\",\"target_amount\":-100.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVALID FIELD VALUES"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error.fields[0].field").value("targetAmount"))
                .andExpect(jsonPath("$.error.fields[0].reason").value("Must not be negative"));
    }

    @Test
    void createGoal_shouldReturn409WhenMultiplePriority() throws Exception {
        when(goalsManagementService.createGoalForUser(any(GoalRegistrationDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.DUPLICATE_PRIORITY_GOALS, "Can’t set more than 1 priority"));

        mockMvc.perform(post("/api/v1/me/goals")
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"type\":\"retirement\",\"target_amount\":500000.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Can’t set more than 1 priority"))
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void updateGoal_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        GoalDTO goalDto = GoalDTO.builder()
                .id(id)
                .name("Retirement Fund")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();
        when(goalsManagementService.updateGoalForUser(any(UUID.class), any(GoalEditingDTO.class))).thenReturn(goalDto);

        mockMvc.perform(put("/api/v1/me/goals/" + id)
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"target_amount\":500000.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Retirement Fund"))
                .andExpect(jsonPath("$.result.target_amount").value(500000.00))
                .andExpect(jsonPath("$.result.status").value("IN_PROGRESS"));
    }

    @Test
    void updateGoal_shouldReturn400WhenFieldsAreInvalid() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/me/goals/" + id)
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"target_amount\":-100.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVALID FIELD VALUES"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error.fields[0].field").value("targetAmount"))
                .andExpect(jsonPath("$.error.fields[0].reason").value("Must not be negative"));
    }

    @Test
    void updateGoal_shouldReturn409WhenMultiplePriority() throws Exception {
        UUID id = UUID.randomUUID();
        when(goalsManagementService.updateGoalForUser(any(UUID.class), any(GoalEditingDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.DUPLICATE_PRIORITY_GOALS, "Can’t set more than 1 priority"));

        mockMvc.perform(put("/api/v1/me/goals/" + id)
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"target_amount\":500000.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Can’t set more than 1 priority"))
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void updateGoal_shouldReturn403WhenInsufficientIncome() throws Exception {
        UUID id = UUID.randomUUID();
        when(goalsManagementService.updateGoalForUser(any(UUID.class), any(GoalEditingDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.INSUFFICIENT_INCOME, "Can’t set more allocation than income"));

        mockMvc.perform(put("/api/v1/me/goals/" + id)
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"target_amount\":500000.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Can’t set more allocation than income"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateGoal_shouldReturn404WhenGoalNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(goalsManagementService.updateGoalForUser(any(UUID.class), any(GoalEditingDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND, "No valid item with the ID"));

        mockMvc.perform(put("/api/v1/me/goals/" + id)
                        .contentType("application/json")
                        .content("{\"name\":\"Retirement Fund\",\"target_amount\":500000.0,\"monthly_contribution\":1000.0,\"target_date\":\"2040-01-01\",\"is_priority\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No valid item with the ID"))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deleteGoal_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/me/goals/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void deleteGoal_shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND, "No valid item with the ID"))
                .when(goalsManagementService).deleteGoalForUser(any(UUID.class));

        mockMvc.perform(delete("/api/v1/me/goals/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No valid item with the ID"))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deleteGoal_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER, "Risk Profiler Assessment Required"))
                .when(goalsManagementService).deleteGoalForUser(any(UUID.class));

        mockMvc.perform(delete("/api/v1/me/goals/" + id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getGoalProjections_shouldReturnOk() throws Exception {
        GoalProjectionDTO projection = GoalProjectionDTO.builder()
                .id(UUID.randomUUID())
                .name("Retirement Fund")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .projectedDate(java.time.LocalDate.of(2050, Month.OCTOBER, 2))
                .recommendedContribution(new java.math.BigDecimal("2000.00"))
                .timeSeries(java.util.List.of(
                        GoalProjectionDTO.TimeSeriesPointDTO.builder()
                                .month(1)
                                .value(new java.math.BigDecimal("1000.00"))
                                .build()
                ))
                .build();

        when(goalsProjectionService.getProjectionsForUser()).thenReturn(java.util.List.of(projection));

        mockMvc.perform(get("/api/v1/me/goals/projections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].name").value("Retirement Fund"))
                .andExpect(jsonPath("$.result[0].projected-date").value("2050-10-02"))
                .andExpect(jsonPath("$.result[0].recommended-contribution").value(2000.00))
                .andExpect(jsonPath("$.result[0].time-series[0].month").value(1))
                .andExpect(jsonPath("$.result[0].time-series[0].value").value(1000.00));
    }

    @Test
    void getGoalProjections_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        when(goalsProjectionService.getProjectionsForUser())
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER, "Risk Profiler Assessment Required"));

        mockMvc.perform(get("/api/v1/me/goals/projections"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }
}
