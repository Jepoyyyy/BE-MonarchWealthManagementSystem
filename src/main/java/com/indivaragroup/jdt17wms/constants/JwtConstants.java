package com.indivaragroup.jdt17wms.constants;

import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtConstants {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX_BEARER = "Bearer ";
    public static final String PATH_LOGOUT = ApiPath.LOGOUT_ROUTE;
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String AUTHORITY_PREFIX_ROLE = "ROLE_";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Error {
        public static final String INVALID_TOKEN_TYPE = "Invalid token type";
        public static final String TOKEN_EXPIRED = "Token expired";
        public static final String INVALID_TOKEN = "Invalid token";
        public static final String AUTHENTICATION_FAILED = "Authentication failed";
    }
}
