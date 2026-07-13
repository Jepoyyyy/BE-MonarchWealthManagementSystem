package com.indivaragroup.jdt17wms.exceptions;

import com.indivaragroup.jdt17wms.dto.response.ApiError;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Getter
public class CoreThrowHandler extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String message;
    private final Integer code;
    private final Map<String, Serializable> error;

    public CoreThrowHandler(ApiError status,
                            String message,
                            Map<String, Serializable> error
    ) {
        super(message);
        this.code = status.getCode();
        this.message = message;
        this.error = error;
    }
}
