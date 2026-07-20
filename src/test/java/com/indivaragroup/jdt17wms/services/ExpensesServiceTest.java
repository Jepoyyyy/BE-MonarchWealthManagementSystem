package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.ExpenseDTO;
import com.indivaragroup.jdt17wms.dto.request.FinancialProfileDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Expense;
import com.indivaragroup.jdt17wms.models.FinancialProfile;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpensesServiceTest {

    @Mock
    private FinancialProfileRepository financialProfileRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GoalsManagementService goalsManagementService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC);

    private ExpensesService expensesService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        expensesService = new ExpensesService(financialProfileRepository, expenseRepository,goalsManagementService, clock);
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

    @Test
    void getFinancesForUser_Success() {
        UUID profileId = UUID.randomUUID();
        FinancialProfile fp = FinancialProfile.builder()
                .id(profileId)
                .userId(userId)
                .monthlyIncome(BigDecimal.valueOf(20000))
                .build();

        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .financialProfile(fp)
                .housing(BigDecimal.valueOf(5000))
                .food(BigDecimal.valueOf(3000))
                .transport(BigDecimal.valueOf(1500))
                .utilities(BigDecimal.valueOf(1000))
                .healthcare(BigDecimal.valueOf(800))
                .entertainment(BigDecimal.valueOf(1200))
                .insurance(BigDecimal.valueOf(1000))
                .other(BigDecimal.valueOf(500))
                .totalExpenses(BigDecimal.valueOf(14000))
                .build();

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(fp));
        when(expenseRepository.findByFinancialProfileId(profileId)).thenReturn(Optional.of(expense));

        ExpenseDTO result = expensesService.getFinancesForUser();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(14000), result.getTotalExpenses());
        assertEquals(BigDecimal.valueOf(20000), result.getMonthlyIncome());
    }

    @Test
    void getFinancesForUser_NotFound() {
        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> expensesService.getFinancesForUser());
    }

    @Test
    void updateFinancesForUser_CreateNew() {
        ExpenseDTO expDto = ExpenseDTO.builder()
                .housing(BigDecimal.valueOf(6000))
                .food(BigDecimal.valueOf(3500))
                .transport(BigDecimal.valueOf(2000))
                .utilities(BigDecimal.valueOf(1200))
                .healthcare(BigDecimal.valueOf(1000))
                .entertainment(BigDecimal.valueOf(1500))
                .insurance(BigDecimal.valueOf(1500))
                .other(BigDecimal.valueOf(800))
                .build();

        FinancialProfileDTO dto = FinancialProfileDTO.builder()
                .monthlyIncome(BigDecimal.valueOf(25000))
                .expenseDTO(expDto)
                .build();

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(financialProfileRepository.save(any(FinancialProfile.class))).thenAnswer(i -> {
            FinancialProfile fp = i.getArgument(0);
            fp.setId(UUID.randomUUID());
            return fp;
        });
        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        ExpenseDTO result = expensesService.updateFinancesForUser(dto);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(17500), result.getTotalExpenses());
        assertEquals(BigDecimal.valueOf(25000), result.getMonthlyIncome());
        verify(financialProfileRepository, times(1)).save(any(FinancialProfile.class));
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(goalsManagementService).autoAllocateIfNeeded(userId);
    }

    @Test
    void updateFinancesForUser_UpdateExisting() {
        UUID profileId = UUID.randomUUID();
        FinancialProfile existingFp = FinancialProfile.builder()
                .id(profileId)
                .userId(userId)
                .monthlyIncome(BigDecimal.valueOf(20000))
                .build();

        Expense existingExp = Expense.builder()
                .id(UUID.randomUUID())
                .financialProfile(existingFp)
                .housing(BigDecimal.valueOf(5000))
                .food(BigDecimal.valueOf(3000))
                .transport(BigDecimal.valueOf(1500))
                .utilities(BigDecimal.valueOf(1000))
                .healthcare(BigDecimal.valueOf(800))
                .entertainment(BigDecimal.valueOf(1200))
                .insurance(BigDecimal.valueOf(1000))
                .other(BigDecimal.valueOf(500))
                .totalExpenses(BigDecimal.valueOf(14000))
                .build();

        ExpenseDTO expDto = ExpenseDTO.builder()
                .housing(BigDecimal.valueOf(6000))
                .food(BigDecimal.valueOf(3500))
                .transport(BigDecimal.valueOf(2000))
                .utilities(BigDecimal.valueOf(1200))
                .healthcare(BigDecimal.valueOf(1000))
                .entertainment(BigDecimal.valueOf(1500))
                .insurance(BigDecimal.valueOf(1500))
                .other(BigDecimal.valueOf(800))
                .build();

        FinancialProfileDTO dto = FinancialProfileDTO.builder()
                .monthlyIncome(BigDecimal.valueOf(25000))
                .expenseDTO(expDto)
                .build();

        when(financialProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingFp));
        when(financialProfileRepository.save(any(FinancialProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(expenseRepository.findByFinancialProfileId(profileId)).thenReturn(Optional.of(existingExp));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        ExpenseDTO result = expensesService.updateFinancesForUser(dto);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(17500), result.getTotalExpenses());
        assertEquals(BigDecimal.valueOf(25000), result.getMonthlyIncome());
        verify(financialProfileRepository, times(1)).save(any(FinancialProfile.class));
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(goalsManagementService).autoAllocateIfNeeded(userId);
    }
}
