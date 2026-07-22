package com.indivaragroup.jdt17wms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ErrorResponseDTO;
import com.indivaragroup.jdt17wms.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX_BEARER = "Bearer ";
    private static final String PATH_LOGOUT = ApiPath.LOGOUT_PATH;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String AUTHORITY_PREFIX_ROLE = "ROLE_";

    private static final String ERROR_INVALID_TOKEN_TYPE = "Invalid token type";
    private static final String ERROR_TOKEN_EXPIRED = "Token expired";
    private static final String ERROR_INVALID_TOKEN = "Invalid token";
    private static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith(PATH_LOGOUT)) {
            return false;
        }
        return (uri != null && (uri.startsWith(ApiPath.BASE_AUTH_PATH)));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(TOKEN_PREFIX_BEARER.length());

            if (!jwtService.isAccessToken(token)) {
                sendUnauthorizedError(response, ERROR_INVALID_TOKEN_TYPE);
                return;
            }

            // Extract user info from JWT claims (no database lookup)
            final String email = jwtService.getEmailFromToken(token);
            final String role = jwtService.getRoleFromToken(token);
            final UUID userId = jwtService.getUserIdFromToken(token);
            final String name = jwtService.getNameFromToken(token);

            UserDTO principal = UserDTO.builder()
                    .id(userId)
                    .email(email)
                    .name(name)
                    .questionnaireCompleted(false)
                    .isAdmin(ROLE_ADMIN.equals(role))
                    .build();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(AUTHORITY_PREFIX_ROLE + role))
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (ExpiredJwtException e) {
            sendUnauthorizedError(response, ERROR_TOKEN_EXPIRED);
            return;
        } catch (JwtException e) {
            sendUnauthorizedError(response, ERROR_INVALID_TOKEN);
            return;
        } catch (Exception e) {
            sendUnauthorizedError(response, ERROR_AUTHENTICATION_FAILED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error(message)
                .code(HttpServletResponse.SC_UNAUTHORIZED)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
