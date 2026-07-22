package com.indivaragroup.jdt17wms.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false )
public class RegisterDTO {
    @JsonProperty("name")
    @NotBlank(message = "name is Required")
    private String registerRequestName;
    @JsonProperty("email")
    @NotBlank(message = "Email is Required")
    private String registerRequestEmail;
    @JsonProperty("password")
    @NotBlank(message = "Password is Required")
    @Size(min = 8,max = 72)
    private String registerRequestPassword;
}
