package com.indivaragroup.jdt17wms.dto.request;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductUpdateDTO {
    private String name;
    private String issuer;
    private String type;
    private Integer riskLevel;
    private BigDecimal annualReturn;
    private BigDecimal minInvestment;
    private BigDecimal currentPrice;
    private String description;
    private String tenor;
    private Integer lotSize;
    private Boolean isFractionalAllowed;
    private Boolean visible;
}
