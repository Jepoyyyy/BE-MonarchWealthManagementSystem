package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AssetsPnLResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProgressResponseDTO;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.GoalStatus;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalProgressServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PnLCalculationService pnlCalculationService;

    @InjectMocks
    private GoalProgressService goalProgressService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");

        UserDTO userDTO = UserDTO.builder()
                .id(userId)
                .email("test@example.com")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDTO, null);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getGoalProgressForUser_calculatesCorrectly() {
        UUID goalId1 = UUID.randomUUID();
        UUID goalId2 = UUID.randomUUID();
        UUID assetId1 = UUID.randomUUID();
        UUID assetId2 = UUID.randomUUID();

        Goal goal1 = createGoal(goalId1, "Emergency Fund", "EMERGENCY_FUND",
                new BigDecimal("50000000"), new BigDecimal("2000000"));
        Goal goal2 = createGoal(goalId2, "House", "PROPERTY",
                new BigDecimal("500000000"), new BigDecimal("5000000"));

        Asset asset1 = createAsset(assetId1, goalId1);
        Asset asset2 = createAsset(assetId2, goalId2);

        AssetsPnLResponseDTO pnl1 = createPnL(assetId1, new BigDecimal("15000000"),
                new BigDecimal("500000"), new BigDecimal("3.45"));
        AssetsPnLResponseDTO pnl2 = createPnL(assetId2, new BigDecimal("60000000"),
                new BigDecimal("2000000"), new BigDecimal("3.45"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(goal1, goal2));
        when(pnlCalculationService.computePnLForAllAssets()).thenReturn(List.of(pnl1, pnl2));
        when(assetRepository.findAllByGoalId(goalId1)).thenReturn(List.of(asset1));
        when(assetRepository.findAllByGoalId(goalId2)).thenReturn(List.of(asset2));

        List<GoalProgressResponseDTO> result = goalProgressService.getGoalProgressForUser();

        assertEquals(2, result.size());

        GoalProgressResponseDTO progress1 = result.stream()
                .filter(p -> p.getGoalId().equals(goalId1))
                .findFirst()
                .orElseThrow();

        assertEquals(goalId1, progress1.getGoalId());
        assertEquals("Emergency Fund", progress1.getGoalName());
        assertEquals(new BigDecimal("50000000"), progress1.getTargetAmount());
        assertEquals(new BigDecimal("15000000"), progress1.getCurrentSaved());
        assertEquals(new BigDecimal("2000000"), progress1.getMonthlyContribution());
        assertEquals(1, progress1.getAssignedAssetsCount());
        assertEquals(new BigDecimal("500000"), progress1.getTotalPotentialPnL());
        assertNotNull(progress1.getProjectedEtaMonths());
    }

    @Test
    void getGoalProgressForUser_whenCurrentSavedIsZero_setsTotalPotentialPnLPercentToZero() {
        UUID goalId = UUID.randomUUID();
        Goal goal = createGoal(goalId, "Savings", "SAVINGS",
                new BigDecimal("10000000"), new BigDecimal("1000000"));
        Asset asset = createAsset(UUID.randomUUID(), goalId);

        AssetsPnLResponseDTO pnl = createPnL(asset.getId(), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(goal));
        when(pnlCalculationService.computePnLForAllAssets()).thenReturn(List.of(pnl));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));

        List<GoalProgressResponseDTO> result = goalProgressService.getGoalProgressForUser();

        assertEquals(1, result.size());
        GoalProgressResponseDTO progress = result.get(0);
        assertEquals(BigDecimal.ZERO, progress.getCurrentSaved());
        assertEquals(BigDecimal.ZERO, progress.getTotalPotentialPnLPercent());
    }

    @Test
    void getGoalProgressForUser_whenTargetAmountReached_setsProjectedEtaMonthsToZero() {
        UUID goalId = UUID.randomUUID();
        Goal goal = createGoal(goalId, "Goal Reached", "SAVINGS",
                new BigDecimal("10000000"), new BigDecimal("1000000"));
        Asset asset = createAsset(UUID.randomUUID(), goalId);

        AssetsPnLResponseDTO pnl = createPnL(asset.getId(), new BigDecimal("10000000"),
                new BigDecimal("500000"), new BigDecimal("5.00"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(goal));
        when(pnlCalculationService.computePnLForAllAssets()).thenReturn(List.of(pnl));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));

        List<GoalProgressResponseDTO> result = goalProgressService.getGoalProgressForUser();

        assertEquals(1, result.size());
        GoalProgressResponseDTO progress = result.get(0);
        assertEquals(0, progress.getProjectedEtaMonths());
    }

    @Test
    void getGoalProgressForUser_whenTotalMonthlyIncreaseIsZeroOrNegative_setsProjectedEtaMonthsToMinusOne() {
        UUID goalId = UUID.randomUUID();
        Goal goal = createGoal(goalId, "Stagnant Goal", "SAVINGS",
                new BigDecimal("10000000"), BigDecimal.ZERO);
        Asset asset = createAsset(UUID.randomUUID(), goalId);

        AssetsPnLResponseDTO pnl = createPnL(asset.getId(), new BigDecimal("1000000"),
                BigDecimal.ZERO, BigDecimal.ZERO);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(goal));
        when(pnlCalculationService.computePnLForAllAssets()).thenReturn(List.of(pnl));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));

        List<GoalProgressResponseDTO> result = goalProgressService.getGoalProgressForUser();

        assertEquals(1, result.size());
        GoalProgressResponseDTO progress = result.get(0);
        assertEquals(-1, progress.getProjectedEtaMonths());
    }

    private Goal createGoal(UUID id, String name, String type, BigDecimal target, BigDecimal contribution) {
        Goal goal = new Goal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setName(name);
        goal.setType(type);
        goal.setTargetAmount(target);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setMonthlyContribution(contribution);
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setIsPriority(false);
        return goal;
    }

    private Asset createAsset(UUID id, UUID goalId) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setUserId(userId);
        asset.setGoalId(goalId);
        return asset;
    }

    private AssetsPnLResponseDTO createPnL(UUID assetId, BigDecimal currentValue,
                                            BigDecimal potentialPnL, BigDecimal potentialPnLPercent) {
        return AssetsPnLResponseDTO.builder()
                .assetId(assetId)
                .currentValue(currentValue)
                .potentialPnL(potentialPnL)
                .potentialPnLPercent(potentialPnLPercent)
                .build();
    }
}
