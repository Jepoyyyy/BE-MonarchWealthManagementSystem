package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.constants.JwtConstants;
import com.indivaragroup.jdt17wms.dto.request.BearerHeaderDTO;
import com.indivaragroup.jdt17wms.dto.request.LoginDTO;
import com.indivaragroup.jdt17wms.dto.request.RefreshTokenDTO;
import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.BASE_AUTH_ROUTE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(ApiPath.LOGIN_ROUTE)
    public ApiResponse<AuthSuccessDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResponse.success(ApiSuccess.LOGIN, authService.login(dto));
    }

    @PostMapping(ApiPath.REGISTER_ROUTE)
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(ApiSuccess.REGISTER, null));
    }

    @PostMapping(ApiPath.LOGOUT_ROUTE)
    public ApiResponse<LogoutSuccessDTO> logout(@RequestHeader(value = JwtConstants.HEADER_AUTHORIZATION, required = false) BearerHeaderDTO headerDto) {
        return ApiResponse.success(ApiSuccess.LOGOUT, authService.logout(headerDto));
    }

    @PostMapping(ApiPath.REFRESH_TOKEN_ROUTE)
    public ApiResponse<RefreshTokenSuccessDTO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return ApiResponse.success(ApiSuccess.REFRESH_TOKEN,
                authService.refreshToken(dto.getRefreshToken()));
    }
}
