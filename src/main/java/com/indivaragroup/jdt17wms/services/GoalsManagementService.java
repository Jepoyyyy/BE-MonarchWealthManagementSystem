package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.GoalConstants;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.aspects.RiskProfileAssessmentRequired;
import com.indivaragroup.jdt17wms.dto.response.GoalDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.FinancialProfile;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.enums.GoalStatus;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import com.indivaragroup.jdt17wms.models.Expense;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoalsManagementService implements VerifiedUserProvider {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TARGET_DATE = "target_date";
    private static final String BUSINESS_ERROR_CODE = "ERR-001";

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final AssetRepository assetRepository;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;

  public static final Integer PERCENT_VALUE = 100;
  public static final Integer FIFTY_PERCENT = 50;
  public static final Integer BY_FOUR = 4;



  public GoalsManagementService(GoalRepository goalRepository, UserRepository userRepository, FinancialProfileRepository financialProfileRepository, AssetRepository assetRepository, ExpenseRepository expenseRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.assetRepository = assetRepository;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
    }

    @RiskProfileAssessmentRequired
    public List<GoalDTO> getGoalsForUser() {
        User user = getVerifiedUser();

        List<Goal> goals = goalRepository.findAllByUserId(user.getId());

        return goals.stream()
                .map(goal -> GoalDTO.builder()
                        .id(goal.getId())
                        .userId(goal.getUserId())
                        .name(goal.getName())
                        .type(goal.getType())
                        .targetAmount(goal.getTargetAmount())
                        .currentAmount(goal.getCurrentAmount())
                        .monthlyContribution(goal.getMonthlyContribution())
                        .targetDate(goal.getTargetDate())
                        .isPriority(goal.getIsPriority())
                        .notes(goal.getNotes())
                        .status(goal.getStatus())
                        .currentAmount(goal.getCurrentAmount())
                        .createdAt(goal.getCreatedAt())
                        .updatedAt(goal.getUpdatedAt())
                        .build())
                .toList();
    }

  @RiskProfileAssessmentRequired
  public GoalDTO createGoalForUser(GoalRegistrationDTO dto) {
    User user = getVerifiedUser();

    List<ValidationErrorDetailDTO> errors = new ArrayList<>();
    String type = dto.getType(); // Enforced non-blank and lowercase by DTO @Pattern

    if (type == null || !GoalConstants.GOAL_MAX_MONTHS.containsKey(type)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TYPE).reason("Invalid goal type").type(BUSINESS_ERROR_CODE).build());
    }

    LocalDate now = LocalDate.now(clock);
    LocalDate targetDate = dto.getTargetDate();

    if (targetDate.isBefore(now)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date must be in the future").type(BUSINESS_ERROR_CODE).build());
    } else if (type != null && GoalConstants.GOAL_MAX_MONTHS.containsKey(type)) {
      int maxMonths = GoalConstants.GOAL_MAX_MONTHS.get(type);
      long months = ChronoUnit.MONTHS.between(now, targetDate);
      if (months > maxMonths) {
        errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date exceeds maximum limit of " + maxMonths + " months").type(BUSINESS_ERROR_CODE).build());
      }
    }

    if (!errors.isEmpty()) {
      throw new CoreThrowHandler(ApiError.VALIDATION,errors);
    }


    if (Boolean.TRUE.equals(dto.getIsPriority())) {
        Optional<Goal> exitingPriorityGoal = goalRepository.findAllByUserId(user.getId()).stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsPriority()) && g.getStatus() == GoalStatus.IN_PROGRESS)
                .findFirst();
        exitingPriorityGoal.ifPresent(goals ->{
            goals.setIsPriority(false);
            goalRepository.save(goals);
        });
    }

    Goal goal = Goal.builder()
      .userId(user.getId())
      .name(dto.getName())
      .type(type)
      .targetAmount(dto.getTargetAmount())
      .currentAmount(dto.getCurrentAmount() != null ? dto.getCurrentAmount() : BigDecimal.ZERO)
      .monthlyContribution(dto.getMonthlyContribution())
      .targetDate(targetDate)
      .isPriority(dto.getIsPriority())
      .notes(dto.getNotes())
      .status(GoalStatus.IN_PROGRESS)
      .build();

    goal = goalRepository.save(goal);

    // Auto-allocate if needed after creating new goal
    autoAllocateIfNeeded(user.getId());

    return GoalDTO.builder()
      .id(goal.getId())
      .userId(goal.getUserId())
      .name(goal.getName())
      .type(goal.getType())
      .targetAmount(goal.getTargetAmount())
      .currentAmount(goal.getCurrentAmount())
      .monthlyContribution(goal.getMonthlyContribution())
      .targetDate(goal.getTargetDate())
      .isPriority(goal.getIsPriority())
      .notes(goal.getNotes())
      .status(goal.getStatus())
      .createdAt(goal.getCreatedAt())
      .updatedAt(goal.getUpdatedAt())
      .build();
  }

  @RiskProfileAssessmentRequired
  public GoalDTO updateGoalForUser(UUID goalId, GoalEditingDTO dto) {
    User user = getVerifiedUser();

    Goal goal = goalRepository.findById(goalId)
      .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

    if (!goal.getUserId().equals(user.getId())) {
      throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
    }

    List<ValidationErrorDetailDTO> errors = new ArrayList<>();

    String type = dto.getType() != null ? dto.getType() : goal.getType();
    if (type != null) {
      type = type.toLowerCase();
    }

    if (type == null || !GoalConstants.GOAL_MAX_MONTHS.containsKey(type)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TYPE).reason("Invalid goal type").type(BUSINESS_ERROR_CODE).build());
    }

    // 1. Validate Target Date logic (Enforced non-null by DTO @NotNull)
    LocalDate now = LocalDate.now(clock);
    LocalDate targetDate = dto.getTargetDate();

    if (targetDate.isBefore(now)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date must be in the future").type(BUSINESS_ERROR_CODE).build());
    } else if (type != null && GoalConstants.GOAL_MAX_MONTHS.containsKey(type)) {
      int maxMonths = GoalConstants.GOAL_MAX_MONTHS.get(type);
      long months = ChronoUnit.MONTHS.between(now, targetDate);
      if (months > maxMonths) {
        errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date exceeds maximum limit of " + maxMonths + " months").type(BUSINESS_ERROR_CODE).build());
      }
    }

    if (!errors.isEmpty()) {
      throw new CoreThrowHandler(ApiError.VALIDATION,errors);
    }

    // 2. Check duplicate priority (Safely defaults null to false)
    boolean isDtoPriority = Boolean.TRUE.equals(dto.getIsPriority());
    boolean isGoalPriority = Boolean.TRUE.equals(goal.getIsPriority());

    if (isDtoPriority && !isGoalPriority) {
        goalRepository.findAllByUserId(user.getId()).stream()
            .filter(g -> !g.getId().equals(goalId)
              && Boolean.TRUE.equals(g.getIsPriority())
              && g.getStatus() == GoalStatus.IN_PROGRESS)
            .findFirst()
            .ifPresent(existingPriority -> {
                existingPriority.setIsPriority(false);
                goalRepository.save(existingPriority);
            });
    }

    // 3. Financial validation (dto.getMonthlyContribution() is guaranteed non-null)
    BigDecimal monthlyIncome = financialProfileRepository.findByUserId(user.getId())
      .map(FinancialProfile::getMonthlyIncome)
      .orElse(BigDecimal.ZERO);

    BigDecimal totalContribution = goalRepository.findAllByUserId(user.getId()).stream()
      .map(g -> g.getId().equals(goalId) ? dto.getMonthlyContribution() : g.getMonthlyContribution())
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalContribution.compareTo(monthlyIncome) > 0) {
      throw new CoreThrowHandler(ApiError.INSUFFICIENT_INCOME);
    }

    // 4. Update and Save
    goal.setName(dto.getName());
    goal.setTargetAmount(dto.getTargetAmount());
    goal.setMonthlyContribution(dto.getMonthlyContribution());
    if (dto.getCurrentAmount() != null)
        goal.setCurrentAmount(dto.getCurrentAmount());
    goal.setType(type);
    goal.setTargetDate(targetDate);
    goal.setIsPriority(isDtoPriority); // Reuses the boolean evaluated above
    goal.setNotes(dto.getNotes());

    goal = goalRepository.save(goal);

    // Trigger auto-allocation if needed after updating goal
    autoAllocateIfNeeded(user.getId());

    return GoalDTO.builder()
      .id(goal.getId())
      .userId(goal.getUserId())
      .name(goal.getName())
      .type(goal.getType())
      .targetAmount(goal.getTargetAmount())
      .currentAmount(goal.getCurrentAmount())
      .monthlyContribution(goal.getMonthlyContribution())
      .targetDate(goal.getTargetDate())
      .isPriority(goal.getIsPriority())
      .notes(goal.getNotes())
      .status(goal.getStatus())
      .createdAt(goal.getCreatedAt())
      .updatedAt(goal.getUpdatedAt())
      .build();
  }
    @Transactional
    @RiskProfileAssessmentRequired
    public void deleteGoalForUser(UUID goalId) {
        User user = getVerifiedUser();

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!goal.getUserId().equals(user.getId())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        List<Asset> assets = assetRepository.findAllByGoalId(goalId);
        for (Asset asset : assets) {
            asset.setGoalId(null);
            assetRepository.save(asset);
        }

        goalRepository.delete(goal);

        // Auto-allocate if needed after deleting goal
        autoAllocateIfNeeded(user.getId());
    }

    @Transactional
    @RiskProfileAssessmentRequired
    public List<GoalDTO> autoAllocateGoalsForUser(int percentage) {
        User user = getVerifiedUser();
        doAutoAllocate(user.getId(), percentage);
        return getGoalsForUser();
    }

    private void doAutoAllocate(UUID userId, int percentage) {
        List<Goal> goals = goalRepository.findAllByUserId(userId);
        if (goals.isEmpty()) {
            return;
        }

        // Calculate investable surplus
        BigDecimal monthlyIncome = financialProfileRepository.findByUserId(userId)
                .map(FinancialProfile::getMonthlyIncome)
                .orElse(BigDecimal.ZERO);

        BigDecimal monthlyExpenses = financialProfileRepository.findByUserId(userId)
                .flatMap(fp -> expenseRepository.findByFinancialProfileId(fp.getId()))
                .map(Expense::getTotalExpenses)
                .orElse(BigDecimal.ZERO);

        BigDecimal surplus = monthlyIncome.subtract(monthlyExpenses).max(BigDecimal.ZERO);

        // Find priority goal
        Goal priorityGoal = goals.stream()
                .filter(GoalsManagementService::isPriorityGoal)
                .findFirst()
                .orElse(null);

        long otherCount = goals.stream()
                .filter(GoalsManagementService::isNonPriorityGoal)
                .count();

        BigDecimal primaryAmt = BigDecimal.ZERO;
        BigDecimal eachOther = BigDecimal.ZERO;

        if (surplus.compareTo(BigDecimal.ZERO) > 0 && priorityGoal != null) {
            // Priority goal gets percentage of surplus
            primaryAmt = surplus.multiply(BigDecimal.valueOf(percentage))
                    .divide(BigDecimal.valueOf(PERCENT_VALUE), BY_FOUR, RoundingMode.HALF_UP);

            BigDecimal remaining = surplus.subtract(primaryAmt).max(BigDecimal.ZERO);
            if (otherCount > 0) {
                eachOther = remaining.divide(BigDecimal.valueOf(otherCount), BY_FOUR, RoundingMode.HALF_UP);
            }
        }

        // Update all active goals
        for (Goal g : goals) {
            if (g.getStatus() == GoalStatus.IN_PROGRESS) {
                if (isPriorityGoal(g)) {
                    g.setMonthlyContribution(primaryAmt);
                } else {
                    g.setMonthlyContribution(eachOther);
                }
                goalRepository.save(g);
            }
        }
    }

  void autoAllocateIfNeeded(UUID userId) {
    // Check if auto-allocation is enabled
    FinancialProfile profile = financialProfileRepository.findByUserId(userId).orElse(null);
    if (profile == null || !Boolean.TRUE.equals(profile.getAutoAllocationEnabled())) {
      return;
    }

    List<Goal> goals = goalRepository.findAllByUserId(userId);
    long activeGoals = goals.stream()
      .filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS)
      .count();

    boolean hasPriorityGoal = goals.stream()
      .anyMatch(GoalsManagementService::isPriorityGoal);

    // Only auto-allocate if we have 2+ active goals and a priority goal
    if (activeGoals > 1 && hasPriorityGoal) {
      Integer percentage = profile.getPriorityAllocationPercentage();
      if (percentage == null) {
        percentage = FIFTY_PERCENT; // Default fallback
      }
      doAutoAllocate(userId, percentage);
    }
  }

    private static boolean isPriorityGoal(Goal g) {
        return Boolean.TRUE.equals(g.getIsPriority()) && g.getStatus() == GoalStatus.IN_PROGRESS;
    }

    private static boolean isNonPriorityGoal(Goal g) {
        return !Boolean.TRUE.equals(g.getIsPriority()) && g.getStatus() == GoalStatus.IN_PROGRESS;
    }

    @Override
    public User getVerifiedUser() {
        return VerifiedUserProvider.super.getVerifiedUser();
    }
    @Override
    public UserRepository userRepository() {
        return this.userRepository;
    }
}
