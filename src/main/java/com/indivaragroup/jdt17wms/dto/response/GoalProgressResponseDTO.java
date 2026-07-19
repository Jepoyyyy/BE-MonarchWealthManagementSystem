package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalProgressResponseDTO {
    @JsonProperty("goal_id")
    private UUID goalId;
    
    @JsonProperty("goal_name")
    private String goalName;
    
    @JsonProperty("goal_type")
    private String goalType;
    
    @JsonProperty("target_amount")
    private BigDecimal targetAmount;
    
    @JsonProperty("current_saved")
    private BigDecimal currentSaved;
    
    @JsonProperty("monthly_contribution")
    private BigDecimal monthlyContribution;
    
    @JsonProperty("assigned_assets_count")
    private Integer assignedAssetsCount;
    
    @JsonProperty("total_potential_pnl")
    private BigDecimal totalPotentialPnL;
    
    @JsonProperty("total_potential_pnl_percent")
    private BigDecimal totalPotentialPnLPercent;
    
    @JsonProperty("avg_monthly_growth")
    private BigDecimal avgMonthlyGrowth;
    
    @JsonProperty("projected_eta_months")
    private Integer projectedEtaMonths;
    
    @JsonProperty("is_priority")
    private Boolean isPriority;
}
