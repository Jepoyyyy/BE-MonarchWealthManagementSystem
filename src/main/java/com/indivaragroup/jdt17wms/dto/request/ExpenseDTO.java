package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal housing;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal food;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal transport;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal utilities;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal healthcare;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal entertainment;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal insurance;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal other;

    private UUID id;

    @JsonProperty("monthly_income")
    private BigDecimal monthlyIncome;

    @JsonProperty("total_expenses")
    private BigDecimal totalExpenses;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant updatedAt;
}
