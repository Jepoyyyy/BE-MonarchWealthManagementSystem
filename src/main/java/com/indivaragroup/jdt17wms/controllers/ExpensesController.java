package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.dto.request.FinancialProfileDTO;
import com.indivaragroup.jdt17wms.dto.request.ExpenseDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.ExpensesService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.BASE_USER_PATH + "/finances")
public class ExpensesController {

    private final ExpensesService expensesService;

    public ExpensesController(ExpensesService expensesService) {
        this.expensesService = expensesService;
    }

    @GetMapping
    public ApiResponse<ExpenseDTO> getFinances() {
        return ApiResponse.success(ApiSuccess.FINANCES_FETCHED,
                expensesService.getFinancesForUser());
    }

    @PutMapping
    @AuditLogged(action = "UPDATE_FINANCES", category = "FINANCES")
    public ApiResponse<ExpenseDTO> updateFinances(@Valid @RequestBody FinancialProfileDTO financialProfileDTO) {
        return ApiResponse.success(ApiSuccess.FINANCES_UPDATED,
                expensesService.updateFinancesForUser(financialProfileDTO));
    }
}
