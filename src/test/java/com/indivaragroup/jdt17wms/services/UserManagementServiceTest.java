package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.AdminUserDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dtos.input.UserStatusUpdateDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagementService userManagementService;

    @Test
    void serviceShouldBeInitialized() {
        assertNotNull(userManagementService);
    }

    @Test
    void getAllUsers_shouldReturnPaginatedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail("john@example.com");
        Page<User> expectedPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findByStatusAndSearch(any(), any(), any(Pageable.class))).thenReturn(expectedPage);

        Page<AdminUserDTO> actualPage = userManagementService.getAllUsers(null, null, pageable);

        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        assertEquals("John Doe", actualPage.getContent().getFirst().getName());
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setName("John Doe");
        user.setEmail("john@example.com");

        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));

        AdminUserDTO actualUser = userManagementService.getUserById(id);

        assertNotNull(actualUser);
        assertEquals("John Doe", actualUser.getName());
    }

    @Test
    void getUserById_shouldThrowNotFound_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            userManagementService.getUserById(id);
        });
    }

    @Test
    void updateUserStatus_shouldUpdateAndReturnUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setStatus("disabled");

        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserStatusUpdateDTO dto = UserStatusUpdateDTO.builder().status("active").build();
        User updatedUser = userManagementService.updateUserStatus(id, dto);

        assertNotNull(updatedUser);
        assertEquals("active", updatedUser.getStatus());
    }

    @Test
    void updateUserStatus_shouldThrowNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(java.util.Optional.empty());

        UserStatusUpdateDTO dto = UserStatusUpdateDTO.builder().status("active").build();
        assertThrows(CoreThrowHandler.class, () -> {
            userManagementService.updateUserStatus(id, dto);
        });
    }
}



