package com.indivaragroup.jdt17wms.dto.response;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {
    public static final String ROOT_ROUTE = "/api/v1";
    public static final String SPRING_ERROR_URL = "/error";
    public static final String DASHBOARD_SLUG = "/dashboard";
    public static final String ID_SLUG = "/{id}";

    // Auth
    public static final String BASE_AUTH_ROUTE = ROOT_ROUTE + "/auth";
    public static final String LOGIN_ROUTE = "/login";
    public static final String REGISTER_ROUTE = "/register";
    public static final String LOGOUT_ROUTE = "/logout";
    public static final String REFRESH_TOKEN_ROUTE = "/refresh";

    // Admin
    public static final String BASE_ADMIN_ROUTE = ROOT_ROUTE + "/admin";
    public static final String AUDIT_ROUTE = "/audit";
    public static final String BASE_AUDIT_ROUTE = ROOT_ROUTE + AUDIT_ROUTE;
    public static final String AUDIT_SEARCH_ROUTE = "/audit/search";
    public static final String PRODUCTS_ROUTE = "/products";
    public static final String PRODUCTS_ID_ROUTE = "/products/{id}";

    // User & Recommendations
    public static final String BASE_USER_ROUTE = ROOT_ROUTE + "/me";
    public static final String HEALTH_ROUTE = "/health";
    public static final String RECOMMENDATIONS_ROUTE = "/recommendations";

    // Assets
    public static final String BASE_ASSETS_ROUTE = ROOT_ROUTE + "/me/assets";
    public static final String PNL_ROUTE = "/pnl";
    public static final String TRANSACTION_LOGS_ROUTE = "/transaction-logs";
    public static final String ASSET_TRANSACTIONS_ROUTE = "/{assetId}/transactions";
    public static final String ASSET_VALUE_ROUTE = "/{assetId}/value";
    public static final String ASSET_GOAL_ROUTE = "/{assetId}/goal";
    public static final String ASSET_PNL_ROUTE = "/{assetId}/pnl";

    // Goals
    public static final String BASE_GOALS_ROUTE = ROOT_ROUTE + "/me/goals";
    public static final String PROJECTIONS_ROUTE = "/projections";
    public static final String AUTO_ALLOCATE_ROUTE = "/auto-allocate";
    public static final String PROGRESS_ROUTE = "/progress";

    // Product
    public static final String BASE_PRODUCTS_ROUTE = ROOT_ROUTE + PRODUCTS_ROUTE;

    // Risk Profiler
    public static final String BASE_PROFILER_ROUTE = ROOT_ROUTE + "/me/profiler";

    // Finances
    public static final String BASE_FINANCES_ROUTE = ROOT_ROUTE + "/me/finances";

    // Users
    public static final String BASE_USERS_ROUTE = ROOT_ROUTE + "/users";
}
