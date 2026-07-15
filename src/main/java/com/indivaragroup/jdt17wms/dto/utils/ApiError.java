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


    REQUIRED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST.value(), "Refresh Token Required"),
    NOT_UNIQUE_EMAIL(HttpStatus.CONFLICT.value(), "Email Already Used")

    ;
    private final int code;
    private final String message;

}
