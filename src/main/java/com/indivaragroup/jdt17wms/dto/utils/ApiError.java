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
//  Khusus Invalid
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
    FINANCIAL_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "Financial profile not found"),

    // Transaction errors
    INSUFFICIENT_UNITS(HttpStatus.BAD_REQUEST.value(), "Insufficient units available for sale"),
    STOCK_AMOUNT_SELL_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "Stocks can only be sold by units"),
    INVALID_LOT_SIZE(HttpStatus.BAD_REQUEST.value(), "Transaction must be in lot multiples"),
    FRACTIONAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "Fractional units not allowed for this product"),
    TRANSACTION_TYPE_REQUIRED(HttpStatus.BAD_REQUEST.value(), "Must specify either units or amount"),
    BOTH_UNITS_AND_AMOUNT(HttpStatus.BAD_REQUEST.value(), "Cannot specify both units and amount"),
    ;
    private final int code;
    private final String message;

}
