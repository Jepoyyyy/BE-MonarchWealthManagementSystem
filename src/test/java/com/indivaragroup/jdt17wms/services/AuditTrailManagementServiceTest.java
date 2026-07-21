package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AuditLogDTO;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditTrailManagementServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditTrailManagementService auditTrailManagementService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);


    @Test
    @DisplayName("serviceShouldBeInitialized")
    void serviceShouldBeInitialized() {
        assertNotNull(auditTrailManagementService);
    }

    @Test
    @DisplayName("getAuditLogs - when headView is true, should query first 5 ordered by timestamp DESC")
    void getAuditLogs_whenHeadViewIsTrue_shouldQueryFirstFiveOrderedByTimestampDesc() {
        Page<AuditLog> mockPage = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<AuditLogDTO> result = auditTrailManagementService.getAuditLogs(true, Pageable.unpaged());

        assertNotNull(result);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(5, capturedPageable.getPageSize());
        Sort.Order order = capturedPageable.getSort().getOrderFor("timestamp");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("getAuditLogs - when headView is false, should query with passed pageable")
    void getAuditLogs_whenHeadViewIsFalse_shouldQueryWithPassedPageable() {
        Page<AuditLog> mockPage = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Pageable passedPageable = PageRequest.of(2, 10, Sort.by("action"));
        Page<AuditLogDTO> result = auditTrailManagementService.getAuditLogs(false, passedPageable);

        assertNotNull(result);
        verify(auditLogRepository).findAll(passedPageable);
    }

    @Test
    @DisplayName("getAuditLogs - when headView is null, should query with passed pageable")
    void getAuditLogs_whenHeadViewIsNull_shouldQueryWithPassedPageable() {
        Page<AuditLog> mockPage = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Pageable passedPageable = PageRequest.of(1, 15);
        Page<AuditLogDTO> result = auditTrailManagementService.getAuditLogs(null, passedPageable);

        assertNotNull(result);
        verify(auditLogRepository).findAll(passedPageable);
    }

    @Test
    @DisplayName("getFilteredAuditLogs - when category and search have text, pass values directly to repository")
    void getFilteredAuditLogs_whenCategoryAndSearchAreValid_shouldPassValuesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        Page<AuditLog> mockPage = new PageImpl<>(Collections.emptyList());

        when(auditLogRepository.findFiltered("SECURITY", "LOGIN", from, to, pageable))
                .thenReturn(mockPage);

        Page<AuditLogDTO> result = auditTrailManagementService.getFilteredAuditLogs(
                "SECURITY", "LOGIN", from, to, pageable);

        assertNotNull(result);
        assertEquals(mockPage.getTotalElements(), result.getTotalElements());
        verify(auditLogRepository).findFiltered("SECURITY", "LOGIN", from, to, pageable);
    }

    @Test
    @DisplayName("getFilteredAuditLogs - when category and search are blank or empty strings, sanitize to null")
    void getFilteredAuditLogs_whenCategoryAndSearchAreBlank_shouldSanitizeToNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        Page<AuditLog> mockPage = new PageImpl<>(Collections.emptyList());

        when(auditLogRepository.findFiltered(null, null, from, to, pageable))
                .thenReturn(mockPage);

        Page<AuditLogDTO> result = auditTrailManagementService.getFilteredAuditLogs(
                "   ", "", from, to, pageable);

        assertNotNull(result);
        assertEquals(mockPage.getTotalElements(), result.getTotalElements());
        verify(auditLogRepository).findFiltered(null, null, from, to, pageable);
    }

    @Test
    @DisplayName("getFilteredAuditLogs - when category and search are null, pass null to repository")
    void getFilteredAuditLogs_whenCategoryAndSearchAreNull_shouldPassNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> mockPage = new PageImpl<>(Collections.emptyList());

        when(auditLogRepository.findFiltered(null, null, null, null, pageable))
                .thenReturn(mockPage);

        Page<AuditLogDTO> result = auditTrailManagementService.getFilteredAuditLogs(
                null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(mockPage.getTotalElements(), result.getTotalElements());
        verify(auditLogRepository).findFiltered(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("getAuditLogs - should map AuditLog entity to AuditLogDTO correctly")
    void getAuditLogs_shouldMapAuditLogToDTO() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now(clock);

        AuditLog log = AuditLog.builder()
                .id(id)
                .userId(userId)
                .userName("John Doe")
                .action("LOGIN")
                .details("Logged in successfully")
                .category("SECURITY")
                .timestamp(now)
                .changedValue("status: active")
                .build();

        Page<AuditLog> mockPage = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<AuditLogDTO> result = auditTrailManagementService.getAuditLogs(false, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        AuditLogDTO dto = result.getContent().getFirst();
        assertEquals(id, dto.getId());
        assertEquals(userId, dto.getUserId());
        assertEquals("John Doe", dto.getUserName());
        assertEquals("LOGIN", dto.getAction());
        assertEquals("Logged in successfully", dto.getDetails());
        assertEquals("SECURITY", dto.getCategory());
        assertEquals(now, dto.getTimestamp());
        assertEquals("status: active", dto.getChangedValue());
    }
}
