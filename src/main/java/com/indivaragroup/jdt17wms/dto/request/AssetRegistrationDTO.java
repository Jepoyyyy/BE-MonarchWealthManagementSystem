package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetRegistrationDTO {
    @JsonProperty("product_id")
    @NotNull(message = "Must not be null")
    private UUID productId;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Must be at least positive")
    private BigDecimal units;

    @NotNull(message = "Must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Must be at least positive")
    private BigDecimal amount;

    @JsonProperty("purchase_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "Must not be null")
    private LocalDateTime purchaseDate;

    private Integer tenor;

    private String platform;

    private String notes;
}
