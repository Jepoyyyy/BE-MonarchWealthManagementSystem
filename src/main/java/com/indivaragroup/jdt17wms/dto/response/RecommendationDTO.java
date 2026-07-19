package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indivaragroup.jdt17wms.models.enums.RecommendationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    private String priority;

    private String category;

    private String title;

    private String reason;

    @JsonProperty("product_id")
    private UUID productId;

    private ProductResponseDTO product;

    @JsonProperty("suggested_amount")
    private BigDecimal suggestedAmount;

    @JsonProperty("goal_id")
    private UUID goalId;

    private GoalDTO goal;

    @JsonProperty("recommended_allocation")
    private Map<String, Object> recommendedAllocation;

    private RecommendationStatus status;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant updatedAt;

    @JsonProperty("resolved_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant resolvedAt;

    @JsonProperty("resolved_by_asset_id")
    private UUID resolvedByAssetId;
}
