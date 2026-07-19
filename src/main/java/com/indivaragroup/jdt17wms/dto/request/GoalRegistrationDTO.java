package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalRegistrationDTO {
    @NotBlank(message = "Must not be blank")
    private String name;

    @NotBlank(message = "Must not be blank")
    @Pattern(regexp = "^(savings|vacation|vehicle|property|retirement|custom)$", message = "Invalid goal type")
    private String type;

    @JsonProperty("target_amount")
    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", message = "Must not be negative")
    @DecimalMax(value = "100000000.00", message = "Target amount exceeds maximum allowed")
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
    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    @JsonProperty("is_priority")
    @NotNull(message = "Must not be null")
    private Boolean isPriority;

  @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
    private String notes;
}
