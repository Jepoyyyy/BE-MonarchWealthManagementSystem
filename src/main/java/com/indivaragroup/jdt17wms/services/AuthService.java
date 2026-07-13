package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AuthDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogRepository auditLogRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogRepository = auditLogRepository;
    }

    public String extractEmailFromToken(String token) {
        return jwtService.getEmailFromToken(token);
    }

    // Login
    public AuthSuccessDTO login(AuthDTO dto) {
        List<ValidationErrorDetailDTO> errors = new ArrayList<>();

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            errors.add(new ValidationErrorDetailDTO("email", "Email is required", "ERR-001"));
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            errors.add(new ValidationErrorDetailDTO("password","Password is Required", "ERR-001"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors, "VALIDATION");
        }

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadRequestException("Email Or Password Invalid"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Email or Password Invalid");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuditLog auditLog = AuditLog.builder()
                .userId(user.getId())
                .userName(user.getName())
                .action("LOGIN")
                .details("User logged in: " + user.getEmail())
                .category("AUTH")
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        UserDTO userDto = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .questionnaireCompleted(user.getQuestionnaireCompleted())
                .isAdmin(user.getRole() == UserRole.admin)
                .build();

        return AuthSuccessDTO.builder()
                .success(true)
                .message("Login successful")
                .accessToken(accessToken)
                .expiresIn(900)
                .refreshToken(refreshToken)
                .refreshExpiresIn(604800)
                .user(userDto)
                .build();
    }

    //logout
    @Transactional
    public LogoutSuccessDTO logout(String userEmail) {
        AuditLog auditLog = AuditLog.builder()
                .userName(userEmail != null ? userEmail : "anonymous")
                .action("LOGOUT")
                .details("User logged out")
                .category("AUTH")
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        return LogoutSuccessDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();
    }

    //Register Harusnya Udah,coba crosscheck lagi
    @Transactional
    public AuthSuccessDTO register(AuthDTO dto) {
        List<ValidationErrorDetailDTO> errors = new ArrayList<>();
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            errors.add(new ValidationErrorDetailDTO("email", "Email is required", "ERR-001"));
        } else if (!Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$").matcher(dto.getEmail()).matches()) {
            errors.add(new ValidationErrorDetailDTO("email", "Invalid email format", "ERR-002"));
        }

        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            errors.add(new ValidationErrorDetailDTO("password", "Password is required", "ERR-001"));
        } else {
            if (dto.getPassword().length() < 8) {
                errors.add(new ValidationErrorDetailDTO("password", "Must be at least 8 characters", "ERR-003"));
            }
            if (dto.getPassword().length() > 72) {
                errors.add(new ValidationErrorDetailDTO("password", "Must not exceed 72 characters", "ERR-003"));
            }
            if (!Pattern.compile("[a-z]").matcher(dto.getPassword()).find()) {
                errors.add(new ValidationErrorDetailDTO("password", "Must contain lowercase letter", "ERR-003"));
            }
            if (!Pattern.compile("[A-Z]").matcher(dto.getPassword()).find()) {
                errors.add(new ValidationErrorDetailDTO("password", "Must contain uppercase letter", "ERR-003"));
            }
            if (!Pattern.compile("[^a-zA-Z0-9]").matcher(dto.getPassword()).find()) {
                errors.add(new ValidationErrorDetailDTO("password", "Must contain symbol", "ERR-003"));
            }
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.add(new ValidationErrorDetailDTO("name", "Name is required", "ERR-001"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors, "VALIDATION");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email already in use");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(UserRole.user)
                .status("ACTIVE")
                .questionnaireCompleted(false)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Audit Log
        AuditLog auditLog = AuditLog.builder()
                .userId(savedUser.getId())
                .userName(savedUser.getName())
                .action("REGISTER")
                .details("User successfully registered: " + savedUser.getEmail())
                .category("AUTH")
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        UserDTO userDto = UserDTO.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .questionnaireCompleted(user.getQuestionnaireCompleted())
                .isAdmin(savedUser.getRole() == UserRole.admin)
                .build();

        return AuthSuccessDTO.builder()
                .success(true)
                .message("Registration successful")
                .accessToken(accessToken)
                .expiresIn(900)
                .refreshToken(refreshToken)
                .refreshExpiresIn(604800)
                .user(userDto)
                .build();
    }

    // Refresh Token
    @Transactional
    public RefreshTokenSuccessDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Invalid Message Body");
        }

        try {
            // Validate token type
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new InvalidTokenException("Token is not a refresh token");
            }

            // Extract email and load user
            String email = jwtService.getEmailFromToken(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Generate new tokens (rotation)
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            // Audit log
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .action("TOKEN_REFRESH")
                    .details("Access token refreshed for: " + user.getEmail())
                    .category("AUTH")
                    .timestamp(Instant.now())
                    .build();
            auditLogRepository.save(auditLog);

            return RefreshTokenSuccessDTO.builder()
                    .success(true)
                    .message("Token refreshed successfully")
                    .accessToken(newAccessToken)
                    .expiresIn(900)
                    .refreshToken(newRefreshToken)
                    .refreshExpiresIn(604800)
                    .build();

        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("Refresh token expired");
        } catch (InvalidTokenException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid refresh token");
        }
    }
}
