package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterDTOServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuthService authService;

    // 📝 REGISTER — success
    @Test
    void register_withValidData_shouldRegisterSuccessfully() {
        RegisterDTO dto = new RegisterDTO("John Doe", "johndoe@example.com", "Password123!");
        User mockSavedUser = User.builder()
                .id(UUID.randomUUID())
                .name(dto.getRegisterRequestName())
                .email(dto.getRegisterRequestEmail())
                .passwordHash("encodedPassword")
                .role(UserRole.user)
                .status("ACTIVE")
                .questionnaireCompleted(false)
                .build();

        when(userRepository.existsByEmail(dto.getRegisterRequestEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getRegisterRequestPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("mockJwtToken");

        AuthSuccessDTO response = authService.register(dto);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Registration successful", response.getMessage());
        assertEquals("mockJwtToken", response.getAccessToken());
        assertEquals(900, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("johndoe@example.com", response.getUser().getEmail());
        assertEquals("John Doe", response.getUser().getName());
        assertFalse(response.getUser().getQuestionnaireCompleted());
        assertFalse(response.getUser().getIsAdmin());

        verify(userRepository, times(1)).existsByEmail(dto.getRegisterRequestEmail());
        verify(passwordEncoder, times(1)).encode(dto.getRegisterRequestPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtService, times(1)).generateAccessToken(any(User.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // 📝 REGISTER — duplicate email (conflict exception)
    @Test
    void register_withDuplicateEmail_shouldThrowConflictException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "duplicate@example.com", "Password123!");

        when(userRepository.existsByEmail(dto.getRegisterRequestEmail())).thenReturn(true);

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        assertEquals("Email already in use", exception.getMessage());

        verify(userRepository, times(1)).existsByEmail(dto.getRegisterRequestEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    // 📝 REGISTER — missing email (validation exception)
    @Test
    void register_withMissingEmail_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", null, "Password123!");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertFalse(details.isEmpty());
        ValidationErrorDetailDTO emailError = details.stream()
                .filter(d -> "email".equals(d.getField()))
                .findFirst()
                .orElse(null);

        assertNotNull(emailError);
        assertEquals("Email is required", emailError.getReason());
        assertEquals("ERR-001", emailError.getType());

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // 📝 REGISTER — invalid email format (validation exception)
    @Test
    void register_withInvalidEmailFormat_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "invalidEmailFormat", "Password123!");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertFalse(details.isEmpty());
        ValidationErrorDetailDTO emailError = details.stream()
                .filter(d -> "email".equals(d.getField()))
                .findFirst()
                .orElse(null);

        assertNotNull(emailError);
        assertEquals("Invalid email format", emailError.getReason());
        assertEquals("ERR-002", emailError.getType());
    }

    // 📝 REGISTER — missing password (validation exception)
    @Test
    void register_withMissingPassword_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "johndoe@example.com", "");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertFalse(details.isEmpty());
        ValidationErrorDetailDTO passwordError = details.stream()
                .filter(d -> "password".equals(d.getField()))
                .findFirst()
                .orElse(null);

        assertNotNull(passwordError);
        assertEquals("Password is required", passwordError.getReason());
        assertEquals("ERR-001", passwordError.getType());
    }

    // 📝 REGISTER — password too short (validation exception)
    @Test
    void register_withShortPassword_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "johndoe@example.com", "Pass1!");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertTrue(details.stream().anyMatch(d -> "password".equals(d.getField()) && "Must be at least 8 characters".equals(d.getReason())));
    }

    // 📝 REGISTER — password missing lowercase letter (validation exception)
    @Test
    void register_withPasswordMissingLowercase_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "johndoe@example.com", "PASSWORD123!");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertTrue(details.stream().anyMatch(d -> "password".equals(d.getField()) && "Must contain lowercase letter".equals(d.getReason())));
    }

    // 📝 REGISTER — password missing uppercase letter (validation exception)
    @Test
    void register_withPasswordMissingUppercase_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "johndoe@example.com", "password123!");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertTrue(details.stream().anyMatch(d -> "password".equals(d.getField()) && "Must contain uppercase letter".equals(d.getReason())));
    }

    // 📝 REGISTER — password missing symbol (validation exception)
    @Test
    void register_withPasswordMissingSymbol_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("John Doe", "johndoe@example.com", "Password123");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertTrue(details.stream().anyMatch(d -> "password".equals(d.getField()) && "Must contain symbol".equals(d.getReason())));
    }

    // 📝 REGISTER — missing name (validation exception)
    @Test
    void register_withMissingName_shouldThrowValidationException() {
        RegisterDTO dto = new RegisterDTO("", "johndoe@example.com", "Password123!");

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertFalse(details.isEmpty());
        ValidationErrorDetailDTO nameError = details.stream()
                .filter(d -> "name".equals(d.getField()))
                .findFirst()
                .orElse(null);

        assertNotNull(nameError);
        assertEquals("Name is required", nameError.getReason());
        assertEquals("ERR-001", nameError.getType());
    }
}
