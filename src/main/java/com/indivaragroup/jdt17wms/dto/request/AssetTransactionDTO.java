package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetTransactionDTO {

    @NotNull(message = "Invalid transaction type. Must be one of: BUY, SELL")
    @JsonProperty("action")
    private TransactionAction action;

    @JsonProperty("units")
    @DecimalMin(value = "0.0", inclusive = false, message = "Units must be positive")
    private BigDecimal units;

    @JsonProperty("amount")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
    private BigDecimal amount;

    @JsonProperty("transaction_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;

    private String notes;

    @JsonProperty("goal_id")
    private UUID goalId;
}
