package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AssetRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetTransactionDTO;
import com.indivaragroup.jdt17wms.dto.request.AssetValueUpdateDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalSettingDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetDTO;
import com.indivaragroup.jdt17wms.dto.response.AssetUpdateResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.TransactionHistoryDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.Recommendation;
import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.RecommendationRepository;
import com.indivaragroup.jdt17wms.repositories.TransactionHistoryRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetsManagementServiceTest {

    @Mock
    private AssetRepository assetRepository;

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

    @Mock
    private AssetTransactionService assetTransactionService;

    @InjectMocks
    private AssetsManagementService assetsManagementService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-21T10:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    assetsManagementService = new AssetsManagementService(assetRepository,
      userRepository, transactionHistoryRepository, productRepository, goalRepository, recommendationRepository, assetTransactionService, clock);
  }

  private void mockAuthenticatedUser() {
        UserDTO principal = UserDTO.builder().id(SecurityUtils.STATIC_USER_ID).build();
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
    @DisplayName("userRepository - should return injected UserRepository instance")
    void userRepository_shouldReturnInjectedUserRepository() {
        assertEquals(userRepository, assetsManagementService.userRepository());
    }

    @Test
    @DisplayName("getVerifiedUser - should return authenticated User instance when questionnaire completed")
    void getVerifiedUser_shouldReturnAuthenticatedUser() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        User result = assetsManagementService.getVerifiedUser();
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void getAssetsForUser_shouldReturnAssetsWhenQuestionnaireCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        Asset asset = Asset.builder().id(UUID.randomUUID()).userId(SecurityUtils.STATIC_USER_ID).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(asset));

        List<AssetDTO> result = assetsManagementService.getAssetsForUser();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(asset.getId(), result.getFirst().getId());
    }

    @Test
    void getAssetsForUser_shouldThrowNotFoundExceptionWhenUserNotFound() {
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.getAssetsForUser());
    }

    @Test
    void getTransactionLogsForUser_shouldReturnLogsWhenQuestionnaireCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        TransactionHistory log = TransactionHistory.builder().id(UUID.randomUUID()).userId(SecurityUtils.STATIC_USER_ID).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(transactionHistoryRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(log));

        List<TransactionHistoryDTO> result = assetsManagementService.getTransactionLogsForUser();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(log.getId(), result.getFirst().getId());
    }

    @Test
    void getTransactionLogsForUser_shouldThrowNotFoundExceptionWhenUserNotFound() {
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.getTransactionLogsForUser());
    }

    @Test
    @DisplayName("getTransactionHistoryForAsset - when valid asset and user, return transaction history")
    void getTransactionHistoryForAsset_whenValid_shouldReturnHistoryList() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(user.getId()).build();
        TransactionHistory log = TransactionHistory.builder().id(UUID.randomUUID()).assetId(assetId).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(transactionHistoryRepository.findAllByAssetIdOrderByTransactionDateDesc(assetId))
                .thenReturn(List.of(log));

        List<TransactionHistoryDTO> result = assetsManagementService.getTransactionHistoryForAsset(assetId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(log.getId(), result.getFirst().getId());
    }

    @Test
    @DisplayName("getTransactionHistoryForAsset - when asset not found, throw ITEM_NOT_FOUND exception")
    void getTransactionHistoryForAsset_whenAssetNotFound_shouldThrowNotFoundException() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.getTransactionHistoryForAsset(assetId));
    }

    @Test
    @DisplayName("getTransactionHistoryForAsset - when asset belongs to another user, throw ITEM_NOT_FOUND exception")
    void getTransactionHistoryForAsset_whenAssetDoesNotBelongToUser_shouldThrowNotFoundException() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(UUID.randomUUID()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.getTransactionHistoryForAsset(assetId));
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
                .currentPrice(new BigDecimal("11.0"))
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

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);

        AssetDTO result = assetsManagementService.createAssetForUser(dto);

        assertNotNull(result);
        assertEquals(savedAsset.getId(), result.getId());
        verify(assetRepository).save(any(Asset.class));
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
    }

    @Test
    @DisplayName("createAssetForUser - when units is null, throw BAD_REQUEST exception")
    void createAssetForUser_shouldThrowBadRequestException_whenUnitsIsNull() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        Product product = Product.builder().id(UUID.randomUUID()).visible(true).build();
        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(product.getId())
                .units(null)
                .build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> assetsManagementService.createAssetForUser(dto));
        assertEquals(ApiError.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("Units must be greater than zero", ex.getMessage());
    }

    @Test
    @DisplayName("createAssetForUser - when units is zero or negative, throw BAD_REQUEST exception")
    void createAssetForUser_shouldThrowBadRequestException_whenUnitsIsZeroOrNegative() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        Product product = Product.builder().id(UUID.randomUUID()).visible(true).build();
        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(product.getId())
                .units(BigDecimal.ZERO)
                .build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> assetsManagementService.createAssetForUser(dto));
        assertEquals(ApiError.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("Units must be greater than zero", ex.getMessage());
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
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.createAssetForUser(dto));
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
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.createAssetForUser(dto));
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

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.createAssetForUser(dto));
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

        Goal goal = Goal.builder().id(goalId).userId(user.getId()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetDTO result = assetsManagementService.updateAssetForUser(assetId, dto);

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

        GoalSettingDTO dto = GoalSettingDTO.builder().build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetDTO result = assetsManagementService.updateAssetForUser(assetId, dto);

        assertNotNull(result);
        assertNull(result.getGoalId());
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void updateAssetForUser_shouldThrowForbiddenException_whenGoalBelongsToDifferentUser() {
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

        Goal goal = Goal.builder()
                .id(goalId)
                .userId(UUID.randomUUID())
                .build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetForUser(assetId, dto));
        assertEquals(ApiError.GOAL_BELONGS_TO_DIFFERENT_USER.getCode(), ex.getCode());
        assertEquals("Access denied. Goal belongs to different user", ex.getMessage());
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
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetForUser(assetId, dto));
    }

    @Test
    void updateAssetForUser_shouldThrowNotFoundException_whenAssetNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        GoalSettingDTO dto = GoalSettingDTO.builder().build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetForUser(assetId, dto));
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
                .userId(UUID.randomUUID())
                .build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetForUser(assetId, dto));
    }

    @Test
    void updateAssetForUser_shouldThrowMissingRiskProfileException_whenQuestionnaireNotCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(false)
                .build();

        UUID assetId = UUID.randomUUID();
        GoalSettingDTO dto = GoalSettingDTO.builder().build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetForUser(assetId, dto));
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
        mockAuthenticatedUser();
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
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.deleteAssetForUser(assetId));
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

        mockAuthenticatedUser();
        assetsManagementService.deleteAssetForUser(assetId);

        ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository).save(captor.capture());
        TransactionHistory saved = captor.getValue();
        assertEquals(new BigDecimal("50.0000"), saved.getPricePerUnit());
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

        mockAuthenticatedUser();
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
                .userId(UUID.randomUUID())
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        mockAuthenticatedUser();
        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.deleteAssetForUser(assetId));
    }

    @Test
    void deleteAssetForUser_shouldThrowMissingRiskProfileException_whenQuestionnaireNotCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(false)
                .build();

        UUID assetId = UUID.randomUUID();
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.deleteAssetForUser(assetId));
    }

    @Test
    @DisplayName("executeTransaction - when action is BUY, call assetTransactionService.executeBuyTransaction")
    void executeTransaction_shouldCallExecuteBuyTransaction_whenActionIsBuy() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        AssetTransactionDTO dto = AssetTransactionDTO.builder().action(TransactionAction.BUY).build();
        AssetUpdateResponseDTO expectedResponse = AssetUpdateResponseDTO.builder().build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetTransactionService.executeBuyTransaction(assetId, dto, user)).thenReturn(expectedResponse);

        AssetUpdateResponseDTO result = assetsManagementService.executeTransaction(assetId, dto);
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(assetTransactionService).executeBuyTransaction(assetId, dto, user);
    }

    @Test
    @DisplayName("executeTransaction - when action is SELL, call assetTransactionService.executeSellTransaction")
    void executeTransaction_shouldCallExecuteSellTransaction_whenActionIsSell() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        AssetTransactionDTO dto = AssetTransactionDTO.builder().action(TransactionAction.SELL).build();
        AssetUpdateResponseDTO expectedResponse = AssetUpdateResponseDTO.builder().build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetTransactionService.executeSellTransaction(assetId, dto, user)).thenReturn(expectedResponse);

        AssetUpdateResponseDTO result = assetsManagementService.executeTransaction(assetId, dto);
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(assetTransactionService).executeSellTransaction(assetId, dto, user);
    }

    @Test
    @DisplayName("executeTransaction - when action is null or invalid, throw BAD_REQUEST exception")
    void executeTransaction_shouldThrowBadRequest_whenActionIsInvalid() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        AssetTransactionDTO dto = AssetTransactionDTO.builder().action(null).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> assetsManagementService.executeTransaction(assetId, dto));
        assertEquals(ApiError.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("Invalid transaction action", ex.getMessage());
    }

    @Test
    @DisplayName("updateAssetGoal - when goalId provided and valid, update asset goal and save")
    void updateAssetGoal_whenGoalIdProvidedAndValid_shouldUpdateAndSave() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(user.getId()).build();
        Goal goal = Goal.builder().id(goalId).userId(user.getId()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetDTO result = assetsManagementService.updateAssetGoal(assetId, goalId);

        assertNotNull(result);
        assertEquals(goalId, result.getGoalId());
        verify(assetRepository).save(asset);
    }

    @Test
    @DisplayName("updateAssetGoal - when goalId is null, clear asset goal and save")
    void updateAssetGoal_whenGoalIdIsNull_shouldClearGoalAndSave() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(user.getId()).goalId(UUID.randomUUID()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetDTO result = assetsManagementService.updateAssetGoal(assetId, null);

        assertNotNull(result);
        assertNull(result.getGoalId());
        verify(assetRepository).save(asset);
    }

    @Test
    @DisplayName("updateAssetGoal - when asset not found, throw ITEM_NOT_FOUND exception")
    void updateAssetGoal_whenAssetNotFound_shouldThrowNotFoundException() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        UUID testUUID = UUID.randomUUID();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetGoal(assetId, testUUID));
    }

    @Test
    @DisplayName("updateAssetGoal - when asset belongs to another user, throw ITEM_NOT_FOUND exception")
    void updateAssetGoal_whenAssetDoesNotBelongToUser_shouldThrowNotFoundException() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        UUID testUUID = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(UUID.randomUUID()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetGoal(assetId, testUUID));
    }

    @Test
    @DisplayName("updateAssetGoal - when goal belongs to another user, throw FORBIDDEN exception")
    void updateAssetGoal_whenGoalBelongsToDifferentUser_shouldThrowForbiddenException() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(user.getId()).build();
        Goal goal = Goal.builder().id(goalId).userId(UUID.randomUUID()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetGoal(assetId, goalId));
        assertEquals(ApiError.GOAL_BELONGS_TO_DIFFERENT_USER.getCode(), ex.getCode());
        assertEquals("Access denied. Goal belongs to different user", ex.getMessage());
    }

    @Test
    @DisplayName("updateAssetGoal - when goalId provided but goal not found, throw ITEM_NOT_FOUND exception")
    void updateAssetGoal_whenGoalNotFound_shouldThrowNotFoundException() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(user.getId()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetGoal(assetId, goalId));
    }

    @Test
    void updateAssetValue_shouldReturnAssetDTOWithEnrichedProductFields() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        BigDecimal newValue = BigDecimal.valueOf(600000);

        Asset savedAsset = Asset.builder()
                .id(assetId)
                .userId(user.getId())
                .productId(UUID.randomUUID())
                .currentValue(newValue)
                .build();

        Product product = Product.builder()
                .id(savedAsset.getProductId())
                .name("Test Stock")
                .issuer("Test Corp")
                .type("stock")
                .build();

        when(assetTransactionService.updateAssetCurrentValue(any(UUID.class), any(BigDecimal.class), nullable(String.class), any(User.class)))
                .thenReturn(savedAsset);
        when(productRepository.findById(savedAsset.getProductId())).thenReturn(Optional.of(product));
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        AssetValueUpdateDTO dto = new AssetValueUpdateDTO();
        dto.setCurrentValue(newValue);

        AssetDTO result = assetsManagementService.updateAssetValue(assetId, dto);

        assertNotNull(result);
        assertEquals(assetId, result.getId());
        assertEquals(newValue, result.getCurrentValue());
        assertEquals("Test Stock", result.getAssetsName());
        assertEquals("Test Corp", result.getAssetsIssuer());
        assertEquals("stock", result.getAssetsType());
    }

    @Test
    void updateAssetValue_shouldThrowNotFound_whenAssetNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID assetId = UUID.randomUUID();
        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetTransactionService.updateAssetCurrentValue(any(UUID.class), any(BigDecimal.class), nullable(String.class), any(User.class)))
                .thenThrow(new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        AssetValueUpdateDTO dto = new AssetValueUpdateDTO();
        dto.setCurrentValue(BigDecimal.valueOf(600000));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.updateAssetValue(assetId, dto));
    }

    @Test
    @DisplayName("createAssetForUser - when tenor is provided, calculate maturity date")
    void createAssetForUser_whenTenorIsProvided_shouldCalculateMaturityDate() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        Product product = Product.builder().id(UUID.randomUUID()).visible(true).currentPrice(new BigDecimal("100.00")).build();

        AssetRegistrationDTO dto = AssetRegistrationDTO.builder()
                .productId(product.getId())
                .units(new BigDecimal("10.00"))
                .amount(new BigDecimal("1000.00"))
                .purchaseDate(LocalDateTime.now(clock))
                .platform("Bank A")
                .tenor(12)
                .build();

        Asset savedAsset = Asset.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .productId(product.getId())
                .units(dto.getUnits())
                .amount(dto.getAmount())
                .currentValue(new BigDecimal("1000.0000"))
                .purchaseDate(Instant.now(clock))
                .tenor(LocalDate.now(clock).plusMonths(12))
                .platform("Bank A")
                .build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);

        AssetDTO result = assetsManagementService.createAssetForUser(dto);

        assertNotNull(result);
        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        assertNotNull(assetCaptor.getValue().getTenor());
        assertEquals(LocalDate.now(clock).plusMonths(12), assetCaptor.getValue().getTenor());
    }

    @Test
    void findAssetByIdAndUser_shouldReturnAsset_whenAssetBelongsToUser() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder().id(assetId).userId(user.getId()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        Asset result = assetsManagementService.findAssetByIdAndUser(assetId);

        assertNotNull(result);
        assertEquals(assetId, result.getId());
        assertEquals(user.getId(), result.getUserId());
    }

    @Test
    void findAssetByIdAndUser_shouldThrowNotFoundException_whenAssetNotFound() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.findAssetByIdAndUser(assetId));
    }

    @Test
    void findAssetByIdAndUser_shouldThrowNotFoundException_whenAssetBelongsToOtherUser() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        UUID assetId = UUID.randomUUID();
        Asset assetOfOtherUser = Asset.builder().id(assetId).userId(UUID.randomUUID()).build();

        mockAuthenticatedUser();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(assetOfOtherUser));

        assertThrows(CoreThrowHandler.class, () -> assetsManagementService.findAssetByIdAndUser(assetId));
    }
}
