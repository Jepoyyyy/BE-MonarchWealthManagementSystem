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
public class
AssetsPnLResponseDTO {
    private UUID assetId;
    private UUID productId;
    private String productName;
    private String productType;
    private BigDecimal units;
    private BigDecimal currentValue;
    @JsonProperty("avg_price")
    private BigDecimal avgPrice;
    @JsonProperty("potential_pnl")
    private BigDecimal potentialPnL;
    @JsonProperty("potential_pnl_percent")
    private BigDecimal potentialPnLPercent;
    @JsonProperty("realized_pnl")
    private BigDecimal realizedPnL;
    @JsonProperty("realized_pnl_percent")
    private BigDecimal realizedPnLPercent;
}
