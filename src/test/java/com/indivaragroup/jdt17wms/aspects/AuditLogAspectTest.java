package com.indivaragroup.jdt17wms.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.models.Goal;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinancialProfileRepository financialProfileRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private AuditLogAspect aspect;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;
    private final UUID userId = UUID.randomUUID();
    private final String userName = "Test User";

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);


  @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        UserDTO userDTO = UserDTO.builder().id(userId).name(userName).build();

        when(authentication.getPrincipal()).thenReturn(userDTO);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void logAudit_createGoal_savesAuditLogWithChangedValue() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        AuditLogged auditLogged = mock(AuditLogged.class);
        when(auditLogged.action()).thenReturn("CREATE_GOAL");
        when(auditLogged.category()).thenReturn("GOAL");

        GoalEditingDTO dto = GoalEditingDTO.builder()
                .name("New Laptop")
                .targetAmount(BigDecimal.valueOf(1000.00))
                .monthlyContribution(BigDecimal.valueOf(100.00))
                .targetDate(LocalDate.now(clock).plusMonths(10))
                .isPriority(true)
                .notes("Saving for new laptop")
                .build();

        when(pjp.getArgs()).thenReturn(new Object[]{dto});
        when(pjp.proceed()).thenReturn(ApiResponse.success(ApiSuccess.GOAL_CREATED, null));

        aspect.logAudit(pjp, auditLogged);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("CREATE_GOAL", savedLog.getAction());
        assertEquals("GOAL", savedLog.getCategory());
        assertEquals(userId, savedLog.getUserId());
        assertEquals(userName, savedLog.getUserName());
        assertNotNull(savedLog.getChangedValue());

        String json = savedLog.getChangedValue();
        assertTrue(json.contains("\"field\":\"name\""));
        assertTrue(json.contains("\"old_value\":null"));
        assertTrue(json.contains("\"new_value\":\"New Laptop\""));
        assertTrue(savedLog.getDetails().contains("New Laptop"));
    }

    @Test
    void logAudit_updateGoal_tracksChangesAndSavesAuditLog() throws Throwable {
        UUID goalId = UUID.randomUUID();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        AuditLogged auditLogged = mock(AuditLogged.class);
        when(auditLogged.action()).thenReturn("UPDATE_GOAL");
        when(auditLogged.category()).thenReturn("GOAL");

        Goal oldGoal = Goal.builder()
                .id(goalId)
                .userId(userId)
                .name("Old Laptop")
                .targetAmount(BigDecimal.valueOf(500000.00))
                .monthlyContribution(BigDecimal.valueOf(500.00))
                .targetDate(LocalDate.now(clock).plusMonths(10))
                .isPriority(false)
                .notes("Old notes")
                .build();

        GoalEditingDTO dto = GoalEditingDTO.builder()
                .name("Old Laptop") // unchanged
                .targetAmount(BigDecimal.valueOf(600000.00)) // changed
                .monthlyContribution(BigDecimal.valueOf(500.00)) // unchanged
                .targetDate(LocalDate.now(clock).plusMonths(10)) // unchanged
                .isPriority(true) // changed
                .notes("New notes") // changed
                .build();

        when(pjp.getArgs()).thenReturn(new Object[]{goalId, dto});
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(oldGoal));
        when(pjp.proceed()).thenReturn(ApiResponse.success(ApiSuccess.GOAL_UPDATED, null));

        aspect.logAudit(pjp, auditLogged);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("UPDATE_GOAL", savedLog.getAction());
        assertEquals("GOAL", savedLog.getCategory());
        assertNotNull(savedLog.getChangedValue());

        // Verify JSON content has old and new values for changed fields only
        String json = savedLog.getChangedValue();
        assertTrue(json.contains("\"field\":\"target_amount\""));
        assertTrue(json.contains("\"old_value\":500000.0"));
        assertTrue(json.contains("\"new_value\":600000.0"));

        assertTrue(json.contains("\"field\":\"is_priority\""));
        assertTrue(json.contains("\"old_value\":false"));
        assertTrue(json.contains("\"new_value\":true"));

        assertTrue(json.contains("\"field\":\"notes\""));
        assertTrue(json.contains("\"old_value\":\"Old notes\""));
        assertTrue(json.contains("\"new_value\":\"New notes\""));

        // Unchanged fields should not be tracked
        assertFalse(json.contains("\"field\":\"name\""));
        assertFalse(json.contains("\"field\":\"monthly_contribution\""));
    }

    @Test
    void logAudit_deleteGoal_savesAuditLogWithoutChangedValue() throws Throwable {
        UUID goalId = UUID.randomUUID();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        AuditLogged auditLogged = mock(AuditLogged.class);
        when(auditLogged.action()).thenReturn("DELETE_GOAL");
        when(auditLogged.category()).thenReturn("GOAL");

        Goal oldGoal = Goal.builder()
                .id(goalId)
                .userId(userId)
                .name("Old Laptop")
                .build();

        when(pjp.getArgs()).thenReturn(new Object[]{goalId});
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(oldGoal));
        when(pjp.proceed()).thenReturn(ApiResponse.success(ApiSuccess.GOAL_DELETED, null));

        aspect.logAudit(pjp, auditLogged);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("DELETE_GOAL", savedLog.getAction());
        assertEquals("GOAL", savedLog.getCategory());
        assertNull(savedLog.getChangedValue());
        assertTrue(savedLog.getDetails().contains("Deleted Goal"));
    }
}
