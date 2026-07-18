package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
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
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PnLCalculationServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @InjectMocks
    private PnLCalculationService pnLCalculationService;

    private Asset asset;
    private Product product;
    private final UUID assetId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UserDTO userDTO = UserDTO.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDTO, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        product = Product.builder()
                .id(productId)
                .name("Test Stock")
                .type("stock")
                .currentPrice(BigDecimal.valueOf(5500.00))
                .build();

        asset = Asset.builder()
                .id(assetId)
                .productId(productId)
                .units(BigDecimal.valueOf(100.00))
                .amount(BigDecimal.valueOf(500000.00)) // 100 * 5000
                .build();
    }

    // --- computePnLForAllAssets Tests ---

    @Test
    void testComputePnLForAllAssets_success() {
        User user = User.builder().id(TEST_USER_ID).build();
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(user.getId())).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(Collections.emptyList());
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(Collections.emptyList());

        List<AssetsPnLResponseDTO> results = pnLCalculationService.computePnLForAllAssets();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(productId, results.getFirst().getProductId());
    }

    @Test
    void testComputePnLForAllAssets_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> pnLCalculationService.computePnLForAllAssets());
        assertEquals("User Not Found", ex.getMessage());
    }

    // --- computePnLForAsset Tests ---

    @Test
    void testComputePnL_productNotFound_throwsNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> pnLCalculationService.computePnLForAsset(asset));
        assertEquals("Item Not Found", ex.getMessage());
    }



    @Test
    void testComputePnL_SingleBuyOnly_PositivePnL() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        TransactionHistory buy = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.valueOf(100))
                .pricePerUnit(BigDecimal.valueOf(5000))
                .totalAmount(BigDecimal.valueOf(500000))
                .build();

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of(buy));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of());

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(BigDecimal.valueOf(5000).setScale(4, RoundingMode.HALF_UP), result.getAvgPrice());
        assertEquals(BigDecimal.valueOf(100.0), result.getUnits());
        assertEquals(BigDecimal.valueOf(550000).setScale(2, RoundingMode.HALF_UP), result.getCurrentValue());
        assertEquals(BigDecimal.valueOf(50000).setScale(2, RoundingMode.HALF_UP), result.getPotentialPnL());
        assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP), result.getPotentialPnLPercent());
        assertEquals(BigDecimal.ZERO, result.getRealizedPnL());
        assertEquals(BigDecimal.ZERO, result.getRealizedPnLPercent());
    }

    @Test
    void testComputePnL_MultipleBuys_WeightedAverage() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        TransactionHistory buy1 = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.valueOf(100))
                .pricePerUnit(BigDecimal.valueOf(5000))
                .totalAmount(BigDecimal.valueOf(500000))
                .build();

        TransactionHistory buy2 = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.valueOf(50))
                .pricePerUnit(BigDecimal.valueOf(5200))
                .totalAmount(BigDecimal.valueOf(260000))
                .build();

        asset.setUnits(BigDecimal.valueOf(150));

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of(buy1, buy2));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of());

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(BigDecimal.valueOf(5066.6667).setScale(4, RoundingMode.HALF_UP), result.getAvgPrice());
    }

    @Test
    void testComputePnL_WithPartialSell_RealizedAndPotentialPnL() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        TransactionHistory buy = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.valueOf(100))
                .pricePerUnit(BigDecimal.valueOf(5000))
                .totalAmount(BigDecimal.valueOf(500000))
                .build();

        TransactionHistory sell = TransactionHistory.builder()
                .action(TransactionAction.SELL)
                .units(BigDecimal.valueOf(40))
                .pricePerUnit(BigDecimal.valueOf(5300))
                .totalAmount(BigDecimal.valueOf(212000))
                .build();

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of(buy));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of(sell));

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(BigDecimal.valueOf(60.0), result.getUnits());
        assertEquals(BigDecimal.valueOf(30000).setScale(2, RoundingMode.HALF_UP), result.getPotentialPnL());
        assertEquals(BigDecimal.valueOf(12000).setScale(2, RoundingMode.HALF_UP), result.getRealizedPnL());
        assertEquals(BigDecimal.valueOf(6.00).setScale(2, RoundingMode.HALF_UP), result.getRealizedPnLPercent());
    }

    @Test
    void testComputePnL_NoBuyTransactions_FallbackToAssetAmount() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of());
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of());

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(BigDecimal.valueOf(5000).setScale(4, RoundingMode.HALF_UP), result.getAvgPrice());
        assertEquals(BigDecimal.valueOf(50000).setScale(2, RoundingMode.HALF_UP), result.getPotentialPnL());
    }

    @Test
    void testComputePnL_NoBuyTransactions_ZeroUnitsFallback() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        asset.setUnits(BigDecimal.ZERO);

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of());
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of());

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(0, result.getAvgPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testComputePnL_BuyTransactionsWithZeroTotalUnits() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        TransactionHistory buyZero = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.ZERO)
                .pricePerUnit(BigDecimal.valueOf(5000))
                .build();

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of(buyZero));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of());

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(0, result.getAvgPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testComputePnL_FullySoldAsset_ZeroRemainingUnits() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        TransactionHistory buy = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.valueOf(100))
                .pricePerUnit(BigDecimal.valueOf(5000))
                .build();
        TransactionHistory sellAll = TransactionHistory.builder()
                .action(TransactionAction.SELL)
                .units(BigDecimal.valueOf(100))
                .totalAmount(BigDecimal.valueOf(510000))
                .build();

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of(buy));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of(sellAll));

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(0, result.getUnits().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getPotentialPnL().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getPotentialPnLPercent().compareTo(BigDecimal.ZERO));
        assertEquals(BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP), result.getRealizedPnL());
    }

    @Test
    void testComputePnL_ZeroAveragePrice_BypassesDivisions() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        TransactionHistory buyFree = TransactionHistory.builder()
                .action(TransactionAction.BUY)
                .units(BigDecimal.valueOf(100))
                .pricePerUnit(BigDecimal.ZERO)
                .build();
        TransactionHistory sell = TransactionHistory.builder()
                .action(TransactionAction.SELL)
                .units(BigDecimal.valueOf(40))
                .totalAmount(BigDecimal.valueOf(10000))
                .build();

        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.BUY))
                .thenReturn(List.of(buyFree));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(assetId, TransactionAction.SELL))
                .thenReturn(List.of(sell));

        AssetsPnLResponseDTO result = pnLCalculationService.computePnLForAsset(asset);

        assertEquals(0, result.getAvgPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getPotentialPnL().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getPotentialPnLPercent().compareTo(BigDecimal.ZERO));
        assertEquals(BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP), result.getRealizedPnL());
        assertEquals(0, result.getRealizedPnLPercent().compareTo(BigDecimal.ZERO));
    }
}
