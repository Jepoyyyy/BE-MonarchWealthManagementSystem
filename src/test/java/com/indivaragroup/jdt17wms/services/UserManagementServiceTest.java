package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.ApiError;
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
        Page<User> expectedPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        Page<User> actualPage = userManagementService.getAllUsers(pageable);

        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        assertEquals(user, actualPage.getContent().get(0));
    }

    @Test
    void updateUserStatus_shouldUpdateAndReturnUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setStatus("disabled");

        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userManagementService.updateUserStatus(id, "active");

        assertNotNull(updatedUser);
        assertEquals("active", updatedUser.getStatus());
    }

    @Test
    void updateUserStatus_shouldThrowNotFoundException_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(CoreThrowHandler.class, () -> {
            userManagementService.updateUserStatus(id, "active");
        });
    }

    @Test
    void updateUserStatus_shouldThrowBadRequestException_whenStatusIsInvalid() {
        UUID id = UUID.randomUUID();
        org.junit.jupiter.api.Assertions.assertThrows(CoreThrowHandler.class, () -> {
            userManagementService.updateUserStatus(id, "invalid_status");
        });
    }
}



