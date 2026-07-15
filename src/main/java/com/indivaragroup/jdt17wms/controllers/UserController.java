package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.AdminUserAccessDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.services.UserManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/api/v1/users")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(Pageable pageable) {
        Page<User> result = userManagementService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.<Page<User>>builder()
                .restApiResponseHttpCode(ApiSuccess.USERS_FETCHED.getCode())
                .restApiResponseMessage(ApiSuccess.USERS_FETCHED.getMessage())
                .restApiResponseResult(result)
                .restApiResponseError(null)
                .build());
    }

    @PutMapping("/api/v1/users/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable UUID id,
            @RequestBody AdminUserAccessDTO adminUserAccessDTO) {
        User result = userManagementService.updateUserStatus(id, adminUserAccessDTO.getStatus());
        return ResponseEntity.ok(ApiResponse.<User>builder()
                .restApiResponseHttpCode(ApiSuccess.USER_UPDATED.getCode())
                .restApiResponseMessage(ApiSuccess.USER_UPDATED.getMessage())
                .restApiResponseResult(result)
                .restApiResponseError(null)
                .build());
    }
}
