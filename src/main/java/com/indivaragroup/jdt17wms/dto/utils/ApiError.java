package com.indivaragroup.jdt17wms.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@AllArgsConstructor
@Getter
public enum ApiError {
    //Global Errornya ini
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(),"BAD REQUEST"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(),"UNAUTHORIZED"),
    CONFLICT(HttpStatus.CONFLICT.value(),"RESOURCE ALREADY EXISTS"),
    NOT_FOUND(HttpStatus.NOT_FOUND.value(), "RESOURCE NOT FOUND"),
    VALIDATION(HttpStatus.BAD_REQUEST.value(), "INVALID FIELD VALUES"),

    // Khusus Invalid
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST.value(),"Invalid Request Body"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "User Not Found"),
    DELISTED_PRODUCT(HttpStatus.CONFLICT.value(), "Can’t track delisted products"),
    REQUIRED_RISK_PROFILER(HttpStatus.FORBIDDEN.value(), "Risk Profiler Assessment Required"),
    REQUIRED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST.value(), "Refresh Token Required"),
    NOT_UNIQUE_EMAIL(HttpStatus.CONFLICT.value(), "Email Already Used"),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "Item Not Found"),
    DUPLICATE_PRIORITY_GOALS(HttpStatus.CONFLICT.value(), "Can’t set more than 1 priority"),
    INSUFFICIENT_INCOME(HttpStatus.FORBIDDEN.value(), "Can’t set more allocation than income"),
    GOAL_BELONGS_TO_DIFFERENT_USER(HttpStatus.FORBIDDEN.value(), "Access denied. Goal belongs to different user"),
    FINANCIAL_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "Financial profile not found"),
    NOT_ABOVE_ZERO(HttpStatus.BAD_REQUEST.value(), "Units must be greater than zero"),

    // Auth errors
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST.value(), "Email Or Password Invalid"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST.value(), "Email or Password Invalid"),
    ACCOUNT_INACTIVE(HttpStatus.UNAUTHORIZED.value(), "Account is Not active. Please Contact Admin"),
    NOT_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED.value(), "Token is not a refresh token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED.value(), "Token Expired"),

    // Transaction & PnL errors
    INSUFFICIENT_UNITS(HttpStatus.BAD_REQUEST.value(), "Insufficient units: available %s, requested %s"),
    NO_UNITS_AVAILABLE(HttpStatus.BAD_REQUEST.value(), "No units available to sell"),
    STOCK_AMOUNT_SELL_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "Stocks can only be sold by units, not by amount"),
    INVALID_LOT_SIZE(HttpStatus.BAD_REQUEST.value(), "Stock must be traded in lot multiples of %d"),
    FRACTIONAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "Fractional units not allowed for this product"),
    TRANSACTION_TYPE_REQUIRED(HttpStatus.BAD_REQUEST.value(), "Either units or amount must be provided"),
    BOTH_UNITS_AND_AMOUNT(HttpStatus.BAD_REQUEST.value(), "Provide either units or amount, not both"),
    INVALID_TRANSACTION(HttpStatus.BAD_REQUEST.value(), "Invalid transaction action"),
    BELOW_MIN_INVESTMENT(HttpStatus.BAD_REQUEST.value(), "Amount must be at least minimum investment of %s"),
    CORRUPT_DATA(HttpStatus.BAD_REQUEST.value(), "Asset has null units — data corrupt"),
    CORRUPT_DATA_DETAIL(HttpStatus.BAD_REQUEST.value(), "Asset has negative remaining units (%s): sold units exceed owned units — data corrupt"),
    NULL_CURRENT_VALUE(HttpStatus.BAD_REQUEST.value(), "null currentValue"),
    INVALID_ANSWER_COUNT(HttpStatus.BAD_REQUEST.value(), "Invalid answer count"),
    ;

    private final int code;
    private final String message;

    public String format(Object... args) {
        return String.format(this.message, args);
    }
}
