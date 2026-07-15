package com.indivaragroup.jdt17wms.exceptions;

import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlingAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);

    @ExceptionHandler(CoreThrowHandler.class)
    public ResponseEntity<Map<String, Object>> handleCoreThrowHandler(CoreThrowHandler ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        body.put("code", ex.getCode());
        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            body.put("details", ex.getDetails());
        }
        return ResponseEntity.status(ex.getCode()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseError(HttpMessageNotReadableException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        String message = ex.getMessage();

        if (message != null && message.contains("UnrecognizedPropertyException")) {
            String field = message.replaceAll(".*\\[\"([^\"]+)\"\\].*", "$1");
            errorResponse.put("error", "Unrecognized field: " + field);
        } else {
            errorResponse.put("error", "Malformed JSON request body");
        }
        errorResponse.put("code", 400);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationErrorDetailDTO> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ValidationErrorDetailDTO.builder()
                       .field(error instanceof FieldError f ? f.getField() : error.getObjectName())
                        .reason(error.getDefaultMessage())
                        .type("ERR-001")
                        .build())
                .collect(Collectors.toList());

        ValidationErrorResponseDTO body = ValidationErrorResponseDTO.builder()
                .error("Invalid field values")
                .type("ERR-VALIDATION")
                .code(400)
                .details(details)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUncaught(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Internal server error");
        body.put("code", 500);
        return ResponseEntity.status(500).body(body);
    }
    }

