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
public class HealthDTO {
    private Integer totalScore;
    private Integer maxScore;
    private String status;

    @JsonProperty("portfolio-value")
    private BigDecimal portfolioValue;

    @JsonProperty("available-surplus")
    private BigDecimal availableSurplus;

    private List<ComponentDTO> components;

}
