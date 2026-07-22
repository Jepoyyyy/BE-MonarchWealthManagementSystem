package com.indivaragroup.jdt17wms.dto.response;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {
  public static final String ROOT_URL = "/api/v1";
  public static final String SPRING_ERROR_URL = "/error";

    //Auth
    public static final String BASE_AUTH_PATH = ROOT_URL + "/auth";
    public static final String LOGIN_PATH= "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String LOGOUT_PATH = "/logout";
    public static final String REFRESH_TOKEN_PATH  = "/refresh";

    //Admin
    public static final String BASE_ADMIN_PATH =ROOT_URL + "/admin";
    public static final String BASE_AUDIT_PATH =ROOT_URL + "/audit";


    public static final String BASE_USER_PATH = ROOT_URL + "/me";
    public static final String BASE_ASSETS_PATH = ROOT_URL + "/me/assets";
    public static final String BASE_GOALS_PATH = ROOT_URL + "/me/goals";

    //product
    public static final String BASE_PRODUCTS_PATH = ROOT_URL + "/products";

    //risk profiler
    public static final String BASE_PROFILER_PATH = ROOT_URL + "/me/profiler";

    //users
    public static final String BASE_USERS_PATH = ROOT_URL + "/users";
}
