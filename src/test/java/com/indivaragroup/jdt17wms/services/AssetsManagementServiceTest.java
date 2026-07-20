package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.Recommendation;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.RecommendationRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetsManagementServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private AssetsManagementService assetsManagementService;

    private void mockAuthenticatedUser(UUID userId) {
        UserDTO principal = UserDTO.builder().id(userId).build();
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void serviceShouldBeInitialized() {
        assertNotNull(assetsManagementService);
    }

    @Test
    void getAssetsForUser_shouldReturnAssetsWhenQuestionnaireCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        Asset asset = Asset.builder().id(UUID.randomUUID()).userId(SecurityUtils.STATIC_USER_ID).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(asset));

        List<Asset> result = assetsManagementService.getAssetsForUser();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(asset, result.getFirst());
    }



    @Test
    void getAssetsForUser_shouldThrowNotFoundExceptionWhenUserNotFound() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.getAssetsForUser();
        });
    }

    @Test
    void getTransactionLogsForUser_shouldReturnLogsWhenQuestionnaireCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        TransactionHistory log = TransactionHistory.builder().id(UUID.randomUUID()).userId(SecurityUtils.STATIC_USER_ID).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(transactionHistoryRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(log));

        List<TransactionHistory> result = assetsManagementService.getTransactionLogsForUser();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(log, result.getFirst());
    }



    @Test
    void getTransactionLogsForUser_shouldThrowNotFoundExceptionWhenUserNotFound() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.getTransactionLogsForUser();
        });
    }

    @Test
    void createAssetForUser_shouldCreateAssetAndRecordTransactionHistory_whenValid() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .visible(true)
                .currentPrice(new BigDecimal("11.0")) // 10 units × 11 = 110
                .build();

        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(product.getId())
                .units(new BigDecimal("10.0"))
                .amount(new BigDecimal("100.0"))
                .purchaseDate(LocalDateTime.of(2024, Month.MAY, 20, 10, 0, 0))
                .platform("Bibit")
                .notes("New investment")
                .build();

        Asset savedAsset = Asset.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .productId(product.getId())
                .build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);

        Asset result = assetsManagementService.createAssetForUser(dto);

        assertNotNull(result);
        assertEquals(savedAsset, result);
        verify(assetRepository).save(any(Asset.class));
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
    }

    @Test
    void createAssetForUser_shouldThrowDelistedProductException_whenProductIsHidden() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .visible(false)
                .build();

        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(product.getId())
                .build();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.createAssetForUser(dto);
        });
    }

    @Test
    void createAssetForUser_shouldThrowMissingRiskProfileException_whenQuestionnaireNotCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(false)
                .build();

        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(UUID.randomUUID())
                .build();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.createAssetForUser(dto);
        });
    }

    @Test
    void createAssetForUser_shouldThrowNotFoundException_whenProductNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID productId = UUID.randomUUID();
        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(productId)
                .build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.createAssetForUser(dto);
        });
    }

    @Test
    void updateAssetForUser_shouldUpdateAssetGoal_whenValid() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        Asset asset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .build();

        GoalSettingDTO dto = GoalSettingDTO.builder()
                .goalId(goalId)
                .build();

        Goal goal = Goal.builder().id(goalId).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = assetsManagementService.updateAssetForUser(assetId, dto);

        assertNotNull(result);
        assertEquals(goalId, result.getGoalId());
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void updateAssetForUser_shouldClearGoalId_whenGoalIdIsNull() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        UUID existingGoalId = UUID.randomUUID();

        Asset asset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .goalId(existingGoalId)
                .build();

        GoalSettingDTO dto = GoalSettingDTO.builder().build(); // goalId null

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = assetsManagementService.updateAssetForUser(assetId, dto);

        assertNotNull(result);
        assertNull(result.getGoalId());
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void updateAssetForUser_shouldThrowNotFoundException_whenGoalNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        Asset asset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .build();

        GoalSettingDTO dto = GoalSettingDTO.builder()
                .goalId(goalId)
                .build();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.updateAssetForUser(assetId, dto);
        });
    }

    @Test
    void updateAssetForUser_shouldThrowNotFoundException_whenAssetNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        GoalSettingDTO dto = GoalSettingDTO.builder().build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.updateAssetForUser(assetId, dto);
        });
    }

    @Test
    void updateAssetForUser_shouldThrowNotFoundException_whenAssetDoesNotBelongToUser() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        GoalSettingDTO dto = GoalSettingDTO.builder().build();

        Asset asset = Asset.builder()
                .id(assetId)
                .userId(UUID.randomUUID()) // different user
                .build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.updateAssetForUser(assetId, dto);
        });
    }

    @Test
    void updateAssetForUser_shouldThrowMissingRiskProfileException_whenQuestionnaireNotCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(false)
                .build();

        UUID assetId = UUID.randomUUID();
        GoalSettingDTO dto = GoalSettingDTO.builder().build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.updateAssetForUser(assetId, dto);
        });
    }

    @Test
    void deleteAssetForUser_shouldDeleteAssetAndDeallocateReferences_whenValid() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .units(new BigDecimal("10.0"))
                .amount(new BigDecimal("100.0"))
                .productId(UUID.randomUUID())
                .build();

        Recommendation recommendation = Recommendation.builder()
                .id(UUID.randomUUID())
                .resolvedByAssetId(assetId)
                .build();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(recommendationRepository.findAllByResolvedByAssetId(assetId)).thenReturn(List.of(recommendation));

        assetsManagementService.deleteAssetForUser(assetId);

        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        verify(recommendationRepository).save(recommendation);
        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAssetForUser_shouldThrowNotFoundException_whenAssetNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.deleteAssetForUser(assetId);
        });
    }

    @Test
    void deleteAssetForUser_shouldDeleteAssetAndCalculatePricePerUnit_whenUnitsPositive() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        BigDecimal units = new BigDecimal("5");
        BigDecimal amount = new BigDecimal("250");
        Asset asset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .units(units)
                .amount(amount)
                .productId(UUID.randomUUID())
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(recommendationRepository.findAllByResolvedByAssetId(assetId)).thenReturn(List.of());

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        assetsManagementService.deleteAssetForUser(assetId);

        // Capture the TransactionHistory saved and verify pricePerUnit = amount / units
        ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository).save(captor.capture());
        TransactionHistory saved = captor.getValue();
        assertEquals(new BigDecimal("50.0000"), saved.getPricePerUnit()); // 250 / 5 = 50 with scale 4
    }

    @Test
    void deleteAssetForUser_shouldDeleteAssetAndSetZeroPricePerUnit_whenUnitsZero() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .units(BigDecimal.ZERO)
                .amount(new BigDecimal("100"))
                .productId(UUID.randomUUID())
                .build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(recommendationRepository.findAllByResolvedByAssetId(assetId)).thenReturn(List.of());

        assetsManagementService.deleteAssetForUser(assetId);

        ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository).save(captor.capture());
        TransactionHistory saved = captor.getValue();
        assertEquals(BigDecimal.ZERO, saved.getPricePerUnit());
    }

    @Test
    void deleteAssetForUser_shouldThrowNotFoundException_whenAssetDoesNotBelongToUser() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(assetId)
                .userId(UUID.randomUUID()) // different user
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.deleteAssetForUser(assetId);
        });
    }

    @Test
    void deleteAssetForUser_shouldThrowMissingRiskProfileException_whenQuestionnaireNotCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(false)
                .build();

        UUID assetId = UUID.randomUUID();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(CoreThrowHandler.class, () -> {
            assetsManagementService.deleteAssetForUser(assetId);
        });
    }
}
