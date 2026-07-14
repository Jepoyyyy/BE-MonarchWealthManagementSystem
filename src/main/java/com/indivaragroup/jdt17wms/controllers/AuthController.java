package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.AuthDTO;
import com.indivaragroup.jdt17wms.dto.request.RefreshTokenDTO;
import com.indivaragroup.jdt17wms.dto.response.RestApiPath;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.exceptions.BadRequestException;
import com.indivaragroup.jdt17wms.services.AuthService;
import com.indivaragroup.jdt17wms.services.JwtService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(RestApiPath.BASE_AUTH_PATH)
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping(RestApiPath.LOGIN_PATH)
    public AuthSuccessDTO login(@RequestBody(required = false) AuthDTO dto) {
        if (dto == null){
            throw new BadRequestException("Invalid Request Body");
        }
        return authService.login(dto);
    }

    @PostMapping(RestApiPath.REGISTER_PATH)
    public AuthSuccessDTO register(@RequestBody(required = false) AuthDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Invalid Request Body");
        }
        return authService.register(dto);
    }


    @PostMapping(RestApiPath.LOGOUT_PATH)
    public LogoutSuccessDTO logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        String email = null;
        UUID userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                email = jwtService.getEmailFromToken(token);
                userId = jwtService.getUserIdFromToken(token);
            } catch (Exception ignored) {}
        }
        return authService.logout(email, userId);
    }

    //refresh
    @PostMapping(RestApiPath.REFRESH_TOKEN_PATH)
    public RefreshTokenSuccessDTO refresh(@RequestBody(required = false) RefreshTokenDTO dto) {
        if (dto == null || dto.getRefreshToken() == null || dto.getRefreshToken().trim().isEmpty()) {
            throw new BadRequestException("Refresh token is required");
        }
        return authService.refreshToken(dto.getRefreshToken());
    }
}
