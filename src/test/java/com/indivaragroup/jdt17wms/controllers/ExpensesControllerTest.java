package com.indivaragroup.jdt17wms.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.request.ExpenseDTO;
import com.indivaragroup.jdt17wms.dto.request.FinancialProfileDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.services.ExpensesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpensesController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpensesControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpensesService expensesService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getFinances_shouldReturnOk() throws Exception {
        ExpenseDTO expense = ExpenseDTO.builder()
                .id(UUID.randomUUID())
                .monthlyIncome(BigDecimal.valueOf(20000))
                .housing(BigDecimal.valueOf(5000))
                .food(BigDecimal.valueOf(3000))
                .transport(BigDecimal.valueOf(1500))
                .utilities(BigDecimal.valueOf(1000))
                .healthcare(BigDecimal.valueOf(800))
                .entertainment(BigDecimal.valueOf(1200))
                .insurance(BigDecimal.valueOf(1000))
                .other(BigDecimal.valueOf(500))
                .totalExpenses(BigDecimal.valueOf(14000))
                .build();

        when(expensesService.getFinancesForUser()).thenReturn(expense);

        mockMvc.perform(get("/api/v1/me/finances")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Financial profile retrieved successfully"))
                .andExpect(jsonPath("$.result.monthly_income").value(20000.0))
                .andExpect(jsonPath("$.result.total_expenses").value(14000.0));
    }

    @Test
    void getFinances_shouldReturnNotFound() throws Exception {
        when(expensesService.getFinancesForUser())
                .thenThrow(new CoreThrowHandler(ApiError.FINANCIAL_PROFILE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/me/finances")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Financial profile not found"));
    }

    @Test
    void updateFinances_shouldReturnOk() throws Exception {
        ExpenseDTO expense = ExpenseDTO.builder()
                .id(UUID.randomUUID())
                .monthlyIncome(BigDecimal.valueOf(25000))
                .housing(BigDecimal.valueOf(6000))
                .food(BigDecimal.valueOf(3500))
                .transport(BigDecimal.valueOf(2000))
                .utilities(BigDecimal.valueOf(1200))
                .healthcare(BigDecimal.valueOf(1000))
                .entertainment(BigDecimal.valueOf(1500))
                .insurance(BigDecimal.valueOf(1500))
                .other(BigDecimal.valueOf(800))
                .totalExpenses(BigDecimal.valueOf(17500))
                .build();

        ExpenseDTO inputExpDto = ExpenseDTO.builder()
                .housing(BigDecimal.valueOf(6000))
                .food(BigDecimal.valueOf(3500))
                .transport(BigDecimal.valueOf(2000))
                .utilities(BigDecimal.valueOf(1200))
                .healthcare(BigDecimal.valueOf(1000))
                .entertainment(BigDecimal.valueOf(1500))
                .insurance(BigDecimal.valueOf(1500))
                .other(BigDecimal.valueOf(800))
                .build();

        FinancialProfileDTO dto = FinancialProfileDTO.builder()
                .monthlyIncome(BigDecimal.valueOf(25000))
                .expenseDTO(inputExpDto)
                .build();

        when(expensesService.updateFinancesForUser(any(FinancialProfileDTO.class))).thenReturn(expense);

        mockMvc.perform(put("/api/v1/me/finances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Financial profile updated successfully"))
                .andExpect(jsonPath("$.result.monthly_income").value(25000.0))
                .andExpect(jsonPath("$.result.total_expenses").value(17500.0));
    }

    @Test
    void updateFinances_shouldReturnBadRequestWhenValidationFails() throws Exception {
        ExpenseDTO expDto = ExpenseDTO.builder()
                .housing(BigDecimal.valueOf(-6000)) // negative
                .food(BigDecimal.valueOf(3500))
                .transport(BigDecimal.valueOf(2000))
                .utilities(BigDecimal.valueOf(1200))
                .healthcare(BigDecimal.valueOf(1000))
                .entertainment(BigDecimal.valueOf(1500))
                .insurance(BigDecimal.valueOf(1500))
                .other(BigDecimal.valueOf(800))
                .build();

        FinancialProfileDTO dto = FinancialProfileDTO.builder()
                .monthlyIncome(BigDecimal.valueOf(25000))
                .expenseDTO(expDto)
                .build();

        mockMvc.perform(put("/api/v1/me/finances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("INVALID FIELD VALUES"));
    }
}
