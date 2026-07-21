package com.indivaragroup.jdt17wms.dto.utils;

import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<SecurityUtils> constructor = SecurityUtils.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        SecurityUtils instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void getCurrentUserId_whenAuthenticationIsNull_shouldThrowCoreThrowHandler() {
        SecurityContextHolder.clearContext();
        assertThrows(CoreThrowHandler.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void getCurrentUserId_whenPrincipalIsNotUserDTO_shouldThrowCoreThrowHandler() {
        Authentication auth = new UsernamePasswordAuthenticationToken("anonymous", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(CoreThrowHandler.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void getCurrentUserId_whenPrincipalIsUserDTO_shouldReturnUserId() {
        UUID expectedId = UUID.randomUUID();
        UserDTO userDTO = UserDTO.builder()
                .id(expectedId)
                .email("user@example.com")
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDTO, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(expectedId, SecurityUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_whenUserDtoIdIsNull_shouldThrowCoreThrowHandler() {
        UserDTO userDTO = UserDTO.builder()
                .id(null)
                .email("user@example.com")
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDTO, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(CoreThrowHandler.class, SecurityUtils::getCurrentUserId);
    }
}
