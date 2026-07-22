package com.indivaragroup.jdt17wms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ErrorResponseDTO;
import com.indivaragroup.jdt17wms.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, objectMapper);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- shouldNotFilter Tests ---

    @ParameterizedTest
    @CsvSource({
        "/api/v1/auth/logout, /api/v1/auth/logout",
        "null, /api/v1/auth/logout"
    })
    void testShouldNotFilter_PathEndsWithLogout_ReturnsFalse(String servletPath, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("null".equals(servletPath) ? null : servletPath);
        request.setRequestURI("null".equals(requestUri) ? null : requestUri);
        assertFalse(jwtAuthenticationFilter.shouldNotFilter(request));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/v1/auth/login, /api/v1/auth/login",
        "/api/v1/auth/register, /api/v1/auth/register",
        "null, /api/v1/auth/login",
        "null, /api/v1/auth/register"
    })
    void testShouldNotFilter_PathStartsWithAuth_ReturnsTrue(String servletPath, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("null".equals(servletPath) ? null : servletPath);
        request.setRequestURI("null".equals(requestUri) ? null : requestUri);
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));
    }

    @Test
    void testShouldNotFilter_NullPathAndUri_ReturnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(null);
        request.setRequestURI(null);
        assertFalse(jwtAuthenticationFilter.shouldNotFilter(request));
    }

    @Test
    void testShouldNotFilter_NoMatch_ReturnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/me/dashboard");
        request.setRequestURI("/api/v1/me/dashboard");
        assertFalse(jwtAuthenticationFilter.shouldNotFilter(request));
    }

    // --- doFilterInternal Tests ---

    @Test
    void testDoFilterInternal_NoAuthHeader_ProceedsToFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InvalidHeaderPrefix_ProceedsToFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abcdef");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InvalidTokenType_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.isAccessToken("invalid-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("application/json", response.getContentType());

        ErrorResponseDTO expectedResponse = ErrorResponseDTO.builder()
                .error("Invalid token type")
                .code(401)
                .build();
        String jsonResponse = response.getContentAsString();
        assertEquals(objectMapper.writeValueAsString(expectedResponse), jsonResponse);
    }

    @Test
    void testDoFilterInternal_ExpiredToken_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.isAccessToken("expired-token")).thenReturn(true);
        ExpiredJwtException expiredJwtException = mock(ExpiredJwtException.class);
        when(jwtService.getEmailFromToken("expired-token")).thenThrow(expiredJwtException);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        ErrorResponseDTO expectedResponse = ErrorResponseDTO.builder()
                .error("Token expired")
                .code(401)
                .build();
        assertEquals(objectMapper.writeValueAsString(expectedResponse), response.getContentAsString());
    }

    @Test
    void testDoFilterInternal_InvalidTokenException_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.isAccessToken("bad-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("bad-token")).thenThrow(new JwtException("Invalid token signature"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        ErrorResponseDTO expectedResponse = ErrorResponseDTO.builder()
                .error("Invalid token")
                .code(401)
                .build();
        assertEquals(objectMapper.writeValueAsString(expectedResponse), response.getContentAsString());
    }

    @Test
    void testDoFilterInternal_Success_UserRoleAdmin_SetsRoleAdmin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.isAccessToken("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenReturn("admin@example.com");
        when(jwtService.getRoleFromToken("valid-token")).thenReturn("ADMIN");
        UUID userId = UUID.randomUUID();
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(jwtService.getNameFromToken("valid-token")).thenReturn("Admin User");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getPrincipal() instanceof UserDTO);
        UserDTO principal = (UserDTO) auth.getPrincipal();
        assertEquals(userId, principal.getId());
        assertEquals("Admin User", principal.getName());
        assertEquals("admin@example.com", principal.getEmail());
        assertTrue(principal.getIsAdmin());

        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testDoFilterInternal_Success_UserRoleUser_SetsRoleUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.isAccessToken("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenReturn("user@example.com");
        when(jwtService.getRoleFromToken("valid-token")).thenReturn("USER");
        UUID userId = UUID.randomUUID();
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(jwtService.getNameFromToken("valid-token")).thenReturn("Normal User");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getPrincipal() instanceof UserDTO);
        UserDTO principal = (UserDTO) auth.getPrincipal();
        assertEquals(userId, principal.getId());
        assertEquals("Normal User", principal.getName());
        assertEquals("user@example.com", principal.getEmail());
        assertFalse(principal.getIsAdmin());

        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testDoFilterInternal_GeneralException_Returns401WithAuthenticationFailed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.isAccessToken("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenThrow(new RuntimeException("Unexpected error during processing"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        ErrorResponseDTO expectedResponse = ErrorResponseDTO.builder()
                .error("Authentication failed")
                .code(401)
                .build();
        assertEquals(objectMapper.writeValueAsString(expectedResponse), response.getContentAsString());
    }
}

