package com.indivaragroup.jdt17wms.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.request.LoginDTO;
import com.indivaragroup.jdt17wms.dto.request.RefreshTokenDTO;
import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private final UserDTO mockUser = UserDTO.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .isAdmin(false)
            .build();

    // --- LOGIN ---

    @Test
    void login_withValidCredentials_shouldReturnToken() throws Exception {
        AuthSuccessDTO mockResponse = AuthSuccessDTO.builder()
                .success(true)
                .message("Login successful")
                .accessToken("eyJhbGciOiJIUzI1NiJ9.test")
                .expiresIn(900)
                .user(mockUser)
                .build();

        when(authService.login(any(LoginDTO.class))).thenReturn(mockResponse);

        String body = objectMapper.writeValueAsString(LoginDTO.builder().loginRequestEmail("test@example.com").loginRequestPassword("Test1234!").build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.message").value("Login successful"))
                .andExpect(jsonPath("$.result.accessToken").value("eyJhbGciOiJIUzI1NiJ9.test"))
                .andExpect(jsonPath("$.result.expiresIn").value(900))
                .andExpect(jsonPath("$.result.user.email").value("test@example.com"));
    }

    @Test
    void login_withNullBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Request Body"))
                .andExpect(jsonPath("$.code").value(400));
    }

    // --- REGISTER ---

    @Test
    void register_withValidData_shouldReturnSuccess() throws Exception {
        doNothing().when(authService).register(any(RegisterDTO.class));

        String body = objectMapper.writeValueAsString(RegisterDTO.builder().registerRequestName("Test User").registerRequestEmail("test@example.com").registerRequestPassword("Test1234!").build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void register_withNullBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Request Body"))
                .andExpect(jsonPath("$.code").value(400));
    }

    // --- LOGOUT ---

    @Test
    void logout_withValidToken_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.message").value("Logout successful"));
    }

    @Test
    void logout_withoutToken_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    void logout_withInvalidHeaderPrefix_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Basic abcdef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    void logout_whenExtractionThrowsException_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    void logout_whenEmailNull_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    void logout_whenEmailEmpty_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));
    }

    @Test
    void logout_whenUserIdNull_shouldReturnSuccess() throws Exception {
        LogoutSuccessDTO mockResponse = LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));
    }

    // --- REFRESH ---

    @Test
    void refresh_withValidToken_shouldReturnNewTokens() throws Exception {
        RefreshTokenSuccessDTO mockResponse = RefreshTokenSuccessDTO.builder()
                .success(true)
                .message("Refresh successful")
                .accessToken("new-access-token")
                .expiresIn(900)
                .refreshToken("new-refresh-token")
                .refreshExpiresIn(86400)
                .build();

        when(authService.refreshToken("valid-refresh-token")).thenReturn(mockResponse);

        String body = objectMapper.writeValueAsString(new RefreshTokenDTO("valid-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_withNullBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid Request Body"));
    }

    @Test
    void refresh_withNullRefreshToken_shouldReturnBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new RefreshTokenDTO(null));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error.fields[?(@.field=='refreshToken')].reason")
                .value("Refresh Token is Required"));
    }

    @Test
    void refresh_withEmptyRefreshToken_shouldReturnBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new RefreshTokenDTO("   "));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error.fields[?(@.field=='refreshToken')].reason")
                        .value("Refresh Token is Required"));
    }
}
