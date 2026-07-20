package com.indivaragroup.jdt17wms.exceptions;

import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import com.indivaragroup.jdt17wms.dto.request.LoginDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExceptionHandlingAdviceTest {

    private final ExceptionHandlingAdvice advice = new ExceptionHandlingAdvice();

    // --- CoreThrowHandler ---

    @Test
    void handleCoreThrowHandler_shouldReturnProperResponse() {
        CoreThrowHandler ex = new CoreThrowHandler(ApiError.NOT_FOUND, "Resource not found");

        ResponseEntity<ApiResponse<?>> response = advice.handleCoreThrowHandler(ex);

        assertEquals(404, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getRestApiResponseHttpCode());
        assertEquals("Resource not found", body.getRestApiResponseMessage());
        assertNull(body.getRestApiResponseError());
    }

    @Test
    void handleCoreThrowHandler_withErrorMap_shouldIncludeError() {
        Map<String, Serializable> errorMap = Map.of("detail", "something went wrong");
        CoreThrowHandler ex = new CoreThrowHandler(ApiError.BAD_REQUEST, "Bad request", errorMap);

        ResponseEntity<ApiResponse<?>> response = advice.handleCoreThrowHandler(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getRestApiResponseHttpCode());
        assertEquals("Bad request", body.getRestApiResponseMessage());
        assertNotNull(body.getRestApiResponseError());
        assertEquals("something went wrong", body.getRestApiResponseError().get("detail"));
    }

    // --- HttpMessageNotReadableException ---

    @Test
    void handleJsonParseError_withUnrecognizedField_shouldExtractFieldName() {
        // Message must contain "UnrecognizedPropertyException" + field in ["<name>"] format for regex match
        String message = "JSON parse error: UnrecognizedPropertyException: Unrecognized field [\"unknownField\"]";
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(message);

        ResponseEntity<ApiResponse<?>> response = advice.handleJsonParseError(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(ApiError.INVALID_REQUEST_BODY.getCode(), body.getRestApiResponseHttpCode());
        assertEquals("Invalid Request Body", body.getRestApiResponseMessage());
        assertNotNull(body.getRestApiResponseError());
        assertEquals("Unrecognized field: unknownField", body.getRestApiResponseError().get("detail"));
    }

    @Test
    void handleJsonParseError_withMalformedJson_shouldReturnGenericError() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error: Cannot deserialize value of type");

        ResponseEntity<ApiResponse<?>> response = advice.handleJsonParseError(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals("Malformed JSON request body", body.getRestApiResponseError().get("detail"));
    }

    @Test
    void handleJsonParseError_withNullMessage_shouldReturnGenericError() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException((String) null);

        ResponseEntity<ApiResponse<?>> response = advice.handleJsonParseError(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals("Malformed JSON request body", body.getRestApiResponseError().get("detail"));
    }

    // --- MethodArgumentNotValidException ---

    @Test
    void handleValidationErrors_shouldExtractFieldErrors() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("testObject", "email", "must not be null");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<?>> response = advice.handleValidationErrors(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(ApiError.VALIDATION.getCode(), body.getRestApiResponseHttpCode());
        assertEquals("INVALID FIELD VALUES", body.getRestApiResponseMessage());

        Map<String, Serializable> error = body.getRestApiResponseError();
        assertNotNull(error);
        assertTrue(error.containsKey("fields"));
        @SuppressWarnings("unchecked")
        List<ValidationErrorDetailDTO> details = (List<ValidationErrorDetailDTO>) error.get("fields");
        assertEquals(1, details.size());
        assertEquals("email", details.get(0).getField());
        assertEquals("must not be null", details.get(0).getReason());
        assertEquals("ERR-001", details.get(0).getType());
    }

    @Test
    void handleValidationErrors_withNonFieldError_shouldUseObjectName() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        // Use ObjectError (not FieldError) to test the branch where error is not a FieldError instance
        org.springframework.validation.ObjectError objectError = new org.springframework.validation.ObjectError("testObject", "global error message");
        when(bindingResult.getAllErrors()).thenReturn(List.of(objectError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<?>> response = advice.handleValidationErrors(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID FIELD VALUES", body.getRestApiResponseMessage());
    }

    // --- ConstraintViolationException ---

    @Test
    void handleConstraintViolation_shouldMapViolationsToDetails() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));

        ResponseEntity<ApiResponse<?>> response = advice.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(ApiError.VALIDATION.getCode(), body.getRestApiResponseHttpCode());
        assertEquals("INVALID FIELD VALUES", body.getRestApiResponseMessage());

        Map<String, Serializable> error = body.getRestApiResponseError();
        assertNotNull(error);
        @SuppressWarnings("unchecked")
        List<ValidationErrorDetailDTO> details = (List<ValidationErrorDetailDTO>) error.get("fields");
        assertEquals(1, details.size());
        assertEquals("email", details.get(0).getField());
        assertEquals("must not be blank", details.get(0).getReason());
        assertEquals("ERR-001", details.get(0).getType());
    }

    @Test
    void handleConstraintViolation_withMultipleViolations_shouldMapAll() {
        ConstraintViolation<?> v1 = mock(ConstraintViolation.class);
        Path p1 = mock(Path.class);
        when(p1.toString()).thenReturn("name");
        when(v1.getPropertyPath()).thenReturn(p1);
        when(v1.getMessage()).thenReturn("must not be null");

        ConstraintViolation<?> v2 = mock(ConstraintViolation.class);
        Path p2 = mock(Path.class);
        when(p2.toString()).thenReturn("email");
        when(v2.getPropertyPath()).thenReturn(p2);
        when(v2.getMessage()).thenReturn("must be a valid email");

        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(v1, v2));

        ResponseEntity<ApiResponse<?>> response = advice.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        List<ValidationErrorDetailDTO> details = (List<ValidationErrorDetailDTO>) body.getRestApiResponseError().get("fields");
        assertEquals(2, details.size());
    }

    @Test
    void handleConstraintViolation_withRealValidator_shouldReadAnnotationMessage() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();

        LoginDTO dto = LoginDTO.builder()
                .loginRequestEmail("")          // @NotBlank(message = "Email is Required")
                .loginRequestPassword("abc")    // @Size(min = 8) → default message
                .build();

        Set<ConstraintViolation<LoginDTO>> violations = validator.validate(dto);
        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<ApiResponse<?>> response = advice.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        List<ValidationErrorDetailDTO> details =
                (List<ValidationErrorDetailDTO>) response.getBody().getRestApiResponseError().get("fields");

        // message dari annotation @NotBlank(message = "Email is Required")
        assertTrue(details.stream().anyMatch(d -> "Email is Required".equals(d.getReason())),
                "Should read custom message from @NotBlank annotation");

        // message default @Size — tanpa message attribute
        assertTrue(details.stream().anyMatch(d -> d.getReason().contains("size must be between")),
                "Should read default interpolation message for @Size");
    }

    // --- NoHandlerFoundException ---

    @Test
    void handleNotFound_shouldReturn404WithPathAndMethod() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/v1/unknown", null);

        ResponseEntity<ApiResponse<?>> response = advice.handleNotFound(ex);

        assertEquals(404, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getRestApiResponseHttpCode());
        assertEquals("RESOURCE NOT FOUND", body.getRestApiResponseMessage());
        assertNotNull(body.getRestApiResponseError());
        assertEquals("/api/v1/unknown", body.getRestApiResponseError().get("path"));
        assertEquals("GET", body.getRestApiResponseError().get("method"));
    }

    // --- NoResourceFoundException ---

    @Test
    void handleNoResourceFound_shouldReturn404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/static/image.png");

        ResponseEntity<ApiResponse<?>> response = advice.handleNoResourceFound(ex);

        assertEquals(404, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getRestApiResponseHttpCode());
        assertEquals("RESOURCE NOT FOUND", body.getRestApiResponseMessage());
        assertNull(body.getRestApiResponseError());
    }

    // --- Throwable catch-all ---

    @Test
    void handleUncaught_shouldReturn500WithErrorId() {
        Throwable ex = new RuntimeException("Unexpected error");

        ResponseEntity<ApiResponse<?>> response = advice.handleUncaught(ex);

        assertEquals(500, response.getStatusCode().value());
        ApiResponse<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getRestApiResponseHttpCode());
        assertEquals("Internal server error", body.getRestApiResponseMessage());
        assertNotNull(body.getRestApiResponseError());
        assertNotNull(body.getRestApiResponseError().get("errorId"));
    }
}
