package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.response.AdminUserDTO;
import com.indivaragroup.jdt17wms.dtos.input.UserStatusUpdateDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class UserManagementService {

    private final UserRepository userRepository;

    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private AdminUserDTO toDTO(User u) {
        return AdminUserDTO.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .riskProfile(u.getRiskProfile())
                .questionnaireCompleted(u.getQuestionnaireCompleted())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())

                .build();
    }

    public Page<AdminUserDTO> getAllUsers(String search, String status, Pageable pageable) {
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String statusParam = (status != null && !status.isBlank()) ? status : null;
        return userRepository.findByStatusAndSearch(statusParam, searchParam, pageable).map(this::toDTO);
    }

    public AdminUserDTO getUserById(UUID id) {
        return toDTO(userRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND)));
    }

    public User updateUserStatus(UUID id, UserStatusUpdateDTO userStatusUpdateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));
        user.setStatus(userStatusUpdateDTO.getStatus());
        return userRepository.save(user);
    }
}
