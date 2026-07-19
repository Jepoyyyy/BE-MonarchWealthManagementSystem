package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indivaragroup.jdt17wms.models.enums.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalDTO {
    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    private String name;
    
    private String type;

    @JsonProperty("target_amount")
    private BigDecimal targetAmount;

    @JsonProperty("monthly_contribution")
    private BigDecimal monthlyContribution;

    @JsonProperty("current_amount")
    private BigDecimal currentAmount;

    @JsonProperty("target_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @JsonProperty("is_priority")
    private Boolean isPriority;

    private String notes;

    private GoalStatus status;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant updatedAt;

    @JsonProperty("projected-date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate projectedDate;

    @JsonProperty("recommended-contribution")
    private BigDecimal recommendedContribution;
}
