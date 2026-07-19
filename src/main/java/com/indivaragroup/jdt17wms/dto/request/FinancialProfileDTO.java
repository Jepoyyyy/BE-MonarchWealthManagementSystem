package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialProfileDTO {

    @JsonProperty("monthly_income")
    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal monthlyIncome;

    @JsonProperty("expense")
    @NotNull(message = "Must not be null")
    @Valid
    private ExpenseDTO expenseDTO;

    @JsonProperty("auto_allocation_enabled")
    private Boolean autoAllocationEnabled;

    @JsonProperty("priority_allocation_percentage")
    private Integer priorityAllocationPercentage;
}
