package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AdminDashboardDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDashboardDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.ProductPrice;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.ProductPriceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProductPriceRepository productPriceRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        UserDTO userDTO = UserDTO.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .email("test@example.com")
                .isAdmin(false)
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDTO, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void serviceShouldBeInitialized() {
        assertNotNull(dashboardService);
    }

    // --- getUserDashboard Tests ---

    @Test
    void getUserDashboard_shouldReturnDashboardWhenQuestionnaireCompletedIsTrue() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .userId(SecurityUtils.STATIC_USER_ID)
                .productId(productId)
                .units(new BigDecimal("10.00"))
                .amount(new BigDecimal("100.00"))
                .currentValue(new BigDecimal("120.0000"))
                .purchaseDate(Instant.parse("2026-01-10T10:00:00Z"))
                .build();

        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .currentPrice(new BigDecimal("12.00"))
                .build();

        ProductPrice productPrice = ProductPrice.builder()
                .productId(productId)
                .price(new BigDecimal("11.50"))
                .recordedDate(LocalDate.now())
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productPriceRepository.findAllByProductIdInAndRecordedDateLessThanEqual(
                anySet(), any(LocalDate.class)))
                .thenReturn(List.of(productPrice));

        UserDashboardDTO result = dashboardService.getUserDashboard();

        assertNotNull(result);
        assertNotNull(result.getPortofolio());
        assertEquals("120.0000", result.getPortofolio().getValue()); // 10 units * 12.00 price = 120.0000
        assertEquals("100.00", result.getPortofolio().getInvested());
        assertEquals(1, result.getPortofolio().getHoldings());
        assertEquals("Test Product", result.getPortofolio().getItems().getFirst().getName());
        assertNotNull(result.getPerformance());
    }

    @Test
    void getUserDashboard_withFuturePurchaseDate_shouldSkipInPerformanceTrend() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID productId = UUID.randomUUID();
        Asset assetFuture = Asset.builder()
                .userId(SecurityUtils.STATIC_USER_ID)
                .productId(productId)
                .units(new BigDecimal("10.00"))
                .amount(new BigDecimal("100.00"))
                .currentValue(new BigDecimal("120.0000"))
                .purchaseDate(Instant.parse("2026-12-10T10:00:00Z"))
                .build();

        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .currentPrice(new BigDecimal("12.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(assetFuture));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UserDashboardDTO result = dashboardService.getUserDashboard();

        assertNotNull(result);
        assertEquals("120.0000", result.getPortofolio().getValue());
        result.getPerformance().forEach(p -> assertEquals(0, p.getValue().compareTo(BigDecimal.ZERO)));
    }

    @Test
    void getUserDashboard_withPriceMissing_shouldDefaultToZeroValue() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .userId(SecurityUtils.STATIC_USER_ID)
                .productId(productId)
                .units(new BigDecimal("10.00"))
                .amount(new BigDecimal("100.00"))
                .currentValue(BigDecimal.ZERO)
                .purchaseDate(Instant.parse("2026-01-10T10:00:00Z"))
                .build();

        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .currentPrice(new BigDecimal("12.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UserDashboardDTO result = dashboardService.getUserDashboard();

        assertNotNull(result);
        result.getPerformance().forEach(p -> assertEquals(0, p.getValue().compareTo(BigDecimal.ZERO)));
    }



    @Test
    void getUserDashboard_shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> dashboardService.getUserDashboard());
    }

    @Test
    void getUserDashboard_shouldThrowNotFoundExceptionWhenProductNotFound() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .userId(SecurityUtils.STATIC_USER_ID)
                .productId(productId)
                .units(new BigDecimal("10.00"))
                .amount(new BigDecimal("100.00"))
                .purchaseDate(Instant.parse("2026-01-10T10:00:00Z"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(assetRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> dashboardService.getUserDashboard());
    }

    // --- getAdminDashboard Tests ---

    @Test
    void getAdminDashboard_shouldReturnDashboardStats() {
        UserRepository.RiskProfileCount riskAverse = mock(UserRepository.RiskProfileCount.class);
        when(riskAverse.getRiskProfile()).thenReturn("risk_averse");
        when(riskAverse.getCount()).thenReturn(2L);
        UserRepository.RiskProfileCount moderate = mock(UserRepository.RiskProfileCount.class);
        when(moderate.getRiskProfile()).thenReturn("moderate");
        when(moderate.getCount()).thenReturn(1L);
        UserRepository.RiskProfileCount riskTaker = mock(UserRepository.RiskProfileCount.class);
        when(riskTaker.getRiskProfile()).thenReturn("risk_taker");
        when(riskTaker.getCount()).thenReturn(3L);

        when(userRepository.countByRiskProfile()).thenReturn(List.of(riskAverse, moderate, riskTaker));
        when(assetRepository.sumTotalAmount()).thenReturn(new BigDecimal("10000.00"));
        when(userRepository.count()).thenReturn(6L);
        when(productRepository.count()).thenReturn(10L);
        when(auditLogRepository.count()).thenReturn(50L);

        UUID productId = UUID.randomUUID();
        Asset asset1 = Asset.builder()
                .productId(productId)
                .units(new BigDecimal("100.00"))
                .purchaseDate(Instant.parse("2026-01-10T10:00:00Z"))
                .build();
        Asset assetFuture = Asset.builder()
                .productId(productId)
                .units(new BigDecimal("200.00"))
                .purchaseDate(Instant.parse("2026-12-10T10:00:00Z"))
                .build();

        when(assetRepository.findAllByPurchaseDateGreaterThanEqual(any(Instant.class)))
                .thenReturn(List.of(asset1, assetFuture));

        ProductPrice productPrice = ProductPrice.builder()
                .productId(productId)
                .price(new BigDecimal("1.50"))
                .recordedDate(LocalDate.now())
                .build();
        when(productPriceRepository.findAllByProductIdInAndRecordedDateLessThanEqual(
                anySet(), any(LocalDate.class)))
                .thenReturn(List.of(productPrice));

        AdminDashboardDTO result = dashboardService.getAdminDashboard();

        assertNotNull(result);
        assertEquals(new BigDecimal("10000.00"), result.getAum());
        assertEquals(6, result.getUserCount());
        assertEquals(10, result.getProductCount());
        assertEquals(50, result.getTotalAuditEvents());
        assertNotNull(result.getRiskProfiles());
        assertEquals(2, result.getRiskProfiles().getRiskAverse());
        assertEquals(1, result.getRiskProfiles().getModerate());
        assertEquals(3, result.getRiskProfiles().getRiskTaker());
        assertNotNull(result.getAumTrend());
        assertFalse(result.getAumTrend().isEmpty());
    }
}
