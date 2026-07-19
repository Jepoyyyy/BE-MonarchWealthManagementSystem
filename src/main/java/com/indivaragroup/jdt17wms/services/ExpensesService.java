package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.aspects.RiskProfileAssessmentRequired;
import com.indivaragroup.jdt17wms.dto.request.FinancialProfileDTO;
import com.indivaragroup.jdt17wms.dto.request.ExpenseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Expense;
import com.indivaragroup.jdt17wms.models.FinancialProfile;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ExpensesService {

    private final FinancialProfileRepository financialProfileRepository;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;

    public ExpensesService(FinancialProfileRepository financialProfileRepository,
                           ExpenseRepository expenseRepository,
                           Clock clock) {
        this.financialProfileRepository = financialProfileRepository;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
    }

    @RiskProfileAssessmentRequired
    public ExpenseDTO getFinancesForUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        FinancialProfile fp = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.FINANCIAL_PROFILE_NOT_FOUND));

        Expense expense = expenseRepository.findByFinancialProfileId(fp.getId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.FINANCIAL_PROFILE_NOT_FOUND));

        return mapToDTO(expense);
    }

    @RiskProfileAssessmentRequired
    @Transactional
    public ExpenseDTO updateFinancesForUser(FinancialProfileDTO dto) {
        UUID userId = SecurityUtils.getCurrentUserId();
        FinancialProfile fp = financialProfileRepository.findByUserId(userId)
                .orElse(null);

        boolean isNew = false;
        if (fp == null) {
            fp = new FinancialProfile();
            fp.setUserId(userId);
            isNew = true;
        }

        fp.setMonthlyIncome(dto.getMonthlyIncome());
        if (dto.getAutoAllocationEnabled() != null) {
            fp.setAutoAllocationEnabled(dto.getAutoAllocationEnabled());
        }
        if (dto.getPriorityAllocationPercentage() != null) {
            fp.setPriorityAllocationPercentage(dto.getPriorityAllocationPercentage());
        }
        fp.setUpdatedAt(Instant.now(clock));
        fp = financialProfileRepository.save(fp);

        ExpenseDTO expenseDTO = dto.getExpenseDTO();
        BigDecimal totalExpenses = expenseDTO.getHousing()
                .add(expenseDTO.getFood())
                .add(expenseDTO.getTransport())
                .add(expenseDTO.getUtilities())
                .add(expenseDTO.getHealthcare())
                .add(expenseDTO.getEntertainment())
                .add(expenseDTO.getInsurance())
                .add(expenseDTO.getOther());

        Expense expense = null;
        if (!isNew) {
            expense = expenseRepository.findByFinancialProfileId(fp.getId()).orElse(null);
        }

        if (expense == null) {
            expense = new Expense();
            expense.setFinancialProfile(fp);
            expense.setCreatedAt(Instant.now(clock));
        }

        expense.setHousing(expenseDTO.getHousing());
        expense.setFood(expenseDTO.getFood());
        expense.setTransport(expenseDTO.getTransport());
        expense.setUtilities(expenseDTO.getUtilities());
        expense.setHealthcare(expenseDTO.getHealthcare());
        expense.setEntertainment(expenseDTO.getEntertainment());
        expense.setInsurance(expenseDTO.getInsurance());
        expense.setOther(expenseDTO.getOther());
        expense.setTotalExpenses(totalExpenses);
        expense.setUpdatedAt(Instant.now(clock));

        Expense savedExpense = expenseRepository.save(expense);
        return mapToDTO(savedExpense);
    }

    private ExpenseDTO mapToDTO(Expense expense) {
        if (expense == null) {
            return null;
        }
        return ExpenseDTO.builder()
                .id(expense.getId())
                .monthlyIncome(expense.getFinancialProfile() != null ? expense.getFinancialProfile().getMonthlyIncome() : null)
                .housing(expense.getHousing())
                .food(expense.getFood())
                .transport(expense.getTransport())
                .utilities(expense.getUtilities())
                .healthcare(expense.getHealthcare())
                .entertainment(expense.getEntertainment())
                .insurance(expense.getInsurance())
                .other(expense.getOther())
                .totalExpenses(expense.getTotalExpenses())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
