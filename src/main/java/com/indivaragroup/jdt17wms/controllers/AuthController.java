package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.LoginDTO;
import com.indivaragroup.jdt17wms.dto.request.RefreshTokenDTO;
import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiError;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.services.AuthService;
import com.indivaragroup.jdt17wms.services.JwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_AUTH_PATH)
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping(ApiPath.LOGIN_PATH)
    public AuthSuccessDTO login(@Valid @RequestBody(required = false) LoginDTO dto) {
        if (dto == null){
            throw new CoreThrowHandler(ApiError.INVALID_REQUEST_BODY);
        }
        return authService.login(dto);
    }

    @PostMapping(ApiPath.REGISTER_PATH)
    public AuthSuccessDTO register(@Valid @RequestBody(required = false) RegisterDTO dto) {
        if (dto == null) {
            throw new CoreThrowHandler(ApiError.INVALID_REQUEST_BODY);
        }
        return authService.register(dto);
    }


    @PostMapping(ApiPath.LOGOUT_PATH)
    public LogoutSuccessDTO logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        String email = null;
        UUID userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                email = jwtService.getEmailFromToken(token);
                userId = jwtService.getUserIdFromToken(token);
            } catch (Exception e) {
                log.debug("Logout token parse failed {}",e.getMessage());
            }
        }
        return authService.logout(email, userId);
    }

    //refresh
    @PostMapping(ApiPath.REFRESH_TOKEN_PATH)
    public RefreshTokenSuccessDTO refresh(@RequestBody(required = false) RefreshTokenDTO dto) {
        if (dto == null || dto.getRefreshToken() == null || dto.getRefreshToken().trim().isEmpty()) {
            throw new CoreThrowHandler(ApiError.REQUIRED_REFRESH_TOKEN);
        }
        return authService.refreshToken(dto.getRefreshToken());
    }
}
