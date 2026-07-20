package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.GoalProjectionDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalsProjectionServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinancialProfileRepository financialProfileRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ProductRepository productRepository;

    private GoalsProjectionService goalsProjectionService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        goalsProjectionService = new GoalsProjectionService(goalRepository, userRepository, financialProfileRepository, assetRepository, productRepository, clock);
    }

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
        assertNotNull(goalsProjectionService);
    }

    @Test
    void getProjectionsForUser_shouldReturnProjectionsWhenNoAssets() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Retirement Fund")
                .type("property")
                .targetAmount(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2036, Month.JULY, 13))
                .currentAmount(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));
        when(assetRepository.findAllByGoalId(any(UUID.class))).thenReturn(List.of());

        List<GoalProjectionDTO> results = goalsProjectionService.getProjectionsForUser();

        assertNotNull(results);
        assertEquals(1, results.size());
        GoalProjectionDTO res = results.getFirst();
        assertEquals("Retirement Fund", res.getName());

        // Under 0% savings growth, projectedDate should be plus 500 months (500000 target / 1000 contribution)
        LocalDate expectedProjectedDate = LocalDate.of(2068, Month.MARCH, 13);
        assertEquals(expectedProjectedDate, res.getProjectedDate());

        // Under 0% savings growth, recommendedContribution should be target / months = 500000 / 120 = 4166.67
        assertEquals(new BigDecimal("4166.67"), res.getRecommendedContribution());

        assertNotNull(res.getTimeSeries());
        assertEquals(60, res.getTimeSeries().size());
        assertEquals(new BigDecimal("1000.00"), res.getTimeSeries().getFirst().getValue());
        assertEquals(new BigDecimal("60000.00"), res.getTimeSeries().get(59).getValue());
    }

    @Test
    void getProjectionsForUser_shouldReturnProjectionsWhenAssetsTied() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        UUID goalId = UUID.randomUUID();
        Goal goal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Retirement Fund")
                .type("property")
                .targetAmount(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2036, Month.JULY, 13))
                .currentAmount(BigDecimal.ZERO)
                .build();
        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .goalId(goalId)
                .currentValue(new BigDecimal("10000.00"))
                .build();
        Product product = Product.builder()
                .id(productId)
                .annualReturn(new BigDecimal("12.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        List<GoalProjectionDTO> results = goalsProjectionService.getProjectionsForUser();

        assertNotNull(results);
        assertEquals(1, results.size());
        GoalProjectionDTO res = results.getFirst();
        assertEquals("Retirement Fund", res.getName());
        assertNotNull(res.getProjectedDate());
        assertNotNull(res.getRecommendedContribution());
        assertNotNull(res.getTimeSeries());
        assertEquals(60, res.getTimeSeries().size());
    }



    @Test
    void getProjectionsForUser_shouldThrowNotFoundExceptionWhenUserNotFound() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> goalsProjectionService.getProjectionsForUser());
    }

    @Test
    void getProjectionsForUser_shouldUseTargetDateWhenTimelineIsEarlierThanMaxMonths() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Retirement Fund")
                .type("property") // Max limit is 120 months
                .targetAmount(new BigDecimal("300000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .targetDate(LocalDate.of(2031, Month.JULY, 13)) // 60 months < 120 max months
                .currentAmount(BigDecimal.ZERO)
                .build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));
        when(assetRepository.findAllByGoalId(any(UUID.class))).thenReturn(List.of());

        List<GoalProjectionDTO> results = goalsProjectionService.getProjectionsForUser();

        assertNotNull(results);
        assertEquals(1, results.size());
        GoalProjectionDTO res = results.getFirst();
        // Recommended contribution should be 300000 / 60 = 5000.00 instead of 300000 / 120 = 2500.00
        assertEquals(new BigDecimal("5000.00"), res.getRecommendedContribution());
    }
    @Test
    void getProjectionsForUser_whenCannotGrow_returnsMaxSimulationMonths() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Stagnant Goal")
                .type("property")
                .targetAmount(new BigDecimal("10000.00"))
                .monthlyContribution(BigDecimal.ZERO) // no contribution
                .targetDate(LocalDate.of(2030, Month.JANUARY, 1))
                .currentAmount(BigDecimal.ZERO)
                .build();


        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));
        // No assets tied to the goal
        when(assetRepository.findAllByGoalId(any(UUID.class))).thenReturn(List.of());

        List<GoalProjectionDTO> results = goalsProjectionService.getProjectionsForUser();
        assertNotNull(results);
        assertEquals(1, results.size());
        GoalProjectionDTO res = results.getFirst();

        // Expected projected date is now plus MAX_SIMULATION_MONTHS (1200) months because growth is impossible
        LocalDate expectedDate = LocalDate.now(clock).plusMonths(1200);
        assertEquals(expectedDate, res.getProjectedDate());
    }

    @Test
    void getProjectionsForUser_whenGrowthTooSmall_returnsMaxSimulationMonths() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        UUID goalId = UUID.randomUUID();
        Goal goal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Slow Growth Goal")
                .type("property")
                .targetAmount(new BigDecimal("100000.00")) // target 100k
                .monthlyContribution(BigDecimal.ZERO) // no monthly contribution
                .targetDate(LocalDate.of(2030, Month.JANUARY, 1))
                .currentAmount(BigDecimal.ZERO)
                .build();

        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .goalId(goalId)
                .currentValue(new BigDecimal("1.00")) // starts with 1
                .build();

        // 0.00001% annual return -> extremely slow growth
        Product product = Product.builder()
                .id(productId)
                .annualReturn(new BigDecimal("0.00001"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        List<GoalProjectionDTO> results = goalsProjectionService.getProjectionsForUser();
        assertNotNull(results);
        assertEquals(1, results.size());
        GoalProjectionDTO res = results.getFirst();

        // Expected projected date is now plus MAX_SIMULATION_MONTHS (1200) months because growth exceeds the cap
        LocalDate expectedDate = LocalDate.now(clock).plusMonths(1200);
        assertEquals(expectedDate, res.getProjectedDate());
    }

    @Test
    void getProjectionsForUser_whenBalanceAlreadyExceedsTarget_returnsZeroMonths() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        UUID goalId = UUID.randomUUID();
        Goal goal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Achieved Goal")
                .type("property")
                .targetAmount(new BigDecimal("10000.00")) // target 10k
                .monthlyContribution(new BigDecimal("100.00"))
                .targetDate(LocalDate.of(2030, Month.JANUARY, 1))
                .currentAmount(BigDecimal.ZERO)
                .build();

        UUID productId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .goalId(goalId)
                .currentValue(new BigDecimal("15000.00")) // starts with 15k (already exceeds 10k target)
                .build();

        Product product = Product.builder()
                .id(productId)
                .annualReturn(new BigDecimal("5.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        List<GoalProjectionDTO> results = goalsProjectionService.getProjectionsForUser();
        assertNotNull(results);
        assertEquals(1, results.size());
        GoalProjectionDTO res = results.getFirst();

        // Expected projected date is now (plus 0 months) because target is already met
        LocalDate expectedDate = LocalDate.now(clock);
        assertEquals(expectedDate, res.getProjectedDate());
    }

    @Test
    void testCalculateMonthsToUse_variousTimelines() throws Exception {
        var method = GoalsProjectionService.class.getDeclaredMethod("calculateMonthsToUse", String.class, LocalDate.class);
        method.setAccessible(true);

        // case 1: actualMonths <= 0 (target date is in the past) -> should return maxMonths
        // Type "property" maxMonths is 120.
        LocalDate pastDate = LocalDate.now(clock).minusMonths(5);
        double resultPast = (double) method.invoke(goalsProjectionService, "property", pastDate);
        assertEquals(120.0, resultPast);

        // case 2: actualMonths >= maxMonths (target date is far in the future) -> should return maxMonths
        LocalDate futureDateFar = LocalDate.now(clock).plusMonths(200);
        double resultFar = (double) method.invoke(goalsProjectionService, "property", futureDateFar);
        assertEquals(120.0, resultFar);

        // case 3: 0 < actualMonths < maxMonths -> should return actualMonths
        LocalDate futureDateNear = LocalDate.now(clock).plusMonths(30);
        double resultNear = (double) method.invoke(goalsProjectionService, "property", futureDateNear);
        assertEquals(30.0, resultNear);
    }

    @Test
    void testCalculateRecommendedContribution_divisionByZeroGuards() throws Exception {
        var method = GoalsProjectionService.class.getDeclaredMethod("calculateRecommendedContribution", double[].class, double[].class, double.class, double.class);
        method.setAccessible(true);

        // case 1: rates[j] <= 0 (division by zero rate guard)
        // target = 10000, monthsToUse = 10. rates = {0.0}. balances = {1000.0}
        // futureValueFactor = (1+0)^10 = 1
        // num = 10000 - 1000*1 = 9000
        // rate is 0 -> sumS += monthsToUse (10.0) -> denom = 10.0
        // recContribution = 9000 / 10.0 = 900.0
        BigDecimal resZeroRate = (BigDecimal) method.invoke(goalsProjectionService, new double[]{1000.0}, new double[]{0.0}, 10000.0, 10.0);
        assertEquals(new BigDecimal("900.00"), resZeroRate);

        // case 2: denom <= 0 (division by zero denom guard)
        // We can force denom to 0 by passing monthsToUse = 0 and rates = {0.0}
        // target = 10000, monthsToUse = 0, rates = {0.0}, balances = {1000.0}
        // num = 10000 - 1000*1 = 9000
        // sumS = 0.0 -> denom = 0.0 -> recContribution should return 0.0
        BigDecimal resZeroDenom = (BigDecimal) method.invoke(goalsProjectionService, new double[]{1000.0}, new double[]{0.0}, 10000.0, 0.0);
        assertEquals(new BigDecimal("0.00"), resZeroDenom);
    }

    @Test
    void testHasGrowthPotential_edgeCases() throws Exception {
        var method = GoalsProjectionService.class.getDeclaredMethod("hasGrowthPotential", double[].class, double[].class, double.class);
        method.setAccessible(true);

        // case 1: contributionPerBucket > 0 -> should return true immediately
        assertTrue((boolean) method.invoke(goalsProjectionService, new double[]{0.0}, new double[]{0.0}, 1.0));

        // case 2: contributionPerBucket <= 0 and loop matches:
        // rate > 0 and balance > 0 -> should return true
        assertTrue((boolean) method.invoke(goalsProjectionService, new double[]{100.0}, new double[]{0.05}, 0.0));

        // rate <= 0 and balance > 0 -> should return false
        assertFalse((boolean) method.invoke(goalsProjectionService, new double[]{100.0}, new double[]{0.0}, 0.0));
        assertFalse((boolean) method.invoke(goalsProjectionService, new double[]{100.0}, new double[]{-0.02}, 0.0));

        // rate > 0 and balance <= 0 -> should return false
        assertFalse((boolean) method.invoke(goalsProjectionService, new double[]{0.0}, new double[]{0.05}, 0.0));
        assertFalse((boolean) method.invoke(goalsProjectionService, new double[]{-10.0}, new double[]{0.05}, 0.0));

        // multiple buckets with one having potential -> should return true
        assertTrue((boolean) method.invoke(goalsProjectionService, new double[]{0.0, 100.0}, new double[]{0.05, 0.05}, 0.0));
    }
}
