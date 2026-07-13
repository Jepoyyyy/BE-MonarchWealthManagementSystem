package com.indivaragroup.jdt17wms.dto.response.auth;

import com.indivaragroup.jdt17wms.dto.response.UserDTO;
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
public class AuthSuccessDTO {
    private Boolean success;
    private String message;
    private String accessToken;
    private Integer expiresIn;
    private String refreshToken;
    private Integer refreshExpiresIn;
    private UserDTO user;

}
