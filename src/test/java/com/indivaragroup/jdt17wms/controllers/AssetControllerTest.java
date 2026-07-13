package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.services.AssetsManagementService;
import com.indivaragroup.jdt17wms.services.InvestmentProductTrackingService;
import com.indivaragroup.jdt17wms.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetsManagementService assetsManagementService;

    @MockBean
    private InvestmentProductTrackingService investmentProductTrackingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void createAsset_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/me/assets"))
                .andExpect(status().isOk());
    }

    @Test
    void getAssets_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/me/assets"))
                .andExpect(status().isOk());
    }

    @Test
    void getTransactionLogs_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/me/assets/transactions-logs"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAsset_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/me/assets/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAsset_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/me/assets/" + id))
                .andExpect(status().isOk());
    }
}
