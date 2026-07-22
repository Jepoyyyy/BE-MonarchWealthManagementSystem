package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.BearerHeaderDTO;
import com.indivaragroup.jdt17wms.dto.request.LoginDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
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
import static org.mockito.ArgumentMatchers.any;
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

    // --- Token Extraction Tests ---

    @Test
    void extractEmailFromToken_shouldReturnEmail() {
        when(jwtService.getEmailFromToken("token")).thenReturn("user@example.com");
        assertEquals("user@example.com", authService.extractEmailFromToken("token"));
    }

    @Test
    void extractUserIdFromToken_withValidUserId_shouldReturnUUID() {
        UUID userId = UUID.randomUUID();
        when(jwtService.getUserIdFromToken("token")).thenReturn(userId);
        assertEquals(userId, authService.extractUserIdFromToken("token"));
    }

    @Test
    void extractUserIdFromToken_withNullUserId_shouldReturnNull() {
        when(jwtService.getUserIdFromToken("token")).thenReturn(null);
        assertNull(authService.extractUserIdFromToken("token"));
    }

    // --- Login Tests ---

    @Test
    void login_withNullEmail_shouldThrowBadRequestException() {
        LoginDTO dto = LoginDTO.builder().loginRequestPassword("Password123!").build();
        assertThrows(CoreThrowHandler.class, () -> authService.login(dto));
    }

    @Test
    void login_withUserNotFound_shouldThrowBadRequestException() {
        LoginDTO dto = LoginDTO.builder().loginRequestEmail("notfound@example.com").loginRequestPassword("Password123!").build();
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.login(dto));
        assertEquals("Email Or Password Invalid", ex.getMessage());
    }

    @Test
    void login_withUserNotActive_shouldThrowUnauthorizedException() {
        LoginDTO dto = LoginDTO.builder()
                .loginRequestEmail("inactive@example.com")
                .loginRequestPassword("Password123!")
                .build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("inactive@example.com")
                .status("DISABLED")
                .build();

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.login(dto));
        assertEquals(ApiError.UNAUTHORIZED.getCode(), ex.getCode());
        assertEquals("Account is Not active. Please Contact Admin", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void login_withPasswordMismatch_shouldThrowBadRequestException() {
        LoginDTO dto = LoginDTO.builder().loginRequestEmail("user@example.com").loginRequestPassword("WrongPassword").build();
        User user = User.builder().email("user@example.com").passwordHash("hash").status("ACTIVE").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hash")).thenReturn(false);

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.login(dto));
        assertEquals("Email or Password Invalid", ex.getMessage());
    }

    @Test
    void login_success_firstUser_shouldReturnAdminSuccessResponse() {
        LoginDTO dto = LoginDTO.builder().loginRequestEmail("user@example.com").loginRequestPassword("Password123!").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .questionnaireCompleted(false)
                .status("ACTIVE")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTokenExpirationMs()).thenReturn(900);
        lenient().when(jwtService.getRefreshTokenExpirationMs()).thenReturn(86400);

        AuthSuccessDTO response = authService.login(dto);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertFalse(response.getUser().getIsAdmin());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void login_success_notFirstUser_userRoleUser_shouldReturnUserSuccessResponse() {
        LoginDTO dto = LoginDTO.builder().loginRequestEmail("user@example.com").loginRequestPassword("Password123!").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .questionnaireCompleted(false)
                .status("ACTIVE")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTokenExpirationMs()).thenReturn(900);
        lenient().when(jwtService.getRefreshTokenExpirationMs()).thenReturn(86400);

        AuthSuccessDTO response = authService.login(dto);

        assertNotNull(response);
        assertFalse(response.getUser().getIsAdmin());
    }

    @Test
    void login_success_notFirstUser_userRoleAdmin_shouldReturnAdminSuccessResponse() {
        LoginDTO dto = LoginDTO.builder().loginRequestEmail("admin@example.com").loginRequestPassword("Password123!").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Admin User")
                .email("admin@example.com")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .questionnaireCompleted(false)
                .status("ACTIVE")
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTokenExpirationMs()).thenReturn(900);
        lenient().when(jwtService.getRefreshTokenExpirationMs()).thenReturn(86400);

        AuthSuccessDTO response = authService.login(dto);

        assertNotNull(response);
        assertTrue(response.getUser().getIsAdmin());
    }

    // --- Logout Tests ---

    @Test
    void logout_success_shouldSaveAuditLogAndReturnSuccess() {
        UUID userId = UUID.randomUUID();
        LogoutSuccessDTO response = authService.logout("user@example.com", userId);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Logout successful", response.getMessage());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void logout_withNullEmail_shouldSaveAuditLogWithAnonymousAndReturnSuccess() {
        UUID userId = UUID.randomUUID();
        LogoutSuccessDTO response = authService.logout(null, userId);

        assertNotNull(response);
        assertTrue(response.getSuccess());

        verify(auditLogRepository, times(1)).save(argThat(audit -> "anonymous".equals(audit.getUserName())));
    }

    @Test
    void logout_withValidBearerHeaderDTO_shouldExtractTokenDetailsAndLogout() {
        UUID userId = UUID.randomUUID();
        BearerHeaderDTO headerDTO = BearerHeaderDTO.builder()
                .authHeader("Bearer valid.token.string")
                .build();

        when(jwtService.getEmailFromToken("valid.token.string")).thenReturn("user@example.com");
        when(jwtService.getUserIdFromToken("valid.token.string")).thenReturn(userId);

        LogoutSuccessDTO response = authService.logout(headerDTO);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Logout successful", response.getMessage());

        verify(auditLogRepository, times(1)).save(argThat(audit ->
                "user@example.com".equals(audit.getUserName()) && userId.equals(audit.getUserId())
        ));
    }

    @Test
    void logout_withNullBearerHeaderDTO_shouldLogoutAsAnonymous() {
        LogoutSuccessDTO response = authService.logout((BearerHeaderDTO) null);

        assertNotNull(response);
        assertTrue(response.getSuccess());

        verify(auditLogRepository, times(1)).save(argThat(audit ->
                "anonymous".equals(audit.getUserName()) && audit.getUserId() == null
        ));
    }

    @Test
    void logout_withNullAuthHeaderInDTO_shouldLogoutAsAnonymous() {
        BearerHeaderDTO headerDTO = BearerHeaderDTO.builder().authHeader(null).build();

        LogoutSuccessDTO response = authService.logout(headerDTO);

        assertNotNull(response);
        assertTrue(response.getSuccess());

        verify(auditLogRepository, times(1)).save(argThat(audit ->
                "anonymous".equals(audit.getUserName()) && audit.getUserId() == null
        ));
    }

    @Test
    void logout_withNonBearerHeader_shouldLogoutAsAnonymous() {
        BearerHeaderDTO headerDTO = BearerHeaderDTO.builder().authHeader("Basic 123456").build();

        LogoutSuccessDTO response = authService.logout(headerDTO);

        assertNotNull(response);
        assertTrue(response.getSuccess());

        verify(jwtService, never()).getEmailFromToken(any());
        verify(auditLogRepository, times(1)).save(argThat(audit ->
                "anonymous".equals(audit.getUserName()) && audit.getUserId() == null
        ));
    }

    @Test
    void logout_withTokenParsingException_shouldCatchAndLogoutAsAnonymous() {
        BearerHeaderDTO headerDTO = BearerHeaderDTO.builder().authHeader("Bearer malformed.token").build();

        when(jwtService.getEmailFromToken("malformed.token")).thenThrow(new RuntimeException("Token parse error"));

        LogoutSuccessDTO response = authService.logout(headerDTO);

        assertNotNull(response);
        assertTrue(response.getSuccess());

        verify(auditLogRepository, times(1)).save(argThat(audit ->
                "anonymous".equals(audit.getUserName()) && audit.getUserId() == null
        ));
    }

    // --- Refresh Token Tests ---

    @Test
    void refreshToken_withNullOrEmptyToken_shouldThrowBadRequestException() {
        assertThrows(CoreThrowHandler.class, () -> authService.refreshToken(null));
        assertThrows(CoreThrowHandler.class, () -> authService.refreshToken("   "));
    }

    @Test
    void refreshToken_withInvalidTokenType_shouldThrowInvalidTokenException() {
        when(jwtService.isRefreshToken("bad-token")).thenReturn(false);
        assertThrows(CoreThrowHandler.class, () -> authService.refreshToken("bad-token"));
    }

    @Test
    void refreshToken_withExpiredToken_shouldThrowUnauthorizedException() {
        when(jwtService.isRefreshToken("expired-token")).thenReturn(true);
        ExpiredJwtException expiredEx = mock(ExpiredJwtException.class);
        when(jwtService.getEmailFromToken("expired-token")).thenThrow(expiredEx);

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.refreshToken("expired-token"));
        assertEquals("Token Expired", ex.getMessage());
    }

    @Test
    void refreshToken_withInvalidTokenSignature_shouldThrowInvalidTokenException() {
        when(jwtService.isRefreshToken("invalid-sig-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("invalid-sig-token")).thenThrow(new CoreThrowHandler(ApiError.INVALID_TOKEN, "Invalid signature"));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.refreshToken("invalid-sig-token"));
        assertEquals("INVALID TOKEN", ex.getMessage());
    }

    @Test
    void refreshToken_withGeneralException_shouldThrowInvalidTokenException() {
        when(jwtService.isRefreshToken("error-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("error-token")).thenThrow(new RuntimeException("Unknown error"));

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.refreshToken("error-token"));
        assertEquals("INVALID TOKEN", ex.getMessage());
    }

    @Test
    void refreshToken_withUserNotFound_shouldThrowCoreThrowHandler() {
        when(jwtService.isRefreshToken("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenReturn("notfound@example.com");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> authService.refreshToken("valid-token"));
        assertEquals("INVALID TOKEN", ex.getMessage());
    }

    @Test
    void refreshToken_success_shouldReturnNewTokens() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@example.com")
                .build();

        when(jwtService.isRefreshToken("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        RefreshTokenSuccessDTO response = authService.refreshToken("valid-token");

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Token refreshed successfully", response.getMessage());
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }
}
