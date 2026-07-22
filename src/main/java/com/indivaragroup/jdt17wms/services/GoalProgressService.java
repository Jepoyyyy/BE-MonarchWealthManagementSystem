package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProgressResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class GoalProgressService {
    private final GoalRepository goalRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final PnLCalculationService pnlCalculationService;

    public static final Integer PERCENT_VALUE = 100;
    public static final Integer DOUBLE = 2;
    public static final Integer BY_FOUR = 4;
    public static final Integer TWELVE_MONTHS = 12;

    public GoalProgressService(
            GoalRepository goalRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            PnLCalculationService pnlCalculationService) {
        this.goalRepository = goalRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.pnlCalculationService = pnlCalculationService;
    }

    public List<GoalProgressResponseDTO> getGoalProgressForUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

        List<Goal> goals = goalRepository.findAllByUserId(user.getId());
        List<AssetsPnLResponseDTO> allPnlData = pnlCalculationService.computePnLForAllAssets();

        return goals.stream()
                .map(goal -> computeProgressForGoal(goal, allPnlData))
                .toList();
    }

    private GoalProgressResponseDTO computeProgressForGoal(Goal goal, List<AssetsPnLResponseDTO> allPnlData) {
        List<Asset> goalAssets = assetRepository.findAllByGoalId(goal.getId());

        List<AssetsPnLResponseDTO> goalPnlData = allPnlData.stream()
                .filter(pnl -> goalAssets.stream()
                        .anyMatch(asset -> asset.getId().equals(pnl.getAssetId())))
                .toList();

        BigDecimal initialAmount = goal.getCurrentAmount();
        BigDecimal currentSaved = goalPnlData.stream()
                .map(AssetsPnLResponseDTO::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(initialAmount);

        BigDecimal totalPotentialPnL = goalPnlData.stream()
                .map(AssetsPnLResponseDTO::getPotentialPnL)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPotentialPnLPercent = BigDecimal.ZERO;
        if (currentSaved.compareTo(BigDecimal.ZERO) > 0) {
            totalPotentialPnLPercent = totalPotentialPnL
                    .divide(currentSaved.subtract(totalPotentialPnL), BY_FOUR, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(PERCENT_VALUE))
                    .setScale(DOUBLE, RoundingMode.HALF_UP);
        }

        BigDecimal avgMonthlyGrowth = totalPotentialPnL.divide(BigDecimal.valueOf(TWELVE_MONTHS), DOUBLE, RoundingMode.HALF_UP);

      Integer projectedEtaMonths = getEtaMonths(goal, currentSaved, avgMonthlyGrowth);

      return GoalProgressResponseDTO.builder()
                .goalId(goal.getId())
                .goalName(goal.getName())
                .goalType(goal.getType())
                .targetAmount(goal.getTargetAmount())
                .currentSaved(currentSaved)
                .monthlyContribution(goal.getMonthlyContribution())
                .assignedAssetsCount(goalAssets.size())
                .totalPotentialPnL(totalPotentialPnL)
                .totalPotentialPnLPercent(totalPotentialPnLPercent)
                .avgMonthlyGrowth(avgMonthlyGrowth)
                .projectedEtaMonths(projectedEtaMonths)
                .isPriority(goal.getIsPriority())
                .build();
    }

  private static Integer getEtaMonths(Goal goal, BigDecimal currentSaved, BigDecimal avgMonthlyGrowth) {
    BigDecimal remaining = goal.getTargetAmount().subtract(currentSaved);
    BigDecimal totalMonthlyIncrease = goal.getMonthlyContribution().add(avgMonthlyGrowth);

    Integer projectedEtaMonths = null;
    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        projectedEtaMonths = 0;
    } else if (totalMonthlyIncrease.compareTo(BigDecimal.ZERO) > 0) {
        projectedEtaMonths = remaining
                .divide(totalMonthlyIncrease, 0, RoundingMode.CEILING)
                .intValue();
    } else {
        projectedEtaMonths = -1;
    }
    return projectedEtaMonths;
  }
}
