package com.indivaragroup.jdt17wms.dto.response;

import com.indivaragroup.jdt17wms.models.Product;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@EqualsAndHashCode
public class ProductResponseDTO {
    private UUID id;
    private String code;
    private String name;
    private String issuer;
    private String type;
    private Integer riskLevel;
    private BigDecimal annualReturn;
    private BigDecimal minInvestment;
    private BigDecimal currentPrice;
    private Boolean visible;
    private String description;
    private String tenor;
    private Integer lotSize;
    private Boolean isFractionalAllowed;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponseDTO fromEntity(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .issuer(product.getIssuer())
                .type(product.getType())
                .riskLevel(product.getRiskLevel())
                .annualReturn(product.getAnnualReturn())
                .minInvestment(product.getMinInvestment())
                .currentPrice(product.getCurrentPrice())
                .visible(product.getVisible())
                .description(product.getDescription())
                .tenor(product.getTenor())
                .lotSize(product.getLotSize())
                .isFractionalAllowed(product.getIsFractionalAllowed())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
