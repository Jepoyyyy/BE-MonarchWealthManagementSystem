package com.indivaragroup.jdt17wms.dto.response;

import com.indivaragroup.jdt17wms.models.enums.UserRole;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserDTO {
    private UUID id;
    private String name;
    private String email;
    private UserRole role;
    private String status;
    private String riskProfile;
    private Boolean questionnaireCompleted;
    private Instant createdAt;
    private Instant updatedAt;
}
