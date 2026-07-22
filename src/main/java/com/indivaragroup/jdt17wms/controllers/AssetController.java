package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.constants.AuditConstants;
import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetValueUpdateDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.AssetDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.TransactionHistoryDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.services.AssetsManagementService;
import com.indivaragroup.jdt17wms.services.PnLCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_ASSETS_ROUTE)
public class AssetController {

    private final AssetsManagementService assetsManagementService;
    private final PnLCalculationService pnLCalculationService;

    public AssetController(AssetsManagementService assetsManagementService,
                           PnLCalculationService pnLCalculationService) {
        this.assetsManagementService = assetsManagementService;
        this.pnLCalculationService = pnLCalculationService;
    }


    @PostMapping
    @AuditLogged(action = AuditConstants.Action.CREATE_ASSET, category = AuditConstants.ASSET_CATEGORY)
    public ApiResponse<AssetDTO> createAsset(@Valid @RequestBody AssetRegistrationDTO dto) {
        return ApiResponse.created(ApiSuccess.ASSET_CREATED,
                assetsManagementService.createAssetForUser(dto));
    }

    @GetMapping
    public ApiResponse<List<AssetDTO>> getAssets() {
        return ApiResponse.success(ApiSuccess.ASSETS_FETCHED,
                assetsManagementService.getAssetsForUser());
    }


    @GetMapping(ApiPath.PNL_ROUTE)
    public ApiResponse<List<AssetsPnLResponseDTO>> getAssetsPnL() {
        return ApiResponse.success(ApiSuccess.ASSETS_FETCHED,
                pnLCalculationService.computePnLForAllAssets());
    }

    @GetMapping(ApiPath.TRANSACTION_LOGS_ROUTE)
    public ApiResponse<List<TransactionHistoryDTO>> getTransactionLogs() {
        return ApiResponse.success(ApiSuccess.TRANSACTION_LOGS_FETCHED,
                assetsManagementService.getTransactionLogsForUser());
    }

    @PutMapping(ApiPath.ID_SLUG)
    @AuditLogged(action = AuditConstants.Action.UPDATE_ASSET, category = AuditConstants.ASSET_CATEGORY)
    public ApiResponse<AssetDTO> updateAsset(@PathVariable UUID id,
                                           @Valid @RequestBody GoalSettingDTO goalSettingDTO) {
        return ApiResponse.success(ApiSuccess.ASSET_UPDATED,
                assetsManagementService.updateAssetForUser(id, goalSettingDTO));
    }

    @DeleteMapping(ApiPath.ID_SLUG)
    @AuditLogged(action = AuditConstants.Action.DELETE_ASSET, category = AuditConstants.ASSET_CATEGORY)
    public ApiResponse<Void> deleteAsset(@PathVariable UUID id) {
        assetsManagementService.deleteAssetForUser(id);
        return ApiResponse.success(ApiSuccess.ASSET_DELETED, null);
    }


    @PostMapping(ApiPath.ASSET_TRANSACTIONS_ROUTE)
    public ApiResponse<AssetUpdateResponseDTO> executeTransaction(
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetTransactionDTO dto) {
        return ApiResponse.success(ApiSuccess.EXECUTED,
                assetsManagementService.executeTransaction(assetId, dto));
    }

    @PatchMapping(ApiPath.ASSET_VALUE_ROUTE)
    public ApiResponse<AssetDTO> updateAssetValue(
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetValueUpdateDTO dto) {
        return ApiResponse.success(ApiSuccess.ASSET_UPDATED,
                assetsManagementService.updateAssetValue(assetId, dto));
    }

    @PatchMapping(ApiPath.ASSET_GOAL_ROUTE)
    public ApiResponse<AssetDTO> updateAssetGoal(
            @PathVariable UUID assetId,
            @RequestParam(required = false) UUID goalId) {
        return ApiResponse.success(ApiSuccess.ASSET_UPDATED,
                assetsManagementService.updateAssetGoal(assetId, goalId));
    }

    @GetMapping(ApiPath.ASSET_PNL_ROUTE)
    public ApiResponse<AssetsPnLResponseDTO> getAssetPnL(@PathVariable UUID assetId) {
        Asset asset = assetsManagementService.findAssetByIdAndUser(assetId);
        return ApiResponse.success(ApiSuccess.ASSETS_FETCHED,
                pnLCalculationService.computePnLForAsset(asset));
    }

    @GetMapping(ApiPath.ASSET_TRANSACTIONS_ROUTE)
    public ApiResponse<List<TransactionHistoryDTO>> getAssetTransactions(@PathVariable UUID assetId) {
        return ApiResponse.success(ApiSuccess.TRANSACTION_LOGS_FETCHED,
                assetsManagementService.getTransactionHistoryForAsset(assetId));
    }
}
