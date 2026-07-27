package com.indivaragroup.jdt17wms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.constants.JwtConstants;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
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

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith(JwtConstants.PATH_LOGOUT)) {
            return false;
        }
        return (uri != null && (uri.startsWith(ApiPath.BASE_AUTH_ROUTE)));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(JwtConstants.HEADER_AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(JwtConstants.TOKEN_PREFIX_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(JwtConstants.TOKEN_PREFIX_BEARER.length());

            if (!jwtService.isAccessToken(token)) {
                sendUnauthorizedError(response, JwtConstants.Error.INVALID_TOKEN_TYPE);
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
                    .isAdmin(JwtConstants.ROLE_ADMIN.equals(role))
                    .build();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(JwtConstants.AUTHORITY_PREFIX_ROLE + role))
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (ExpiredJwtException e) {
            sendUnauthorizedError(response, JwtConstants.Error.TOKEN_EXPIRED);
            return;
        } catch (JwtException e) {
            sendUnauthorizedError(response, JwtConstants.Error.INVALID_TOKEN);
            return;
        } catch (Exception e) {
            sendUnauthorizedError(response, JwtConstants.Error.AUTHENTICATION_FAILED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<?> errorResponse = ApiResponse.builder()
                .restApiResponseHttpCode(HttpServletResponse.SC_UNAUTHORIZED)
                .restApiResponseResult(null)
                .restApiResponseMessage(message)
                .restApiResponseError(null)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
