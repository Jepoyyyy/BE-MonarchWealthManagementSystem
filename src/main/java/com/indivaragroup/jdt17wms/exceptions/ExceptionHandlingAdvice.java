package com.indivaragroup.jdt17wms.exceptions;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ExceptionHandlingAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);
    private static final String BUSINESS_ERROR_CODE = "ERR-001";
    private static final String UNRECOGNIZED_FIELD_PREFIX = "Unrecognized field: ";
    private static final String MALFORMED_JSON_MESSAGE = "Malformed JSON request body";

    private static final String KEY_DETAIL = "detail";
    private static final String KEY_FIELDS = "fields";
    private static final String KEY_PATH = "path";
    private static final String KEY_METHOD = "method";
    private static final String KEY_ERROR_ID = "errorId";

    private static final String LOG_UNHANDLED_EXCEPTION = "[{}] Unhandled exception";

    @ExceptionHandler(CoreThrowHandler.class)
    public ResponseEntity<ApiResponse<?>> handleCoreThrowHandler(CoreThrowHandler ex) {
        Map<String, Serializable> errorMap;

        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            errorMap = ex.getError() != null ? new HashMap<>(ex.getError()) : new HashMap<>();
            errorMap.put(KEY_FIELDS, (Serializable) ex.getDetails());
        } else if (ex.getError() != null && !ex.getError().isEmpty()) {
            errorMap = ex.getError();
        } else {
            errorMap = null;
        }

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
    String errorMsg;

    Throwable cause = ex.getCause();
    if (cause instanceof UnrecognizedPropertyException upe) {
      errorMsg = UNRECOGNIZED_FIELD_PREFIX + upe.getPropertyName();
    } else {
      errorMsg = MALFORMED_JSON_MESSAGE;
    }

    Map<String, Serializable> errorMap = new HashMap<>();
    errorMap.put(KEY_DETAIL, errorMsg);

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
                        .type(BUSINESS_ERROR_CODE)
                        .build())
                .toList();

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put(KEY_FIELDS, (Serializable) details);

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
                        .type(BUSINESS_ERROR_CODE)
                        .build())
                .toList();

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put(KEY_FIELDS, (Serializable) details);

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
        errorMap.put(KEY_PATH, ex.getRequestURL());
        errorMap.put(KEY_METHOD, ex.getHttpMethod());

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(HttpStatus.NOT_FOUND.value())
                .restApiResponseMessage(ApiError.NOT_FOUND.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(body);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(HttpStatus.NOT_FOUND.value())
                .restApiResponseMessage(ApiError.NOT_FOUND.getMessage())
                .restApiResponseResult(null)
                .restApiResponseError(null)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(body);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<?>> handleUncaught(Throwable ex) {
        String errorId = UUID.randomUUID().toString();
        log.error(LOG_UNHANDLED_EXCEPTION, errorId, ex);

        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put(KEY_ERROR_ID, errorId);

        ApiResponse<?> body = ApiResponse.builder()
                .restApiResponseHttpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .restApiResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .restApiResponseResult(null)
                .restApiResponseError(errorMap)
                .build();

        return ResponseEntity.status(500).body(body);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : null;
        String errorMsg = "Invalid value '" + value + "' for parameter '" + paramName + "'";


        Map<String, Serializable> errorMap = new HashMap<>();
                errorMap.put("detail", errorMsg);
                errorMap.put("parameter", paramName);errorMap.put("value", value);

                ApiResponse<?> body = ApiResponse.builder()
                        .restApiResponseHttpCode(ApiError.INVALID_REQUEST_PARAMETER.getCode())
                        .restApiResponseMessage(ApiError.INVALID_REQUEST_PARAMETER.getMessage())
                        .restApiResponseResult(null)
                       .restApiResponseError(errorMap)
                        .build();
        return ResponseEntity.badRequest().body(body);
  }
}

