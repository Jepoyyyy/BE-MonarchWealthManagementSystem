package com.indivaragroup.jdt17wms.exceptions;

import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class CoreThrowHandler extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;
    private final Map<String, Serializable> error;
    private final List<ValidationErrorDetailDTO> details;

    // Business error — paling sering dipake
    public CoreThrowHandler(ApiError apiError, String message) {
        super(message);
        this.code = apiError.getCode();
        this.error = Collections.emptyMap();
        this.details = null;
    }

    // Dengan error map tambahan
    public CoreThrowHandler(ApiError apiError, String message, Map<String, Serializable> error) {
        super(message);
        this.code = apiError.getCode();
        this.error = error;
        this.details = null;
    }

    public CoreThrowHandler(ApiError apiError, List<ValidationErrorDetailDTO> details) {
        super(ApiError.VALIDATION.getMessage());
        this.code = apiError.getCode();
        this.error = Collections.emptyMap();
        this.details = details;
    }

    public CoreThrowHandler(ApiError apiError, String message, Map<String, Serializable> error, List<ValidationErrorDetailDTO> details) {
        super(message);
        this.code = apiError.getCode();
        this.error = error;
        this.details = details;
    }

    public CoreThrowHandler(ApiError apiError){
        super(apiError.getMessage());
        this.code = apiError.getCode();
        this.error=Collections.emptyMap();
        this.details=null;
    }

}
