package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDTO {
    private BigDecimal aum;

    @JsonProperty("user_count")
    private Long userCount;

    @JsonProperty("active_user_count")
    private Long activeUserCount;

    @JsonProperty("product_count")
    private Long productCount;

    @JsonProperty("active_product_count")
    private Long activeProductCount;

    @JsonProperty("total_audit_events")
    private Long totalAuditEvents;

    @JsonProperty("risk_profiles")
    private RiskProfilesDTO riskProfiles;

    @JsonProperty("aum_trend")
    private List<AumTrendDTO> aumTrend;

}
