package com.indivaragroup.jdt17wms.exceptions;

import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlingAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);

    @ExceptionHandler(CoreThrowHandler.class)
    public ResponseEntity<ApiResponse<?>> handleCoreThrowHandler(CoreThrowHandler ex) {
        Map<String, Serializable> errorMap = (ex.getError() != null && !ex.getError().isEmpty())
                ? ex.getError()
                : null;

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(ex.getCode())
                .restApiResponseMessage(ex.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.status(ex.getCode()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleJsonParseError(HttpMessageNotReadableException ex) {
        String message = ex.getMessage();
        String errorMsg;

        if (message != null && message.contains("UnrecognizedPropertyException")) {
            String field = message.replaceAll(".*\\[\"([^\"]+)\"].*", "$1");
            errorMsg = "Unrecognized field: " + field;
        } else {
            errorMsg = "Malformed JSON request body";
        }

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put("detail", errorMsg);

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(ApiError.INVALID_REQUEST_BODY.getCode())
                .restApiResponseMessage(ApiError.INVALID_REQUEST_BODY.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationErrorDetailDTO> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ValidationErrorDetailDTO.builder()
                        .field(error instanceof FieldError f ? f.getField() : error.getObjectName())
                        .reason(error.getDefaultMessage())
                        .type("ERR-001")
                        .build())
                .toList();

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put("fields", (Serializable) details);

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(ApiError.VALIDATION.getCode())
                .restApiResponseMessage(ApiError.VALIDATION.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationErrorDetailDTO> details = ex.getConstraintViolations().stream()
                .map(v -> ValidationErrorDetailDTO.builder()
                        .field(v.getPropertyPath().toString())
                        .reason(v.getMessage())
                        .type("ERR-001")
                        .build())
                .toList();

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put("fields", (Serializable) details);

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(ApiError.VALIDATION.getCode())
                .restApiResponseMessage(ApiError.VALIDATION.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(NoHandlerFoundException ex) {
        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put("path", ex.getRequestURL());
        errorMap.put("method", ex.getHttpMethod());

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(404)
                .restApiResponseMessage(ApiError.NOT_FOUND.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(404)
                .restApiResponseMessage(ApiError.NOT_FOUND.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(null)
                .build();

        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<?>> handleUncaught(Throwable ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Unhandled exception", errorId, ex);

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put("errorId", errorId);

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(500)
                .restApiResponseMessage("Internal server error")
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.status(500).body(body);
    }
}

