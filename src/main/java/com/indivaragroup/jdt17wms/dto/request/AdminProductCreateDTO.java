package com.indivaragroup.jdt17wms.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductCreateDTO {
    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotBlank
    private String issuer;

    @NotBlank
    private String type;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer riskLevel;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal annualReturn;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal minInvestment;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal currentPrice;

    @NotBlank
    private String description;

    private String tenor;

    @NotNull
    @Min(1)
    private Integer lotSize;

    @NotNull
    private Boolean isFractionalAllowed;

    @NotNull
    private Boolean visible;
}
