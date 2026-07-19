package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalEditingDTO {
    @NotBlank(message = "Must not be blank")
    private String name;

    @JsonProperty("target_amount")
    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal targetAmount;

    @JsonProperty("monthly_contribution")
    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal monthlyContribution;

    @JsonProperty("current_amount")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    private BigDecimal currentAmount;

    @JsonProperty("target_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Must not be null")
    private LocalDate targetDate;

    @JsonProperty("is_priority")
    private Boolean isPriority;

    private String notes;
}
