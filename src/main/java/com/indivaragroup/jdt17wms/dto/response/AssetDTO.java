package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AssetDTO {
    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("product_id")
    private UUID productId;

    @JsonProperty("goal_id")
    private UUID goalId;

    private BigDecimal units;

    private BigDecimal amount;

    @JsonProperty("name")
    private String assetsName;

    @JsonProperty("issuer")
    private String assetsIssuer;

    @JsonProperty("type")
    private String assetsType;

    @JsonProperty("current_value")
    private BigDecimal currentValue;

    @JsonProperty("tenor")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate tenor;

    @JsonProperty("purchase_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant purchaseDate;

    private String platform;

    private String notes;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant updatedAt;
}
