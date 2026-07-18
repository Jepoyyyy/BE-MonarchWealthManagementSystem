package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Scheduled service that recalculates asset current_value daily based on current product prices.
 * Runs at 2:00 AM every day to ensure portfolio valuations reflect market prices.
 */
@Slf4j
@Service
public class AssetValueUpdateScheduler {

    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public AssetValueUpdateScheduler(AssetRepository assetRepository,
                                     ProductRepository productRepository,
                                     TransactionHistoryRepository transactionHistoryRepository) {
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    /**
     * Recalculates current_value for all assets based on current product prices.
     * Formula: current_value = (units - sold_units) × product.current_price
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @Transactional
    public void updateAllAssetValues() {
        log.info("Starting daily asset value update...");

        List<Asset> allAssets = assetRepository.findAll();
        int updated = 0;
        int skipped = 0;

        for (Asset asset : allAssets) {
            try {
                Product product = productRepository.findById(asset.getProductId()).orElse(null);
                if (product == null) {
                    log.warn("Product not found for asset {}, skipping", asset.getId());
                    skipped++;
                    continue;
                }

                BigDecimal currentPrice = product.getCurrentPrice();
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Invalid price for product {} (asset {}), skipping", product.getId(), asset.getId());
                    skipped++;
                    continue;
                }

                // Calculate remaining units (total - sold)
                List<TransactionHistory> sellTransactions = transactionHistoryRepository
                        .findAllByAssetIdAndActionOrderByTransactionDateAsc(asset.getId(), TransactionAction.SELL);

                BigDecimal soldUnits = sellTransactions.stream()
                        .map(TransactionHistory::getUnits)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal remainingUnits = asset.getUnits().subtract(soldUnits);

                // Calculate new current_value
                BigDecimal newCurrentValue = remainingUnits.multiply(currentPrice)
                        .setScale(4, RoundingMode.HALF_UP);

                BigDecimal oldValue = asset.getCurrentValue();
                asset.setCurrentValue(newCurrentValue);
                assetRepository.save(asset);

                updated++;
                log.debug("Updated asset {}: {} → {} (price: {}, units: {})",
                        asset.getId(), oldValue, newCurrentValue, currentPrice, remainingUnits);

            } catch (Exception e) {
                log.error("Error updating asset {}: {}", asset.getId(), e.getMessage(), e);
                skipped++;
            }
        }

        log.info("Daily asset value update complete: {} updated, {} skipped", updated, skipped);
    }
}
