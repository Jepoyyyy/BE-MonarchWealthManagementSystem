package com.indivaragroup.jdt17wms.dto.response;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {
    //Auth
    public static final String BASE_AUTH_PATH = "/api/v1/auth";
    public static final String LOGIN_PATH= "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String LOGOUT_PATH = "/logout";
    public static final String REFRESH_TOKEN_PATH  = "/refresh";

    //Admin
    public static final String BASE_ADMIN_PATH ="/api/v1/admin";


    public static final String BASE_USER_PATH = "/api/v1/me";
    public static final String BASE_ASSETS_PATH = "/api/v1/me/assets";
    public static final String BASE_GOALS_PATH = "/api/v1/me/goals";

    //product
    public static final String BASE_PRODUCTS_PATH = "/api/v1/products";

    //risk profiler
    public static final String BASE_PROFILER_PATH = "/api/v1/me/profiler";

    //users
    public static final String BASE_USERS_PATH = "/api/v1/users";
}