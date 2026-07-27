package com.indivaragroup.jdt17wms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static com.indivaragroup.jdt17wms.dto.response.ApiPath.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public static final String ANY_WILDCARD = "/**";
  public static final String API_PATH_PATTERN = "/api" + ANY_WILDCARD;

  private static final String MSG_UNAUTHORIZED_USER = "Unauthorized User";
  private static final String MSG_ACCESS_DENIED = "Access Denied";

  private static final List<String> ALLOWED_ORIGINS = List.of(
          "http://localhost:5174",
          "https://monarch-plum-zeta.vercel.app"
  );
  private static final List<String> ALLOWED_METHODS = List.of(
          HttpMethod.GET.name(),
          HttpMethod.POST.name(),
          HttpMethod.PUT.name(),
          HttpMethod.DELETE.name(),
          HttpMethod.OPTIONS.name()
  );
  private static final List<String> ALLOWED_HEADERS = List.of("*");
  private static final List<String> EXPOSED_HEADERS = List.of("Authorization", "Content-Type");

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(){
      CorsConfiguration corsConfig = new CorsConfiguration();
      corsConfig.setAllowedOrigins(ALLOWED_ORIGINS);
      corsConfig.setAllowedMethods(ALLOWED_METHODS);
      corsConfig.setAllowedHeaders(ALLOWED_HEADERS);
      corsConfig.setExposedHeaders(EXPOSED_HEADERS);
      corsConfig.setAllowCredentials(true);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration(ANY_WILDCARD, corsConfig);
      return source;
  }

  @Bean
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, ANY_WILDCARD).permitAll()
        .requestMatchers(
                BASE_AUTH_ROUTE + LOGIN_ROUTE,
                BASE_AUTH_ROUTE + REGISTER_ROUTE,
                BASE_AUTH_ROUTE + REFRESH_TOKEN_ROUTE,
                SPRING_ERROR_URL
        ).permitAll()
        .requestMatchers(HttpMethod.GET, BASE_PRODUCTS_ROUTE).hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
        .requestMatchers(HttpMethod.PUT, BASE_PRODUCTS_ROUTE + ANY_WILDCARD).hasRole(UserRole.ADMIN.name())
        .requestMatchers(BASE_ADMIN_ROUTE + ANY_WILDCARD).hasRole(UserRole.ADMIN.name())
        .requestMatchers(BASE_AUDIT_ROUTE, BASE_AUDIT_ROUTE + ANY_WILDCARD).hasRole(UserRole.ADMIN.name())
        .requestMatchers(BASE_USERS_ROUTE, BASE_USERS_ROUTE + ANY_WILDCARD).hasRole(UserRole.ADMIN.name())
        .requestMatchers(BASE_USER_ROUTE + ANY_WILDCARD).hasRole(UserRole.USER.name())
        .anyRequest().authenticated()
      )
      .exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<?> body = ApiResponse.builder()
                    .restApiResponseHttpCode(HttpServletResponse.SC_UNAUTHORIZED)
                    .restApiResponseResult(null)
                    .restApiResponseMessage(MSG_UNAUTHORIZED_USER)
                    .restApiResponseError(null)
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(body));
        })
        .accessDeniedHandler((request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<?> body = ApiResponse.builder()
                    .restApiResponseHttpCode(HttpServletResponse.SC_FORBIDDEN)
                    .restApiResponseError(null)
                    .restApiResponseMessage(MSG_ACCESS_DENIED)
                    .restApiResponseError(null)
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(body));
        })
      )
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
