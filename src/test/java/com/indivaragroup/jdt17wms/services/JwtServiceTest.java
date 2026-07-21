package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);
    private static final String VALID_SECRET_64 = "indivaragroupwmsjsonwebtokensecretkey2026supersecretkey123456789012";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(clock);
        ReflectionTestUtils.setField(jwtService, "secretKey", VALID_SECRET_64);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 900000);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 604800000);
    }

    @Test
    @DisplayName("validateConfiguration - when secretKey is null, throw IllegalStateException")
    void validateConfiguration_whenSecretKeyIsNull_shouldThrowIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> jwtService.validateConfiguration());
        assertEquals("JWT secret not configured. Set JWT_SECRET environment variable.", ex.getMessage());
    }

    @Test
    @DisplayName("validateConfiguration - when secretKey is blank, throw IllegalStateException")
    void validateConfiguration_whenSecretKeyIsBlank_shouldThrowIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> jwtService.validateConfiguration());
        assertEquals("JWT secret not configured. Set JWT_SECRET environment variable.", ex.getMessage());
    }

    @Test
    @DisplayName("validateConfiguration - when secretKey is less than 64 chars, throw IllegalStateException")
    void validateConfiguration_whenSecretKeyTooShort_shouldThrowIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "shortsecretkey");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> jwtService.validateConfiguration());
        assertTrue(ex.getMessage().startsWith("JWT secret too short. Minimum 64 characters (256 bits) required."));
    }

    @Test
    @DisplayName("validateConfiguration - when secretKey and expirations valid, complete without exception")
    void validateConfiguration_whenValid_shouldInitializeSuccessfully() {
        assertDoesNotThrow(() -> jwtService.validateConfiguration());
    }

    @Test
    @DisplayName("getAccessTokenExpirationMs & getRefreshTokenExpirationMs - return configured values")
    void getExpirations_shouldReturnConfiguredValues() {
        assertEquals(900000, jwtService.getAccessTokenExpirationMs());
        assertEquals(604800000, jwtService.getRefreshTokenExpirationMs());
    }

    @Test
    void generateAccessToken_shouldContainCorrectClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com")
                .role(UserRole.USER)
                .build();

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
        assertEquals("john@example.com", jwtService.getEmailFromToken(token));
        assertEquals("USER", jwtService.getRoleFromToken(token));
        assertEquals("John Doe", jwtService.getNameFromToken(token));
        assertEquals(userId, jwtService.getUserIdFromToken(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void generateRefreshToken_shouldContainCorrectClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("Admin User")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtService.generateRefreshToken(user);
        assertNotNull(token);
        assertFalse(jwtService.isAccessToken(token));
        assertTrue(jwtService.isRefreshToken(token));
        assertEquals("admin@example.com", jwtService.getEmailFromToken(token));
    }

    @Test
    void getEmailClaimFromToken_expiredToken_returnsEmailClaim() {
        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET_64.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now(clock);
        String expiredToken = Jwts.builder()
                .subject("expired@example.com")
                .claim("email", "expired@example.com")
                .claim("userId", UUID.randomUUID().toString())
                .claim("role", "USER")
                .claim("token_type", "access")
                .claim("iat", now.minusSeconds(20).getEpochSecond())
                .claim("exp", now.minusSeconds(10).getEpochSecond())
                .signWith(key)
                .compact();

        assertEquals("expired@example.com", jwtService.getEmailFromToken(expiredToken));
    }

    @Test
    void getUserIdClaimFromToken_expiredToken_returnsUserIdClaim() {
        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET_64.getBytes(StandardCharsets.UTF_8));
        UUID uid = UUID.randomUUID();
        Instant now = Instant.now(clock);
        String expiredToken = Jwts.builder()
                .subject("user@example.com")
                .claim("email", "user@example.com")
                .claim("userId", uid.toString())
                .claim("role", "USER")
                .claim("token_type", "access")
                .claim("iat", now.minusSeconds(20).getEpochSecond())
                .claim("exp", now.minusSeconds(10).getEpochSecond())
                .signWith(key)
                .compact();

        assertEquals(uid, jwtService.getUserIdFromToken(expiredToken));
    }

    @Test
    @DisplayName("getUserIdFromToken - when userId claim is missing/null, return null")
    void getUserIdFromToken_whenUserIdClaimNull_shouldReturnNull() {
        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET_64.getBytes(StandardCharsets.UTF_8));
        String tokenWithoutUserId = Jwts.builder()
                .subject("user@example.com")
                .signWith(key)
                .compact();

        assertNull(jwtService.getUserIdFromToken(tokenWithoutUserId));
    }

    @Test
    void isTokenValid_mismatchedEmail_returnsFalse() {
        UUID userId = UUID.randomUUID();
        User tokenUser = User.builder()
                .id(userId)
                .name("Alice")
                .email("alice@example.com")
                .role(UserRole.USER)
                .build();
        User differentUser = User.builder()
                .id(userId)
                .name("Bob")
                .email("bob@example.com")
                .role(UserRole.USER)
                .build();

        String token = jwtService.generateAccessToken(tokenUser);
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void isAccessToken_refreshToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("Carol")
                .email("carol@example.com")
                .role(UserRole.USER)
                .build();

        String refreshToken = jwtService.generateRefreshToken(user);
        assertFalse(jwtService.isAccessToken(refreshToken));
        assertTrue(jwtService.isRefreshToken(refreshToken));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("Dave")
                .email("dave@example.com")
                .role(UserRole.USER)
                .build();

        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET_64.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now(clock);
        String expiredToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("email", user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", "USER")
                .claim("token_type", "access")
                .claim("iat", now.minusSeconds(20).getEpochSecond())
                .claim("exp", now.minusSeconds(10).getEpochSecond())
                .signWith(key)
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken, user));
    }

    @Test
    @DisplayName("isTokenValid - when token has no expiration claim, return true if email matches")
    void isTokenValid_tokenWithoutExpiration_shouldReturnTrueIfEmailMatches() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("Eve")
                .email("eve@example.com")
                .role(UserRole.USER)
                .build();

        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET_64.getBytes(StandardCharsets.UTF_8));
        String tokenNoExp = Jwts.builder()
                .subject(user.getEmail())
                .signWith(key)
                .compact();

        assertTrue(jwtService.isTokenValid(tokenNoExp, user));
    }
}
