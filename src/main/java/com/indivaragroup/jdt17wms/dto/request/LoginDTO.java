package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@JsonIgnoreProperties(ignoreUnknown = false )
public class LoginDTO {
    @JsonProperty("email")
    @NotBlank(message = "Email is Required")
    private String loginRequestEmail;

    @JsonProperty("password")
    @NotBlank(message = "Password is Required")
    @Size(min = 8,max = 72)
    private String loginRequestPassword;
}
