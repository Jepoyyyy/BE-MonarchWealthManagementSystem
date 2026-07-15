package com.indivaragroup.jdt17wms.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginDTOControllerTest {

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
            .email("test@example.com")
            .isAdmin(false)
            .build();

    // 📝 REGISTER — success
    @Test
    void register_withValidData_shouldReturnSuccess() throws Exception {
        AuthSuccessDTO mockResponse = AuthSuccessDTO.builder()
                .success(true)
                .message("Registration successful")
                .accessToken("eyJhbGciOiJIUzI1NiJ9.test-register")
                .expiresIn(900)
                .user(mockUser)
                .build();

        when(authService.register(any(RegisterDTO.class))).thenReturn(mockResponse);

        String body = objectMapper.writeValueAsString(new RegisterDTO("Test User", "test@example.com", "Test1234!"));

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

    // 📝 REGISTER — validation error (e.g. invalid fields)
    @Test
    void register_withInvalidData_shouldReturnValidationError() throws Exception {
        List<ValidationErrorDetailDTO> details = new ArrayList<>();
        details.add(new ValidationErrorDetailDTO("email", "Invalid email format", "ERR-002"));

        when(authService.register(any(RegisterDTO.class))).thenThrow(new CoreThrowHandler(ApiError.VALIDATION, "Invalid field values", details));

        String body = objectMapper.writeValueAsString(new RegisterDTO("Test User", "invalid-email", "Test1234!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid field values"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.details[0].field").value("email"))
                .andExpect(jsonPath("$.details[0].reason").value("Invalid email format"))
                .andExpect(jsonPath("$.details[0].type").value("ERR-002"));
    }

    // 📝 REGISTER — duplicate email (conflict error)
    @Test
    void register_withDuplicateEmail_shouldReturnConflictError() throws Exception {
        when(authService.register(any(RegisterDTO.class))).thenThrow(new CoreThrowHandler(ApiError.CONFLICT, "Email already in use"));

        String body = objectMapper.writeValueAsString(new RegisterDTO("Test User", "duplicate@example.com", "Test1234!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use"))
                .andExpect(jsonPath("$.code").value(409));
    }

    // 📝 REGISTER — null request body
    @Test
    void register_withNullBody_shouldReturn400() throws Exception {
        when(authService.register(any())).thenThrow(new CoreThrowHandler(ApiError.BAD_REQUEST, "Invalid Request Body"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request Body"));
    }
}
