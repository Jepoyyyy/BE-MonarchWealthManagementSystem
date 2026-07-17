package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.aspects.RiskProfileAssessmentRequired;
import com.indivaragroup.jdt17wms.constants.GoalConstants;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.dto.response.GoalProjectionDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProjectionDTO.TimeSeriesPointDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalsProjectionService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

  private static final int MAX_SIMULATION_MONTHS = 1_200;
  private static final int PROJECTION_WINDOW_MONTHS = 60;
  private static final Double MONTHS_COUNT = 12.0;
  private static final Double ONE_HUNDRED_PERCENT = 100.0;
  private static final int DOUBLE_DECIMAL_DIGITS = 2;


    public GoalsProjectionService(GoalRepository goalRepository,
                                  UserRepository userRepository,
                                  FinancialProfileRepository financialProfileRepository,
                                  AssetRepository assetRepository,
                                  ProductRepository productRepository,
                                  Clock clock) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

  @RiskProfileAssessmentRequired
  public List<GoalProjectionDTO> getProjectionsForUser() {
    User user = userRepository.findById(SecurityUtils.getCurrentUserId())
      .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

    if (!Boolean.TRUE.equals(user.getQuestionnaireCompleted())) {
      throw new CoreThrowHandler(ApiError.REQUIRED_RISK_PROFILER);
    }

    double defaultMonthlyRate = 0.0; // Savings do not grow like assets do, so rate is 0.0

    List<Goal> goals = goalRepository.findAllByUserId(user.getId());

    return goals.stream()
      .map(goal -> buildProjection(goal, defaultMonthlyRate))
      .toList();
  }

  private GoalProjectionDTO buildProjection(Goal goal, double defaultMonthlyRate) {
    List<Asset> assets = assetRepository.findAllByGoalId(goal.getId());

    double target = goal.getTargetAmount().doubleValue();
    double totalContribution = goal.getMonthlyContribution().doubleValue();
    double monthsToUse = calculateMonthsToUse(goal.getType(), goal.getTargetDate());

    LocalDate projectedDate;
    BigDecimal recommendedContribution;
    List<TimeSeriesPointDTO> timeSeries;

    if (assets.isEmpty()) {
      // Scenario A: No assets tied to the goal. Savings do not grow (0% return rate).
      double balance = goal.getCurrentAmount().doubleValue();

      int monthsToTarget = simulateMonthsToTarget(
        new double[]{balance}, new double[]{defaultMonthlyRate}, totalContribution, target);
      projectedDate = LocalDate.now(clock).plusMonths(monthsToTarget);

      double recContributionVal = (target - balance) / monthsToUse;
      recommendedContribution = toScaledBigDecimal(Math.max(0.0, recContributionVal));

      timeSeries = buildTimeSeries(new double[]{balance}, new double[]{defaultMonthlyRate}, totalContribution);

    } else {
      // Scenario B: Assets are tied. Contribution is split evenly across all assets.
      int kValue = assets.size();
      double[] balances = new double[kValue];
      double[] rates = new double[kValue];
      double contributionPerAsset = totalContribution / kValue;

      for (int j = 0; j < kValue; j++) {
        Asset asset = assets.get(j);
        balances[j] = asset.getCurrentValue().doubleValue();

        Product product = productRepository.findById(asset.getProductId())
          .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (product.getAnnualReturn() == null) {
          throw new CoreThrowHandler(ApiError.BAD_REQUEST,"Missing Annual Return");
        }
        rates[j] = product.getAnnualReturn().doubleValue() / ONE_HUNDRED_PERCENT / MONTHS_COUNT;
      }

      int monthsToTarget = simulateMonthsToTarget(balances, rates, contributionPerAsset, target);
      projectedDate = LocalDate.now(clock).plusMonths(monthsToTarget);

      recommendedContribution = calculateRecommendedContribution(balances, rates, target, monthsToUse);

      timeSeries = buildTimeSeries(balances, rates, contributionPerAsset);
    }

    return GoalProjectionDTO.builder()
      .id(goal.getId())
      .name(goal.getName())
      .type(goal.getType())
      .targetAmount(goal.getTargetAmount())
      .targetDate(goal.getTargetDate())
      .isPriority(goal.getIsPriority())
      .notes(goal.getNotes())
      .status(goal.getStatus())
      .projectedDate(projectedDate)
      .recommendedContribution(recommendedContribution)
      .timeSeries(timeSeries)
      .build();
  }

  /**
   * Number of months to use as the contribution-planning horizon: capped by the
   * goal type's default horizon, but shortened if the target date arrives sooner.
   */
  private double calculateMonthsToUse(String type, LocalDate targetDate) {
    double maxMonths = GoalConstants.GOAL_MAX_MONTHS.getOrDefault(type, 60);
    long actualMonths = ChronoUnit.MONTHS.between(LocalDate.now(clock), targetDate);
    return (actualMonths > 0 && actualMonths < maxMonths) ? actualMonths : maxMonths;
  }

  /**
   * Simulates monthly compounding across one or more balances/rates with an even
   * per-bucket contribution, returning the month count at which the combined sum
   * first reaches target (capped at MAX_SIMULATION_MONTHS).
   */
  private int simulateMonthsToTarget(double[] balances, double[] rates, double contributionPerBucket, double target) {
    double sum = sumOf(balances);
    if (sum >= target) {
      return 0;
    }

    if (!hasGrowthPotential(balances, rates, contributionPerBucket)) {
      return MAX_SIMULATION_MONTHS;
    }

    double[] simBalances = balances.clone();
    int months = 0;
    while (sum < target && months < MAX_SIMULATION_MONTHS) {
      sum = 0.0;
      for (int j = 0; j < simBalances.length; j++) {
        simBalances[j] = simBalances[j] * (1 + rates[j]) + contributionPerBucket;
        sum += simBalances[j];
      }
      months++;
    }
    return months;
  }

  /** Standard PMT-style recommended monthly contribution, averaged evenly across buckets. */
  private BigDecimal calculateRecommendedContribution(double[] balances, double[] rates, double target, double monthsToUse) {
    double num = target;
    double sumS = 0.0;
    for (int j = 0; j < balances.length; j++) {
      double futureValueFactor = Math.pow(1 + rates[j], monthsToUse);
      num -= balances[j] * futureValueFactor;
      sumS += (rates[j] > 0) ? (futureValueFactor - 1) / rates[j] : monthsToUse;
    }
    double denom = sumS / balances.length;
    double recContributionVal = denom > 0 ? num / denom : 0.0;
    return toScaledBigDecimal(Math.max(0.0, recContributionVal));
  }

  /** Projects the combined balance forward PROJECTION_WINDOW_MONTHS for the response chart. */
  private List<TimeSeriesPointDTO> buildTimeSeries(double[] balances, double[] rates, double contributionPerBucket) {
    List<TimeSeriesPointDTO> series = new ArrayList<>(PROJECTION_WINDOW_MONTHS);
    double[] runBalances = balances.clone();
    for (int m = 1; m <= PROJECTION_WINDOW_MONTHS; m++) {
      double stepSum = 0.0;
      for (int j = 0; j < runBalances.length; j++) {
        runBalances[j] = runBalances[j] * (1 + rates[j]) + contributionPerBucket;
        stepSum += runBalances[j];
      }
      series.add(TimeSeriesPointDTO.builder()
        .month(m)
        .value(toScaledBigDecimal(stepSum))
        .build());
    }
    return series;
  }

  private double sumOf(double[] values) {
    double total = 0.0;
    for (double v : values) total += v;
    return total;
  }

  private BigDecimal toScaledBigDecimal(double value) {
    return BigDecimal.valueOf(value).setScale(DOUBLE_DECIMAL_DIGITS, RoundingMode.HALF_UP);
  }

  private boolean hasGrowthPotential(double[] balances, double[] rates, double contributionPerBucket) {
    if (contributionPerBucket > 0) {
      return true;
    }
    for (int j = 0; j < balances.length; j++) {
      if (rates[j] > 0 && balances[j] > 0) {
        return true;
      }
    }
    return false;
  }

}
