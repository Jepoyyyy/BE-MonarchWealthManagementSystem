package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AuthDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.exceptions.BadRequestException;
import com.indivaragroup.jdt17wms.exceptions.ConflictException;
import com.indivaragroup.jdt17wms.exceptions.InvalidTokenException;
import com.indivaragroup.jdt17wms.exceptions.UnauthorizedException;
import com.indivaragroup.jdt17wms.exceptions.ValidationException;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    private final UUID userId = UUID.randomUUID();
    private final User mockUser = User.builder()
            .id(userId)
            .name("Test User")
            .email("test@example.com")
            .passwordHash("encoded-pass")
            .role(UserRole.user)
            .status("ACTIVE")
            .questionnaireCompleted(false)
            .build();

    private final AuthDTO validDto = new AuthDTO("Test User", "test@example.com", "Test1234!");

    // ───────────────────── LOGIN ─────────────────────

    @Test
    void login_withValidCredentials_shouldReturnSuccess() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Test1234!", "encoded-pass")).thenReturn(true);
        when(jwtService.generateAccessToken(mockUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh-token");

        AuthSuccessDTO result = authService.login(validDto);

        assertTrue(result.getSuccess());
        assertEquals("Login successful", result.getMessage());
        assertEquals("access-token", result.getAccessToken());
        assertEquals(900, result.getExpiresIn());
        assertNotNull(result.getRefreshToken());
        assertEquals(604800, result.getRefreshExpiresIn());
        assertEquals("test@example.com", result.getUser().getEmail());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void login_withEmptyEmail_shouldThrowValidationException() {
        AuthDTO dto = new AuthDTO("Test", "", "Test1234!");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.login(dto));
        assertEquals("VALIDATION", ex.getType());
        assertEquals("ERR-001", ex.getDetails().get(0).getType());
        assertEquals("email", ex.getDetails().get(0).getField());
    }

    @Test
    void login_withEmptyPassword_shouldThrowValidationException() {
        AuthDTO dto = new AuthDTO("Test", "test@example.com", "");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.login(dto));
        assertEquals("password", ex.getDetails().get(0).getField());
    }

    @Test
    void login_withUserNotFound_shouldThrowBadRequest() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        AuthDTO dto = new AuthDTO("Test", "unknown@example.com", "Test1234!");
        assertThrows(BadRequestException.class, () -> authService.login(dto));
    }

    @Test
    void login_withWrongPassword_shouldThrowBadRequest() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPass1!", "encoded-pass")).thenReturn(false);

        AuthDTO dto = new AuthDTO("Test", "test@example.com", "WrongPass1!");
        assertThrows(BadRequestException.class, () -> authService.login(dto));
    }

    // ───────────────────── REGISTER ─────────────────────

    @Test
    void register_withValidData_shouldReturnSuccess() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Test1234!")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateAccessToken(mockUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh-token");

        AuthSuccessDTO result = authService.register(validDto);

        assertTrue(result.getSuccess());
        assertEquals("Registration successful", result.getMessage());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void register_withEmptyEmail_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "", "Test1234!");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.getField().equals("email")));
    }

    @Test
    void register_withInvalidEmailFormat_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "invalid-email", "Test1234!");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertEquals("ERR-002", ex.getDetails().stream()
                .filter(d -> d.getField().equals("email"))
                .findFirst().get().getType());
    }

    @Test
    void register_withEmptyPassword_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "test@example.com", "");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.getField().equals("password")));
    }

    @Test
    void register_withWeakPassword_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "test@example.com", "short");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        long passwordErrors = ex.getDetails().stream()
                .filter(d -> d.getField().equals("password"))
                .count();
        assertTrue(passwordErrors >= 1);
    }

    @Test
    void register_withoutLowercase_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "test@example.com", "ABCD1234!");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().stream()
                .anyMatch(d -> d.getField().equals("password")
                        && d.getReason().contains("lowercase")));
    }

    @Test
    void register_withoutUppercase_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "test@example.com", "abcd1234!");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().stream()
                .anyMatch(d -> d.getField().equals("password")
                        && d.getReason().contains("uppercase")));
    }

    @Test
    void register_withoutSymbol_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("Test", "test@example.com", "Abcd1234");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().stream()
                .anyMatch(d -> d.getField().equals("password")
                        && d.getReason().contains("symbol")));
    }

    @Test
    void register_withEmptyName_shouldThrowValidation() {
        AuthDTO dto = new AuthDTO("", "test@example.com", "Test1234!");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.getField().equals("name")));
    }

    @Test
    void register_withDuplicateEmail_shouldThrowConflict() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(validDto));
    }

    @Test
    void register_withAllFieldsInvalid_shouldCollectAllErrors() {
        AuthDTO dto = new AuthDTO("", "bad", "x");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register(dto));
        assertTrue(ex.getDetails().size() >= 3);
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.getField().equals("email")));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.getField().equals("password")));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.getField().equals("name")));
    }

    // ───────────────────── LOGOUT ─────────────────────

    @Test
    void logout_withEmail_shouldReturnSuccess() {
        LogoutSuccessDTO result = authService.logout("test@example.com");

        assertTrue(result.getSuccess());
        assertEquals("Logout successful", result.getMessage());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logout_withoutEmail_shouldReturnSuccessAsAnonymous() {
        LogoutSuccessDTO result = authService.logout(null);

        assertTrue(result.getSuccess());
        verify(auditLogRepository).save(argThat(log -> "anonymous".equals(log.getUserName())));
    }

    // ───────────────────── REFRESH TOKEN ─────────────────────

    @Test
    void refreshToken_withValidToken_shouldReturnNewTokens() {
        when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-refresh-token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateAccessToken(mockUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("new-refresh");

        RefreshTokenSuccessDTO result = authService.refreshToken("valid-refresh-token");

        assertTrue(result.getSuccess());
        assertEquals("Token refreshed successfully", result.getMessage());
        assertEquals("new-access", result.getAccessToken());
        assertEquals("new-refresh", result.getRefreshToken());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void refreshToken_withEmptyToken_shouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.refreshToken(""));
        assertThrows(BadRequestException.class, () -> authService.refreshToken(null));
    }

    @Test
    void refreshToken_withNonRefreshToken_shouldThrowInvalidToken() {
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("access-token"));
    }

    @Test
    void refreshToken_withUserNotFound_shouldThrowUnauthorized() {
        when(jwtService.isRefreshToken("valid-refresh")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-refresh")).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("valid-refresh"));
    }

    @Test
    void refreshToken_withExpiredToken_shouldThrowUnauthorized() {
        when(jwtService.isRefreshToken("expired-refresh")).thenThrow(ExpiredJwtException.class);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("expired-refresh"));
    }

    @Test
    void refreshToken_withMalformedToken_shouldThrowInvalidToken() {
        when(jwtService.isRefreshToken("malformed")).thenThrow(RuntimeException.class);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("malformed"));
    }

    // ───────────────────── UTILITY ─────────────────────

    @Test
    void extractEmailFromToken_shouldDelegate() {
        when(jwtService.getEmailFromToken("some-token")).thenReturn("test@example.com");

        String result = authService.extractEmailFromToken("some-token");

        assertEquals("test@example.com", result);
    }
}
