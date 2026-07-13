package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    // Token type constants
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    
    // User claims constants
    private static final String USER_ID_CLAIM = "userId";
    private static final String USER_ROLE_CLAIM = "role";
    private static final String USER_NAME_CLAIM = "name";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration-ms:900000}") // 15 minutes
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:604800000}") // 7 days
    private long refreshTokenExpirationMs;
    
    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret not configured. Set JWT_SECRET environment variable."
            );
        }
        
        if (secretKey.length() < 64) {
            throw new IllegalStateException(
                "JWT secret too short. Minimum 64 characters (256 bits) required. Current: " 
                + secretKey.length()
            );
        }
        
        log.info("JWT Service initialized successfully. Secret length: {} characters", secretKey.length());
        log.info("Access token expiration: {} ms ({} minutes)", accessTokenExpirationMs, accessTokenExpirationMs / 60000);
        log.info("Refresh token expiration: {} ms ({} days)", refreshTokenExpirationMs, refreshTokenExpirationMs / 86400000);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(USER_ID_CLAIM, user.getId().toString())
                .claim(USER_ROLE_CLAIM, user.getRole().name())
                .claim(USER_NAME_CLAIM, user.getName())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
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
        return getExpiration(token).before(new Date());
    }

    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
