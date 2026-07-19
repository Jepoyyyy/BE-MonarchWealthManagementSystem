package com.indivaragroup.jdt17wms.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ApiSuccess {

    // Auth
    LOGIN(HttpStatus.OK.value(), "Login successful"),
    REGISTER(HttpStatus.CREATED.value(), "Registration successful"),
    LOGOUT(HttpStatus.OK.value(), "Logout successful"),
    REFRESH_TOKEN(HttpStatus.OK.value(), "Token refreshed successfully"),

    // Generic CRUD
    OK(HttpStatus.OK.value(), "Request successful"),
    CREATED(HttpStatus.CREATED.value(), "Resource created successfully"),
    UPDATED(HttpStatus.OK.value(), "Resource updated successfully"),
    DELETED(HttpStatus.OK.value(), "Resource deleted successfully"),
    FETCHED(HttpStatus.OK.value(), "Data retrieved successfully"),

    // User & Admin
    USER_FETCHED(HttpStatus.OK.value(), "User data retrieved successfully"),
    USER_UPDATED(HttpStatus.OK.value(), "User updated successfully"),
    USERS_FETCHED(HttpStatus.OK.value(), "Users retrieved successfully"),
    USER_DETAIL_FETCHED(HttpStatus.OK.value(), "User detail retrieved successfully"),
    AUDIT_LOGS_FETCHED(HttpStatus.OK.value(), "Audit logs retrieved successfully"),

    // Dashboard
    DASHBOARD_FETCHED(HttpStatus.OK.value(), "Dashboard data retrieved successfully"),

    // Goals
    GOALS_FETCHED(HttpStatus.OK.value(), "Goals retrieved successfully"),
    GOAL_CREATED(HttpStatus.CREATED.value(), "Goal created successfully"),
    GOAL_UPDATED(HttpStatus.OK.value(), "Goal updated successfully"),
    GOAL_DELETED(HttpStatus.OK.value(), "Goal deleted successfully"),
    GOAL_PROJECTIONS_FETCHED(HttpStatus.OK.value(), "Goal projections retrieved successfully"),
    GOAL_PROGRESS_FETCHED(HttpStatus.OK.value(), "Goal progress fetched successfully"),

    // Assets
    ASSETS_FETCHED(HttpStatus.OK.value(), "Assets retrieved successfully"),
    ASSET_CREATED(HttpStatus.CREATED.value(), "Asset created successfully"),
    ASSET_UPDATED(HttpStatus.OK.value(), "Asset updated successfully"),
    ASSET_DELETED(HttpStatus.OK.value(), "Asset deleted successfully"),
    TRANSACTION_LOGS_FETCHED(HttpStatus.OK.value(), "Transaction logs retrieved successfully"),

    // Products
    PRODUCTS_FETCHED(HttpStatus.OK.value(), "Products retrieved successfully"),
    PRODUCT_UPDATED(HttpStatus.OK.value(), "Product updated successfully"),
    PRODUCT_FETCHED(HttpStatus.OK.value(), "Product retreived successfully"),
    ADMIN_PRODUCTS_FETCHED(HttpStatus.OK.value(), "Admin products retrieved successfully"),
    ADMIN_PRODUCT_CREATED(HttpStatus.CREATED.value(), "Admin product created successfully"),
    ADMIN_PRODUCT_UPDATED(HttpStatus.OK.value(), "Admin product updated successfully"),

    // Risk Profiler
    PROFILER_FETCHED(HttpStatus.OK.value(), "Risk profile retrieved successfully"),
    PROFILER_UPDATED(HttpStatus.OK.value(), "Risk profile updated successfully"),

    // Recommendations
    RECOMMENDATIONS_FETCHED(HttpStatus.OK.value(), "Recommendations retrieved successfully"),

    // Finances
    FINANCES_FETCHED(HttpStatus.OK.value(), "Financial profile retrieved successfully"),
    FINANCES_UPDATED(HttpStatus.OK.value(), "Financial profile updated successfully"),

    // Health
    HEALTH_OK(HttpStatus.OK.value(), "Service is healthy"),

    // Transactions
    EXECUTED(HttpStatus.OK.value(), "Transaction executed successfully");

    private final int code;
    private final String message;
}
