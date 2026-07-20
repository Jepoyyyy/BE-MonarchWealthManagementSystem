package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import static com.indivaragroup.jdt17wms.constants.GoalConstants.*;
import static com.indivaragroup.jdt17wms.constants.PriorityConstants.*;
import static com.indivaragroup.jdt17wms.constants.ProductConstants.*;
import static com.indivaragroup.jdt17wms.constants.RiskConstants.*;

import com.indivaragroup.jdt17wms.dto.response.ComponentDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalDTO;
import com.indivaragroup.jdt17wms.dto.response.HealthDTO;
import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.RecommendationDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.*;
import com.indivaragroup.jdt17wms.models.enums.RecommendationStatus;
import com.indivaragroup.jdt17wms.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ActionRecommendationService {

    // ── Emergency Fund Constants ──
    private static final int EMERGENCY_FUND_EXPENSES_MULTIPLIER = 6;
    private static final double EMERGENCY_FUND_THRESHOLD_RATIO = 0.8;
    private static final int EMERGENCY_FUND_MAX_RISK_LEVEL = 2;

    // ── Health Score Constants ──
    private static final int MAX_TOTAL_SCORE = 100;
    private static final int MAX_COMPONENT_SCORE = 25;
    private static final int MIDPOINT_COMPONENT_SCORE = 12;

    // ── Health Status Thresholds ──
    private static final int EXCELLENT_SCORE_THRESHOLD = 80;
    private static final int GOOD_SCORE_THRESHOLD = 60;
    private static final int FAIR_SCORE_THRESHOLD = 40;

    // ── Risk Alignment Difference Thresholds ──
    private static final double RISK_DIFF_THRESHOLD_VERY_LOW = 0.5;
    private static final double RISK_DIFF_THRESHOLD_LOW = 1.0;
    private static final double RISK_DIFF_THRESHOLD_MEDIUM = 1.5;
    private static final double RISK_DIFF_THRESHOLD_HIGH = 2.0;

    // ── Risk Alignment Component Scores ──
    private static final int RISK_ALIGNMENT_SCORE_EXCELLENT = 25;
    private static final int RISK_ALIGNMENT_SCORE_GOOD = 20;
    private static final int RISK_ALIGNMENT_SCORE_FAIR = 14;
    private static final int RISK_ALIGNMENT_SCORE_MARGINAL = 8;
    private static final int RISK_ALIGNMENT_SCORE_POOR = 4;

    // ── Portfolio & Surplus Constants ──
    private static final double CONCENTRATION_LIMIT = 0.65;
    private static final int FIVE_YEAR_PROJECTION_MONTHS = 60;
    private static final int PERCENTAGE_MULTIPLIER = 100;

    // ── Priority Sorting Weights ──
    private static final int PRIORITY_WEIGHT_HIGH = 0;
    private static final int PRIORITY_WEIGHT_MEDIUM = 1;
    private static final int PRIORITY_WEIGHT_LOW = 2;
    private static final int PRIORITY_WEIGHT_DEFAULT = 3;

    // ── Risk Profile Default Max Risk ──
    private static final int DEFAULT_MAX_RISK_LEVEL = 5;

    // ── Health Score Component Names & Labels ──
    private static final String COMPONENT_EMERGENCY_NAME = "emergency";
    private static final String COMPONENT_EMERGENCY_LABEL = "Emergency Fund";
    private static final String COMPONENT_DIVERSIFICATION_NAME = "diversification";
    private static final String COMPONENT_DIVERSIFICATION_LABEL = "Diversification";
    private static final String COMPONENT_GOAL_COVERAGE_NAME = "goalCoverage";
    private static final String COMPONENT_GOAL_COVERAGE_LABEL = "Goal Coverage";
    private static final String COMPONENT_RISK_ALIGNMENT_NAME = "riskAlignment";
    private static final String COMPONENT_RISK_ALIGNMENT_LABEL = "Risk Alignment";

    // ── Health Status Labels ──
    private static final String STATUS_EXCELLENT = "Excellent";
    private static final String STATUS_GOOD = "Good";
    private static final String STATUS_FAIR = "Fair";
    private static final String STATUS_POOR = "Poor";

    // ── Risk Profile Strings ──
    private static final String RISK_PROFILE_RISK_AVERSE_LABEL = "risk-averse";
    private static final String RISK_PROFILE_RISK_TAKER_LABEL = "risk-taker";
    private static final String RISK_PROFILE_MODERATE_DEFAULT = "moderate";

    // ── Rule Key and Formatting Fallbacks ──
    private static final String RULE_KEY_DELIMITER = ":";
    private static final String RULE_KEY_NONE_PLACEHOLDER = "none";
    private static final String FORMAT_ZERO_FALLBACK = "0";

    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final GoalRepository goalRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;

    public ActionRecommendationService(RecommendationRepository recommendationRepository,
                                       UserRepository userRepository,
                                       AssetRepository assetRepository,
                                       ProductRepository productRepository,
                                       GoalRepository goalRepository,
                                       FinancialProfileRepository financialProfileRepository,
                                       ExpenseRepository expenseRepository,
                                       Clock clock) {
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.goalRepository = goalRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
    }

    // ══════════════════════════════════════════════════════════════════
    //  GET /api/v1/me/health — Financial Health Score
    // ══════════════════════════════════════════════════════════════════

    /**
     * Calculates the user's financial health score (0–100) composed of four
     * equally-weighted components (25 pts each):
     * 1. Emergency Fund — liquid assets vs. 6× monthly expenses
     * 2. Diversification — unique product types owned vs. eligible
     * 3. Goal Coverage — goals with a matching product type in portfolio
     * 4. Risk Alignment — weighted avg portfolio risk vs. profile target
     */
    public HealthDTO getHealthScore() {
        // ── Fetch all required data ──
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

      if (!Boolean.TRUE.equals(user.getQuestionnaireCompleted())) {
        throw new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER);
      }

        List<Asset> assets = assetRepository.findAllByUserId(user.getId());
        List<Product> products = productRepository.findAll();
        List<Goal> goals = goalRepository.findAllByUserId(user.getId());

        BigDecimal monthlyIncome = financialProfileRepository.findByUserId(user.getId())
                .map(FinancialProfile::getMonthlyIncome)
                .orElse(BigDecimal.ZERO);

        BigDecimal monthlyExpenses = financialProfileRepository.findByUserId(user.getId())
                .flatMap(fp -> expenseRepository.findByFinancialProfileId(fp.getId()))
                .map(Expense::getTotalExpenses)
                .orElse(BigDecimal.ZERO);

        // ── Portfolio total value ──
        BigDecimal totalValue = assets.stream()
                .map(a -> Optional.ofNullable(a.getCurrentValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Max risk level for user's profile ──
        String riskProfile = Optional.ofNullable(user.getRiskProfile())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER));
        int maxRiskLv = MAX_RISK_LEVELS.getOrDefault(riskProfile.toLowerCase(), DEFAULT_MAX_RISK_LEVEL);

        // Product lookup by ID for O(1) access
        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // ══════════════════════════════════════════════════════════════
        // COMPONENT 1: Emergency Fund (25 pts)
        // Target = 6× monthly expenses in liquid assets (money_market, deposit)
        // ══════════════════════════════════════════════════════════════
        BigDecimal emergencyTarget = monthlyExpenses.multiply(BigDecimal.valueOf(EMERGENCY_FUND_EXPENSES_MULTIPLIER));

        BigDecimal liquidValue = calcLiquidValue(assets, productMap);

        int emergency;
        if (emergencyTarget.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = liquidValue.doubleValue() / emergencyTarget.doubleValue();
            emergency = Math.min(MAX_COMPONENT_SCORE, (int) Math.round(ratio * MAX_COMPONENT_SCORE));
        } else {
            emergency = MIDPOINT_COMPONENT_SCORE; // No expenses → midpoint score
        }

        // ══════════════════════════════════════════════════════════════
        // COMPONENT 2: Diversification (25 pts)
        // Unique product types owned vs. eligible (visible + within risk)
        // ══════════════════════════════════════════════════════════════
        final int finalMaxRiskLv = maxRiskLv;

        Set<String> eligibleTypes = products.stream()
                .filter(p ->  Boolean.TRUE.equals(p.getVisible())
                        && p.getRiskLevel() <= finalMaxRiskLv)
                .map(p ->  p.getType().toLowerCase())
                .collect(Collectors.toSet());

        Set<String> ownedTypes = calcOwnedTypes(assets, productMap);

        int diversification;
        if (!eligibleTypes.isEmpty()) {
            diversification = Math.min(MAX_COMPONENT_SCORE,
                    (int) Math.round((double) ownedTypes.size() / eligibleTypes.size() * MAX_COMPONENT_SCORE));
        } else {
            diversification = 0;
        }

        // ══════════════════════════════════════════════════════════════
        // COMPONENT 3: Goal Coverage (25 pts)
        // Goals that have a matching product type in portfolio
        // ══════════════════════════════════════════════════════════════
        int goalCoverage;
        if (goals.isEmpty()) {
            goalCoverage = MIDPOINT_COMPONENT_SCORE; // No goals → midpoint score
        } else {
            long coveredGoals = goals.stream()
                    .filter(g -> {
                        String goalType = g.getType().toLowerCase();
                        List<String> suitableTypes = GOAL_PRODUCT_TYPES
                                .getOrDefault(goalType, GOAL_PRODUCT_TYPES.get(CUSTOM_GOAL));
                        return suitableTypes.stream()
                                .anyMatch(t -> ownedTypes.contains(t.toLowerCase()));
                    })
                    .count();
            goalCoverage = (int) Math.round((double) coveredGoals / goals.size() * MAX_COMPONENT_SCORE);
        }

        // ══════════════════════════════════════════════════════════════
        // COMPONENT 4: Risk Alignment (25 pts)
        // Weighted avg portfolio risk vs. profile target
        // ══════════════════════════════════════════════════════════════
        int riskAlignment = MIDPOINT_COMPONENT_SCORE; // default midpoint
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            double avgRisk = assets.stream()
                    .mapToDouble(a -> {
                        Product p = productMap.get(a.getProductId());
                        if (p == null) return 0.0;
                        return (a.getCurrentValue().doubleValue()
                                / totalValue.doubleValue()) * p.getRiskLevel();

                    })
                    .sum();

            double target = RISK_TARGETS.get(riskProfile.toLowerCase());
            double diff = Math.abs(avgRisk - target);

            if (diff <= RISK_DIFF_THRESHOLD_VERY_LOW) {
                riskAlignment = RISK_ALIGNMENT_SCORE_EXCELLENT;
            } else if (diff <= RISK_DIFF_THRESHOLD_LOW) {
                riskAlignment = RISK_ALIGNMENT_SCORE_GOOD;
            } else if (diff <= RISK_DIFF_THRESHOLD_MEDIUM) {
                riskAlignment = RISK_ALIGNMENT_SCORE_FAIR;
            } else if (diff <= RISK_DIFF_THRESHOLD_HIGH) {
                riskAlignment = RISK_ALIGNMENT_SCORE_MARGINAL;
            } else {
                riskAlignment = RISK_ALIGNMENT_SCORE_POOR;
            }
        }

        // ── Aggregate score ──
        int totalScore = emergency + diversification + goalCoverage + riskAlignment;

        // ── Available surplus (monthly income – expenses) ──
        BigDecimal availableSurplus = monthlyIncome.subtract(monthlyExpenses);

        // ── Status label derived from total score ──
        String status;
        if (totalScore >= EXCELLENT_SCORE_THRESHOLD) {
            status = STATUS_EXCELLENT;
        } else if (totalScore >= GOOD_SCORE_THRESHOLD) {
            status = STATUS_GOOD;
        } else if (totalScore >= FAIR_SCORE_THRESHOLD) {
            status = STATUS_FAIR;
        } else {
            status = STATUS_POOR;
        }

        // ── Build component DTOs ──
        List<ComponentDTO> components = List.of(
                ComponentDTO.builder()
                        .componentName(COMPONENT_EMERGENCY_NAME)
                        .label(COMPONENT_EMERGENCY_LABEL)
                        .score(emergency)
                        .maxScore(MAX_COMPONENT_SCORE)
                        .build(),
                ComponentDTO.builder()
                        .componentName(COMPONENT_DIVERSIFICATION_NAME)
                        .label(COMPONENT_DIVERSIFICATION_LABEL)
                        .score(diversification)
                        .maxScore(MAX_COMPONENT_SCORE)
                        .build(),
                ComponentDTO.builder()
                        .componentName(COMPONENT_GOAL_COVERAGE_NAME)
                        .label(COMPONENT_GOAL_COVERAGE_LABEL)
                        .score(goalCoverage)
                        .maxScore(MAX_COMPONENT_SCORE)
                        .build(),
                ComponentDTO.builder()
                        .componentName(COMPONENT_RISK_ALIGNMENT_NAME)
                        .label(COMPONENT_RISK_ALIGNMENT_LABEL)
                        .score(riskAlignment)
                        .maxScore(MAX_COMPONENT_SCORE)
                        .build()
        );

        return HealthDTO.builder()
                .totalScore(totalScore)
                .maxScore(MAX_TOTAL_SCORE)
                .status(status)
                .portfolioValue(totalValue)
                .availableSurplus(availableSurplus)
                .components(components)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    //  POST /api/v1/me/recommendations — Generate Action Recommendations
    // ══════════════════════════════════════════════════════════════════

    /**
     * Evaluates 7 financial rules against the user's current state and
     * persists actionable recommendations. Previously PENDING recommendations
     * that are no longer triggered are marked APPLIED (condition met).
     * <p>
     * Rules evaluated:
     * 1. Emergency fund shortfall
     * 2. Portfolio concentration risk
     * 3. Priority goal product alignment
     * 4. Other goals product alignment
     * 5. Diversification gaps
     * 6. Highest-return opportunity
     * 7. Idle surplus
     */
    @Transactional
    public List<RecommendationDTO> generateRecommendations() {
        // ── Fetch all required data ──
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getQuestionnaireCompleted())) {
          throw new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER);
        }

        UUID userId = user.getId();

        List<Asset> assets = assetRepository.findAllByUserId(userId);
        List<Product> products = productRepository.findAll();
        List<Goal> goals = goalRepository.findAllByUserId(userId);

        BigDecimal monthlyIncome = financialProfileRepository.findByUserId(userId)
                .map(FinancialProfile::getMonthlyIncome)
                .orElse(BigDecimal.ZERO);

        BigDecimal monthlyExpenses = financialProfileRepository.findByUserId(userId)
                .flatMap(fp -> expenseRepository.findByFinancialProfileId(fp.getId()))
                .map(Expense::getTotalExpenses)
                .orElse(BigDecimal.ZERO);

        BigDecimal surplus = monthlyIncome.subtract(monthlyExpenses);
        BigDecimal totalValue = assets.stream()
                .map(a -> Optional.ofNullable(a.getCurrentValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (user.getRiskProfile() == null) {
            throw new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER);
        }
        String riskProfile = user.getRiskProfile();
        int maxRiskLv = MAX_RISK_LEVELS.getOrDefault(riskProfile.toLowerCase(), DEFAULT_MAX_RISK_LEVEL);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Set<UUID> ownedIds = assets.stream().map(Asset::getProductId).collect(Collectors.toSet());
        Set<String> ownedTypes = calcOwnedTypes(assets, productMap);

        // ── Compute fresh recommendations from 7 rules ──
        List<Recommendation> freshRecs = new ArrayList<>();

        // ─────────────────────────────────────────
        // Rule 1: Emergency fund shortfall
        // ─────────────────────────────────────────
        BigDecimal emergencyTarget = monthlyExpenses.multiply(BigDecimal.valueOf(EMERGENCY_FUND_EXPENSES_MULTIPLIER));
        BigDecimal liquidValue = calcLiquidValue(assets, productMap);
        BigDecimal emergencyThreshold = emergencyTarget.multiply(BigDecimal.valueOf(EMERGENCY_FUND_THRESHOLD_RATIO));

        if (liquidValue.compareTo(emergencyThreshold) < 0) {
            Product p = bestOf(products, List.of(MONEY_MARKET, DEPOSIT), EMERGENCY_FUND_MAX_RISK_LEVEL, null);
            BigDecimal suggested = null;
            if (p != null) {
                BigDecimal gap = emergencyTarget.subtract(liquidValue);
                suggested = gap.max(p.getMinInvestment());
            }

            int pct = emergencyTarget.compareTo(BigDecimal.ZERO) > 0
                    ? (int) Math.round(liquidValue.doubleValue() / emergencyTarget.doubleValue() * PERCENTAGE_MULTIPLIER)
                    : 0;

            freshRecs.add(buildRecommendation(HIGH_PRIORITY, COMPONENT_EMERGENCY_NAME,
                    "Build your emergency fund",
                    String.format("You have %s in liquid assets — only %d%% of the recommended 6-month buffer (%s). "
                                    + "Without this, a crisis could force you to liquidate long-term investments at a loss.",
                            fmt(liquidValue), pct, fmt(emergencyTarget)),
                    p != null ? p.getId() : null, suggested, null));
        }

        // ─────────────────────────────────────────
        // Rule 2: Concentration risk (>65% in one product)
        // ─────────────────────────────────────────
        if (totalValue.compareTo(BigDecimal.ZERO) > 0 && !assets.isEmpty()) {
            // Aggregate value per product
            Map<UUID, BigDecimal> byProduct = new HashMap<>();
            for (Asset a : assets) {
                BigDecimal val = Optional.ofNullable(a.getCurrentValue()).orElse(BigDecimal.ZERO);
                byProduct.merge(a.getProductId(), val, BigDecimal::add);
            }
            //sampebawah
            // Find the most concentrated product
            Map.Entry<UUID, BigDecimal> top = byProduct.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (top != null) {
                double concentration = top.getValue().doubleValue() / totalValue.doubleValue();
                if (concentration > CONCENTRATION_LIMIT) {
                    Product topProduct = productMap.get(top.getKey());
                    String topType = topProduct != null && topProduct.getType() != null
                            ? topProduct.getType().toLowerCase() : "";

                    // Find complement: best product in a different type not yet owned
                    List<String> complementTypes = ALL_PRODUCT_TYPES.stream()
                            .filter(t -> !t.equalsIgnoreCase(topType))
                            .toList();
                    Product complement = bestOf(products, complementTypes, maxRiskLv, ownedIds);

                    String topName = topProduct != null ? topProduct.getName() : "One position";
                    int pct = (int) Math.round(concentration * PERCENTAGE_MULTIPLIER);

                    freshRecs.add(buildRecommendation( HIGH_PRIORITY, "rebalance",
                            String.format("%s is %d%% of your portfolio", topName, pct),
                            "Heavy concentration in a single product amplifies loss if it underperforms. "
                                    + "Adding a second product type reduces correlated risk without lowering "
                                    + "your expected return significantly.",
                            complement != null ? complement.getId() : null,
                            complement != null && complement.getMinInvestment() != null
                                    ? complement.getMinInvestment() : null,
                            null));
                }
            }
        }

        // ─────────────────────────────────────────
        // Rule 3: Priority goal alignment
        // ─────────────────────────────────────────
        Goal priorityGoal = goals.stream()
                .filter(Goal::getIsPriority)
                .findFirst().orElse(null);

        if (priorityGoal != null) {
            String goalType = priorityGoal.getType().toLowerCase() ;
            List<String> types = GOAL_PRODUCT_TYPES
                    .getOrDefault(goalType, GOAL_PRODUCT_TYPES.get(CUSTOM_GOAL));

            boolean hasMatchingType = types.stream().anyMatch(ownedTypes::contains);
            if (!hasMatchingType) {
                // Allow maxRiskLv + 1 for priority goals (slight risk uplift)
                Product p = bestOf(products, types, maxRiskLv + 1, ownedIds);
                if (p != null) {
                    String typeNames = types.stream()
                            .map(t -> TYPE_LABELS.getOrDefault(t, t))
                            .collect(Collectors.joining(" or "));

                    BigDecimal suggested = priorityGoal.getMonthlyContribution()
                      .max(p.getMinInvestment());

                  freshRecs.add(buildRecommendation( HIGH_PRIORITY, "goal",
                            String.format("Start building toward \"%s\"", priorityGoal.getName()),
                            String.format("Your priority goal needs %s%s. "
                                            + "You don't yet hold any %s — the product categories best aligned with this goal type.",
                                    fmt(priorityGoal.getTargetAmount()), " by " + priorityGoal.getTargetDate() , typeNames),
                            p.getId(), suggested, priorityGoal.getId()));
                }
            }
        }

        // ─────────────────────────────────────────
        // Rule 4: Other goal alignment
        // ─────────────────────────────────────────
        Set<UUID> usedProductIds = freshRecs.stream()
                .map(Recommendation::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Goal goal : goals) {
            if (Boolean.TRUE.equals(goal.getIsPriority())) continue;

            String goalType = goal.getType().toLowerCase();
            List<String> types = GOAL_PRODUCT_TYPES
                    .getOrDefault(goalType, GOAL_PRODUCT_TYPES.get(CUSTOM_GOAL));

            boolean hasMatchingType = types.stream().anyMatch(ownedTypes::contains);
            if (!hasMatchingType) {
                Product p = bestOf(products, types, maxRiskLv + 1, ownedIds);
                if (p != null && !usedProductIds.contains(p.getId())) {
                    String typeNames = types.stream()
                            .map(t -> TYPE_LABELS.getOrDefault(t, t))
                            .collect(Collectors.joining(" or "));

                    BigDecimal suggested = goal.getMonthlyContribution()
                      .max(p.getMinInvestment());


                  freshRecs.add(buildRecommendation(MEDIUM_PRIORITY, "goal",
                            String.format("No product aligned with \"%s\"", goal.getName()),
                            String.format("This goal works best with %s. %s (%s%% p.a.) fits the profile.",
                                    typeNames, p.getName(),
                                    Optional.ofNullable(p.getAnnualReturn()).map(BigDecimal::toPlainString).orElse("N/A")),
                            p.getId(), suggested, goal.getId()));

                    usedProductIds.add(p.getId());
                }
            }
        }

        // ─────────────────────────────────────────
        // Rule 5: Diversification gaps — missing eligible product types
        // ─────────────────────────────────────────
        for (String type : ALL_PRODUCT_TYPES) {
            if (!ownedTypes.contains(type)) {
                Product p = bestOf(products, List.of(type), maxRiskLv, null);
                if (p != null && !usedProductIds.contains(p.getId())) {
                    freshRecs.add(buildRecommendation(MEDIUM_PRIORITY, COMPONENT_DIVERSIFICATION_NAME,
                            String.format("Add %s exposure", TYPE_LABELS.getOrDefault(type, type)),
                            String.format("You hold no %s products. %s returns %s%% p.a. and fits within your %s profile "
                                            + "— adding it reduces single-category concentration.",
                                    TYPE_LABELS.getOrDefault(type, type), p.getName(),
                                                                        Optional.ofNullable(p.getAnnualReturn()).map(BigDecimal::toPlainString).orElse("N/A"),
                                    riskLabel(riskProfile)),
                            p.getId(),
                            p.getMinInvestment(),
                            null));

                    usedProductIds.add(p.getId());
                }
            }
        }

        // ─────────────────────────────────────────
        // Rule 6: Highest-return opportunity not yet owned
        // ─────────────────────────────────────────
        final int fMaxRisk = maxRiskLv;
        Optional<Product> topGrowth = products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getVisible())
                        && !ownedIds.contains(p.getId())
                        && p.getRiskLevel() <= fMaxRisk
                        && !usedProductIds.contains(p.getId()))
                .filter(p -> p.getAnnualReturn() != null)
                .max(Comparator.comparing(Product::getAnnualReturn));

      topGrowth.ifPresent(tg -> freshRecs.add(buildRecommendation( LOW_PRIORITY, "growth",
        String.format("Best unowned opportunity: %s", tg.getName()),
        String.format("At %s%% p.a., this is the highest-returning product within your %s profile "
            + "that you don't yet hold. Min. investment: %s.",
          Optional.ofNullable(tg.getAnnualReturn()).map(BigDecimal::toPlainString).orElse("N/A"),
          riskLabel(riskProfile),
          fmt(tg.getMinInvestment())),
        tg.getId(),
        tg.getMinInvestment(),
        null)));

        // ─────────────────────────────────────────
        // Rule 7: Idle surplus
        // ─────────────────────────────────────────
        if (surplus.compareTo(SURPLUS_THRESHOLD) > 0 && !freshRecs.isEmpty()) {
            BigDecimal goalsTotal = goals.stream()
                    .map(Goal::getMonthlyContribution)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal undeployed = surplus.subtract(goalsTotal);

            if (undeployed.compareTo(SURPLUS_THRESHOLD) > 0) {
                // Rough 5-year simple projection: monthly × 12 × 5
                BigDecimal fiveYearTotal = undeployed.multiply(BigDecimal.valueOf(FIVE_YEAR_PROJECTION_MONTHS));

                freshRecs.add(buildRecommendation( LOW_PRIORITY, "surplus",
                        String.format("%s/mo is not yet allocated", fmt(undeployed)),
                        String.format("After expenses and goal contributions, you still have %s per month "
                                        + "that could be working for you. Even at 5%% p.a., that compounds to %s over 5 years.",
                                fmt(undeployed), fmt(fiveYearTotal)),
                        null, null, null));
            }
        }

        // ── Reconcile with DB: match existing PENDING, update or resolve ──
        List<Recommendation> existingPending = recommendationRepository
                .findAllByUserIdAndStatus(userId, RecommendationStatus.PENDING);

        // Build productId → assetId lookup for resolving recommendations
        Map<UUID, UUID> productToAssetId = new HashMap<>();
        for (Asset a : assets) {
          productToAssetId.putIfAbsent(a.getProductId(), a.getId());
        }

        // Index fresh recs by rule key for O(1) matching
        Map<String, Recommendation> freshByKey = new LinkedHashMap<>();
        for (Recommendation r : freshRecs) {
            freshByKey.put(ruleKey(r), r);
        }

        // Index existing PENDING by rule key
        Map<String, Recommendation> existingByKey = new LinkedHashMap<>();
        for (Recommendation r : existingPending) {
            existingByKey.put(ruleKey(r), r);
        }

        Instant now = Instant.now(clock);
        Set<String> matchedKeys = new HashSet<>();
        List<Recommendation> toReturn = new ArrayList<>();

        // Pass 1: reconcile existing PENDING against fresh rules
        for (Recommendation existing : existingPending) {
            String key = ruleKey(existing);
            Recommendation freshMatch = freshByKey.get(key);

            if (freshMatch != null) {
                // Rule still active → update text/amounts in place, keep same record
                existing.setTitle(freshMatch.getTitle());
                existing.setReason(freshMatch.getReason());
                existing.setSuggestedAmount(freshMatch.getSuggestedAmount());
                existing.setPriority(freshMatch.getPriority());
                toReturn.add(existing);
                matchedKeys.add(key);
            } else {
                // Rule no longer active → condition met, mark resolved
                existing.setStatus(RecommendationStatus.APPLIED);
                existing.setResolvedAt(now);
                UUID resolverAssetId = productToAssetId.get(existing.getProductId());
                if (resolverAssetId != null) {
                  existing.setResolvedByAssetId(resolverAssetId);
                }
            }
        }
        // Flush so @UpdateTimestamp is populated on kept records
        recommendationRepository.saveAllAndFlush(existingPending);

        // Pass 2: create new PENDING for rules not already in DB
        List<Recommendation> newRecs = new ArrayList<>();
        for (Map.Entry<String, Recommendation> entry : freshByKey.entrySet()) {
            if (!matchedKeys.contains(entry.getKey())) {
                newRecs.add(entry.getValue());
            }
        }
        if (!newRecs.isEmpty()) {
            List<Recommendation> savedNew = recommendationRepository.saveAllAndFlush(newRecs);
            toReturn.addAll(savedNew);
        }

        // ── Sort by priority weight (high → medium → low) and return as DTOs ──
        Map<String, Integer> priorityWeight = Map.of(
                HIGH_PRIORITY, PRIORITY_WEIGHT_HIGH,
                MEDIUM_PRIORITY, PRIORITY_WEIGHT_MEDIUM,
                LOW_PRIORITY, PRIORITY_WEIGHT_LOW
        );
        toReturn.sort(Comparator.comparingInt(r ->
                priorityWeight.getOrDefault(r.getPriority(), PRIORITY_WEIGHT_DEFAULT)));

        return toReturn.stream().map(this::toRecommendationDTO).toList();
    }

    // ══════════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════════

    /**
     * Composite key for matching recommendations across evaluations.
     * Uses category + productId + goalId to identify "the same rule".
     */
    private static String ruleKey(Recommendation r) {
        return r.getCategory()
                + RULE_KEY_DELIMITER + (r.getProductId() != null ? r.getProductId() : RULE_KEY_NONE_PLACEHOLDER)
                + RULE_KEY_DELIMITER + (r.getGoalId() != null ? r.getGoalId() : RULE_KEY_NONE_PLACEHOLDER);
    }

    /**
     * Finds the best (highest annual return) visible product matching the given
     * types and risk level, optionally excluding already-owned product IDs.
     */
    private Product bestOf(List<Product> products, List<String> types,
                           int maxRisk, Set<UUID> excludeIds) {
        Set<String> lowerTypes = types.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return products.stream()
                .filter(p ->  Boolean.TRUE.equals(p.getVisible())
                        && lowerTypes.contains(p.getType().toLowerCase())
                        && p.getRiskLevel() <= maxRisk
                        && (excludeIds == null || !excludeIds.contains(p.getId())))
                .filter(p -> p.getAnnualReturn() != null)
                .max(Comparator.comparing(Product::getAnnualReturn))
                .orElse(null);
    }

    /** Sum of currentValue for assets in liquid product types (money_market, deposit). */
    private BigDecimal calcLiquidValue(List<Asset> assets, Map<UUID, Product> productMap) {
        return assets.stream()
                .filter(a -> {
                    Product p = productMap.get(a.getProductId());
                    if (p == null) {
                        log.warn("Asset {} references deleted product {}, skipping liquid calc", a.getId(), a.getProductId());
                        return false;
                    }
                    return LIQUID_PRODUCT_TYPES.contains(p.getType().toLowerCase());
                })
                .map(a -> a.getCurrentValue() != null ? a.getCurrentValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Unique lowercase product types owned by the user. */
    private Set<String> calcOwnedTypes(List<Asset> assets, Map<UUID, Product> productMap) {
        return assets.stream()
                .map(a -> {
                    Product p = productMap.get(a.getProductId());
                    if (p == null) {
                        log.warn("Asset {} references deleted product {}, skipping owned type", a.getId(), a.getProductId());
                    }
                    return p;
                })
                .filter(Objects::nonNull)
                .map(p -> p.getType().toLowerCase())
                .collect(Collectors.toSet());
    }

    /** Builds a Recommendation entity with PENDING status. */
    private Recommendation buildRecommendation(String priority, String category,
                                                String title, String reason,
                                                UUID productId, BigDecimal suggestedAmount,
                                                UUID goalId) {
        return Recommendation.builder()
                .userId(SecurityUtils.getCurrentUserId())
                .priority(priority)
                .category(category)
                .title(title)
                .reason(reason)
                .productId(productId)
                .suggestedAmount(suggestedAmount)
                .goalId(goalId)
                .status(RecommendationStatus.PENDING)
                .build();
    }

    /** Maps Recommendation entity → RecommendationDTO. */
    private RecommendationDTO toRecommendationDTO(Recommendation r) {
        // Hydrate product object if productId exists
        ProductResponseDTO productDTO = null;
        if (r.getProductId() != null) {
            productDTO = productRepository.findById(r.getProductId())
                    .map(ProductResponseDTO::fromEntity)
                    .orElse(null);
        }

        // Hydrate goal object if goalId exists
        GoalDTO goalDTO = null;
        if (r.getGoalId() != null) {
            goalDTO = goalRepository.findById(r.getGoalId())
                    .map(this::toGoalDTO)
                    .orElse(null);
        }

        return RecommendationDTO.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .priority(r.getPriority())
                .category(r.getCategory())
                .title(r.getTitle())
                .reason(r.getReason())
                .productId(r.getProductId())
                .product(productDTO)
                .suggestedAmount(r.getSuggestedAmount())
                .goalId(r.getGoalId())
                .goal(goalDTO)
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .resolvedAt(r.getResolvedAt())
                .resolvedByAssetId(r.getResolvedByAssetId())
                .build();
    }

    /** Maps Goal entity → GoalDTO. */
    private GoalDTO toGoalDTO(Goal g) {
        return GoalDTO.builder()
                .id(g.getId())
                .userId(g.getUserId())
                .name(g.getName())
                .type(g.getType())
                .targetAmount(g.getTargetAmount())
                .monthlyContribution(g.getMonthlyContribution())
                .currentAmount(g.getCurrentAmount())
                .targetDate(g.getTargetDate())
                .isPriority(g.getIsPriority())
                .notes(g.getNotes())
                .status(g.getStatus())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    /** Formats a BigDecimal as a readable currency string (e.g., "1,000,000"). */
    private static String fmt(BigDecimal value) {
        if (value == null) return FORMAT_ZERO_FALLBACK;
        BigDecimal truncated = value.setScale(0, java.math.RoundingMode.DOWN);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(0);
        return nf.format(truncated);
    }

    /** Human-readable label for a risk profile string. */
    private static String riskLabel(String riskProfile) {
        if (riskProfile == null) return RISK_PROFILE_MODERATE_DEFAULT;
        return switch (riskProfile.toLowerCase()) {
          case RISK_AVERSE -> RISK_PROFILE_RISK_AVERSE_LABEL;
          case RISK_TAKER -> RISK_PROFILE_RISK_TAKER_LABEL;
            default -> riskProfile.toLowerCase();
        };
    }
}
