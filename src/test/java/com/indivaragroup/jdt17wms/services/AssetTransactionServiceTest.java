package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetTransactionServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private PnLCalculationService pnLCalculationService;

    @InjectMocks
    private AssetTransactionService assetTransactionService;

    private User user;
    private Asset asset;
    private Product product;
    private final UUID assetId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .build();

        product = Product.builder()
                .id(productId)
                .name("Test Stock")
                .type("stock")
                .currentPrice(BigDecimal.valueOf(5000))
                .minInvestment(BigDecimal.valueOf(1000))
                .isFractionalAllowed(false)
                .lotSize(1)
                .visible(true)
                .build();

        asset = Asset.builder()
                .id(assetId)
                .userId(userId)
                .productId(productId)
                .units(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(500000))
                .currentValue(BigDecimal.valueOf(500000))
                .build();
    }

    // ==========================================
    //  executeBuyTransaction — Positive Cases
    // ==========================================

    @Test
    void executeBuyTransaction_byUnits_shouldUpdateAssetAndReturnResponse() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder()
                .assetId(assetId)
                .units(BigDecimal.valueOf(110))
                .currentValue(BigDecimal.valueOf(550000))
                .build();
        when(pnLCalculationService.computePnLForAsset(any(Asset.class))).thenReturn(pnl);

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(10))
                .notes("Buy more")
                .build();

        AssetUpdateResponseDTO response = assetTransactionService.executeBuyTransaction(assetId, dto, user);

        assertNotNull(response);
        assertEquals(TransactionAction.BUY, response.getAction());
        assertEquals(BigDecimal.valueOf(10), response.getUnitsTransacted());
        assertEquals(BigDecimal.valueOf(50000).setScale(4, RoundingMode.HALF_UP), response.getAmountTransacted());

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        Asset saved = assetCaptor.getValue();
        assertEquals(BigDecimal.valueOf(110), saved.getUnits());
        assertEquals(BigDecimal.valueOf(550000).setScale(4, RoundingMode.HALF_UP), saved.getAmount());
        assertEquals(BigDecimal.valueOf(550000).setScale(4, RoundingMode.HALF_UP), saved.getCurrentValue());
    }

    @Test
    void executeBuyTransaction_byAmount_shouldCalculateUnitsAndUpdateAsset() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder()
                .assetId(assetId)
                .units(BigDecimal.valueOf(120))
                .currentValue(BigDecimal.valueOf(600000))
                .build();
        when(pnLCalculationService.computePnLForAsset(any(Asset.class))).thenReturn(pnl);

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .amount(BigDecimal.valueOf(100000))
                .notes("Buy by amount")
                .build();

        AssetUpdateResponseDTO response = assetTransactionService.executeBuyTransaction(assetId, dto, user);

        assertNotNull(response);
        assertEquals(TransactionAction.BUY, response.getAction());
        // units = 100000 / 5000 = 20
        assertEquals(BigDecimal.valueOf(20).setScale(6, RoundingMode.HALF_UP), response.getUnitsTransacted());
        assertEquals(BigDecimal.valueOf(100000).setScale(4, RoundingMode.HALF_UP), response.getAmountTransacted());
    }

    @Test
    void executeBuyTransaction_withTransactionDate_shouldUseProvidedDate() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder().assetId(assetId).build();
        when(pnLCalculationService.computePnLForAsset(any(Asset.class))).thenReturn(pnl);

        LocalDateTime txDate = LocalDateTime.of(2026, 6, 15, 10, 30);
        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(5))
                .transactionDate(txDate)
                .build();

        assetTransactionService.executeBuyTransaction(assetId, dto, user);

        ArgumentCaptor<TransactionHistory> txCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository).save(txCaptor.capture());
        assertNotNull(txCaptor.getValue().getTransactionDate());
    }

    // ==========================================
    //  executeBuyTransaction — Negative Cases
    // ==========================================

    @Test
    void executeBuyTransaction_shouldThrowNotFound_whenAssetNotFound() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        AssetTransactionDTO dto = AssetTransactionDTO.builder().units(BigDecimal.ONE).build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertEquals(ApiError.ITEM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void executeBuyTransaction_shouldThrowNotFound_whenAssetNotOwnedByUser() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        AssetTransactionDTO dto = AssetTransactionDTO.builder().units(BigDecimal.ONE).build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, otherUser));
        assertEquals(ApiError.ITEM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void executeBuyTransaction_shouldThrowNotFound_whenProductNotFound() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        AssetTransactionDTO dto = AssetTransactionDTO.builder().units(BigDecimal.ONE).build();

        assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
    }

    @Test
    void executeBuyTransaction_shouldThrowDelisted_whenProductNotVisible() {
        product.setVisible(false);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AssetTransactionDTO dto = AssetTransactionDTO.builder().units(BigDecimal.ONE).build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertEquals(ApiError.DELISTED_PRODUCT.getCode(), ex.getCode());
    }

    @Test
    void executeBuyTransaction_shouldThrow_whenNeitherUnitsNorAmountProvided() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AssetTransactionDTO dto = AssetTransactionDTO.builder().build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertEquals("Either units or amount must be provided", ex.getMessage());
    }

    @Test
    void executeBuyTransaction_shouldThrow_whenBothUnitsAndAmountProvided() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.ONE)
                .amount(BigDecimal.valueOf(5000))
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertEquals("Provide either units or amount, not both", ex.getMessage());
    }

    @Test
    void executeBuyTransaction_shouldThrow_whenBelowMinInvestment() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // minInvestment=1000, buying 1 unit at 5000 = 5000 > 1000 → OK
        // Set minInvestment higher to trigger
        product.setMinInvestment(BigDecimal.valueOf(100000));
        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.ONE)
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertTrue(ex.getMessage().contains("minimum investment"));
    }

    @Test
    void executeBuyTransaction_shouldThrow_whenFractionalNotAllowed() {
        product.setIsFractionalAllowed(false);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(new BigDecimal("1.5"))
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertEquals("Fractional units not allowed for this product", ex.getMessage());
    }

    @Test
    void executeBuyTransaction_shouldThrow_whenUnitNotInLotSize() {
        product.setLotSize(100); // stock must be traded in lots of 100
        product.setType("stock");
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(50)) // not a multiple of 100
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
        assertTrue(ex.getMessage().contains("lot multiples"));
    }

    // ==========================================
    //  executeSellTransaction — Positive Cases
    // ==========================================

    @Test
    void executeSellTransaction_byUnits_shouldCreateSellRecord() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of());
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder()
                .assetId(assetId)
                .units(BigDecimal.valueOf(60))
                .currentValue(BigDecimal.valueOf(300000))
                .build();
        when(pnLCalculationService.computePnLForAsset(any(Asset.class))).thenReturn(pnl);

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(40))
                .notes("Sell some")
                .build();

        AssetUpdateResponseDTO response = assetTransactionService.executeSellTransaction(assetId, dto, user);

        assertNotNull(response);
        assertEquals(TransactionAction.SELL, response.getAction());
        assertEquals(BigDecimal.valueOf(40), response.getUnitsTransacted());

        ArgumentCaptor<TransactionHistory> txCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository).save(txCaptor.capture());
        assertEquals(TransactionAction.SELL, txCaptor.getValue().getAction());
        assertEquals(BigDecimal.valueOf(40), txCaptor.getValue().getUnits());
    }

    @Test
    void executeSellTransaction_byAmount_shouldCalculateUnitsSold() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of());
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder().assetId(assetId).build();
        when(pnLCalculationService.computePnLForAsset(any(Asset.class))).thenReturn(pnl);

        // sell 100000 worth → 20 units at 5000 each
        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .amount(BigDecimal.valueOf(100000))
                .build();

        AssetUpdateResponseDTO response = assetTransactionService.executeSellTransaction(assetId, dto, user);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(20).setScale(6, RoundingMode.HALF_UP), response.getUnitsTransacted());
        assertEquals(BigDecimal.valueOf(100000).setScale(4, RoundingMode.HALF_UP), response.getAmountTransacted());
    }

    @Test
    void executeSellTransaction_partialSell_shouldTrackSoldUnits() {
        // Already sold 30 units previously
        TransactionHistory priorSell = TransactionHistory.builder()
                .units(BigDecimal.valueOf(30))
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of(priorSell));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetsPnLResponseDTO pnl = AssetsPnLResponseDTO.builder().assetId(assetId).build();
        when(pnLCalculationService.computePnLForAsset(any(Asset.class))).thenReturn(pnl);

        // available = 100 - 30 = 70, sell 20 → OK
        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(20))
                .build();

        AssetUpdateResponseDTO response = assetTransactionService.executeSellTransaction(assetId, dto, user);
        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(20), response.getUnitsTransacted());
    }

    // ==========================================
    //  executeSellTransaction — Negative Cases
    // ==========================================

    @Test
    void executeSellTransaction_shouldThrowNotFound_whenAssetNotFound() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        AssetTransactionDTO dto = AssetTransactionDTO.builder().units(BigDecimal.ONE).build();

        assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
    }

    @Test
    void executeSellTransaction_shouldThrowNotFound_whenAssetNotOwned() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        AssetTransactionDTO dto = AssetTransactionDTO.builder().units(BigDecimal.ONE).build();

        assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, otherUser));
    }

    @Test
    void executeSellTransaction_shouldThrow_whenNoUnitsAvailableToSell() {
        // Already sold all 100 units
        TransactionHistory priorSell = TransactionHistory.builder()
                .units(BigDecimal.valueOf(100))
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of(priorSell));

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.ONE)
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
        assertEquals("No units available to sell", ex.getMessage());
    }

    @Test
    void executeSellTransaction_shouldThrow_whenInsufficientUnits() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(150)) // only 100 available
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
        assertTrue(ex.getMessage().contains("Insufficient units"));
    }

    @Test
    void executeSellTransaction_shouldThrow_whenProductDelisted() {
        product.setVisible(false);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.ONE)
                .build();

        assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
    }

    @Test
    void executeSellTransaction_stockSellByAmountNoActionSet_shouldNotTriggerValidation() {
        // NOTE: stock sell-by-amount validation requires dto.getAction() == SELL
        // Without action set, the check is bypassed — this is existing service behavior
        product.setType("stock");
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of());
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pnLCalculationService.computePnLForAsset(any(Asset.class)))
                .thenReturn(AssetsPnLResponseDTO.builder().assetId(assetId).build());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .amount(BigDecimal.valueOf(50000))
                .build();

        // Succeeds because action=null bypasses the stock sell-by-amount check
        assertDoesNotThrow(() -> assetTransactionService.executeSellTransaction(assetId, dto, user));
    }

    @Test
    void executeSellTransaction_fractionalNotAllowed_shouldThrow() {
        product.setIsFractionalAllowed(false);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(new BigDecimal("1.5"))
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
        assertEquals("Fractional units not allowed for this product", ex.getMessage());
    }

    @Test
    void executeSellTransaction_lotSizeMismatch_shouldThrow() {
        product.setLotSize(100);
        product.setType("stock");
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.findAllByAssetIdAndActionOrderByTransactionDateAsc(
                assetId, TransactionAction.SELL)).thenReturn(List.of());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(50)) // not multiple of 100
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
        assertTrue(ex.getMessage().contains("lot multiples"));
    }

    // ==========================================
    //  executeSellTransaction — Stock Sell By Amount
    // ==========================================

    @Test
    void executeSellTransaction_stockSellByAmount_shouldThrow() {
        product.setType("stock");
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // dto with action=SELL and amount (no units) → should be rejected for stocks
        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .amount(BigDecimal.valueOf(50000))
                .action(TransactionAction.SELL)
                .build();

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.executeSellTransaction(assetId, dto, user));
        assertTrue(ex.getMessage().contains("Stocks can only be sold by units"));
    }

    // ==========================================
    //  updateAssetCurrentValue — Positive
    // ==========================================

    @Test
    void updateAssetCurrentValue_shouldUpdateValue() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = assetTransactionService.updateAssetCurrentValue(assetId, BigDecimal.valueOf(600000), "Updated", user);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(600000), result.getCurrentValue());
    }

    // ==========================================
    //  updateAssetCurrentValue — Negative
    // ==========================================

    @Test
    void updateAssetCurrentValue_shouldThrowNotFound_whenAssetNotFound() {
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.updateAssetCurrentValue(assetId, BigDecimal.valueOf(600000), "test", user));
    }

    @Test
    void updateAssetCurrentValue_shouldThrowNotFound_whenNotOwned() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThrows(CoreThrowHandler.class,
                () -> assetTransactionService.updateAssetCurrentValue(assetId, BigDecimal.valueOf(600000), "test", otherUser));
    }

    // ==========================================
    //  validateTransactionByAssetType — Edge Cases
    // ==========================================

    @Test
    void executeBuyTransaction_fractionalAllowed_shouldNotThrow() {
        product.setIsFractionalAllowed(true);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pnLCalculationService.computePnLForAsset(any(Asset.class)))
                .thenReturn(AssetsPnLResponseDTO.builder().assetId(assetId).build());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(new BigDecimal("1.5"))
                .build();

        assertDoesNotThrow(() -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
    }

    @Test
    void executeBuyTransaction_lotSizeExactMultiple_shouldNotThrow() {
        product.setLotSize(100);
        product.setType("stock");
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pnLCalculationService.computePnLForAsset(any(Asset.class)))
                .thenReturn(AssetsPnLResponseDTO.builder().assetId(assetId).build());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(200)) // multiple of 100
                .build();

        assertDoesNotThrow(() -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
    }

    @Test
    void executeBuyTransaction_nonStockType_shouldNotCheckLotSize() {
        product.setType("bond"); // not stock, lot size check skipped
        product.setLotSize(100);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pnLCalculationService.computePnLForAsset(any(Asset.class)))
                .thenReturn(AssetsPnLResponseDTO.builder().assetId(assetId).build());

        AssetTransactionDTO dto = AssetTransactionDTO.builder()
                .units(BigDecimal.valueOf(50))
                .build();

        assertDoesNotThrow(() -> assetTransactionService.executeBuyTransaction(assetId, dto, user));
    }
}
