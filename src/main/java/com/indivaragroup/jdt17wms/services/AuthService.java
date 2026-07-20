package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.LoginDTO;
import com.indivaragroup.jdt17wms.dto.request.RegisterDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.response.auth.AuthSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.LogoutSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.auth.RefreshTokenSuccessDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.dto.utils.ActiveStatus;
import com.indivaragroup.jdt17wms.dto.utils.AuditLogAction;
import com.indivaragroup.jdt17wms.dto.utils.AuditLogCategory;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    public UUID extractUserIdFromToken(String token) {
        return jwtService.getUserIdFromToken(token);
    }

    // Login
    public AuthSuccessDTO login( LoginDTO dto) {

        User user = userRepository.findByEmail(dto.getLoginRequestEmail())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.BAD_REQUEST,"Email Or Password Invalid"));

        if (user.getStatus() == null || !"active".equalsIgnoreCase(user.getStatus())) {
            throw new CoreThrowHandler(ApiError.UNAUTHORIZED,"Account is Not active. Please Contact Admin");
        }

        if (!passwordEncoder.matches(dto.getLoginRequestPassword(), user.getPasswordHash())) {
            throw new CoreThrowHandler(ApiError.VALIDATION,"Email or Password Invalid");
        }


        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuditLog auditLog = AuditLog.builder()
                .userId(user.getId())
                .userName(user.getName())
                .action(AuditLogAction.LOGIN.name())
                .details(user.getName() + " logged in")
                .category(AuditLogCategory.AUTH.toString())
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        UserDTO userDto = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .questionnaireCompleted(user.getQuestionnaireCompleted())
                .isAdmin(user.getRole() == UserRole.ADMIN)
                .risk_profile(user.getRiskProfile())
                .build();

        return AuthSuccessDTO.builder()
                .success(true)
                .message("Login successful")
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessTokenExpirationMs())
                .refreshToken(refreshToken)
                .refreshExpiresIn(jwtService.getRefreshTokenExpirationMs())
                .user(userDto)
                .build();
    }

    //logout
    @Transactional
    public LogoutSuccessDTO logout(String userEmail, UUID userId) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .userName(userEmail != null ? userEmail : "anonymous")
                .action(AuditLogAction.LOGOUT.name())
                .details("User logged out")
                .category(AuditLogCategory.AUTH.name())
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
    public void register(RegisterDTO dto) {
        List<ValidationErrorDetailDTO> errors = new ArrayList<>();
         if (!Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$").matcher(dto.getRegisterRequestEmail()).matches()) {
            errors.add(new ValidationErrorDetailDTO("email", "Invalid email format", "ERR-001"));
        }
            if (!Pattern.compile("[a-z]").matcher(dto.getRegisterRequestPassword()).find()) {
                errors.add(new ValidationErrorDetailDTO("password", "Must contain lowercase letter", "ERR-001"));
            }
            if (!Pattern.compile("[A-Z]").matcher(dto.getRegisterRequestPassword()).find()) {
                errors.add(new ValidationErrorDetailDTO("password", "Must contain uppercase letter", "ERR-001"));
            }
            if (!Pattern.compile("[^a-zA-Z0-9]").matcher(dto.getRegisterRequestPassword()).find()) {
                errors.add(new ValidationErrorDetailDTO("password", "Must contain symbol", "ERR-001"));
        }

        if (!errors.isEmpty()) {
            throw new CoreThrowHandler(ApiError.VALIDATION, errors);
        }

        if (userRepository.existsByEmail(dto.getRegisterRequestEmail())) {
            throw new CoreThrowHandler(ApiError.NOT_UNIQUE_EMAIL);
        }

        User user = User.builder()
                .name(dto.getRegisterRequestName())
                .email(dto.getRegisterRequestEmail())
                .passwordHash(passwordEncoder.encode(dto.getRegisterRequestPassword()))
                .role(UserRole.USER)
                .status(ActiveStatus.ACTIVE.name())
                .questionnaireCompleted(false)
                .build();

        User savedUser = userRepository.save(user);

        // Audit Log
        AuditLog auditLog = AuditLog.builder()
                .userId(savedUser.getId())
                .userName(savedUser.getName())
                .action(AuditLogAction.REGISTER.name())
                .details(user.getName()+ "Successfully registered: ")
                .category(AuditLogCategory.AUTH.name())
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    // Refresh Token
    @Transactional
    public RefreshTokenSuccessDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new CoreThrowHandler(ApiError.INVALID_REQUEST_BODY);
        }

        try {
            // Validate token type
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new CoreThrowHandler(ApiError.INVALID_TOKEN, "Token is not a refresh token");
            }

            // Extract email and load user
            String email = jwtService.getEmailFromToken(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

            // Generate new tokens (rotation)
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            // Audit log
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .action(AuditLogAction.TOKEN_REFRESH.name())
                    .details("Access token refreshed for: " + user.getName())
                    .category(AuditLogCategory.AUTH.name())
                    .timestamp(Instant.now())
                    .build();
            auditLogRepository.save(auditLog);

            return RefreshTokenSuccessDTO.builder()
                    .success(true)
                    .message("Token refreshed successfully")
                    .accessToken(newAccessToken)
                    .expiresIn(jwtService.getAccessTokenExpirationMs())
                    .refreshToken(newRefreshToken)
                    .refreshExpiresIn(jwtService.getRefreshTokenExpirationMs())
                    .build();

        } catch (ExpiredJwtException e) {
            throw new CoreThrowHandler(ApiError.UNAUTHORIZED,"Token Expired");
        } catch (Exception e) {
            throw new CoreThrowHandler(ApiError.INVALID_TOKEN);
        }
    }
}
