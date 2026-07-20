package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetValueUpdateDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.AssetDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.services.AssetsManagementService;
import com.indivaragroup.jdt17wms.services.PnLCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_ASSETS_PATH)
public class AssetController {

    private final AssetsManagementService assetsManagementService;
    private final PnLCalculationService pnLCalculationService;

    public AssetController(AssetsManagementService assetsManagementService,
                           PnLCalculationService pnLCalculationService) {
        this.assetsManagementService = assetsManagementService;
        this.pnLCalculationService = pnLCalculationService;
    }


    @PostMapping
    @AuditLogged(action = "CREATE_ASSET", category = "ASSET")
    public ApiResponse<AssetDTO> createAsset(@Valid @RequestBody AssetRegistrationDTO dto) {
        return ApiResponse.created(ApiSuccess.ASSET_CREATED,
                assetsManagementService.createAssetForUser(dto));
    }

    @GetMapping
    public ApiResponse<List<AssetDTO>> getAssets() {
        return ApiResponse.success(ApiSuccess.ASSETS_FETCHED,
                assetsManagementService.getAssetsForUser());
    }


    @GetMapping("/pnl")
    public ApiResponse<List<AssetsPnLResponseDTO>> getAssetsPnL() {
        return ApiResponse.success(ApiSuccess.ASSETS_FETCHED,
                pnLCalculationService.computePnLForAllAssets());
    }

    @GetMapping("/transaction-logs")
    public ApiResponse<List<TransactionHistory>> getTransactionLogs() {
        return ApiResponse.success(ApiSuccess.TRANSACTION_LOGS_FETCHED,
                assetsManagementService.getTransactionLogsForUser());
    }

    @PutMapping("/{id}")
    @AuditLogged(action = "UPDATE_ASSET", category = "ASSET")
    public ApiResponse<AssetDTO> updateAsset(@PathVariable UUID id,
                                           @Valid @RequestBody GoalSettingDTO goalSettingDTO) {
        return ApiResponse.success(ApiSuccess.ASSET_UPDATED,
                assetsManagementService.updateAssetForUser(id, goalSettingDTO));
    }

    @DeleteMapping("/{id}")
    @AuditLogged(action = "DELETE_ASSET", category = "ASSET")
    public ApiResponse<Void> deleteAsset(@PathVariable UUID id) {
        assetsManagementService.deleteAssetForUser(id);
        return ApiResponse.success(ApiSuccess.ASSET_DELETED, null);
    }


    @PostMapping("/{assetId}/transactions")
    public ApiResponse<AssetUpdateResponseDTO> executeTransaction(
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetTransactionDTO dto) {
        return ApiResponse.success(ApiSuccess.EXECUTED,
                assetsManagementService.executeTransaction(assetId, dto));
    }

    @PatchMapping("/{assetId}/value")
    public ApiResponse<AssetDTO> updateAssetValue(
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetValueUpdateDTO dto) {
        return ApiResponse.success(ApiSuccess.ASSET_UPDATED,
                assetsManagementService.updateAssetValue(assetId, dto));
    }

    @PatchMapping("/{assetId}/goal")
    public ApiResponse<Asset> updateAssetGoal(
            @PathVariable UUID assetId,
            @RequestParam(required = false) UUID goalId) {
        return ApiResponse.success(ApiSuccess.ASSET_UPDATED,
                assetsManagementService.updateAssetGoal(assetId, goalId));
    }

    @GetMapping("/{assetId}/pnl")
    public ApiResponse<AssetsPnLResponseDTO> getAssetPnL(@PathVariable UUID assetId) {
        Asset asset = assetsManagementService.findAssetByIdAndUser(assetId);
        return ApiResponse.success(ApiSuccess.ASSETS_FETCHED,
                pnLCalculationService.computePnLForAsset(asset));
    }

    @GetMapping("/{assetId}/transactions")
    public ApiResponse<List<TransactionHistory>> getAssetTransactions(@PathVariable UUID assetId) {
        return ApiResponse.success(ApiSuccess.TRANSACTION_LOGS_FETCHED,
                assetsManagementService.getTransactionHistoryForAsset(assetId));
    }
}
