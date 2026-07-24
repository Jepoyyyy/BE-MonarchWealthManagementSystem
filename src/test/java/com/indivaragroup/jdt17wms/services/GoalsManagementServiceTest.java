package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.GoalDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.GoalStatus;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.dto.request.GoalRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.models.FinancialProfile;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Expense;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class GoalsManagementServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinancialProfileRepository financialProfileRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private GoalsManagementService goalsManagementService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        goalsManagementService = new GoalsManagementService(goalRepository, userRepository, financialProfileRepository, assetRepository, expenseRepository,clock);
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
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
        assertNotNull(goalsManagementService);
    }

    @Test
    void getGoalsForUser_shouldReturnGoalsWhenQuestionnaireCompleted() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Retirement Fund")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(goal));

        List<GoalDTO> result = goalsManagementService.getGoalsForUser();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Retirement Fund", result.getFirst().getName());
        assertEquals(new java.math.BigDecimal("500000.00"), result.getFirst().getTargetAmount());
    }



    @Test
    void getGoalsForUser_shouldThrowNotFoundExceptionWhenUserNotFound() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> goalsManagementService.getGoalsForUser());
    }

    @Test
    void createGoalForUser_shouldCreateGoalSuccessfully() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .name("Retirement Fund")
                .type("retirement")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .monthlyContribution(new java.math.BigDecimal("1000.00"))
                .targetDate(java.time.LocalDate.of(2040, Month.JANUARY, 1))
                .isPriority(true)
                .build();

        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Retirement Fund")
                .type("retirement")
                .targetAmount(new java.math.BigDecimal("500000.00"))
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of());
        when(goalRepository.save(any(Goal.class))).thenReturn(goal);

        GoalDTO result = goalsManagementService.createGoalForUser(request);

        assertNotNull(result);
        assertEquals("Retirement Fund", result.getName());
        assertEquals(new java.math.BigDecimal("500000.00"), result.getTargetAmount());
    }



    @Test
    void createGoalForUser_shouldDemotePriorityWhenAnotherPriorityGoalAlreadyExists() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .type("savings")
                .targetDate(LocalDate.of(2026, Month.AUGUST, 13))
                .monthlyContribution(new BigDecimal("500.00"))
                .isPriority(true)
                .build();

        Goal existingGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .isPriority(true)
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.save(any(Goal.class))).thenReturn(existingGoal);

        GoalDTO result = goalsManagementService.createGoalForUser(request);

        assertNotNull(result);
        assertThat(existingGoal.getIsPriority()).isFalse();
        verify(goalRepository).save(existingGoal);
    }

    @Test
    void updateGoalForUser_shouldUpdateGoalSuccessfully() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Retirement Fund")
                .type("retirement")
                .targetAmount(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();

        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("New retirement")
                .targetAmount(new BigDecimal("600000.00"))
                .monthlyContribution(new BigDecimal("1200.00"))
                .targetDate(LocalDate.of(2031, Month.JULY, 13))
                .isPriority(true)
                .notes("Updated notes")
                .build();

        FinancialProfile profile = FinancialProfile.builder()
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalDTO result = goalsManagementService.updateGoalForUser(goalId, request);

        assertNotNull(result);
        assertEquals("New retirement", result.getName());
        assertEquals(new BigDecimal("600000.00"), result.getTargetAmount());
        assertEquals(new BigDecimal("1200.00"), result.getMonthlyContribution());
        assertEquals(true, result.getIsPriority());
    }



    @Test
    void updateGoalForUser_shouldThrowNotFoundExceptionWhenGoalNotFound() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .targetDate(LocalDate.of(2030, Month.JANUARY, 1))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> goalsManagementService.updateGoalForUser(goalId, request));
    }

    @Test
    void updateGoalForUser_shouldThrowNotFoundExceptionWhenGoalBelongsToDifferentUser() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .targetDate(LocalDate.of(2030, Month.JANUARY, 1))
                .build();
        Goal goalOfOtherUser = Goal.builder()
                .id(goalId)
                .userId(UUID.randomUUID())
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalOfOtherUser));

        assertThrows(CoreThrowHandler.class, () -> goalsManagementService.updateGoalForUser(goalId, request));
    }

    @Test
    void updateGoalForUser_shouldDemoteExistingPriorityWhenPromotingAnother() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .isPriority(false)
                .monthlyContribution(new BigDecimal("200.00"))
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();
        Goal otherGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .isPriority(true)
                .monthlyContribution(new BigDecimal("100.00"))
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();

      GoalEditingDTO request = GoalEditingDTO.builder()
        .name("Vacation Fund")
        .targetAmount(new BigDecimal("5000.00"))
        .monthlyContribution(new BigDecimal("200.00"))
        .targetDate(LocalDate.now(clock).plusDays(1))
        .isPriority(true)
        .build();


        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal, otherGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalDTO result = goalsManagementService.updateGoalForUser(goalId, request);

        assertNotNull(result);
        assertThat(otherGoal.getIsPriority()).isFalse();
        verify(goalRepository).save(otherGoal);
    }

    @Test
    void updateGoalForUser_shouldThrowInsufficientIncomeExceptionWhenContributionExceedsIncome() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .monthlyContribution(new BigDecimal("1000.00"))
                .isPriority(false)
                .status(com.indivaragroup.jdt17wms.models.enums.GoalStatus.IN_PROGRESS)
                .build();

      GoalEditingDTO request = GoalEditingDTO.builder()
        .name("Emergency Fund")
        .targetAmount(new BigDecimal("10000.00"))
        .monthlyContribution(new BigDecimal("6000.00")) // Exceeds income of 5000
        .targetDate(LocalDate.now(clock).plusDays(1))
        .isPriority(false)
        .build();


        FinancialProfile profile = FinancialProfile.builder()
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));

        assertThrows(CoreThrowHandler.class, () -> goalsManagementService.updateGoalForUser(goalId, request));
    }

    @Test
    void deleteGoalForUser_shouldDeleteGoalSuccessfully() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal goal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .build();
        Asset asset = Asset.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(assetRepository.findAllByGoalId(goalId)).thenReturn(List.of(asset));

        goalsManagementService.deleteGoalForUser(goalId);

        verify(assetRepository).save(asset);
        verify(goalRepository).delete(goal);
    }



    @Test
    void deleteGoalForUser_shouldThrowNotFoundExceptionWhenGoalNotFound() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> goalsManagementService.deleteGoalForUser(goalId));
    }

    @Test
    void deleteGoalForUser_shouldThrowNotFoundExceptionWhenGoalBelongsToDifferentUser() {
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal goalOfOtherUser = Goal.builder()
                .id(goalId)
                .userId(UUID.randomUUID())
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalOfOtherUser));

        assertThrows(CoreThrowHandler.class, () -> goalsManagementService.deleteGoalForUser(goalId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createGoalForUser – GoalValidationException paths
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createGoalForUser_shouldThrowGoalValidationExceptionWhenTypeIsInvalid() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        // "invalid_type" is not a key in AppConstants.GOAL_MAX_MONTHS
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .type("invalid_type")
                .targetDate(LocalDate.now(clock).plusDays(30))
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.createGoalForUser(request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("type");
                    assertThat(err.getReason()).isEqualTo("Invalid goal type");
                });
    }

    @Test
    void createGoalForUser_shouldThrowGoalValidationExceptionWhenTypeIsNull() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .type(null)
                .targetDate(LocalDate.now(clock).plusDays(30))
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.createGoalForUser(request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("type");
                    assertThat(err.getReason()).isEqualTo("Invalid goal type");
                });
    }

    @Test
    void createGoalForUser_shouldThrowGoalValidationExceptionWhenTargetDateIsInThePast() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .type("savings")
                .targetDate(LocalDate.now(clock).minusDays(1)) // past date
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.createGoalForUser(request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("target_date");
                    assertThat(err.getReason()).isEqualTo("Target date must be in the future");
                });
    }

    @Test
    void createGoalForUser_shouldThrowGoalValidationExceptionWhenTargetDateExceedsMaxMonths() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        // "savings" max is 18 months — set target 19 months ahead
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .type("savings")
                .targetDate(LocalDate.now(clock).plusMonths(19))
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.createGoalForUser(request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("target_date");
                    assertThat(err.getReason()).contains("18 months");
                });
    }

    @Test
    void createGoalForUser_shouldCreateGoalSuccessfullyWhenNotPriority() {
        // Covers the branch where isPriority=false — duplicate-priority check is skipped entirely
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .name("Vacation Fund")
                .type("vacation")
                .targetAmount(new BigDecimal("5000.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        Goal saved = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Vacation Fund")
                .type("vacation")
                .targetAmount(new BigDecimal("5000.00"))
                .status(GoalStatus.IN_PROGRESS)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.save(any(Goal.class))).thenReturn(saved);

        GoalDTO result = goalsManagementService.createGoalForUser(request);

        assertNotNull(result);
        assertEquals("Vacation Fund", result.getName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateGoalForUser – GoalValidationException paths
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateGoalForUser_shouldThrowGoalValidationExceptionWhenTargetDateIsInThePast() {
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("retirement")
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Retirement")
                .targetAmount(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .targetDate(LocalDate.now(clock).minusDays(1)) // past date
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.updateGoalForUser(goalId, request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("target_date");
                    assertThat(err.getReason()).isEqualTo("Target date must be in the future");
                });
    }

    @Test
    void updateGoalForUser_shouldThrowGoalValidationExceptionWhenTargetDateExceedsMaxMonths() {
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        // "savings" max is 18 months
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Savings Goal")
                .targetAmount(new BigDecimal("10000.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(19)) // exceeds 18-month cap
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.updateGoalForUser(goalId, request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("target_date");
                    assertThat(err.getReason()).contains("18 months");
                });
    }

    @Test
    void updateGoalForUser_shouldThrowGoalValidationExceptionWhenTypeIsInvalid() {
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type(null) // null type in db
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Savings Goal")
                .type("invalid_type") // invalid type in request
                .targetAmount(new BigDecimal("10000.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.updateGoalForUser(goalId, request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("type");
                    assertThat(err.getReason()).isEqualTo("Invalid goal type");
                });
    }

    @Test
    void updateGoalForUser_shouldThrowGoalValidationExceptionWhenTypeIsNull() {
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type(null) // null type in db
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Savings Goal")
                .type(null) // null type in request
                .targetAmount(new BigDecimal("10000.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.updateGoalForUser(goalId, request));

        assertThat(ex.getDetails())
                .hasSize(1)
                .anySatisfy(err -> {
                    assertThat(err.getField()).isEqualTo("type");
                    assertThat(err.getReason()).isEqualTo("Invalid goal type");
                });
    }

    @Test
    void updateGoalForUser_shouldSkipDuplicatePriorityCheckWhenGoalIsAlreadyPriority() {
        // Covers branch: isDtoPriority=true AND isGoalPriority=true → condition is false → no duplicate check
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("retirement")
                .monthlyContribution(new BigDecimal("1000.00"))
                .isPriority(true) // already priority — re-setting to true must not trigger the check
                .status(GoalStatus.IN_PROGRESS)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Retirement Updated")
                .targetAmount(new BigDecimal("600000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .targetDate(LocalDate.now(clock).plusMonths(12))
                .isPriority(true)
                .build();

        FinancialProfile profile = FinancialProfile.builder()
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalDTO result = goalsManagementService.updateGoalForUser(goalId, request);

        assertNotNull(result);
        assertEquals("Retirement Updated", result.getName());
        assertEquals(true, result.getIsPriority());
    }

    @Test
    void updateGoalForUser_shouldThrowInsufficientIncomeExceptionWhenNoFinancialProfileExists() {
        // Covers branch: financialProfileRepository returns empty → monthlyIncome defaults to ZERO
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .monthlyContribution(new BigDecimal("100.00"))
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Savings")
                .targetAmount(new BigDecimal("5000.00"))
                .monthlyContribution(new BigDecimal("1.00")) // any positive amount > ZERO income
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.updateGoalForUser(goalId, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Remaining branch coverage for inline "// not covered yet!" markers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createGoalForUser_shouldCreateGoalSuccessfullyWhenPriorityAndNoPriorityGoalExists() {
        // Line 116: anyMatch predicate evaluates to FALSE (no existing IN_PROGRESS priority goal)
        // → hasPriorityGoal=false → no exception → goal is saved
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .name("My Priority Goal")
                .type("savings")
                .targetAmount(new BigDecimal("3000.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(true)
                .build();

        // Existing goal: is priority but NOT in progress → anyMatch returns false
        Goal nonInProgressPriorityGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("My Priority Goal")
                .type("savings")
                .targetAmount(new BigDecimal("3000.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(true)
                .status(GoalStatus.ACHIEVED) // not IN_PROGRESS → predicate is false
                .build();

        Goal saved = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("My Priority Goal")
                .type("savings")
                .targetAmount(new BigDecimal("3000.00"))

                .status(GoalStatus.IN_PROGRESS)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(nonInProgressPriorityGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.save(any(Goal.class))).thenReturn(saved);

        GoalDTO result = goalsManagementService.createGoalForUser(request);

        assertNotNull(result);
        assertEquals("My Priority Goal", result.getName());
    }

    @Test
    void updateGoalForUser_shouldNotThrowWhenOtherGoalIsPriorityButNotInProgress() {
        // Lines 194-195: anyMatch lambda — other goal has isPriority=true but status != IN_PROGRESS
        // → hasPriorityGoal=false → no DuplicatePriorityGoalException → falls through to financial check
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .monthlyContribution(new BigDecimal("200.00"))
                .isPriority(false) // currently not priority
                .status(GoalStatus.IN_PROGRESS)
                .build();
        // Other goal: isPriority=true but ACHIEVED → sub-conditions fail at getStatus check
        Goal completedPriorityGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .isPriority(true)
                .status(GoalStatus.ACHIEVED) // not IN_PROGRESS → predicate false
                .monthlyContribution(new BigDecimal("100.00"))
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Updated Savings")
                .targetAmount(new BigDecimal("5000.00"))
                .monthlyContribution(new BigDecimal("200.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(true) // wants to become priority
                .build();

        FinancialProfile profile = FinancialProfile.builder()
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID))
                .thenReturn(List.of(existingGoal, completedPriorityGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalDTO result = goalsManagementService.updateGoalForUser(goalId, request);

        assertNotNull(result);
        assertEquals("Updated Savings", result.getName());
        assertEquals(true, result.getIsPriority());
    }

    @Test
    void updateGoalForUser_shouldAccumulateContributionFromOtherGoalsInIncomeCheck() {
        // Line 207: ternary else-branch — a *different* goal in the list uses g.getMonthlyContribution()
        // Total = dto contribution (for goalId) + other goal's stored contribution → exceeds income
        UUID goalId = UUID.randomUUID();
        UUID otherGoalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .monthlyContribution(new BigDecimal("500.00"))
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        // Another goal with its own contribution that will be picked up by the else-branch
        Goal otherGoal = Goal.builder()
                .id(otherGoalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .monthlyContribution(new BigDecimal("4000.00")) // else-branch: uses this stored value
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Savings")
                .targetAmount(new BigDecimal("5000.00"))
                .monthlyContribution(new BigDecimal("1500.00")) // 1500 + 4000 = 5500 > 5000 income
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        FinancialProfile profile = FinancialProfile.builder()
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID))
                .thenReturn(List.of(existingGoal, otherGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));

        assertThrows(CoreThrowHandler.class,
                () -> goalsManagementService.updateGoalForUser(goalId, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Boolean.TRUE.equals false-hit: anyMatch short-circuits on isPriority=false
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createGoalForUser_shouldCreateGoalSuccessfullyWhenExistingGoalIsNotPriority() {
        // Line 116: Boolean.TRUE.equals(g.getIsPriority()) → FALSE (isPriority=false on existing goal)
        // → && short-circuits, getStatus() never evaluated → hasPriorityGoal=false → goal saved
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .name("Emergency Fund")
                .type("savings")
                .targetAmount(new BigDecimal("2000.00"))
                .monthlyContribution(new BigDecimal("300.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(true)
                .build();

        // Existing goal with isPriority=false → Boolean.TRUE.equals returns false → short-circuit
        Goal nonPriorityGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .isPriority(false)
                .targetAmount(new BigDecimal("2000.00"))
                .monthlyContribution(new BigDecimal("100.00"))
                .status(GoalStatus.IN_PROGRESS)
                .build();

        Goal saved = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Emergency Fund")
                .type("savings")
                .targetAmount(new BigDecimal("2000.00"))
                .status(GoalStatus.IN_PROGRESS)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(nonPriorityGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.save(any(Goal.class))).thenReturn(saved);

        GoalDTO result = goalsManagementService.createGoalForUser(request);

        assertNotNull(result);
        assertEquals("Emergency Fund", result.getName());
    }

    @Test
    void updateGoalForUser_shouldNotThrowWhenOtherGoalIsNotPriority() {
        // Line 194: Boolean.TRUE.equals(g.getIsPriority()) → FALSE (other goal has isPriority=false)
        // → && short-circuits before getStatus() → hasPriorityGoal=false → no exception, falls through
        UUID goalId = UUID.randomUUID();
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .questionnaireCompleted(true)
                .build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .monthlyContribution(new BigDecimal("200.00"))
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();
        // Other goal is NOT priority → Boolean.TRUE.equals(false) = false → short-circuits
        Goal nonPriorityOtherGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .monthlyContribution(new BigDecimal("100.00"))
                .build();
        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Updated Savings")
                .targetAmount(new BigDecimal("5000.00"))
                .monthlyContribution(new BigDecimal("200.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(true)
                .build();

        FinancialProfile profile = FinancialProfile.builder()
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID))
                .thenReturn(List.of(existingGoal, nonPriorityOtherGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalDTO result = goalsManagementService.updateGoalForUser(goalId, request);

        assertNotNull(result);
        assertEquals("Updated Savings", result.getName());
        assertEquals(true, result.getIsPriority());
    }

    @Test
    void createGoalForUser_withNonNullCurrentAmount_setsCurrentAmountFromDto() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        GoalRegistrationDTO request = GoalRegistrationDTO.builder()
                .name("Savings")
                .type("savings")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("2500.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        Goal savedGoal = Goal.builder()
                .id(UUID.randomUUID())
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Savings")
                .type("savings")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("2500.00"))
                .monthlyContribution(new BigDecimal("500.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID))
                .thenReturn(Optional.of(FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build()));
        when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);

        GoalDTO result = goalsManagementService.createGoalForUser(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("2500.00"), result.getCurrentAmount());
    }

    @Test
    void updateGoalForUser_withNonNullCurrentAmount_updatesCurrentAmountOnGoal() {
        UUID goalId = UUID.randomUUID();
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        Goal existingGoal = Goal.builder()
                .id(goalId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .type("savings")
                .currentAmount(new BigDecimal("1000.00"))
                .monthlyContribution(new BigDecimal("200.00"))
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .build();

        GoalEditingDTO request = GoalEditingDTO.builder()
                .name("Updated Savings")
                .targetAmount(new BigDecimal("5000.00"))
                .currentAmount(new BigDecimal("3500.00"))
                .monthlyContribution(new BigDecimal("200.00"))
                .targetDate(LocalDate.now(clock).plusMonths(6))
                .isPriority(false)
                .build();

        FinancialProfile profile = FinancialProfile.builder().monthlyIncome(new BigDecimal("5000.00")).build();

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(existingGoal));
        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(profile));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalDTO result = goalsManagementService.updateGoalForUser(goalId, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("3500.00"), result.getCurrentAmount());
    }

    @Test
    void autoAllocateGoalsForUser_whenUserHasNoGoals_returnsEmptyList() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of());

        List<GoalDTO> result = goalsManagementService.autoAllocateGoalsForUser(50);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void autoAllocateGoalsForUser_whenUserHasGoals_allocatesSurplusCorrectly() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        UUID priorityId = UUID.randomUUID();
        UUID otherId1 = UUID.randomUUID();

        Goal priorityGoal = Goal.builder()
                .id(priorityId)
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Priority House")
                .type("property")
                .isPriority(true)
                .status(GoalStatus.IN_PROGRESS)
                .monthlyContribution(BigDecimal.ZERO)
                .build();

        Goal otherGoal = Goal.builder()
                .id(otherId1)
                .userId(SecurityUtils.STATIC_USER_ID)
                .name("Other Car")
                .type("savings")
                .isPriority(false)
                .status(GoalStatus.IN_PROGRESS)
                .monthlyContribution(BigDecimal.ZERO)
                .build();

        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(priorityGoal, otherGoal));

        UUID fpId = UUID.randomUUID();
        FinancialProfile fp = FinancialProfile.builder().id(fpId).monthlyIncome(new BigDecimal("10000.00")).build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("4000.00")).build();

        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GoalDTO> result = goalsManagementService.autoAllocateGoalsForUser(60);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(new BigDecimal("3600.0000"), priorityGoal.getMonthlyContribution());
        assertEquals(new BigDecimal("2400.0000"), otherGoal.getMonthlyContribution());
    }

    @Test
    void autoAllocateIfNeeded_whenProfileNullOrDisabled_doesNotAllocate() {
        UUID userId = SecurityUtils.STATIC_USER_ID;

        // Case A: Profile null
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        goalsManagementService.autoAllocateIfNeeded(userId);
        verify(goalRepository, never()).findAllByUserId(any());
        verify(goalRepository, never()).save(any());

        // Case B: Auto-allocation disabled
        FinancialProfile disabledProfile = FinancialProfile.builder().autoAllocationEnabled(false).build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(disabledProfile));
        goalsManagementService.autoAllocateIfNeeded(userId);
        verify(goalRepository, never()).findAllByUserId(any());
        verify(goalRepository, never()).save(any());
    }

    @Test
    void autoAllocateIfNeeded_whenActiveGoalsLessThanTwoOrNoPriorityGoal_doesNotAllocate() {
        UUID userId = SecurityUtils.STATIC_USER_ID;
        FinancialProfile profile = FinancialProfile.builder()
                .autoAllocationEnabled(true)
                .priorityAllocationPercentage(50)
                .build();
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Case A: Only 1 active goal
        Goal singleGoal = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.IN_PROGRESS).build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(singleGoal));
        goalsManagementService.autoAllocateIfNeeded(userId);
        verify(goalRepository, never()).save(any());
        assertNull(singleGoal.getMonthlyContribution());

        // Case B: 2 active goals but neither is priority
        Goal nonPriority1 = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        Goal nonPriority2 = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(nonPriority1, nonPriority2));
        goalsManagementService.autoAllocateIfNeeded(userId);
        verify(goalRepository, never()).save(any());
        assertNull(nonPriority1.getMonthlyContribution());
        assertNull(nonPriority2.getMonthlyContribution());
    }

    @Test
    void autoAllocateIfNeeded_whenEnabledWithNullPercentage_usesDefaultFiftyPercent() {
        UUID userId = SecurityUtils.STATIC_USER_ID;
        UUID fpId = UUID.randomUUID();
        FinancialProfile profile = FinancialProfile.builder()
                .id(fpId)
                .monthlyIncome(new BigDecimal("10000.00"))
                .autoAllocationEnabled(true)
                .priorityAllocationPercentage(null)
                .build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("2000.00")).build();

        Goal priorityGoal = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.IN_PROGRESS).build();
        Goal otherGoal = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(priorityGoal, otherGoal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        goalsManagementService.autoAllocateIfNeeded(userId);

        // Surplus = 8000. 50% fallback -> priority gets 4000, other gets 4000
        assertEquals(new BigDecimal("4000.0000"), priorityGoal.getMonthlyContribution());
        assertEquals(new BigDecimal("4000.0000"), otherGoal.getMonthlyContribution());
    }

    @Test
    void autoAllocateGoalsForUser_whenSurplusIsZero_doesNotAllocateAndSetsContributionsToZero() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        Goal priorityGoal = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.IN_PROGRESS).build();
        Goal otherGoal = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(priorityGoal, otherGoal));

        UUID fpId = UUID.randomUUID();
        // Income (2000) <= Expenses (3000) -> Surplus = 0
        FinancialProfile fp = FinancialProfile.builder().id(fpId).monthlyIncome(new BigDecimal("2000.00")).build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("3000.00")).build();

        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GoalDTO> result = goalsManagementService.autoAllocateGoalsForUser(50);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, priorityGoal.getMonthlyContribution());
        assertEquals(BigDecimal.ZERO, otherGoal.getMonthlyContribution());
    }

    @Test
    void autoAllocateGoalsForUser_whenNoPriorityGoalExists_doesNotAllocateAndSetsContributionsToZero() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        // Two IN_PROGRESS goals, but neither is marked priority -> priorityGoal == null
        Goal nonPriority1 = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        Goal nonPriority2 = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(nonPriority1, nonPriority2));

        UUID fpId = UUID.randomUUID();
        FinancialProfile fp = FinancialProfile.builder().id(fpId).monthlyIncome(new BigDecimal("10000.00")).build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("2000.00")).build();

        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GoalDTO> result = goalsManagementService.autoAllocateGoalsForUser(50);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, nonPriority1.getMonthlyContribution());
        assertEquals(BigDecimal.ZERO, nonPriority2.getMonthlyContribution());
    }

    @Test
    void autoAllocateGoalsForUser_whenOnlyPriorityGoalExists_otherCountIsZero() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        // Only 1 priority goal, 0 non-priority goals -> otherCount == 0
        Goal priorityGoal = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.IN_PROGRESS).build();
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(List.of(priorityGoal));

        UUID fpId = UUID.randomUUID();
        FinancialProfile fp = FinancialProfile.builder().id(fpId).monthlyIncome(new BigDecimal("10000.00")).build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("5000.00")).build();

        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GoalDTO> result = goalsManagementService.autoAllocateGoalsForUser(60);

        assertNotNull(result);
        // Surplus = 5000 * 60% = 3000
        assertEquals(new BigDecimal("3000.0000"), priorityGoal.getMonthlyContribution());
    }

    @Test
    void autoAllocateGoalsForUser_withAchievedAndInactiveGoals_coversStatusFiltersAndLoop() {
        User user = User.builder().id(SecurityUtils.STATIC_USER_ID).questionnaireCompleted(true).build();
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));

        Goal achievedPriority = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.ACHIEVED).build();
        Goal nullPriority = Goal.builder().id(UUID.randomUUID()).isPriority(null).status(GoalStatus.IN_PROGRESS).build();
        Goal activePriority = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.IN_PROGRESS).build();
        Goal activeOther = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        Goal achievedOther = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.ACHIEVED).build();

        // Placing achievedPriority and nullPriority BEFORE activePriority ensures findFirst & filter evaluate all branch combinations
        when(goalRepository.findAllByUserId(SecurityUtils.STATIC_USER_ID))
                .thenReturn(List.of(achievedPriority, nullPriority, activePriority, activeOther, achievedOther));

        UUID fpId = UUID.randomUUID();
        FinancialProfile fp = FinancialProfile.builder().id(fpId).monthlyIncome(new BigDecimal("10000.00")).build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("2000.00")).build();

        when(financialProfileRepository.findByUserId(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GoalDTO> result = goalsManagementService.autoAllocateGoalsForUser(50);

        assertNotNull(result);
        // Active priority gets 4000, active other & nullPriority (both non-priority) get 2000 each
        assertEquals(new BigDecimal("4000.0000"), activePriority.getMonthlyContribution());
        assertEquals(new BigDecimal("2000.0000"), activeOther.getMonthlyContribution());
    }

    @Test
    void autoAllocateIfNeeded_whenPercentageIsNotNull_usesConfiguredPercentage() {
        UUID userId = SecurityUtils.STATIC_USER_ID;
        UUID fpId = UUID.randomUUID();
        // Configured percentage = 70 (non-null)
        FinancialProfile profile = FinancialProfile.builder()
                .id(fpId)
                .monthlyIncome(new BigDecimal("10000.00"))
                .autoAllocationEnabled(true)
                .priorityAllocationPercentage(70)
                .build();
        Expense exp = Expense.builder().totalExpenses(new BigDecimal("0.00")).build();

        Goal achievedPriority = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.ACHIEVED).build();
        Goal activePriority = Goal.builder().id(UUID.randomUUID()).isPriority(true).status(GoalStatus.IN_PROGRESS).build();
        Goal otherGoal = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.IN_PROGRESS).build();
        Goal achievedGoal = Goal.builder().id(UUID.randomUUID()).isPriority(false).status(GoalStatus.ACHIEVED).build();

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(expenseRepository.findByFinancialProfileId(fpId)).thenReturn(Optional.of(exp));
        // Placing achievedPriority BEFORE activePriority ensures anyMatch evaluates isPriority=true with status=ACHIEVED (false branch of status check)
        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(achievedPriority, activePriority, otherGoal, achievedGoal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        goalsManagementService.autoAllocateIfNeeded(userId);

        // Surplus = 10000. 70% to priority -> 7000, 30% to other -> 3000
        assertEquals(new BigDecimal("7000.0000"), activePriority.getMonthlyContribution());
        assertEquals(new BigDecimal("3000.0000"), otherGoal.getMonthlyContribution());
    }
}


