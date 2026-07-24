package com.indivaragroup.jdt17wms.constants;

import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorConstants {

    public static final ApiResponse<Void> ERROR_UNAUTHORIZED = ApiResponse.<Void>builder()
            .restApiResponseHttpCode(401)
            .restApiResponseMessage("Unauthorized User")
            .build();

    public static final ApiResponse<Void> ERROR_FORBIDDEN = ApiResponse.<Void>builder()
            .restApiResponseHttpCode(403)
            .restApiResponseMessage("Access Denied")
            .build();
}
