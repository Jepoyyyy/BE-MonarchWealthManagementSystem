package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.dto.response.AdminUserDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.dtos.input.UserStatusUpdateDTO;
import com.indivaragroup.jdt17wms.services.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_USERS_PATH)
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ApiResponse<Page<AdminUserDTO>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ApiResponse.success(ApiSuccess.USERS_FETCHED,
                userManagementService.getAllUsers(search, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserDTO> getUser(@PathVariable UUID id) {
        return ApiResponse.success(ApiSuccess.USER_DETAIL_FETCHED,
                userManagementService.getUserById(id));
    }

    @PutMapping("/{id}")
    @AuditLogged(action = "UPDATE_USER_STATUS", category = "USER")
    public ApiResponse<AdminUserDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusUpdateDTO userStatusUpdateDTO) {
        return ApiResponse.success(ApiSuccess.USER_UPDATED,
                userManagementService.updateUserStatus(id, userStatusUpdateDTO));
    }
}
