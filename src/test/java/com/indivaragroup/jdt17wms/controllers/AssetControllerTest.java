package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetValueUpdateDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.services.AssetsManagementService;
import com.indivaragroup.jdt17wms.services.PnLCalculationService;
import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssetControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetsManagementService assetsManagementService;

    @MockBean
    private PnLCalculationService pnLCalculationService;

    @Test
    void createAsset_shouldReturnOk() throws Exception {
        UUID productId = UUID.randomUUID();
        when(assetsManagementService.createAssetForUser(any(AssetRegistrationDTO.class)))
                .thenReturn(new AssetDTO());

        mockMvc.perform(post("/api/v1/me/assets")
                        .contentType("application/json")
                        .content("{\"product_id\":\"" + productId + "\",\"units\":10.5,\"amount\":100.0,\"purchase_date\":\"2023-01-01 00:00:00\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void createAsset_shouldReturn400WhenFieldsAreInvalid() throws Exception {
        UUID productId = UUID.randomUUID();

        // units are negative -> invalid
        mockMvc.perform(post("/api/v1/me/assets")
                        .contentType("application/json")
                        .content("{\"product_id\":\"" + productId + "\",\"units\":-10.5,\"amount\":100.0,\"purchase_date\":\"2023-01-01 00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVALID FIELD VALUES"))
                .andExpect(jsonPath("$.error.fields[0].field").value("units"))
                .andExpect(jsonPath("$.error.fields[0].reason").value("Must be at least positive"));
    }

    @Test
    void createAsset_shouldReturn409WhenProductIsDelisted() throws Exception {
        UUID productId = UUID.randomUUID();
        when(assetsManagementService.createAssetForUser(any(AssetRegistrationDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.DELISTED_PRODUCT));

        mockMvc.perform(post("/api/v1/me/assets")
                        .contentType("application/json")
                        .content("{\"product_id\":\"" + productId + "\",\"units\":10.5,\"amount\":100.0,\"purchase_date\":\"2023-01-01 00:00:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Can’t track delisted products"))
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void getAssets_shouldReturnOk() throws Exception {
        when(assetsManagementService.getAssetsForUser()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/me/assets"))
                .andExpect(status().isOk());
    }

    @Test
    void getAssets_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        when(assetsManagementService.getAssetsForUser())
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(get("/api/v1/me/assets"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getTransactionLogs_shouldReturnOk() throws Exception {
        when(assetsManagementService.getTransactionLogsForUser()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/me/assets/transaction-logs"))
                .andExpect(status().isOk());
    }

    @Test
    void getTransactionLogs_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        when(assetsManagementService.getTransactionLogsForUser())
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(get("/api/v1/me/assets/transaction-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateAsset_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(assetsManagementService.updateAssetForUser(any(UUID.class), any(GoalSettingDTO.class)))
                .thenReturn(new AssetDTO());

        mockMvc.perform(put("/api/v1/me/assets/" + id)
                        .contentType("application/json")
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAssetValue_shouldReturnOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        AssetDTO dto = AssetDTO.builder()
                .id(assetId)
                .currentValue(BigDecimal.valueOf(600000))
                .build();
        when(assetsManagementService.updateAssetValue(any(UUID.class), any(AssetValueUpdateDTO.class)))
                .thenReturn(dto);

        mockMvc.perform(patch("/api/v1/me/assets/" + assetId + "/value")
                        .contentType("application/json")
                        .content("{\"current_value\":600000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(assetId.toString()))
                .andExpect(jsonPath("$.result.current_value").value(600000));
    }

    @Test
    void updateAssetValue_shouldReturn404WhenAssetNotFound() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.updateAssetValue(any(UUID.class), any(AssetValueUpdateDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/me/assets/" + assetId + "/value")
                        .contentType("application/json")
                        .content("{\"current_value\":600000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateAssetValue_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.updateAssetValue(any(UUID.class), any(AssetValueUpdateDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(patch("/api/v1/me/assets/" + assetId + "/value")
                        .contentType("application/json")
                        .content("{\"current_value\":600000}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Risk Profiler Assessment Required"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deleteAsset_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/me/assets/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void getAssetsPnL_shouldReturnOk() throws Exception {
        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder()
                .assetId(UUID.randomUUID())
                .currentValue(BigDecimal.TEN)
                .build();
        when(pnLCalculationService.computePnLForAllAssets()).thenReturn(List.of(pnl));

        mockMvc.perform(get("/api/v1/me/assets/pnl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].currentValue").value(10));
    }
}
