package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.ComponentDTO;
import com.indivaragroup.jdt17wms.dto.response.HealthDTO;
import com.indivaragroup.jdt17wms.dto.response.RecommendationDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.*;
import com.indivaragroup.jdt17wms.models.enums.RecommendationStatus;
import com.indivaragroup.jdt17wms.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionRecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private FinancialProfileRepository financialProfileRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC);

    private ActionRecommendationService actionRecommendationService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        actionRecommendationService = new ActionRecommendationService(
                recommendationRepository,
                userRepository,
                assetRepository,
                productRepository,
                goalRepository,
                financialProfileRepository,
                expenseRepository,
                clock
        );
        UserDTO userDTO = UserDTO.builder()
                .id(userId)
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

    // ==========================================
    //  getHealthScore Tests
    // ==========================================

    @Test
    void getHealthScore_UserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> actionRecommendationService.getHealthScore());
    }



    @Test
    void getHealthScore_HappyPath_ExcellentScore() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Create products
        Product pDeposit = Product.builder().id(UUID.randomUUID()).type("deposit").riskLevel(1).visible(true).build();
        Product pStock = Product.builder().id(UUID.randomUUID()).type("stock").riskLevel(4).visible(true).build();
        Product pBond = Product.builder().id(UUID.randomUUID()).type("bond").riskLevel(2).visible(true).build();
        when(productRepository.findAll()).thenReturn(List.of(pDeposit, pStock, pBond));

        // Create assets
        Asset a1 = Asset.builder().productId(pDeposit.getId()).currentValue(BigDecimal.valueOf(500000)).build();
        Asset a2 = Asset.builder().productId(pStock.getId()).currentValue(BigDecimal.valueOf(500000)).build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(a1, a2));

        // Create goals
        Goal g1 = Goal.builder().type("retirement").build(); // retirement matches stock, bond, deposit
        Goal g2 = Goal.builder().type("vacation").build();   // vacation matches deposit
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(g1, g2));

        // Create financial profile
        FinancialProfile fp = FinancialProfile.builder().id(UUID.randomUUID()).monthlyIncome(BigDecimal.valueOf(20000)).build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));

        // Create expense
        Expense exp = Expense.builder().totalExpenses(BigDecimal.valueOf(10000)).build();
        when(expenseRepository.findByFinancialProfileId(fp.getId())).thenReturn(Optional.of(exp));

        HealthDTO health = actionRecommendationService.getHealthScore();

        assertNotNull(health);
        assertEquals("Excellent", health.getStatus());
        assertTrue(health.getTotalScore() >= 80);
        assertEquals(BigDecimal.valueOf(1000000), health.getPortfolioValue());
        assertEquals(BigDecimal.valueOf(10000), health.getAvailableSurplus());

        // Check components are calculated
        assertNotNull(health.getComponents());
        assertEquals(4, health.getComponents().size());
    }

    @Test
    void getHealthScore_NoExpenses_ReturnsMidpointEmergencyScore() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        // Monthly income is set, expenses returns 0 (missing expense)
        FinancialProfile fp = FinancialProfile.builder().id(UUID.randomUUID()).monthlyIncome(BigDecimal.valueOf(20000)).build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(fp.getId())).thenReturn(Optional.empty());

        HealthDTO health = actionRecommendationService.getHealthScore();

        assertNotNull(health);
        // Find emergency fund component
        ComponentDTO emergencyComp = health.getComponents().stream()
                .filter(c -> "emergency".equals(c.getComponentName()))
                .findFirst().orElseThrow();
        assertEquals(12, emergencyComp.getScore());
    }

    @Test
    void getHealthScore_NoEligibleProducts_ReturnsZeroDiversification() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Products are either invisible or risk level is too high for "moderate" (max risk 4)
        Product pInvisible = Product.builder().id(UUID.randomUUID()).type("deposit").riskLevel(1).visible(false).build();
        Product pHighRisk = Product.builder().id(UUID.randomUUID()).type("stock").riskLevel(5).visible(true).build();
        when(productRepository.findAll()).thenReturn(List.of(pInvisible, pHighRisk));

        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        HealthDTO health = actionRecommendationService.getHealthScore();

        assertNotNull(health);
        ComponentDTO divComp = health.getComponents().stream()
                .filter(c -> "diversification".equals(c.getComponentName()))
                .findFirst().orElseThrow();
        assertEquals(0, divComp.getScore());
    }

    @Test
    void getHealthScore_NoGoals_ReturnsMidpointGoalCoverage() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList()); // empty goals
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        HealthDTO health = actionRecommendationService.getHealthScore();

        assertNotNull(health);
        ComponentDTO goalComp = health.getComponents().stream()
                .filter(c -> "goalCoverage".equals(c.getComponentName()))
                .findFirst().orElseThrow();
        assertEquals(12, goalComp.getScore());
    }

    @Test
    void getHealthScore_ZeroPortfolioValue_ReturnsMidpointRiskAlignment() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList()); // zero assets
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        HealthDTO health = actionRecommendationService.getHealthScore();

        assertNotNull(health);
        ComponentDTO riskComp = health.getComponents().stream()
                .filter(c -> "riskAlignment".equals(c.getComponentName()))
                .findFirst().orElseThrow();
        assertEquals(12, riskComp.getScore());
    }

    @ParameterizedTest
    @CsvSource({
        "moderate, 1, 4, 25",
        "moderate, 2, 5, 20",
        "moderate, 3, 5, 14",
        "moderate, 4, 5, 8",
        "moderate, 5, 5, 4",
        "risk_averse, 1, 2, 25",
        "risk_taker, 1, 5, 20"
    })
    void getHealthScore_RiskAlignment(String riskProfile, int riskLevel1, int riskLevel2, int expectedRiskAlignmentScore) {
        String profileToUse = "null".equals(riskProfile) ? null : riskProfile;
        User user = User.builder().id(userId).riskProfile(profileToUse).questionnaireCompleted(true).riskProfile(riskProfile).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Product pDeposit = Product.builder().id(UUID.randomUUID()).type("deposit").riskLevel(riskLevel1).visible(true).build();
        Product pStock = Product.builder().id(UUID.randomUUID()).type("stock").riskLevel(riskLevel2).visible(true).build();
        when(productRepository.findAll()).thenReturn(List.of(pDeposit, pStock));
        Asset a1 = Asset.builder().productId(pDeposit.getId()).currentValue(BigDecimal.valueOf(100000)).build();
        Asset a2 = Asset.builder().productId(pStock.getId()).currentValue(BigDecimal.valueOf(100000)).build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(a1, a2));
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        HealthDTO health = actionRecommendationService.getHealthScore();
        assertNotNull(health);

        ComponentDTO riskComp = health.getComponents().stream()
                .filter(c -> "riskAlignment".equals(c.getComponentName()))
                .findFirst().orElseThrow();
        assertEquals(expectedRiskAlignmentScore, riskComp.getScore());
    }

    // ==========================================
    //  generateRecommendations Tests
    // ==========================================

    @Test
    void generateRecommendations_UserNotFound_ThrowsNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> actionRecommendationService.generateRecommendations());
    }



    @Test
    void generateRecommendations_AllRulesTriggered() {
        User user = User.builder().id(userId).riskProfile("risk_taker").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Create products
        Product pDeposit = Product.builder()
                .id(UUID.randomUUID())
                .name("Liquid Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(4.5))
                .minInvestment(BigDecimal.valueOf(1000))
                .build();

        Product pStock = Product.builder()
                .id(UUID.randomUUID())
                .name("Growth Stock")
                .type("stock")
                .riskLevel(4)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(12.0))
                .minInvestment(BigDecimal.valueOf(5000))
                .build();

        Product pBond = Product.builder()
                .id(UUID.randomUUID())
                .name("Government Bond")
                .type("bond")
                .riskLevel(2)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(6.0))
                .minInvestment(BigDecimal.valueOf(2000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pDeposit, pStock, pBond));

        // Create assets to trigger Rule 2 (Concentration Risk: >65% in one product)
        Asset aDeposit = Asset.builder()
                .productId(pDeposit.getId())
                .currentValue(BigDecimal.valueOf(35000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aDeposit));

        // Create goals to trigger Rule 3 (Priority goal alignment) and Rule 4 (Other goal alignment)
        Goal priorityGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Property Goal")
                .type("property") // Property needs balanced_fund, bond, sukuk, stock (owned "deposit" doesn't match)
                .targetAmount(BigDecimal.valueOf(500000))
                .monthlyContribution(BigDecimal.valueOf(5000))
                .targetDate(LocalDate.now(clock).plusYears(10))
                .isPriority(true)
                .build();

        Goal otherGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Property Goal 2")
                .type("property")
                .targetAmount(BigDecimal.valueOf(10000))
                .monthlyContribution(BigDecimal.valueOf(1000))
                .isPriority(false)
                .build();

        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(priorityGoal, otherGoal));

        FinancialProfile fp = FinancialProfile.builder()
                .id(UUID.randomUUID())
                .monthlyIncome(BigDecimal.valueOf(250000)) // High income to trigger Rule 7 (Idle Surplus)
                .build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));

        Expense exp = Expense.builder()
                .totalExpenses(BigDecimal.valueOf(10000)) // emergency target = 60,000 > 35,000 -> triggers Rule 1
                .build();
        when(expenseRepository.findByFinancialProfileId(fp.getId())).thenReturn(Optional.of(exp));

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        assertNotNull(recs);
        assertFalse(recs.isEmpty());

        boolean hasEmergency = recs.stream().anyMatch(r -> "emergency".equals(r.getCategory()));
        boolean hasConcentration = recs.stream().anyMatch(r -> "rebalance".equals(r.getCategory()));
        boolean hasGoal = recs.stream().anyMatch(r -> "goal".equals(r.getCategory()));
        boolean hasDiversification = recs.stream().anyMatch(r -> "diversification".equals(r.getCategory()));
        boolean hasSurplus = recs.stream().anyMatch(r -> "surplus".equals(r.getCategory()));

        assertTrue(hasEmergency, "Rule 1: Emergency Shortfall should be triggered");
        assertTrue(hasConcentration, "Rule 2: Concentration Risk should be triggered");
        assertTrue(hasGoal, "Rule 3/4: Goal alignment should be triggered");
        assertTrue(hasDiversification, "Rule 5: Diversification Gaps should be triggered");
        assertTrue(hasSurplus, "Rule 7: Idle surplus should be triggered");
    }

    @Test
    void generateRecommendations_Reconciliation_ActiveAndInactivePending() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UUID activeRecProductId = UUID.randomUUID();
        UUID inactiveRecProductId = UUID.randomUUID();

        Product pGrowth = Product.builder()
                .id(activeRecProductId)
                .name("Growth Product")
                .type("stock")
                .riskLevel(3)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(10.0))
                .minInvestment(BigDecimal.valueOf(1000))
                .build();
        when(productRepository.findAll()).thenReturn(List.of(pGrowth));

        // Use category "diversification" since it is generated by Rule 5 (missing stock exposure)
        Recommendation pendingActive = Recommendation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .category("diversification")
                .productId(activeRecProductId)
                .status(RecommendationStatus.PENDING)
                .title("Old Title")
                .reason("Old Reason")
                .build();

        Recommendation pendingInactive = Recommendation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .category("diversification")
                .productId(inactiveRecProductId)
                .status(RecommendationStatus.PENDING)
                .title("Old Diversification Title")
                .build();

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(new ArrayList<>(List.of(pendingActive, pendingInactive)));
        when(recommendationRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        assertNotNull(recs);
        assertEquals(1, recs.size());
        RecommendationDTO updated = recs.getFirst();
        assertEquals(pendingActive.getId(), updated.getId());
        assertNotEquals("Old Title", updated.getTitle());

        assertEquals(RecommendationStatus.APPLIED, pendingInactive.getStatus());
        assertNotNull(pendingInactive.getResolvedAt());
    }

    @Test
    void generateRecommendations_Reconciliation_InactivePendingWithMatchingAsset_SetsResolvedByAssetId() {
        // Arrange: an existing PENDING rec references a product that the user NOW owns.
        // When the rule is no longer active (product now owned → condition met), the reconciler should
        // mark it APPLIED and populate resolvedByAssetId with the asset that resolved it.
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID depositProductId = UUID.randomUUID();
        UUID resolverAssetId  = UUID.randomUUID();

        // The product exists and is owned by the user (deposit type, so Rule 5 won't gap-fill it)
        Product pDeposit = Product.builder()
                .id(depositProductId)
                .name("Safe Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(3.5))
                .minInvestment(BigDecimal.valueOf(500))
                .build();
        when(productRepository.findAll()).thenReturn(List.of(pDeposit));

        // User NOW owns the product — this is what makes the old rec's rule no longer active
        Asset ownedAsset = Asset.builder()
                .id(resolverAssetId)
                .productId(depositProductId)
                .currentValue(BigDecimal.valueOf(100000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(ownedAsset));
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        // Existing PENDING recommendation for the deposit product (e.g. was a diversification gap before)
        Recommendation pendingInactive = Recommendation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .category("diversification")
                .productId(depositProductId) // same product the user now owns
                .status(RecommendationStatus.PENDING)
                .title("Add Deposit exposure")
                .build();

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(new ArrayList<>(List.of(pendingInactive)));
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        actionRecommendationService.generateRecommendations();

        // Assert: the previously PENDING rec is now APPLIED and resolvedByAssetId points to the owning asset
        assertEquals(RecommendationStatus.APPLIED, pendingInactive.getStatus(),
                "Inactive rec must be marked APPLIED");
        assertNotNull(pendingInactive.getResolvedAt(),
                "ResolvedAt must be set");
        assertEquals(resolverAssetId, pendingInactive.getResolvedByAssetId(),
                "resolvedByAssetId must be set to the asset that now holds the recommended product");
    }

    // ==========================================
    //  Rule 4: Other Goal Alignment Tests
    // ==========================================

    @Test
    void generateRecommendations_Rule4_NonPriorityGoalWithoutMatchingProduct_TriggersGoalRecommendation() {
        // Arrange: user owns deposit and stock assets so deposit concentration is 50% (< 65% → Rule 2
        // does NOT fire). The non-priority "property" goal needs balanced_fund/bond/sukuk/stock.
        // User owns stock, so stock type IS covered — but bond is unowned, and bond is the best-return
        // product matching "property" types. Wait: stock IS in property types, so this goal IS covered!
        // Instead, use a "vacation" goal (needs money_market/deposit/balanced_fund). User owns deposit →
        // vacation IS covered. Use "property" goal with types [balanced_fund, bond, sukuk, stock] and
        // have user own ONLY deposit and money_market → no owned type in property types → Rule 4 fires.
        //
        // Strategy:
        //   Products available: deposit (owned), money_market (owned), bond (unowned, best return for property)
        //   Assets: deposit 50k + money_market 50k → each 50%, concentration < 65% → Rule 2 suppressed
        //   Goals: one non-priority "property" goal (bond/sukuk/balanced_fund/stock needed, none owned)
        //   Rule 1: no expenses → no trigger
        //   Rule 3: no priority goal
        //   Rule 5: bond type unowned → bestOf picks bond → bond in usedProductIds after Rule 5!
        //
        // To prevent Rule 5 consuming bond before Rule 4: bond must not be picked by Rule 5.
        // Rule 4 runs BEFORE Rule 5 in the service. So bond is available for Rule 4 first.
        // After Rule 4 adds bond to usedProductIds, Rule 5 skips bond.
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID depositProductId = UUID.randomUUID();
        UUID moneyMarketProductId = UUID.randomUUID();
        UUID bondProductId = UUID.randomUUID();

        Product pDeposit = Product.builder()
                .id(depositProductId)
                .name("Safe Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(4.0))
                .minInvestment(BigDecimal.valueOf(1000))
                .build();

        Product pMoneyMarket = Product.builder()
                .id(moneyMarketProductId)
                .name("Money Market Fund")
                .type("money_market")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(3.5))
                .minInvestment(BigDecimal.valueOf(500))
                .build();

        // Bond: unowned, best-return product matching "property" goal types
        Product pBond = Product.builder()
                .id(bondProductId)
                .name("Government Bond")
                .type("bond")
                .riskLevel(2)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(6.0))
                .minInvestment(BigDecimal.valueOf(2000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pDeposit, pMoneyMarket, pBond));

        // User owns both deposit and money_market → 50/50 split → concentration 50% < 65% → Rule 2 suppressed
        Asset aDeposit = Asset.builder()
                .id(UUID.randomUUID())
                .productId(depositProductId)
                .currentValue(BigDecimal.valueOf(50000))
                .build();
        Asset aMoneyMarket = Asset.builder()
                .id(UUID.randomUUID())
                .productId(moneyMarketProductId)
                .currentValue(BigDecimal.valueOf(50000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aDeposit, aMoneyMarket));

        // No financial profile / expenses → Rule 1 midpoint (no expenses = target 0 → skipped), Rule 7 suppressed
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Non-priority "property" goal — needs balanced_fund, bond, sukuk, stock
        // User owns deposit and money_market → neither matches property types → Rule 4 fires
        Goal propertyGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Dream House")
                .type("property")
                .targetAmount(BigDecimal.valueOf(300000))
                .monthlyContribution(BigDecimal.valueOf(3000))
                .isPriority(false)
                .build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(propertyGoal));

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: at least one "goal" category recommendation for the non-priority property goal
        assertNotNull(recs);
        assertFalse(recs.isEmpty(), "Recommendations should not be empty");

        List<RecommendationDTO> goalRecs = recs.stream()
                .filter(r -> "goal".equals(r.getCategory()))
                .toList();
        assertFalse(goalRecs.isEmpty(), "Rule 4: Expected at least one 'goal' recommendation for non-priority goal");

        RecommendationDTO rule4Rec = goalRecs.getFirst();
        assertEquals("medium", rule4Rec.getPriority(), "Rule 4 recommendations must have MEDIUM priority");
        assertEquals(propertyGoal.getId(), rule4Rec.getGoalId(), "GoalId should reference the non-priority goal");
        assertEquals(bondProductId, rule4Rec.getProductId(),
                "Recommended product should be the best bond product (highest return matching property goal types)");

        // Suggested amount = max(monthlyContribution, minInvestment) = max(3000, 2000) = 3000
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(rule4Rec.getSuggestedAmount()),
                "Suggested amount should be max(monthlyContribution, minInvestment)");
    }

    @Test
    void generateRecommendations_Rule4_NonPriorityGoalAlreadyCoveredByOwnedType_DoesNotTrigger() {
        // Arrange: user already owns "stock" which covers a "retirement" non-priority goal.
        // Rule 4 should NOT generate a recommendation for this goal.
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID stockProductId = UUID.randomUUID();
        Product pStock = Product.builder()
                .id(stockProductId)
                .name("Blue Chip Stock")
                .type("stock")
                .riskLevel(3)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(10.0))
                .minInvestment(BigDecimal.valueOf(5000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pStock));

        Asset aStock = Asset.builder()
                .id(UUID.randomUUID())
                .productId(stockProductId)
                .currentValue(BigDecimal.valueOf(100000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aStock));
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // "retirement" goal needs stock/bond/sukuk/balanced_fund/money_market/deposit — user owns stock → covered
        Goal retirementGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Retire Early")
                .type("retirement")
                .targetAmount(BigDecimal.valueOf(1000000))
                .monthlyContribution(BigDecimal.valueOf(5000))
                .isPriority(false)
                .build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(retirementGoal));

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: no "goal" recommendation because the retirement goal is already covered
        boolean hasGoalRec = recs.stream().anyMatch(r -> "goal".equals(r.getCategory()));
        assertFalse(hasGoalRec, "Rule 4: Should NOT recommend for a goal whose type is already covered by owned assets");
    }

    // ==========================================
    //  Rule 6: Highest-Return Opportunity Tests
    // ==========================================

    @Test
    void generateRecommendations_Rule6_BestUnownedProductWithinRisk_TriggersGrowthRecommendation() {
        // Arrange: user owns stock-A. There is a higher-return stock-B of the same type that is unowned.
        // Because both products are the same type ("stock") and user already owns stock-A:
        //   Rule 5 does NOT fire for "stock" type (ownedTypes contains "stock")
        //   Rule 5 finds no products for other types (no money_market/deposit/bond/etc products exist)
        //   Rule 2: stock-A is 100% concentration, fires and picks the best complement product of a
        //           different type — but no other-type products exist → complement is null → Rule 2 skips
        //   Rule 1: no expenses → target 0 → skipped
        //   Rule 3/4: no goals
        //   Rule 6: stock-B is unowned, within risk, highest return → added as "growth" recommendation
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build(); // maxRisk = 4
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID stockAId = UUID.randomUUID();
        UUID stockBId = UUID.randomUUID();

        // stock-A: owned, lower return
        Product pStockA = Product.builder()
                .id(stockAId)
                .name("Equity Fund A")
                .type("stock")
                .riskLevel(3)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(8.0))
                .minInvestment(BigDecimal.valueOf(1000))
                .build();

        // stock-B: unowned, highest return — should be picked by Rule 6
        Product pStockB = Product.builder()
                .id(stockBId)
                .name("Growth Stock B")
                .type("stock")
                .riskLevel(4)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(15.0))
                .minInvestment(BigDecimal.valueOf(2000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pStockA, pStockB));

        // User owns only stock-A
        Asset aStockA = Asset.builder()
                .id(UUID.randomUUID())
                .productId(stockAId)
                .currentValue(BigDecimal.valueOf(100000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aStockA));

        // No expenses, no financial profile → Rule 1 skipped, Rule 7 suppressed
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        // No goals → Rule 3/4 suppressed
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: a "growth" recommendation exists pointing to stock-B (highest-return unowned product)
        assertNotNull(recs);
        assertFalse(recs.isEmpty(), "Recommendations should not be empty");

        List<RecommendationDTO> growthRecs = recs.stream()
                .filter(r -> "growth".equals(r.getCategory()))
                .toList();
        assertFalse(growthRecs.isEmpty(), "Rule 6: Expected a 'growth' recommendation for the best unowned product");

        RecommendationDTO rule6Rec = growthRecs.getFirst();
        assertEquals("low", rule6Rec.getPriority(), "Rule 6 recommendations must have LOW priority");
        assertEquals(stockBId, rule6Rec.getProductId(),
                "Rule 6 should recommend stock-B — the highest-return unowned product at 15% p.a.");
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(rule6Rec.getSuggestedAmount()),
                "Suggested amount should equal the product's minimum investment");
        assertNull(rule6Rec.getGoalId(), "Rule 6 recommendation should have no goalId");
    }

    @Test
    void generateRecommendations_Rule6_AllEligibleProductsAlreadyOwned_DoesNotTrigger() {
        // Arrange: user owns all visible products within their risk profile → Rule 6 cannot find an unowned product.
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build(); // maxRisk = 4
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID depositId = UUID.randomUUID();

        Product pDeposit = Product.builder()
                .id(depositId)
                .name("Money Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(4.0))
                .minInvestment(BigDecimal.valueOf(1000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pDeposit));

        // User owns the only available product
        Asset aDeposit = Asset.builder()
                .id(UUID.randomUUID())
                .productId(depositId)
                .currentValue(BigDecimal.valueOf(100000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aDeposit));
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: no "growth" recommendation since there's no unowned eligible product
        boolean hasGrowthRec = recs.stream().anyMatch(r -> "growth".equals(r.getCategory()));
        assertFalse(hasGrowthRec, "Rule 6: Should NOT generate a growth recommendation when all eligible products are already owned");
    }

    @Test
    void generateRecommendations_Rule6_ProductExceedsUserRiskProfile_DoesNotTrigger() {
        // Arrange: only unowned product exceeds user's max risk level → Rule 6 should not recommend it.
        User user = User.builder().id(userId).riskProfile("risk_averse").questionnaireCompleted(true).build(); // maxRisk = 2
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID depositId = UUID.randomUUID();
        UUID highRiskId = UUID.randomUUID();

        Product pDeposit = Product.builder()
                .id(depositId)
                .name("Safe Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(3.0))
                .minInvestment(BigDecimal.valueOf(500))
                .build();

        // High-risk stock — outside risk_averse max (2)
        Product pStock = Product.builder()
                .id(highRiskId)
                .name("High Yield Stock")
                .type("stock")
                .riskLevel(5) // exceeds risk_averse maxRisk of 2
                .visible(true)
                .annualReturn(BigDecimal.valueOf(20.0))
                .minInvestment(BigDecimal.valueOf(10000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pDeposit, pStock));

        // User owns deposit
        Asset aDeposit = Asset.builder()
                .id(UUID.randomUUID())
                .productId(depositId)
                .currentValue(BigDecimal.valueOf(50000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aDeposit));
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: no "growth" recommendation because the only unowned product exceeds user's risk tolerance
        boolean hasGrowthRec = recs.stream().anyMatch(r -> "growth".equals(r.getCategory()));
        assertFalse(hasGrowthRec, "Rule 6: Should NOT recommend a product that exceeds the user's risk profile");
    }

    @Test
    void generateRecommendations_WithDelistedProducts_BestOfReturnsNull() {
        // Arrange
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Create a single stock product that the user owns. All other potential products are delisted (visible = false).
        UUID ownedStockId = UUID.randomUUID();
        Product pStockOwned = Product.builder()
                .id(ownedStockId)
                .name("Owned Stock")
                .type("stock")
                .riskLevel(3)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(10.0))
                .minInvestment(BigDecimal.valueOf(5000))
                .build();

        // Delisted products that match the other rules
        Product pDepositDelisted = Product.builder()
                .id(UUID.randomUUID())
                .name("Delisted Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(false) // delisted
                .annualReturn(BigDecimal.valueOf(3.0))
                .minInvestment(BigDecimal.valueOf(1000))
                .build();

        Product pBondDelisted = Product.builder()
                .id(UUID.randomUUID())
                .name("Delisted Bond")
                .type("bond")
                .riskLevel(2)
                .visible(false) // delisted
                .annualReturn(BigDecimal.valueOf(5.0))
                .minInvestment(BigDecimal.valueOf(2000))
                .build();

        when(productRepository.findAll()).thenReturn(List.of(pStockOwned, pDepositDelisted, pBondDelisted));

        // Asset causing concentration risk (100% stock)
        Asset aStock = Asset.builder()
                .id(UUID.randomUUID())
                .productId(ownedStockId)
                .currentValue(BigDecimal.valueOf(100000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aStock));

        // Priority and other goals
        Goal priorityGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Priority House")
                .type("vehicle_purchase") // Vehicle purchase needs money_market, deposit, balanced_fund, bond, sukuk. None is stock.
                .targetAmount(BigDecimal.valueOf(50000))
                .monthlyContribution(BigDecimal.valueOf(1000))
                .targetDate(LocalDate.now(clock).plusYears(3))
                .isPriority(true)
                .build();

        Goal otherGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Other Vacation")
                .type("vacation") // Vacation needs money_market, deposit, balanced_fund. None is stock.
                .targetAmount(BigDecimal.valueOf(10000))
                .monthlyContribution(BigDecimal.valueOf(500))
                .isPriority(false)
                .build();

        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(priorityGoal, otherGoal));

        // Income/Expense to trigger emergency fund shortfall and idle surplus
        FinancialProfile fp = FinancialProfile.builder()
                .id(UUID.randomUUID())
                .monthlyIncome(BigDecimal.valueOf(200000))
                .build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));

        Expense exp = Expense.builder()
                .totalExpenses(BigDecimal.valueOf(10000)) // emergency target = 60,000 > 0 liquid assets → shortfall triggers
                .build();
        when(expenseRepository.findByFinancialProfileId(fp.getId())).thenReturn(Optional.of(exp));

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert
        assertNotNull(recs);
        assertFalse(recs.isEmpty());

        // 1. Emergency recommendation is generated (Rule 1 triggers), but productId and suggestedAmount are null
        RecommendationDTO emergencyRec = recs.stream()
                .filter(r -> "emergency".equals(r.getCategory()))
                .findFirst().orElse(null);
        assertNotNull(emergencyRec, "Rule 1: Expected emergency recommendation");
        assertNull(emergencyRec.getProductId(), "Rule 1: Recommended product should be null because it was delisted");
        assertNull(emergencyRec.getSuggestedAmount(), "Rule 1: Suggested amount should be null");

        // 2. Rebalance recommendation is generated (Rule 2 triggers), but productId and suggestedAmount are null
        RecommendationDTO rebalanceRec = recs.stream()
                .filter(r -> "rebalance".equals(r.getCategory()))
                .findFirst().orElse(null);
        assertNotNull(rebalanceRec, "Rule 2: Expected rebalance recommendation");
        assertNull(rebalanceRec.getProductId(), "Rule 2: Complement product should be null because all complements are delisted");
        assertNull(rebalanceRec.getSuggestedAmount(), "Rule 2: Suggested amount should be null");

        // 3. Priority goal (Rule 3) should NOT generate a recommendation because product is null
        boolean hasPriorityGoalRec = recs.stream()
                .anyMatch(r -> "goal".equals(r.getCategory()) && priorityGoal.getId().equals(r.getGoalId()));
        assertFalse(hasPriorityGoalRec, "Rule 3: Should NOT generate a recommendation when the best product is delisted");

        // 4. Other goal (Rule 4) should NOT generate a recommendation because product is null
        boolean hasOtherGoalRec = recs.stream()
                .anyMatch(r -> "goal".equals(r.getCategory()) && otherGoal.getId().equals(r.getGoalId()));
        assertFalse(hasOtherGoalRec, "Rule 4: Should NOT generate a recommendation when the best product is delisted");

        // 5. Diversification gaps (Rule 5) should NOT generate a recommendation because product is delisted
        boolean hasDivRec = recs.stream()
                .anyMatch(r -> "diversification".equals(r.getCategory()));
        assertFalse(hasDivRec, "Rule 5: Should NOT generate a diversification gap recommendation when product is delisted");

        // 6. Growth recommendation (Rule 6) should NOT generate a recommendation because unowned products are delisted
        boolean hasGrowthRec = recs.stream()
                .anyMatch(r -> "growth".equals(r.getCategory()));
        assertFalse(hasGrowthRec, "Rule 6: Should NOT generate growth recommendation when unowned products are delisted");

        // 7. Surplus recommendation (Rule 7) should generate a recommendation
        RecommendationDTO surplusRec = recs.stream()
                .filter(r -> "surplus".equals(r.getCategory()))
                .findFirst().orElse(null);
        assertNotNull(surplusRec, "Rule 7: Expected surplus recommendation");
    }

    @Test
    void generateRecommendations_Rule2_NoAssets_SkipsConcentrationCheck() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Return empty assets list
        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: No concentration/rebalance recommendation since assets are empty
        boolean hasRebalanceRec = recs.stream().anyMatch(r -> "rebalance".equals(r.getCategory()));
        assertFalse(hasRebalanceRec, "Rule 2: Should NOT generate rebalance recommendation when assets are empty");
    }

    @Test
    void generateRecommendations_Rule2_TopProductNotFoundInProductMap_HandlesSafely() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID unknownProductId = UUID.randomUUID();
        // Asset references unknownProductId which won't be returned by productRepository.findAll()
        Asset aStock = Asset.builder()
                .id(UUID.randomUUID())
                .productId(unknownProductId)
                .currentValue(BigDecimal.valueOf(100000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aStock));

        // Create one other visible product (deposit) that can be recommended as complement
        UUID depositProductId = UUID.randomUUID();
        Product pDeposit = Product.builder()
                .id(depositProductId)
                .name("Safe Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(3.5))
                .minInvestment(BigDecimal.valueOf(500))
                .build();
        // Notice that unknownProductId is NOT in the products list returned
        when(productRepository.findAll()).thenReturn(List.of(pDeposit));

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(goalRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: Recommendation is generated, title is "One position is 100% of your portfolio"
        RecommendationDTO rebalanceRec = recs.stream()
                .filter(r -> "rebalance".equals(r.getCategory()))
                .findFirst().orElse(null);
        assertNotNull(rebalanceRec, "Rule 2: Expected rebalance recommendation even if top product is not found in product map");
        assertEquals("One position is 100% of your portfolio", rebalanceRec.getTitle());
        assertEquals(depositProductId, rebalanceRec.getProductId(), "Should recommend the deposit product as complement");
    }

    @Test
    void generateRecommendations_Rule3_PriorityGoalAlreadyCovered_DoesNotTrigger() {
        User user = User.builder().id(userId).riskProfile("moderate").questionnaireCompleted(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID depositProductId = UUID.randomUUID();
        Product pDeposit = Product.builder()
                .id(depositProductId)
                .name("Safe Deposit")
                .type("deposit")
                .riskLevel(1)
                .visible(true)
                .annualReturn(BigDecimal.valueOf(3.5))
                .minInvestment(BigDecimal.valueOf(500))
                .build();
        when(productRepository.findAll()).thenReturn(List.of(pDeposit));

        // User owns the deposit asset -> ownedTypes contains "deposit"
        Asset aDeposit = Asset.builder()
                .id(UUID.randomUUID())
                .productId(depositProductId)
                .currentValue(BigDecimal.valueOf(10000))
                .build();
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(aDeposit));

        // Priority goal of type "vacation" (which accepts money_market, deposit, balanced_fund)
        // Since user owns deposit, the type is already covered (hasMatchingType is true)
        Goal priorityGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Summer Trip")
                .type("vacation")
                .targetAmount(BigDecimal.valueOf(5000))
                .monthlyContribution(BigDecimal.valueOf(500))
                .targetDate(LocalDate.now(clock).plusYears(1))
                .isPriority(true)
                .build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(priorityGoal));

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: No goal recommendation since priority goal is already covered by owned deposit asset
        boolean hasGoalRec = recs.stream().anyMatch(r -> "goal".equals(r.getCategory()));
        assertFalse(hasGoalRec, "Rule 3: Should NOT generate goal recommendation when priority goal is already covered by owned types");
    }

    @Test
    void testGenerateRecommendations_IdleSurplus_freshRecsIsEmpty() {
        User user = User.builder()
                .id(userId)
                .riskProfile("moderate")
                .questionnaireCompleted(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        FinancialProfile fp = FinancialProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .monthlyIncome(BigDecimal.valueOf(500000))
                .build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));

        Expense exp = Expense.builder()
                .id(UUID.randomUUID())
                .financialProfile(fp)
                .totalExpenses(BigDecimal.valueOf(100000))
                .build();
        when(expenseRepository.findByFinancialProfileId(fp.getId())).thenReturn(Optional.of(exp));

        Product prodA = Product.builder()
                .id(UUID.randomUUID())
                .name("Deposit Product")
                .type("deposit")
                .riskLevel(1)
                .annualReturn(BigDecimal.valueOf(5))
                .minInvestment(BigDecimal.valueOf(10000))
                .visible(true)
                .build();
        Product prodB = Product.builder()
                .id(UUID.randomUUID())
                .name("Stock Product")
                .type("stock")
                .riskLevel(3)
                .annualReturn(BigDecimal.valueOf(10))
                .minInvestment(BigDecimal.valueOf(20000))
                .visible(true)
                .build();
        Product prodC = Product.builder()
                .id(UUID.randomUUID())
                .name("Bond Product")
                .type("bond")
                .riskLevel(2)
                .annualReturn(BigDecimal.valueOf(7))
                .minInvestment(BigDecimal.valueOf(15000))
                .visible(true)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(prodA, prodB, prodC));

        Asset assetA = Asset.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(prodA.getId())
                .currentValue(BigDecimal.valueOf(600000))
                .units(BigDecimal.ONE)
                .amount(BigDecimal.valueOf(600000))
                .purchaseDate(Instant.now(clock))
                .build();
        Asset assetB = Asset.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(prodB.getId())
                .currentValue(BigDecimal.valueOf(300000))
                .units(BigDecimal.ONE)
                .amount(BigDecimal.valueOf(300000))
                .purchaseDate(Instant.now(clock))
                .build();
        Asset assetC = Asset.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(prodC.getId())
                .currentValue(BigDecimal.valueOf(300000))
                .units(BigDecimal.ONE)
                .amount(BigDecimal.valueOf(300000))
                .purchaseDate(Instant.now(clock))
                .build();

        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(assetA, assetB, assetC));

        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Summer Trip")
                .type("vacation")
                .targetAmount(BigDecimal.valueOf(500000))
                .monthlyContribution(BigDecimal.valueOf(50000))
                .targetDate(LocalDate.now(clock).plusYears(1))
                .isPriority(true)
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .notes("Fun trip")
                .build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(goal));

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert that freshRecs is empty, meaning Rule 7's check passed and skipped adding because freshRecs is empty
        assertTrue(recs.isEmpty());
    }

    @Test
    void testGenerateRecommendations_IdleSurplus_undeployedSurplusLessThanThreshold() {
        User user = User.builder()
                .id(userId)
                .riskProfile("moderate")
                .questionnaireCompleted(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        FinancialProfile fp = FinancialProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .monthlyIncome(BigDecimal.valueOf(250000))
                .build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));

        Expense exp = Expense.builder()
                .id(UUID.randomUUID())
                .financialProfile(fp)
                .totalExpenses(BigDecimal.valueOf(50000))
                .build();
        when(expenseRepository.findByFinancialProfileId(fp.getId())).thenReturn(Optional.of(exp));

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(assetRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Summer Trip")
                .type("vacation")
                .targetAmount(BigDecimal.valueOf(500000))
                .monthlyContribution(BigDecimal.valueOf(120000))
                .targetDate(LocalDate.now(clock).plusYears(1))
                .isPriority(true)
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .notes("Fun trip")
                .build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(goal));

        when(recommendationRepository.findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationDTO> recs = actionRecommendationService.generateRecommendations();

        // Assert: Rule 1 emergency fund triggers, but Rule 7 surplus check skips adding the surplus recommendation since undeployed (30,000) <= 100,000
        assertEquals(1, recs.size());
        assertEquals("emergency", recs.get(0).getCategory());
    }
}

