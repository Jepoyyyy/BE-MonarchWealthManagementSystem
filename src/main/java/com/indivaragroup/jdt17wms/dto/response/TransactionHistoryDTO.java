package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryDTO {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("product_id")
    private UUID productId;

    @JsonProperty("asset_id")
    private UUID assetId;

    @JsonProperty("goal_id")
    private UUID goalId;

    private TransactionAction action;

    @JsonProperty("price_per_unit")
    private BigDecimal pricePerUnit;

    private BigDecimal units;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("transaction_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant transactionDate;

    private String notes;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdAt;
}
