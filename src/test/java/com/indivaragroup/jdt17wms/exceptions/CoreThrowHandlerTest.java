package com.indivaragroup.jdt17wms.exceptions;

import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.ValidationErrorDetailDTO;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreThrowHandlerTest {

    @Test
    void constructor_withApiErrorAndMessage_shouldSetFieldsCorrectly() {
        CoreThrowHandler ex = new CoreThrowHandler(ApiError.NOT_FOUND, "Resource not found");

        assertEquals(404, ex.getCode());
        assertEquals("Resource not found", ex.getMessage());
        assertTrue(ex.getError().isEmpty());
        assertNull(ex.getDetails());
    }

    @Test
    void constructor_withApiErrorMessageAndErrorMap_shouldSetFieldsCorrectly() {
        Map<String, Serializable> errorMap = new HashMap<>();
        errorMap.put("field", "value");

        CoreThrowHandler ex = new CoreThrowHandler(ApiError.BAD_REQUEST, "Validation failed", errorMap);

        assertEquals(400, ex.getCode());
        assertEquals("Validation failed", ex.getMessage());
        assertEquals(errorMap, ex.getError());
        assertNull(ex.getDetails());
    }

    @Test
    void constructor_withApiErrorAndDetails_shouldSetFieldsCorrectly() {
        ValidationErrorDetailDTO detail = ValidationErrorDetailDTO.builder()
                .field("email")
                .reason("must not be null")
                .type("ERR-001")
                .build();
        List<ValidationErrorDetailDTO> details = List.of(detail);

        CoreThrowHandler ex = new CoreThrowHandler(ApiError.VALIDATION, details);

        assertEquals(400, ex.getCode());
        assertEquals(ApiError.VALIDATION.getMessage(), ex.getMessage());
        assertTrue(ex.getError().isEmpty());
        assertNotNull(ex.getDetails());
        assertEquals(1, ex.getDetails().size());
        assertEquals("email", ex.getDetails().get(0).getField());
        assertEquals("must not be null", ex.getDetails().get(0).getReason());
        assertEquals("ERR-001", ex.getDetails().get(0).getType());
    }

    @Test
    void constructor_withApiErrorOnly_shouldSetFieldsCorrectly() {
        CoreThrowHandler ex = new CoreThrowHandler(ApiError.UNAUTHORIZED);

        assertEquals(401, ex.getCode());
        assertEquals("UNAUTHORIZED", ex.getMessage());
        assertTrue(ex.getError().isEmpty());
        assertNull(ex.getDetails());
    }
}
