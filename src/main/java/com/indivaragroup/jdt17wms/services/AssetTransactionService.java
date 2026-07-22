package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
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
public class AssetTransactionService {

    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final PnLCalculationService pnLCalculationService;


    public AssetTransactionService(AssetRepository assetRepository,
                                   ProductRepository productRepository,
                                   TransactionHistoryRepository transactionHistoryRepository,
                                   PnLCalculationService pnLCalculationService) {
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.pnLCalculationService = pnLCalculationService;
    }

    @Transactional
    public AssetUpdateResponseDTO executeBuyTransaction(UUID assetId, AssetTransactionDTO dto, User user) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        Product product = productRepository.findById(asset.getProductId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!Boolean.TRUE.equals(product.getVisible())) {
            throw new CoreThrowHandler(ApiError.DELISTED_PRODUCT);
        }

        validateTransactionByAssetType(product, dto);
        BigDecimal currentPrice = product.getCurrentPrice();

        // Determine units and amount
        boolean hasUnits = dto.getUnits() != null && dto.getUnits().compareTo(BigDecimal.ZERO) > 0;

        BigDecimal unitsToBuy;
        BigDecimal totalAmount;

        if (hasUnits) {
            unitsToBuy = dto.getUnits();
            totalAmount = unitsToBuy.multiply(currentPrice).setScale(4, RoundingMode.HALF_UP);
        } else {
            totalAmount = dto.getAmount().setScale(4, RoundingMode.HALF_UP);
            unitsToBuy = totalAmount.divide(currentPrice, 6, RoundingMode.HALF_UP);
        }

        // Validate min investment
        if (product.getMinInvestment().compareTo(BigDecimal.ZERO) > 0
                && totalAmount.compareTo(product.getMinInvestment()) < 0) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                    "Amount must be at least minimum investment of " + product.getMinInvestment());
        }

        // Calculate price per unit
        BigDecimal pricePerUnit = totalAmount.divide(unitsToBuy, 4, RoundingMode.HALF_UP);

        // Update asset
        asset.setUnits(asset.getUnits().add(unitsToBuy));
        asset.setAmount(asset.getAmount().add(totalAmount));

        // currentValue = available (unsold) units × current price
        List<TransactionHistory> soldTxs = transactionHistoryRepository
                .findAllByAssetIdAndActionOrderByTransactionDateAsc(asset.getId(), TransactionAction.SELL);
        BigDecimal totalSold = soldTxs.stream()
                .map(TransactionHistory::getUnits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableAfterBuy = asset.getUnits().subtract(totalSold);
        asset.setCurrentValue(availableAfterBuy.multiply(currentPrice).setScale(4, RoundingMode.HALF_UP));
        assetRepository.save(asset);

        // Create BUY transaction record
        Instant txInstant = (dto.getTransactionDate() != null)
                ? dto.getTransactionDate().atZone(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        TransactionHistory tx = TransactionHistory.builder()
                .userId(user.getId())
                .productId(product.getId())
                .assetId(asset.getId())
                .goalId(dto.getGoalId())
                .action(TransactionAction.BUY)
                .pricePerUnit(pricePerUnit)
                .units(unitsToBuy)
                .totalAmount(totalAmount)
                .transactionDate(txInstant)
                .notes(dto.getNotes())
                .build();
        TransactionHistory savedTx = transactionHistoryRepository.save(tx);

        // Recalculate PnL
        AssetsPnLResponseDTO pnl = pnLCalculationService.computePnLForAsset(asset);

        return AssetUpdateResponseDTO.builder()
                .assetId(asset.getId())
                .transactionId(savedTx.getId())
                .action(TransactionAction.BUY)
                .unitsTransacted(unitsToBuy)
                .amountTransacted(totalAmount)
                .remainingUnits(pnl.getUnits())
                .remainingValue(pnl.getCurrentValue())
                .pnl(pnl)
                .build();
    }

    @Transactional
    public AssetUpdateResponseDTO executeSellTransaction(UUID assetId, AssetTransactionDTO dto, User user) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        Product product = productRepository.findById(asset.getProductId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!Boolean.TRUE.equals(product.getVisible())) {
            throw new CoreThrowHandler(ApiError.DELISTED_PRODUCT);
        }

        validateTransactionByAssetType(product, dto);
        BigDecimal currentPrice = product.getCurrentPrice();

        // Calculate already-sold units
        List<TransactionHistory> sellTransactions = transactionHistoryRepository
                .findAllByAssetIdAndActionOrderByTransactionDateAsc(asset.getId(), TransactionAction.SELL);
        BigDecimal totalSoldUnits = sellTransactions.stream()
                .map(TransactionHistory::getUnits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableUnits = Objects.requireNonNullElse(asset.getUnits(), BigDecimal.ZERO).subtract(totalSoldUnits);
        if (availableUnits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST, "No units available to sell");
        }

        // Determine units to sell
        boolean hasUnits = dto.getUnits() != null && dto.getUnits().compareTo(BigDecimal.ZERO) > 0;

        BigDecimal unitsToSell;
        BigDecimal actualAmount;

        if (hasUnits) {
            unitsToSell = dto.getUnits();
            actualAmount = unitsToSell.multiply(currentPrice).setScale(4, RoundingMode.HALF_UP);
        } else {
            actualAmount = dto.getAmount().setScale(4, RoundingMode.HALF_UP);
            unitsToSell = actualAmount.divide(currentPrice, 6, RoundingMode.HALF_UP);
        }

        // Validate sufficient units
        if (unitsToSell.compareTo(availableUnits) > 0) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                    "Insufficient units: available " + availableUnits + ", requested " + unitsToSell);
        }

        BigDecimal pricePerUnit = actualAmount.divide(unitsToSell, 4, RoundingMode.HALF_UP);

        // Create SELL transaction record
        Instant txInstant = (dto.getTransactionDate() != null)
                ? dto.getTransactionDate().atZone(ZoneId.systemDefault()).toInstant()
                : Instant.now();
