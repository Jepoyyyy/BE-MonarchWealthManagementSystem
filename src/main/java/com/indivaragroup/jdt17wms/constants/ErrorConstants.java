package com.indivaragroup.jdt17wms.constants;

import com.indivaragroup.jdt17wms.dto.utils.ErrorResponseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorConstants {

    public static final ErrorResponseDTO ERROR_UNAUTHORIZED = ErrorResponseDTO.builder()
            .error("Unauthorized")
            .code(401)
            .build();

    public static final ErrorResponseDTO ERROR_FORBIDDEN = ErrorResponseDTO.builder()
            .error("Forbidden")
            .code(403)
            .build();
}
