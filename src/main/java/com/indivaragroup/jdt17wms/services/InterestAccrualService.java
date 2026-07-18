package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class InterestAccrualService {

    private static final Set<String> INTEREST_BEARING_TYPES = Set.of("Bond", "Sukuk", "Deposit");

    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;

    public InterestAccrualService(AssetRepository assetRepository, ProductRepository productRepository) {
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
    }

    @Scheduled(cron = "0 0 1 1 * ?") // 1st of every month at 1:00 AM
    @Transactional
    public void accrueMonthlyInterest() {
        List<Asset> interestBearingAssets = assetRepository.findAllInterestBearingAssets();

        for (Asset asset : interestBearingAssets) {
            Product product = productRepository.findById(asset.getProductId()).orElse(null);
            if (product == null || !INTEREST_BEARING_TYPES.contains(product.getType())) {
                continue;
            }

            // monthlyRate = annualReturn / 12 / 100
            BigDecimal monthlyRate = product.getAnnualReturn()
                    .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

            // Interest accrues on PRINCIPAL (amount), not market value
            BigDecimal monthlyReturn = asset.getAmount()
                    .multiply(monthlyRate)
                    .setScale(4, RoundingMode.HALF_UP);

            // Update the principal (amount) — this is the cost basis that grows
            BigDecimal newAmount = asset.getAmount().add(monthlyReturn);
            asset.setAmount(newAmount);

            // Recalculate current_value based on units × current price
            BigDecimal currentPrice = product.getCurrentPrice();
            BigDecimal newCurrentValue = asset.getUnits().multiply(currentPrice)
                    .setScale(4, RoundingMode.HALF_UP);
            asset.setCurrentValue(newCurrentValue);

            assetRepository.save(asset);

            log.info("Accrued interest for asset {}: +{} (new amount: {}, new value: {})",
                    asset.getId(), monthlyReturn, newAmount, newCurrentValue);
        }
    }
}
