package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.TransactionHistoryDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.services.AssetsManagementService;
import com.indivaragroup.jdt17wms.services.PnLCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    void updateAsset_shouldReturn404WhenAssetNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(assetsManagementService.updateAssetForUser(any(UUID.class), any(GoalSettingDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        mockMvc.perform(put("/api/v1/me/assets/" + id)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateAsset_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(assetsManagementService.updateAssetForUser(any(UUID.class), any(GoalSettingDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(put("/api/v1/me/assets/" + id)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateAsset_shouldReturn403WhenGoalBelongsToDifferentUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(assetsManagementService.updateAssetForUser(any(UUID.class), any(GoalSettingDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.GOAL_BELONGS_TO_DIFFERENT_USER));

        mockMvc.perform(put("/api/v1/me/assets/" + id)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied. Goal belongs to different user"));
    }



    @Test
    void deleteAsset_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/me/assets/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAsset_shouldReturn404WhenAssetNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND))
                .when(assetsManagementService).deleteAssetForUser(id);

        mockMvc.perform(delete("/api/v1/me/assets/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deleteAsset_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER))
                .when(assetsManagementService).deleteAssetForUser(id);

        mockMvc.perform(delete("/api/v1/me/assets/" + id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
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

    @Test
    void getAssetsPnL_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        when(pnLCalculationService.computePnLForAllAssets())
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(get("/api/v1/me/assets/pnl"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void executeTransaction_shouldReturnOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        AssetUpdateResponseDTO responseDTO = AssetUpdateResponseDTO.builder()
                .assetId(assetId)
                .build();
        when(assetsManagementService.executeTransaction(eq(assetId), any(AssetTransactionDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/me/assets/" + assetId + "/transactions")
                        .contentType("application/json")
                        .content("{\"action\":\"BUY\",\"units\":10.0,\"amount\":100.0,\"transaction_date\":\"2023-01-01 00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.assetId").value(assetId.toString()));
    }

    @Test
    void executeTransaction_shouldReturn400WhenFieldsAreInvalid() throws Exception {
        UUID assetId = UUID.randomUUID();

        // units are negative -> invalid
        mockMvc.perform(post("/api/v1/me/assets/" + assetId + "/transactions")
                        .contentType("application/json")
                        .content("{\"action\":\"BUY\",\"units\":-5.0,\"amount\":100.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVALID FIELD VALUES"));
    }

    @Test
    void executeTransaction_shouldReturn404WhenAssetNotFound() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.executeTransaction(eq(assetId), any(AssetTransactionDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        mockMvc.perform(post("/api/v1/me/assets/" + assetId + "/transactions")
                        .contentType("application/json")
                        .content("{\"action\":\"BUY\",\"units\":10.0,\"amount\":100.0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void executeTransaction_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.executeTransaction(eq(assetId), any(AssetTransactionDTO.class)))
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(post("/api/v1/me/assets/" + assetId + "/transactions")
                        .contentType("application/json")
                        .content("{\"action\":\"BUY\",\"units\":10.0,\"amount\":100.0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }



    @Test
    void getAssetPnL_shouldReturnOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).build();
        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder().assetId(assetId).build();

        when(assetsManagementService.findAssetByIdAndUser(assetId)).thenReturn(asset);
        when(pnLCalculationService.computePnLForAsset(asset)).thenReturn(pnl);

        mockMvc.perform(get("/api/v1/me/assets/" + assetId + "/pnl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.assetId").value(assetId.toString()));
    }

    @Test
    void getAssetPnL_shouldReturn404WhenAssetNotFound() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.findAssetByIdAndUser(assetId))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/me/assets/" + assetId + "/pnl"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getAssetPnL_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.findAssetByIdAndUser(assetId))
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(get("/api/v1/me/assets/" + assetId + "/pnl"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getAssetTransactions_shouldReturnOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        TransactionHistoryDTO tx = TransactionHistoryDTO.builder().assetId(assetId).build();
        when(assetsManagementService.getTransactionHistoryForAsset(assetId))
                .thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/me/assets/" + assetId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].asset_id").value(assetId.toString()));
    }

    @Test
    void getAssetTransactions_shouldReturn404WhenAssetNotFound() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.getTransactionHistoryForAsset(assetId))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/me/assets/" + assetId + "/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getAssetTransactions_shouldReturn403WhenQuestionnaireNotCompleted() throws Exception {
        UUID assetId = UUID.randomUUID();
        when(assetsManagementService.getTransactionHistoryForAsset(assetId))
                .thenThrow(new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));

        mockMvc.perform(get("/api/v1/me/assets/" + assetId + "/transactions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

