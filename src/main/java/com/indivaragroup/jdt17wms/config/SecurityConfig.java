package com.indivaragroup.jdt17wms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.constants.ErrorConstants;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.indivaragroup.jdt17wms.dto.response.ApiPath.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;


  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
                BASE_AUTH_PATH + LOGIN_PATH,
                BASE_AUTH_PATH + REGISTER_PATH,
                BASE_AUTH_PATH + REFRESH_TOKEN_PATH,
                "/error",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/api-docs/**"
        ).permitAll()
        .requestMatchers(HttpMethod.GET, BASE_PRODUCTS_PATH).hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
        .requestMatchers(HttpMethod.PUT, BASE_PRODUCTS_PATH + "/**").hasRole(UserRole.ADMIN.name())
        .requestMatchers("/api/v1/admin/dashboard").hasRole(UserRole.ADMIN.name())
        .requestMatchers("/api/v1/admin/**").hasRole(UserRole.ADMIN.name())
        .requestMatchers("/api/v1/audit", "/api/v1/audit/**").hasRole(UserRole.ADMIN.name())
        .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole(UserRole.ADMIN.name())
        .requestMatchers("/api/v1/me/**", "/me/**").hasRole(UserRole.USER.name())
        .anyRequest().authenticated()
      )
      .exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(ErrorConstants.ERROR_UNAUTHORIZED));
        })
        .accessDeniedHandler((request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(ErrorConstants.ERROR_FORBIDDEN));
        })
      )
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
