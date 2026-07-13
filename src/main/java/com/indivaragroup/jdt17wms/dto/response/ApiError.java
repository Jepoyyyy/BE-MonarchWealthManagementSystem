package com.indivaragroup.jdt17wms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@AllArgsConstructor
@Getter
public enum ApiError {
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(),"BAD REQUEST"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(),"UNAUTHORIZED"),
    CONFLICT(HttpStatus.CONFLICT.value(),"RESOURCE ALREADY EXISTS"),
    VALIDATION(HttpStatus.BAD_REQUEST.value(), "INVALID FIELD VALUES"),
    NOT_FOUND(HttpStatus.NOT_FOUND.value(), "RESOURCE NOT FOUND"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN")
    ;
    private final int code;
    private final String message;

}
