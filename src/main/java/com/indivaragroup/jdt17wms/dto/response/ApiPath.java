package com.indivaragroup.jdt17wms.dto.response;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {
    public static final String BASE_AUTH_PATH = "/api/v1/auth";
    public static final String LOGIN_PATH= "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String LOGOUT_PATH = "/logout";
    public static final String REFRESH_TOKEN_PATH  = "/refresh";
}