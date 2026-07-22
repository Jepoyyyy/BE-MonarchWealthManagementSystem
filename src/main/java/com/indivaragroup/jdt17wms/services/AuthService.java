package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.JwtConstants;
import com.indivaragroup.jdt17wms.dto.request.BearerHeaderDTO;
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

    public static final String VALIDATION_ERROR_CODE = "ERR-001";
    public static final String EMAIL_FIELD = "email";
    public static final String PASSWORD_FIELD = "password";
    public static final String ACTIVE_STATUS = "active";
    public static final String ANONYMOUS_USER = "anonymous";

    private static final String MSG_LOGIN_SUCCESSFUL = "Login successful";
    private static final String MSG_LOGOUT_SUCCESSFUL = "Logout successful";
    private static final String MSG_TOKEN_REFRESHED_SUCCESSFUL = "Token refreshed successfully";

    private static final String MSG_INVALID_EMAIL_FORMAT = "Invalid email format";
    private static final String MSG_MUST_CONTAIN_LOWERCASE = "Must contain lowercase letter";
    private static final String MSG_MUST_CONTAIN_UPPERCASE = "Must contain uppercase letter";
    private static final String MSG_MUST_CONTAIN_SYMBOL = "Must contain symbol";

    private static final String AUDIT_LOGGED_IN_SUFFIX = " logged in";
    private static final String AUDIT_LOGGED_OUT_DETAILS = "User logged out";
    private static final String AUDIT_REGISTERED_PREFIX = "Successfully registered: ";
    private static final String AUDIT_REFRESHED_PREFIX = "Access token refreshed for: ";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

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
    public AuthSuccessDTO login(LoginDTO dto) {

        User user = userRepository.findByEmail(dto.getLoginRequestEmail())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.INVALID_CREDENTIALS));

        if (!ACTIVE_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new CoreThrowHandler(ApiError.ACCOUNT_INACTIVE);
        }

        if (!passwordEncoder.matches(dto.getLoginRequestPassword(), user.getPasswordHash())) {
            throw new CoreThrowHandler(ApiError.INVALID_PASSWORD);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuditLog auditLog = AuditLog.builder()
                .userId(user.getId())
                .userName(user.getName())
                .action(AuditLogAction.LOGIN.name())
                .details(user.getName() + AUDIT_LOGGED_IN_SUFFIX)
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
                .riskProfile(user.getRiskProfile())
                .build();

        return AuthSuccessDTO.builder()
                .success(true)
                .message(MSG_LOGIN_SUCCESSFUL)
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessTokenExpirationMs())
                .refreshToken(refreshToken)
                .refreshExpiresIn(jwtService.getRefreshTokenExpirationMs())
                .user(userDto)
                .build();
    }

    //logout
    @Transactional
    public LogoutSuccessDTO logout(BearerHeaderDTO headerDTO) {
        String email = null;
        UUID userId = null;
        if (headerDTO != null && headerDTO.getAuthHeader() != null && headerDTO.getAuthHeader().startsWith(JwtConstants.TOKEN_PREFIX_BEARER)) {
            try {
                String token = headerDTO.getAuthHeader().substring(JwtConstants.TOKEN_PREFIX_BEARER.length());
                email = jwtService.getEmailFromToken(token);
                userId = jwtService.getUserIdFromToken(token);
            } catch (Exception e) {
                // Token extraction failed
            }
        }
        return performLogout(email, userId);
    }

    @Transactional
    public LogoutSuccessDTO logout(String userEmail, UUID userId) {
        return performLogout(userEmail, userId);
    }

    private LogoutSuccessDTO performLogout(String userEmail, UUID userId) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .userName(userEmail != null ? userEmail : ANONYMOUS_USER)
                .action(AuditLogAction.LOGOUT.name())
                .details(AUDIT_LOGGED_OUT_DETAILS)
                .category(AuditLogCategory.AUTH.name())
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(auditLog);

        return LogoutSuccessDTO.builder()
                .success(true)
                .message(MSG_LOGOUT_SUCCESSFUL)
                .build();
    }

    @Transactional
    public void register(RegisterDTO dto) {
        List<ValidationErrorDetailDTO> errors = new ArrayList<>();
        if (!EMAIL_PATTERN.matcher(dto.getRegisterRequestEmail()).matches()) {
            errors.add(new ValidationErrorDetailDTO(EMAIL_FIELD, MSG_INVALID_EMAIL_FORMAT, VALIDATION_ERROR_CODE));
        }
        if (!LOWERCASE_PATTERN.matcher(dto.getRegisterRequestPassword()).find()) {
            errors.add(new ValidationErrorDetailDTO(PASSWORD_FIELD, MSG_MUST_CONTAIN_LOWERCASE, VALIDATION_ERROR_CODE));
        }
        if (!UPPERCASE_PATTERN.matcher(dto.getRegisterRequestPassword()).find()) {
            errors.add(new ValidationErrorDetailDTO(PASSWORD_FIELD, MSG_MUST_CONTAIN_UPPERCASE, VALIDATION_ERROR_CODE));
        }
        if (!SYMBOL_PATTERN.matcher(dto.getRegisterRequestPassword()).find()) {
            errors.add(new ValidationErrorDetailDTO(PASSWORD_FIELD, MSG_MUST_CONTAIN_SYMBOL, VALIDATION_ERROR_CODE));
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
                .details(AUDIT_REGISTERED_PREFIX + user.getName())
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
                throw new CoreThrowHandler(ApiError.NOT_REFRESH_TOKEN);
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
                    .details(AUDIT_REFRESHED_PREFIX + user.getName())
                    .category(AuditLogCategory.AUTH.name())
                    .timestamp(Instant.now())
                    .build();
            auditLogRepository.save(auditLog);

            return RefreshTokenSuccessDTO.builder()
                    .success(true)
                    .message(MSG_TOKEN_REFRESHED_SUCCESSFUL)
                    .accessToken(newAccessToken)
                    .expiresIn(jwtService.getAccessTokenExpirationMs())
                    .refreshToken(newRefreshToken)
                    .refreshExpiresIn(jwtService.getRefreshTokenExpirationMs())
                    .build();

        } catch (ExpiredJwtException e) {
            throw new CoreThrowHandler(ApiError.EXPIRED_TOKEN);
        } catch (Exception e) {
            throw new CoreThrowHandler(ApiError.INVALID_TOKEN);
        }
    }
}
