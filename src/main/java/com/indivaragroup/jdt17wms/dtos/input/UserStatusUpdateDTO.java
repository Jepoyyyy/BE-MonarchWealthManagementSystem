package com.indivaragroup.jdt17wms.dtos.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class UserStatusUpdateDTO {
    @NotNull(message = "Status cannot be null")
    @Pattern(regexp = "^(active|disabled)$", message = "Invalid status value")
    private String status;
}
