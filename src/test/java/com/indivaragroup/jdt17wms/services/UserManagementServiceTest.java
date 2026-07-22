package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AdminUserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.request.UserStatusUpdateDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagementService userManagementService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);


  @Test
    void serviceShouldBeInitialized() {
        assertNotNull(userManagementService);
    }

    @Test
    @DisplayName("getAllUsers - when search and status have values, pass them to repository and map to DTO")
    void getAllUsers_whenSearchAndStatusHaveValues_shouldPassValuesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Instant now = Instant.now(clock);
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@example.com")
                .role(UserRole.USER)
                .status("active")
                .riskProfile("CONSERVATIVE")
                .questionnaireCompleted(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Page<User> mockPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findByStatusAndSearch("active", "john", pageable)).thenReturn(mockPage);

        Page<AdminUserDTO> actualPage = userManagementService.getAllUsers("john", "active", pageable);

        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());

        AdminUserDTO dto = actualPage.getContent().getFirst();
        assertEquals(user.getId(), dto.getId());
        assertEquals("John Doe", dto.getName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals(UserRole.USER, dto.getRole());
        assertEquals("active", dto.getStatus());
        assertEquals("CONSERVATIVE", dto.getRiskProfile());
        assertTrue(dto.getQuestionnaireCompleted());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());

        verify(userRepository).findByStatusAndSearch("active", "john", pageable);
    }

    @Test
    @DisplayName("getAllUsers - when search and status are blank or empty strings, sanitize to null")
    void getAllUsers_whenSearchAndStatusAreBlank_shouldSanitizeToNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> mockPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.findByStatusAndSearch(null, null, pageable)).thenReturn(mockPage);

        Page<AdminUserDTO> actualPage = userManagementService.getAllUsers("   ", "", pageable);

        assertNotNull(actualPage);
        assertTrue(actualPage.isEmpty());
        verify(userRepository).findByStatusAndSearch(null, null, pageable);
    }

    @Test
    @DisplayName("getAllUsers - when search and status are null, pass null to repository")
    void getAllUsers_whenSearchAndStatusAreNull_shouldPassNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> mockPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.findByStatusAndSearch(null, null, pageable)).thenReturn(mockPage);

        Page<AdminUserDTO> actualPage = userManagementService.getAllUsers(null, null, pageable);

        assertNotNull(actualPage);
        assertTrue(actualPage.isEmpty());
        verify(userRepository).findByStatusAndSearch(null, null, pageable);
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        AdminUserDTO actualUser = userManagementService.getUserById(id);

        assertNotNull(actualUser);
        assertEquals("John Doe", actualUser.getName());
    }

    @Test
    void getUserById_shouldThrowNotFound_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> userManagementService.getUserById(id));
        assertEquals(ApiError.ITEM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updateUserStatus_shouldUpdateAndReturnUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .status("disabled")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserStatusUpdateDTO dto = UserStatusUpdateDTO.builder().status("active").build();
        AdminUserDTO updatedUser = userManagementService.updateUserStatus(id, dto);

        assertNotNull(updatedUser);
        assertEquals("active", updatedUser.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserStatus_shouldThrowNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserStatusUpdateDTO dto = UserStatusUpdateDTO.builder().status("active").build();
        CoreThrowHandler ex = assertThrows(CoreThrowHandler.class, () -> userManagementService.updateUserStatus(id, dto));
        assertEquals(ApiError.ITEM_NOT_FOUND.getCode(), ex.getCode());
    }
}
