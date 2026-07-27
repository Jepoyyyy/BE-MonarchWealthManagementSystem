package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductManagementServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminProductManagementService adminProductManagementService;

    @Test
    @DisplayName("listProducts - when search and type have text, pass them directly to repository")
    void listProducts_whenSearchAndTypeHaveValue_shouldPassValuesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> expectedPage = new PageImpl<>(Collections.emptyList());

        when(productRepository.findAllAdmin("gold", "MUTUAL_FUND", pageable))
                .thenReturn(expectedPage);

        Page<ProductResponseDTO> result = adminProductManagementService.listProducts("gold", "MUTUAL_FUND", pageable);

        assertNotNull(result);
        assertEquals(expectedPage.getTotalElements(), result.getTotalElements());
        verify(productRepository).findAllAdmin("gold", "MUTUAL_FUND", pageable);
    }

    @Test
    @DisplayName("listProducts - when search and type are blank or empty strings, sanitize to null")
    void listProducts_whenSearchAndTypeAreBlank_shouldSanitizeToNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> expectedPage = new PageImpl<>(Collections.emptyList());

        when(productRepository.findAllAdmin(null, null, pageable))
                .thenReturn(expectedPage);

        Page<ProductResponseDTO> result = adminProductManagementService.listProducts("   ", "", pageable);

        assertNotNull(result);
        assertEquals(expectedPage.getTotalElements(), result.getTotalElements());
        verify(productRepository).findAllAdmin(null, null, pageable);
    }

    @Test
    @DisplayName("listProducts - when search and type are null, pass null to repository")
    void listProducts_whenSearchAndTypeAreNull_shouldPassNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> expectedPage = new PageImpl<>(Collections.emptyList());

        when(productRepository.findAllAdmin(null, null, pageable))
                .thenReturn(expectedPage);

        Page<ProductResponseDTO> result = adminProductManagementService.listProducts(null, null, pageable);

        assertNotNull(result);
        assertEquals(expectedPage.getTotalElements(), result.getTotalElements());
        verify(productRepository).findAllAdmin(null, null, pageable);
    }

    @Test
    @DisplayName("updateProductVisibility - should update and return product")
    void updateProductVisibility_shouldUpdateAndReturnProduct() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setVisible(false);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = adminProductManagementService.updateProductVisibility(id, true);

        assertNotNull(result);
        assertTrue(result.getVisible());
        verify(productRepository).findById(id);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("updateProductVisibility - should throw when product not found")
    void updateProductVisibility_whenProductNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class,
                () -> adminProductManagementService.updateProductVisibility(id, true));

        verify(productRepository).findById(id);
        verify(productRepository, never()).save(any());
    }
}
