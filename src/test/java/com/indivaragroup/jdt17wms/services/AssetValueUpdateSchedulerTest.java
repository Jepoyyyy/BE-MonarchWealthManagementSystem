package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetValueUpdateSchedulerTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private AssetValueUpdateScheduler scheduler;

    @Test
    void updateAllAssetValues_shouldRecalculateBasedOnCurrentPrice() {
        // Given
        UUID assetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .currentPrice(new BigDecimal("150.00"))
                .build();

        Asset asset = Asset.builder()
                .id(assetId)
                .productId(productId)
                .units(new BigDecimal("10.0"))
                .amount(new BigDecimal("1000.00"))
                .currentValue(new BigDecimal("1200.00")) // Outdated value
                .build();

        when(assetRepository.findAll()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of());

        // When
        scheduler.updateAllAssetValues();

        // Then
        verify(assetRepository).save(argThat(a ->
                a.getCurrentValue().compareTo(new BigDecimal("1500.00")) == 0 // 10 × 150
        ));
    }

    @Test
    void updateAllAssetValues_shouldAccountForSoldUnits() {
        // Given
        UUID assetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .currentPrice(new BigDecimal("100.00"))
                .build();

        Asset asset = Asset.builder()
                .id(assetId)
                .productId(productId)
                .units(new BigDecimal("20.0"))
                .amount(new BigDecimal("1500.00"))
                .currentValue(new BigDecimal("1500.00"))
                .build();

        TransactionHistory sellTx = TransactionHistory.builder()
                .assetId(assetId)
                .action(TransactionAction.SELL)
                .units(new BigDecimal("5.0"))
                .build();

        when(assetRepository.findAll()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of(sellTx));

        // When
        scheduler.updateAllAssetValues();

        // Then: remaining 15 units × 100 = 1500
        verify(assetRepository).save(argThat(a ->
                a.getCurrentValue().compareTo(new BigDecimal("1500.00")) == 0
        ));
    }
}
