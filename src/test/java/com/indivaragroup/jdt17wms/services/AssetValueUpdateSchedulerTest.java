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

    @Test
    void updateAllAssetValues_shouldSkipAssetWhenProductNotFound() {
        UUID assetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).productId(productId).units(new BigDecimal("10.0")).build();

        when(assetRepository.findAll()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        scheduler.updateAllAssetValues();

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void updateAllAssetValues_shouldSkipAssetWhenCurrentPriceIsNull() {
        UUID assetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).currentPrice(null).build();
        Asset asset = Asset.builder().id(assetId).productId(productId).units(new BigDecimal("10.0")).build();

        when(assetRepository.findAll()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        scheduler.updateAllAssetValues();

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void updateAllAssetValues_shouldSkipAssetWhenCurrentPriceIsZeroOrNegative() {
        UUID assetId1 = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        Product zeroPriceProduct = Product.builder().id(productId1).currentPrice(BigDecimal.ZERO).build();
        Asset asset1 = Asset.builder().id(assetId1).productId(productId1).units(new BigDecimal("10.0")).build();

        UUID assetId2 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        Product negativePriceProduct = Product.builder().id(productId2).currentPrice(new BigDecimal("-10.00")).build();
        Asset asset2 = Asset.builder().id(assetId2).productId(productId2).units(new BigDecimal("10.0")).build();

        when(assetRepository.findAll()).thenReturn(List.of(asset1, asset2));
        when(productRepository.findById(productId1)).thenReturn(Optional.of(zeroPriceProduct));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(negativePriceProduct));

        scheduler.updateAllAssetValues();

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void updateAllAssetValues_shouldHandleExceptionDuringAssetProcessingAndContinue() {
        UUID assetId1 = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        Asset asset1 = Asset.builder().id(assetId1).productId(productId1).units(new BigDecimal("10.0")).build();

        UUID assetId2 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        Product product2 = Product.builder().id(productId2).currentPrice(new BigDecimal("100.00")).build();
        Asset asset2 = Asset.builder().id(assetId2).productId(productId2).units(new BigDecimal("5.0")).build();

        when(assetRepository.findAll()).thenReturn(List.of(asset1, asset2));
        when(productRepository.findById(productId1)).thenThrow(new RuntimeException("Database connection error"));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId2, TransactionAction.SELL))
                .thenReturn(List.of());

        scheduler.updateAllAssetValues();

        verify(assetRepository, times(1)).save(asset2);
    }
}

