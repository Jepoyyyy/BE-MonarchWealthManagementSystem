package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    // Token type constants
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    // Claim name constants
    private static final String USER_ID_CLAIM = "userId";
    private static final String USER_ROLE_CLAIM = "role";
    private static final String USER_NAME_CLAIM = "name";


    private final Clock clock;

    @Value("${jwt.secret}")
    private String secretKey;

    @Getter
    @Value("${jwt.access-token-expiration-ms}") // 15 minutes
    private Integer accessTokenExpirationMs;

    @Getter
    @Value("${jwt.refresh-token-expiration-ms}") // 7 days
    private Integer refreshTokenExpirationMs;

    public JwtService(Clock clock) {
        this.clock = clock;
    }

    private static final Integer SECRET_KEY_LENGTH = 64;
    private static final Integer MILLISECONDS_PER_MINUTE = 60000;
    private static final Integer MILLISECONDS_PER_DAY = 8640000;


  @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret not configured. Set JWT_SECRET environment variable."
            );
        }

        if (secretKey.length() < SECRET_KEY_LENGTH) {
            throw new IllegalStateException(
                "JWT secret too short. Minimum 64 characters (256 bits) required. Current: "
                + secretKey.length()
            );
        }

        log.info("JWT Service initialized successfully. Secret length: {} characters", secretKey.length());
        log.info("Access token expiration: {} ms ({} minutes)", accessTokenExpirationMs, accessTokenExpirationMs / MILLISECONDS_PER_MINUTE);
        log.info("Refresh token expiration: {} ms ({} days)", refreshTokenExpirationMs, refreshTokenExpirationMs / MILLISECONDS_PER_DAY);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(USER_ID_CLAIM, user.getId().toString())
                .claim(USER_ROLE_CLAIM, user.getRole().name())
                .claim(USER_NAME_CLAIM, user.getName())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusMillis(refreshTokenExpirationMs)))
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public UUID getUserIdFromToken(String token) {
        String userId = getClaim(token, claims -> claims.get(USER_ID_CLAIM, String.class));
        return userId != null ? UUID.fromString(userId) : null;
    }

    public String getRoleFromToken(String token) {
        return getClaim(token, claims -> claims.get(USER_ROLE_CLAIM, String.class));
    }

    public String getNameFromToken(String token) {
        return getClaim(token, claims -> claims.get(USER_NAME_CLAIM, String.class));
    }

    public boolean isTokenValid(String token, User user) {
        final String email = getEmailFromToken(token);
        return (email.equals(user.getEmail())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Instant expiration = getExpiration(token);
        return expiration != null && expiration.isBefore(Instant.now(clock));
    }

    private Instant getExpiration(String token) {
        java.util.Date expiration = getClaim(token, Claims::getExpiration);
        return expiration != null ? expiration.toInstant() : null;
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String getTokenType(String token) {
        return getClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(getTokenType(token));
    }
}
