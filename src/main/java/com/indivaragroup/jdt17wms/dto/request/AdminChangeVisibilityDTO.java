package com.indivaragroup.jdt17wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminChangeVisibilityDTO {
    @NotNull(message = "visibility cannot be null")
    private Boolean visibility;
}
