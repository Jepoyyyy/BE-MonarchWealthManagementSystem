package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PnLCalculationService {
    private final AssetRepository assetRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public static final Integer PERCENT_VALUE = 100;
    public static final Integer BY_TWO = 2;
    public static final Integer BY_FOUR = 4;


    public PnLCalculationService(AssetRepository assetRepository, TransactionHistoryRepository transactionHistoryRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.assetRepository = assetRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<AssetsPnLResponseDTO> computePnLForAllAssets() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

        List<Asset> assets = assetRepository.findAllByUserId(user.getId());

        return assets.stream()
                .map(this::computePnLForAsset)
                .toList();
    }

    public AssetsPnLResponseDTO computePnLForAsset(Asset asset) {
        Product product = productRepository.findById(asset.getProductId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        BigDecimal currentPrice = product.getCurrentPrice();

        List<TransactionHistory> buyTransactions = transactionHistoryRepository
                .findAllByAssetIdAndActionOrderByTransactionDateAsc(asset.getId(), TransactionAction.BUY);

        List<TransactionHistory> sellTransactions = transactionHistoryRepository
                .findAllByAssetIdAndActionOrderByTransactionDateAsc(asset.getId(), TransactionAction.SELL);

        BigDecimal avgPrice = calculateAveragePrice(asset, buyTransactions);

        BigDecimal totalSoldUnits = sellTransactions.stream()
                .map(TransactionHistory::getUnits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal units = Optional.ofNullable(asset.getUnits())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.CORRUPT_DATA));

        BigDecimal remainingUnits = units.subtract(totalSoldUnits);
        if (remainingUnits.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreThrowHandler(ApiError.CORRUPT_DATA_DETAIL,
                    ApiError.CORRUPT_DATA_DETAIL.format(remainingUnits));
        }
        BigDecimal potentialPnL = BigDecimal.ZERO;
        BigDecimal potentialPnLPercent = BigDecimal.ZERO;

        if (remainingUnits.compareTo(BigDecimal.ZERO) > 0 && avgPrice.compareTo(BigDecimal.ZERO) > 0 && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            // Potential P&L = (currentPrice - avgPrice) × remainingUnits
            potentialPnL = currentPrice.subtract(avgPrice)
                    .multiply(remainingUnits)
                    .setScale(BY_TWO, RoundingMode.HALF_UP);

            // Potential P&L % = (currentPrice - avgPrice) / avgPrice × 100
            potentialPnLPercent = currentPrice.subtract(avgPrice)
                    .divide(avgPrice, BY_FOUR, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(PERCENT_VALUE))
                    .setScale(BY_TWO, RoundingMode.HALF_UP);
        }

        BigDecimal realizedPnL = BigDecimal.ZERO;
        BigDecimal realizedPnLPercent = BigDecimal.ZERO;
        if (!sellTransactions.isEmpty()) {
            // Total proceeds from all SELL transactions
            BigDecimal totalSellProceeds = sellTransactions.stream()
                    .map(TransactionHistory::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Original cost of sold units = totalSoldUnits × avgPrice
            BigDecimal originalCostOfSold = totalSoldUnits.multiply(avgPrice);
            // Realized P&L = sellProceeds - originalCost
            realizedPnL = totalSellProceeds.subtract(originalCostOfSold)
                    .setScale(BY_TWO, RoundingMode.HALF_UP);

            // Realized P&L % = realizedPnL / originalCost × 100
            if (originalCostOfSold.compareTo(BigDecimal.ZERO) > 0) {
                realizedPnLPercent = realizedPnL
                        .divide(originalCostOfSold, BY_FOUR, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(PERCENT_VALUE))
                        .setScale(BY_TWO, RoundingMode.HALF_UP);
            }
        }

        return AssetsPnLResponseDTO.builder()
                .assetId(asset.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productType(product.getType())
                .units(remainingUnits)
                .currentValue(remainingUnits.multiply(currentPrice).setScale(BY_TWO, RoundingMode.HALF_UP))
                .avgPrice(avgPrice)
                .potentialPnL(potentialPnL)
                .potentialPnLPercent(potentialPnLPercent)
                .realizedPnL(realizedPnL)
                .realizedPnLPercent(realizedPnLPercent)
                .build();
    }

    private BigDecimal calculateAveragePrice(Asset asset, List<TransactionHistory> buyTransactions) {
        if (buyTransactions.isEmpty()) {
            // Fallback: use asset's stored amount and units
            if (asset.getUnits() != null && asset.getUnits().compareTo(BigDecimal.ZERO) > 0) {
                return asset.getAmount()
                        .divide(asset.getUnits(), BY_FOUR, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        }
        // Weighted average: Σ(price × units) / Σ(units)
        BigDecimal totalCost = buyTransactions.stream()
                .map(tx -> tx.getPricePerUnit().multiply(tx.getUnits()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUnits = buyTransactions.stream()
                .map(TransactionHistory::getUnits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalUnits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(totalUnits, BY_FOUR, RoundingMode.HALF_UP);
    }
}
