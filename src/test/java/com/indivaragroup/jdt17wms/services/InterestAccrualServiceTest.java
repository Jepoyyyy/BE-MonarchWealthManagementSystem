package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InterestAccrualService interestAccrualService;

    // ==========================================
    //  accrueMonthlyInterest — Positive Cases
    // ==========================================

    @Test
    void accrueMonthlyInterest_forBondAsset_shouldAccrueInterest() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .amount(BigDecimal.valueOf(1000000))
                .units(BigDecimal.valueOf(1010000))
                .currentValue(BigDecimal.valueOf(1000000))
                .build();

        Product product = Product.builder()
                .id(productId)
                .type("Bond")
                .annualReturn(BigDecimal.valueOf(12)) // 12% annual
                .currentPrice(BigDecimal.ONE)
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        interestAccrualService.accrueMonthlyInterest();

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        Asset saved = captor.getValue();

        // monthlyRate = 12 / 12 / 100 = 0.01
        // monthlyReturn = 1000000 * 0.01 = 10000
        // newAmount = 1000000 + 10000 = 1010000
        // currentValue = 1010000 units * 1 = 1010000
        assertEquals(BigDecimal.valueOf(1010000).setScale(4, RoundingMode.HALF_UP), saved.getCurrentValue());
    }

    @Test
    void accrueMonthlyInterest_forSukukAsset_shouldAccrueInterest() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .amount(BigDecimal.valueOf(500000))
                .units(BigDecimal.valueOf(502500))
                .currentValue(BigDecimal.valueOf(500000))
                .build();

        Product product = Product.builder()
                .id(productId)
                .type("Sukuk")
                .annualReturn(BigDecimal.valueOf(6)) // 6% annual
                .currentPrice(BigDecimal.ONE)
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        interestAccrualService.accrueMonthlyInterest();

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        Asset saved = captor.getValue();

        // monthlyRate = 6 / 12 / 100 = 0.005
        // monthlyReturn = 500000 * 0.005 = 2500
        // newAmount = 500000 + 2500 = 502500
        // currentValue = 502500 units * 1 = 502500
        assertEquals(BigDecimal.valueOf(502500).setScale(4, RoundingMode.HALF_UP), saved.getCurrentValue());
    }

    @Test
    void accrueMonthlyInterest_forDepositAsset_shouldAccrueInterest() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .amount(BigDecimal.valueOf(2000000))
                .units(BigDecimal.valueOf(3000000))
                .currentValue(BigDecimal.valueOf(2000000))
                .build();

        Product product = Product.builder()
                .id(productId)
                .type("Deposit")
                .annualReturn(BigDecimal.valueOf(3.5)) // 3.5% annual
                .currentPrice(BigDecimal.ONE)
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        interestAccrualService.accrueMonthlyInterest();

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        Asset saved = captor.getValue();

        // monthlyRate = 3.5 / 12 / 100 = 0.002916... (6 decimal scale)
        // monthlyReturn = 2000000 * 0.002916... = 5833.3333...
        // Expect currentValue > 2000000
        assertTrue(saved.getCurrentValue().compareTo(BigDecimal.valueOf(2000000)) > 0);
    }

    @Test
    void accrueMonthlyInterest_multipleAssets_shouldAccrueAll() {
        UUID bondProductId = UUID.randomUUID();
        UUID depositProductId = UUID.randomUUID();

        Asset bondAsset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(bondProductId)
                .amount(BigDecimal.valueOf(1000000))
                .units(BigDecimal.valueOf(1000000))
                .currentValue(BigDecimal.valueOf(1000000))
                .build();

        Asset depositAsset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(depositProductId)
                .amount(BigDecimal.valueOf(2000000))
                .units(BigDecimal.valueOf(2000000))
                .currentValue(BigDecimal.valueOf(2000000))
                .build();

        Product bondProduct = Product.builder()
                .id(bondProductId)
                .type("Bond")
                .annualReturn(BigDecimal.valueOf(12))
                .currentPrice(BigDecimal.ONE)
                .visible(true)
                .build();

        Product depositProduct = Product.builder()
                .id(depositProductId)
                .type("Deposit")
                .annualReturn(BigDecimal.valueOf(6))
                .currentPrice(BigDecimal.ONE)
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(bondAsset, depositAsset));
        when(productRepository.findById(bondProductId)).thenReturn(Optional.of(bondProduct));
        when(productRepository.findById(depositProductId)).thenReturn(Optional.of(depositProduct));

        interestAccrualService.accrueMonthlyInterest();

        verify(assetRepository, times(2)).save(any(Asset.class));
    }

    // ==========================================
    //  accrueMonthlyInterest — Edge / Negative Cases
    // ==========================================

    @Test
    void accrueMonthlyInterest_whenNoInterestBearingAssets_shouldDoNothing() {
        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of());

        interestAccrualService.accrueMonthlyInterest();

        verify(productRepository, never()).findById(any());
        verify(assetRepository, never()).save(any());
    }

    @Test
    void accrueMonthlyInterest_whenProductNotFound_shouldSkip() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .currentValue(BigDecimal.valueOf(1000000))
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        interestAccrualService.accrueMonthlyInterest();

        verify(assetRepository, never()).save(any());
    }

    @Test
    void accrueMonthlyInterest_whenProductTypeNotInterestBearing_shouldSkip() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .currentValue(BigDecimal.valueOf(1000000))
                .build();

        Product product = Product.builder()
                .id(productId)
                .type("stock") // not interest-bearing
                .annualReturn(BigDecimal.valueOf(12))
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        interestAccrualService.accrueMonthlyInterest();

        verify(assetRepository, never()).save(any());
    }

    @Test
    void accrueMonthlyInterest_whenCurrentValueIsZero_shouldAccrueZero() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .amount(BigDecimal.ZERO)
                .units(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();

        Product product = Product.builder()
                .id(productId)
                .type("Bond")
                .annualReturn(BigDecimal.valueOf(12))
                .currentPrice(BigDecimal.ZERO)
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        interestAccrualService.accrueMonthlyInterest();

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertEquals(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), captor.getValue().getCurrentValue());
    }

    @Test
    void accrueMonthlyInterest_whenAnnualReturnIsZero_shouldAccrueZero() {
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .amount(BigDecimal.valueOf(1000000))
                .units(BigDecimal.valueOf(1000000))
                .currentValue(BigDecimal.valueOf(1000000))
                .build();

        Product product = Product.builder()
                .id(productId)
                .type("Bond")
                .annualReturn(BigDecimal.ZERO)
                .currentPrice(BigDecimal.ONE)
                .visible(true)
                .build();

        when(assetRepository.findAllInterestBearingAssets()).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        interestAccrualService.accrueMonthlyInterest();

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertEquals(BigDecimal.valueOf(1000000).setScale(4, RoundingMode.HALF_UP), captor.getValue().getCurrentValue());
    }
}
