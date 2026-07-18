package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetValueUpdateDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.Recommendation;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.RecommendationRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssetsManagementService implements VerifiedUserProvider {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ProductRepository productRepository;
    private final GoalRepository goalRepository;
    private final RecommendationRepository recommendationRepository;
    private final AssetTransactionService assetTransactionService;

    private static final int BY_FOUR = 4;

    @Override
    public UserRepository userRepository() { return userRepository; }

    public AssetsManagementService(AssetRepository assetRepository,
                                   UserRepository userRepository,
                                   TransactionHistoryRepository transactionHistoryRepository,
                                   ProductRepository productRepository,
                                   GoalRepository goalRepository,
                                   RecommendationRepository recommendationRepository,
                                   AssetTransactionService assetTransactionService) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.productRepository = productRepository;
        this.goalRepository = goalRepository;
        this.recommendationRepository = recommendationRepository;
        this.assetTransactionService = assetTransactionService;
    }

    public List<Asset> getAssetsForUser() {
        User user = getVerifiedUser();
        return assetRepository.findAllByUserId(user.getId());
    }

    public List<TransactionHistory> getTransactionLogsForUser() {
        User user = getVerifiedUser();
        return transactionHistoryRepository.findAllByUserId(user.getId());
    }

    @Transactional
    public Asset createAssetForUser(AssetRegistrationDTO dto) {
        User user = getVerifiedUser();

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.NOT_FOUND,"No valid item with the ID"));

        if (!Boolean.TRUE.equals(product.getVisible())) {
            throw new CoreThrowHandler(ApiError.DELISTED_PRODUCT);
        }

        if (dto.getUnits() == null || dto.getUnits().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST, "Units must be greater than zero");
        }

        Instant purchaseInstant = dto.getPurchaseDate().atZone(ZoneId.systemDefault()).toInstant();

        Asset asset = Asset.builder()
                .userId(user.getId())
                .productId(product.getId())
                .units(dto.getUnits())
                .amount(dto.getAmount())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(purchaseInstant)
                .platform(dto.getPlatform())
                .notes(dto.getNotes())
                .build();

        Asset savedAsset = assetRepository.save(asset);

        // Record BUY transaction log
        BigDecimal pricePerUnit = dto.getAmount().divide(dto.getUnits(), BY_FOUR, RoundingMode.HALF_UP);

        TransactionHistory buyHistory = TransactionHistory.builder()
                .userId(user.getId())
                .productId(product.getId())
                .assetId(savedAsset.getId())
                .action(TransactionAction.BUY)
                .pricePerUnit(pricePerUnit)
                .units(dto.getUnits())
                .totalAmount(dto.getAmount())
                .transactionDate(purchaseInstant)
                .notes(dto.getNotes())
                .build();

        transactionHistoryRepository.save(buyHistory);

        return savedAsset;
    }

    @Transactional
    public Asset updateAssetForUser(UUID assetId, GoalSettingDTO dto) {
        User user = getVerifiedUser();

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        if (dto.getGoalId() != null) {
            goalRepository.findById(dto.getGoalId())
                    .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));
            asset.setGoalId(dto.getGoalId());
        } else {
            asset.setGoalId(null);
        }

        return assetRepository.save(asset);
    }

    @Transactional
    public void deleteAssetForUser(UUID assetId) {
        User user = getVerifiedUser();

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        // Record SELL transaction log
        BigDecimal pricePerUnit = BigDecimal.ZERO;
        if (Objects.requireNonNullElse(asset.getUnits(), BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0) {
            pricePerUnit = asset.getAmount().divide(asset.getUnits(), BY_FOUR, RoundingMode.HALF_UP);
        }

        TransactionHistory sellHistory = TransactionHistory.builder()
                .userId(user.getId())
                .productId(asset.getProductId())
                .assetId(asset.getId())
                .action(TransactionAction.SELL)
                .pricePerUnit(pricePerUnit)
                .units(asset.getUnits())
                .totalAmount(asset.getAmount())
                .transactionDate(Instant.now())
                .notes(asset.getNotes())
                .build();

        transactionHistoryRepository.save(sellHistory);

        // Deallocate resolution references in recommendations to prevent constraint violations
        List<Recommendation> recommendations = recommendationRepository.findAllByResolvedByAssetId(asset.getId());
        for (Recommendation recommendation : recommendations) {
            recommendation.setResolvedByAssetId(null);
            recommendationRepository.save(recommendation);
        }

        assetRepository.delete(asset);
    }

    @Transactional
    public AssetUpdateResponseDTO executeTransaction(UUID assetId, AssetTransactionDTO dto) {
        User user = getVerifiedUser();

        if (dto.getAction() == TransactionAction.BUY) {
            return assetTransactionService.executeBuyTransaction(assetId, dto, user);
        } else if (dto.getAction() == TransactionAction.SELL) {
            return assetTransactionService.executeSellTransaction(assetId, dto, user);
        }

        throw new CoreThrowHandler(ApiError.BAD_REQUEST, "Invalid transaction action");
    }

    @Transactional
    public Asset updateAssetGoal(UUID assetId, UUID goalId) {
        User user = getVerifiedUser();
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        if (goalId != null) {
            goalRepository.findById(goalId)
                    .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));
        }

        asset.setGoalId(goalId);
        return assetRepository.save(asset);
    }

    @Transactional
    public Asset updateAssetValue(UUID assetId, AssetValueUpdateDTO dto) {
        User user = getVerifiedUser();
        return assetTransactionService.updateAssetCurrentValue(assetId, dto.getCurrentValue(), dto.getNotes(), user);
    }

    public Asset findAssetByIdAndUser(UUID assetId) {
        User user = getVerifiedUser();
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));
        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }
        return asset;
    }

    @Override
    public User getVerifiedUser() {
        return VerifiedUserProvider.super.getVerifiedUser();
    }
}
