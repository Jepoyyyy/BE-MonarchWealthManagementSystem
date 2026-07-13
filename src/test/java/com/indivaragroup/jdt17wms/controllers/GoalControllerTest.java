package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.services.GoalsManagementService;
import com.indivaragroup.jdt17wms.services.GoalsProjectionService;
import com.indivaragroup.jdt17wms.services.JwtService;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalsManagementService goalsManagementService;

    @MockBean
    private GoalsProjectionService goalsProjectionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getGoals_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/me/goals"))
                .andExpect(status().isOk());
    }

    @Test
    void createGoal_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/me/goals"))
                .andExpect(status().isOk());
    }

    @Test
    void updateGoal_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/me/goals/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void deleteGoal_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/me/goals/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void getGoalProjections_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/me/goals/projections"))
                .andExpect(status().isOk());
    }
}
