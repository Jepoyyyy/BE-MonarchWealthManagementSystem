package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Validation;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

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
        RegisterDTO dto = RegisterDTO.builder().registerRequestName("John Doe").registerRequestEmail("johndoe@example.com").registerRequestPassword("Password123!").build();
        User mockSavedUser = User.builder()
                .id(UUID.randomUUID())
                .name(dto.getRegisterRequestName())
                .email(dto.getRegisterRequestEmail())
                .passwordHash("encodedPassword")
                .role(UserRole.USER)
                .status("ACTIVE")
                .questionnaireCompleted(false)
                .build();

        when(userRepository.existsByEmail(dto.getRegisterRequestEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getRegisterRequestPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        authService.register(dto);

        verify(userRepository, times(1)).existsByEmail(dto.getRegisterRequestEmail());
        verify(passwordEncoder, times(1)).encode(dto.getRegisterRequestPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // 📝 REGISTER — success (first user gets Admin role)
    @Test
    void register_firstUser_shouldRegisterAsAdmin() {
        RegisterDTO dto = RegisterDTO.builder().registerRequestName("Admin User").registerRequestEmail("admin@example.com").registerRequestPassword("Password123!").build();
        User mockSavedAdmin = User.builder()
                .id(UUID.randomUUID())
                .name(dto.getRegisterRequestName())
                .email(dto.getRegisterRequestEmail())
                .passwordHash("encodedPassword")
                .role(UserRole.USER)
                .status("ACTIVE")
                .questionnaireCompleted(false)
                .build();

        when(userRepository.existsByEmail(dto.getRegisterRequestEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getRegisterRequestPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedAdmin);

        authService.register(dto);

        verify(userRepository, times(1)).existsByEmail(dto.getRegisterRequestEmail());
        verify(passwordEncoder, times(1)).encode(dto.getRegisterRequestPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // 📝 REGISTER — duplicate email (conflict exception)
    @Test
    void register_withDuplicateEmail_shouldThrowConflictException() {
        RegisterDTO dto = RegisterDTO.builder().registerRequestName("John Doe").registerRequestEmail("duplicate@example.com").registerRequestPassword("Password123!").build();

        when(userRepository.existsByEmail(dto.getRegisterRequestEmail())).thenReturn(true);

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        assertEquals("Email Already Used", exception.getMessage());

        verify(userRepository, times(1)).existsByEmail(dto.getRegisterRequestEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    // 📝 REGISTER — missing email (validation exception)
    @Test
    void register_withMissingEmail_shouldThrowValidationException() {
        RegisterDTO dto = RegisterDTO.builder().registerRequestName("John Doe").registerRequestPassword("Password123!").build();

        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            jakarta.validation.Validator validator = factory.getValidator();
            var violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> "registerRequestEmail".equals(v.getPropertyPath().toString())
                            && v.getMessage().contains("Email")));
        }
    }

    // 📝 REGISTER — invalid email format (validation exception)
    @Test
    void register_withInvalidEmailFormat_shouldThrowValidationException() {
        RegisterDTO dto = RegisterDTO.builder().registerRequestName("John Doe").registerRequestEmail("invalidEmailFormat").registerRequestPassword("Password123!").build();

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        assertEquals("INVALID FIELD VALUES", exception.getMessage());
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertFalse(details.isEmpty());
        ValidationErrorDetailDTO emailError = details.stream()
                .filter(d -> "email".equals(d.getField()))
                .findFirst()
                .orElse(null);

        assertNotNull(emailError);
        assertEquals("Invalid email format", emailError.getReason());
        assertEquals("ERR-001", emailError.getType());
    }

    // 📝 REGISTER — missing password (validation exception)
    @Test
    void register_withMissingPassword_shouldThrowValidationException() {
        RegisterDTO dto = RegisterDTO.builder()
                .registerRequestName("John Doe")
                .registerRequestEmail("johndoe@example.com")
                .registerRequestPassword("")
                .build();

        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            jakarta.validation.Validator validator = factory.getValidator();
            var violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> "registerRequestPassword".equals(v.getPropertyPath().toString())
                            && v.getMessage().contains("Password")));
        }
    }

    // 📝 REGISTER — password too short (validation exception)
    @Test
    void register_withShortPassword_shouldThrowValidationException() {
        RegisterDTO dto = RegisterDTO.builder()
                .registerRequestName("John Doe")
                .registerRequestEmail("johndoe@example.com")
                .registerRequestPassword("Pass1!")
                .build();

        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            jakarta.validation.Validator validator = factory.getValidator();
            var violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> "registerRequestPassword".equals(v.getPropertyPath().toString())
                            && v.getMessage().contains("size must be between 8 and 72")));
        }
    }

    // 📝 REGISTER — parameterized validation tests (service-level regex checks)
    @ParameterizedTest
    @CsvSource({
        "John Doe, johndoe@example.com, PASSWORD123!, password, Must contain lowercase letter, ERR-001",
        "John Doe, johndoe@example.com, password123!, password, Must contain uppercase letter, ERR-001",
        "John Doe, johndoe@example.com, Password123, password, Must contain symbol, ERR-001"
    })
    void register_withInvalidData_shouldThrowValidationException(
            String name, String email, String password, String expectedField, String expectedReason, String expectedType) {
        RegisterDTO dto = RegisterDTO.builder().registerRequestName(name).registerRequestEmail(email).registerRequestPassword(password).build();

        CoreThrowHandler exception = assertThrows(CoreThrowHandler.class, () -> authService.register(dto));
        List<ValidationErrorDetailDTO> details = exception.getDetails();

        assertFalse(details.isEmpty());
        ValidationErrorDetailDTO error = details.stream()
                .filter(d -> expectedField.equals(d.getField()) && expectedReason.equals(d.getReason()))
                .findFirst()
                .orElse(null);

        assertNotNull(error);
        assertEquals(expectedType, error.getType());
    }

    // 📝 REGISTER — blank name (DTO @NotBlank validation)
    @Test
    void register_withBlankName_shouldThrowValidationException() {
        RegisterDTO dto = RegisterDTO.builder().registerRequestName("").registerRequestEmail("johndoe@example.com").registerRequestPassword("Password123!").build();

        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            jakarta.validation.Validator validator = factory.getValidator();
            var violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> "registerRequestName".equals(v.getPropertyPath().toString())
                            && v.getMessage().contains("name is Required")));
        }
    }
}