//
        TransactionHistory tx = TransactionHistory.builder()
                .userId(user.getId())
                .productId(product.getId())
                .assetId(asset.getId())
                .goalId(dto.getGoalId())
                .action(TransactionAction.SELL)
                .pricePerUnit(pricePerUnit)
                .units(unitsToSell)
                .totalAmount(actualAmount)
                .transactionDate(txInstant)
                .notes(dto.getNotes())
                .build();
        TransactionHistory savedTx = transactionHistoryRepository.save(tx);

        // Update asset.currentValue — amount and units stay (total cost basis)
        BigDecimal remainingUnits = availableUnits.subtract(unitsToSell);
        asset.setCurrentValue(remainingUnits.multiply(currentPrice).setScale(4, RoundingMode.HALF_UP));
        assetRepository.save(asset);

        // Recalculate PnL
        AssetsPnLResponseDTO pnl = pnLCalculationService.computePnLForAsset(asset);

        return AssetUpdateResponseDTO.builder()
                .assetId(asset.getId())
                .transactionId(savedTx.getId())
                .action(TransactionAction.SELL)
                .unitsTransacted(unitsToSell)
                .amountTransacted(actualAmount)
                .remainingUnits(pnl.getUnits())
                .remainingValue(pnl.getCurrentValue())
                .pnl(pnl)
                .build();
    }

    @Transactional
    public Asset updateAssetCurrentValue(UUID assetId, BigDecimal newValue, String notes, User user) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!asset.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        asset.setCurrentValue(newValue);
        return assetRepository.save(asset);
    }

    private void validateTransactionByAssetType(Product product, AssetTransactionDTO dto) {
        String type = product.getType();
        boolean hasUnits = dto.getUnits() != null && dto.getUnits().compareTo(BigDecimal.ZERO) > 0;
        boolean hasAmount = dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) > 0;

        // At least one of units or amount required
        if (!hasUnits && !hasAmount) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                    "Either units or amount must be provided");
        }

        // Cannot provide both
        if (hasUnits && hasAmount) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                    "Provide either units or amount, not both");
        }

        // Stock-specific: sell by units only
        if ("Stock".equalsIgnoreCase(type) && dto.getAction() == TransactionAction.SELL && hasAmount) {
                throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                        "Stocks can only be sold by units, not by amount");
            }


      // Fractional units validation
        if (hasUnits && !Boolean.TRUE.equals(product.getIsFractionalAllowed()) && dto.getUnits().stripTrailingZeros().scale() > 0) {
                throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                        "Fractional units not allowed for this product");
            }


      // Lot size validation for stocks
        if ("stock".equals(type) && !Boolean.TRUE.equals(product.getIsFractionalAllowed())
                && hasUnits) {
            int lotSize = product.getLotSize();
            BigDecimal remainder = dto.getUnits().remainder(BigDecimal.valueOf(lotSize));
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                throw new CoreThrowHandler(ApiError.BAD_REQUEST,
                        "Stock must be traded in lot multiples of " + lotSize);
            }
        }
    }
}
