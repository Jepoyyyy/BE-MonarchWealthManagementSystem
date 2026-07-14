package com.indivaragroup.jdt17wms.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.request.AuthDTO;
import com.indivaragroup.jdt17wms.dto.request.RefreshTokenDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.exceptions.BadRequestException;
import com.indivaragroup.jdt17wms.exceptions.ConflictException;
import com.indivaragroup.jdt17wms.exceptions.ValidationException;
import com.indivaragroup.jdt17wms.services.AuthService;
import com.indivaragroup.jdt17wms.services.JwtService;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private final UserDTO mockUser = UserDTO.builder()
            .id(UUID.randomUUID())
            .name("Test User")
            .email("test@example.com")
            .isAdmin(false)
            .questionnaireCompleted(false)
            .build();

    // ───── LOGIN ─────

    @Test
    void login_withValidCredentials_shouldReturn200() throws Exception {
        AuthSuccessDTO mockResponse = AuthSuccessDTO.builder()
                .success(true)
                .message("Login successful")
                .accessToken("eyJhbGciOiJIUzI1NiJ9.test")
                .expiresIn(900)
                .user(mockUser)
                .build();

        when(authService.login(any(AuthDTO.class))).thenReturn(mockResponse);

        String body = objectMapper.writeValueAsString(new AuthDTO("Test", "test@example.com", "Test1234!"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiJ9.test"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void login_withNullBody_shouldReturn400() throws Exception {
        when(authService.login(any())).thenThrow(new BadRequestException("Request body is required"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidationError_shouldReturn400WithDetails() throws Exception {
        List<ValidationErrorDetailDTO> details = new ArrayList<>();
        details.add(new ValidationErrorDetailDTO("email", "Email is required", "ERR-001"));
        when(authService.login(any(AuthDTO.class))).thenThrow(new ValidationException(details, "VALIDATION"));

        String body = objectMapper.writeValueAsString(new AuthDTO(null, null, null));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid field values"))
                .andExpect(jsonPath("$.type").value("ERR-VALIDATION"))
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }

    // ───── REGISTER ─────

    @Test
    void register_withValidData_shouldReturn200() throws Exception {
        AuthSuccessDTO mockResponse = AuthSuccessDTO.builder()
                .success(true)
                .message("Registration successful")
                .accessToken("eyJhbGciOiJIUzI1NiJ9.test-register")
                .expiresIn(900)
                .user(mockUser)
                .build();

        when(authService.register(any(AuthDTO.class))).thenReturn(mockResponse);

        String body = objectMapper.writeValueAsString(new AuthDTO("Test User", "test@example.com", "Test1234!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiJ9.test-register"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void register_withInvalidData_shouldReturn400WithValidationError() throws Exception {
        List<ValidationErrorDetailDTO> details = new ArrayList<>();
        details.add(new ValidationErrorDetailDTO("email", "Invalid email format", "ERR-002"));

        when(authService.register(any(AuthDTO.class))).thenThrow(new ValidationException(details, "VALIDATION"));

        String body = objectMapper.writeValueAsString(new AuthDTO("Test User", "invalid-email", "Test1234!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid field values"))
                .andExpect(jsonPath("$.type").value("ERR-VALIDATION"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.details[0].field").value("email"))
                .andExpect(jsonPath("$.details[0].reason").value("Invalid email format"))
                .andExpect(jsonPath("$.details[0].type").value("ERR-002"));
    }

    @Test
    void register_withDuplicateEmail_shouldReturn409() throws Exception {
        when(authService.register(any(AuthDTO.class))).thenThrow(new ConflictException("Email already in use"));

        String body = objectMapper.writeValueAsString(new AuthDTO("Test User", "duplicate@example.com", "Test1234!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use"))
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void register_withNullBody_shouldReturn400() throws Exception {
        when(authService.register(any())).thenThrow(new BadRequestException("Invalid Request Body"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ───── LOGOUT ─────

    @Test
    void logout_withValidToken_shouldReturn200() throws Exception {
        when(jwtService.getEmailFromToken(anyString())).thenReturn("test@example.com");
        when(jwtService.getUserIdFromToken(anyString())).thenReturn(UUID.randomUUID());
        when(authService.logout(eq("test@example.com"), any())).thenReturn(
                LogoutSuccessDTO.builder().success(true).message("Logout successful").build());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer eyJhbG...test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void logout_withoutToken_shouldReturn200AsAnonymous() throws Exception {
        when(authService.logout(null, null)).thenReturn(
                LogoutSuccessDTO.builder().success(true).message("Logout successful").build());

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_withInvalidToken_shouldStillReturn200() throws Exception {
        when(jwtService.getEmailFromToken(anyString())).thenThrow(new RuntimeException("bad token"));
        when(authService.logout(null, null)).thenReturn(
                LogoutSuccessDTO.builder().success(true).message("Logout successful").build());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ───── REFRESH TOKEN ─────

    @Test
    void refresh_withValidToken_shouldReturn200() throws Exception {
        RefreshTokenSuccessDTO mockResponse = RefreshTokenSuccessDTO.builder()
                .success(true)
                .message("Token refreshed successfully")
                .accessToken("new-access-token")
                .expiresIn(900)
                .refreshToken("new-refresh-token")
                .refreshExpiresIn(604800)
                .build();

        when(authService.refreshToken(anyString())).thenReturn(mockResponse);

        String body = objectMapper.writeValueAsString(new RefreshTokenDTO("valid-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800));
    }

    @Test
    void refresh_withNullBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Refresh token is required"));
    }

    @Test
    void refresh_withEmptyToken_shouldReturn400() throws Exception {
        String body = objectMapper.writeValueAsString(new RefreshTokenDTO(""));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Refresh token is required"));
    }

    @Test
    void refresh_withInvalidToken_shouldReturn401() throws Exception {
        when(authService.refreshToken("invalid")).thenThrow(new com.indivaragroup.jdt17wms.exceptions.InvalidTokenException("Invalid refresh token"));

        String body = objectMapper.writeValueAsString(new RefreshTokenDTO("invalid"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withExpiredToken_shouldReturn401() throws Exception {
        when(authService.refreshToken("expired")).thenThrow(new com.indivaragroup.jdt17wms.exceptions.UnauthorizedException("Refresh token expired"));

        String body = objectMapper.writeValueAsString(new RefreshTokenDTO("expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
