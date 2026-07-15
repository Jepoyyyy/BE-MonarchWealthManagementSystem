package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductManagementServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductManagementService productManagementService;

    @Test
    void serviceShouldBeInitialized() {
        assertNotNull(productManagementService);
    }

    @Test
    void getAllProducts_shouldReturnPaginatedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product();
        Page<Product> expectedPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        Page<Product> actualPage = productManagementService.getAllProducts(pageable);

        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        assertEquals(product, actualPage.getContent().get(0));
    }

    @Test
    void updateProductVisibility_shouldUpdateAndReturnProduct_whenProductExists() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setVisible(false);

        when(productRepository.findById(id)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updatedProduct = productManagementService.updateProductVisibility(id, true);

        assertNotNull(updatedProduct);
        org.junit.jupiter.api.Assertions.assertTrue(updatedProduct.getVisible());
    }

    @Test
    void updateProductVisibility_shouldThrowNotFoundException_whenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(CoreThrowHandler.class, () -> {
            productManagementService.updateProductVisibility(id, true);
        });
    }
}


