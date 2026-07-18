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
import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GoalsManagementService implements VerifiedUserProvider {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TARGET_DATE = "target_date";

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final AssetRepository assetRepository;
    private final Clock clock;

    @Override
    public UserRepository userRepository() { return userRepository; }

    public GoalsManagementService(GoalRepository goalRepository, UserRepository userRepository, FinancialProfileRepository financialProfileRepository, AssetRepository assetRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.assetRepository = assetRepository;
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
                        .monthlyContribution(goal.getMonthlyContribution())
                        .targetDate(goal.getTargetDate())
                        .isPriority(goal.getIsPriority())
                        .notes(goal.getNotes())
                        .status(goal.getStatus())
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

    if (!GoalConstants.GOAL_MAX_MONTHS.containsKey(type)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TYPE).reason("Invalid goal type").build());
    }

    LocalDate now = LocalDate.now(clock);
    LocalDate targetDate = dto.getTargetDate();

    if (targetDate.isBefore(now)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date must be in the future").build());
    } else if (GoalConstants.GOAL_MAX_MONTHS.containsKey(type)) {
      int maxMonths = GoalConstants.GOAL_MAX_MONTHS.get(type);
      long months = ChronoUnit.MONTHS.between(now, targetDate);
      if (months > maxMonths) {
        errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date exceeds maximum limit of " + maxMonths + " months").build());
      }
    }

    if (!errors.isEmpty()) {
      throw new CoreThrowHandler(ApiError.VALIDATION,errors);
    }

    if (Boolean.TRUE.equals(dto.getIsPriority())) {
      boolean hasPriorityGoal = goalRepository.findAllByUserId(user.getId()).stream()
        .anyMatch(g -> g.getIsPriority() && g.getStatus() == GoalStatus.IN_PROGRESS); // not covered yet!
      if (hasPriorityGoal) {
        throw new CoreThrowHandler(ApiError.DUPLICATE_PRIORITY_GOALS);
      }
    }

    Goal goal = Goal.builder()
      .userId(user.getId())
      .name(dto.getName())
      .type(type)
      .targetAmount(dto.getTargetAmount())
      .currentAmount(BigDecimal.ZERO)
      .monthlyContribution(dto.getMonthlyContribution())
      .targetDate(targetDate)
      .isPriority(dto.getIsPriority())
      .notes(dto.getNotes())
      .status(GoalStatus.IN_PROGRESS)
      .build();

    goal = goalRepository.save(goal);

    return GoalDTO.builder()
      .id(goal.getId())
      .userId(goal.getUserId())
      .name(goal.getName())
      .type(goal.getType())
      .targetAmount(goal.getTargetAmount())
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

    // 1. Validate Target Date logic (Enforced non-null by DTO @NotNull)
    LocalDate now = LocalDate.now(clock);
    LocalDate targetDate = dto.getTargetDate();

    if (targetDate.isBefore(now)) {
      errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date must be in the future").build());
    } else {
      String type = goal.getType();
      int maxMonths = GoalConstants.GOAL_MAX_MONTHS.get(type.toLowerCase());
      long months = ChronoUnit.MONTHS.between(now, targetDate);
      if (months > maxMonths) {
        errors.add(ValidationErrorDetailDTO.builder().field(FIELD_TARGET_DATE).reason("Target date exceeds maximum limit of " + maxMonths + " months").build());
      }
    }

    if (!errors.isEmpty()) {
      throw new CoreThrowHandler(ApiError.VALIDATION,errors);
    }

    // 2. Check duplicate priority (Safely defaults null to false)
    boolean isDtoPriority = Boolean.TRUE.equals(dto.getIsPriority());
    boolean isGoalPriority = Boolean.TRUE.equals(goal.getIsPriority());


    if (isDtoPriority && !isGoalPriority) {
      boolean hasPriorityGoal = goalRepository.findAllByUserId(user.getId()).stream()
        .anyMatch(g -> !g.getId().equals(goalId)
          && g.getIsPriority() // not covered yet!
          && g.getStatus() == GoalStatus.IN_PROGRESS);
      if (hasPriorityGoal) {
        throw new CoreThrowHandler(ApiError.DUPLICATE_PRIORITY_GOALS);
      }
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
    goal.setTargetDate(targetDate);
    goal.setIsPriority(isDtoPriority); // Reuses the boolean evaluated above
    goal.setNotes(dto.getNotes());

    goal = goalRepository.save(goal);

    return GoalDTO.builder()
      .id(goal.getId())
      .userId(goal.getUserId())
      .name(goal.getName())
      .type(goal.getType())
      .targetAmount(goal.getTargetAmount())
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
    }

    @Override
    public User getVerifiedUser() {
        return VerifiedUserProvider.super.getVerifiedUser();
    }
}
