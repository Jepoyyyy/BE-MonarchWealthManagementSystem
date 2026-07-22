package com.indivaragroup.jdt17wms.dto.utils;

import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {

    public static final UUID STATIC_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDTO authData && authData.getId() != null) {
            return authData.getId();
        }
        throw new CoreThrowHandler(ApiError.UNAUTHORIZED);
    }
}
