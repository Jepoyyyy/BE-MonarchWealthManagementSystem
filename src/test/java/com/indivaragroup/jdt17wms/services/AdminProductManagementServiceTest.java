package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AdminProductCreateDTO;
import com.indivaragroup.jdt17wms.dto.request.AdminProductUpdateDTO;
import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
    @DisplayName("createProduct - should build product from DTO and save to repository")
    void createProduct_shouldBuildAndSaveProduct() {
        AdminProductCreateDTO dto = AdminProductCreateDTO.builder()
                .code("PRD-001")
                .name("Gold Investment")
                .issuer("Monarch")
                .type("GOLD")
                .riskLevel(2)
                .annualReturn(new BigDecimal("0.0800"))
                .minInvestment(new BigDecimal("100000.0000"))
                .currentPrice(new BigDecimal("105000.0000"))
                .description("Gold asset")
                .tenor("12 Months")
                .lotSize(1)
                .isFractionalAllowed(true)
                .visible(true)
                .build();

        Product savedProduct = Product.builder()
                .id(UUID.randomUUID())
                .code(dto.getCode())
                .name(dto.getName())
                .issuer(dto.getIssuer())
                .type(dto.getType())
                .riskLevel(dto.getRiskLevel())
                .annualReturn(dto.getAnnualReturn())
                .minInvestment(dto.getMinInvestment())
                .currentPrice(dto.getCurrentPrice())
                .description(dto.getDescription())
                .tenor(dto.getTenor())
                .lotSize(dto.getLotSize())
                .isFractionalAllowed(dto.getIsFractionalAllowed())
                .visible(dto.getVisible())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponseDTO result = adminProductManagementService.createProduct(dto);

        assertNotNull(result);
        assertEquals(savedProduct.getId(), result.getId());
        assertEquals("PRD-001", result.getCode());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();
        assertEquals("PRD-001", capturedProduct.getCode());
        assertEquals("Gold Investment", capturedProduct.getName());
        assertEquals("Monarch", capturedProduct.getIssuer());
        assertEquals("GOLD", capturedProduct.getType());
        assertEquals(2, capturedProduct.getRiskLevel());
        assertEquals(new BigDecimal("0.0800"), capturedProduct.getAnnualReturn());
        assertEquals(new BigDecimal("100000.0000"), capturedProduct.getMinInvestment());
        assertEquals(new BigDecimal("105000.0000"), capturedProduct.getCurrentPrice());
        assertEquals("Gold asset", capturedProduct.getDescription());
        assertEquals("12 Months", capturedProduct.getTenor());
        assertEquals(1, capturedProduct.getLotSize());
        assertTrue(capturedProduct.getIsFractionalAllowed());
        assertTrue(capturedProduct.getVisible());
    }

    @Test
    @DisplayName("updateProduct - when product not found, should throw CoreThrowHandler exception")
    void updateProduct_whenNotFound_shouldThrowException() {
        UUID productId = UUID.randomUUID();
        AdminProductUpdateDTO dto = AdminProductUpdateDTO.builder().name("Updated Name").build();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        CoreThrowHandler exception = assertThrows(
                CoreThrowHandler.class,
                () -> adminProductManagementService.updateProduct(productId, dto)
        );

        assertEquals(ApiError.ITEM_NOT_FOUND.getCode(), exception.getCode());
        assertEquals("Product not found", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProduct - when all fields non-null, should update all product properties")
    void updateProduct_whenAllFieldsPresent_shouldUpdateAllFields() {
        UUID productId = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(productId)
                .code("PRD-OLD")
                .name("Old Name")
                .issuer("Old Issuer")
                .type("OLD_TYPE")
                .riskLevel(1)
                .annualReturn(new BigDecimal("0.0500"))
                .minInvestment(new BigDecimal("50000.0000"))
                .currentPrice(new BigDecimal("50000.0000"))
                .description("Old Description")
                .tenor("6 Months")
                .lotSize(5)
                .isFractionalAllowed(false)
                .visible(false)
                .build();

        AdminProductUpdateDTO dto = AdminProductUpdateDTO.builder()
                .name("New Name")
                .issuer("New Issuer")
                .type("NEW_TYPE")
                .riskLevel(3)
                .annualReturn(new BigDecimal("0.1000"))
                .minInvestment(new BigDecimal("200000.0000"))
                .currentPrice(new BigDecimal("210000.0000"))
                .description("New Description")
                .tenor("24 Months")
                .lotSize(10)
                .isFractionalAllowed(true)
                .visible(true)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = adminProductManagementService.updateProduct(productId, dto);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Issuer", result.getIssuer());
        assertEquals("NEW_TYPE", result.getType());
        assertEquals(3, result.getRiskLevel());
        assertEquals(new BigDecimal("0.1000"), result.getAnnualReturn());
        assertEquals(new BigDecimal("200000.0000"), result.getMinInvestment());
        assertEquals(new BigDecimal("210000.0000"), result.getCurrentPrice());
        assertEquals("New Description", result.getDescription());
        assertEquals("24 Months", result.getTenor());
        assertEquals(10, result.getLotSize());
        assertTrue(result.getIsFractionalAllowed());
        assertTrue(result.getVisible());

        verify(productRepository).save(existingProduct);
    }

    @Test
    @DisplayName("updateProduct - when all fields null in DTO, should keep existing product properties")
    void updateProduct_whenAllFieldsNull_shouldKeepExistingFields() {
        UUID productId = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(productId)
                .code("PRD-EXISTING")
                .name("Existing Name")
                .issuer("Existing Issuer")
                .type("EXISTING_TYPE")
                .riskLevel(2)
                .annualReturn(new BigDecimal("0.0700"))
                .minInvestment(new BigDecimal("100000.0000"))
                .currentPrice(new BigDecimal("100000.0000"))
                .description("Existing Description")
                .tenor("12 Months")
                .lotSize(1)
                .isFractionalAllowed(false)
                .visible(true)
                .build();

        AdminProductUpdateDTO dto = AdminProductUpdateDTO.builder().build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = adminProductManagementService.updateProduct(productId, dto);

        assertNotNull(result);
        assertEquals("Existing Name", result.getName());
        assertEquals("Existing Issuer", result.getIssuer());
        assertEquals("EXISTING_TYPE", result.getType());
        assertEquals(2, result.getRiskLevel());
        assertEquals(new BigDecimal("0.0700"), result.getAnnualReturn());
        assertEquals(new BigDecimal("100000.0000"), result.getMinInvestment());
        assertEquals(new BigDecimal("100000.0000"), result.getCurrentPrice());
        assertEquals("Existing Description", result.getDescription());
        assertEquals("12 Months", result.getTenor());
        assertEquals(1, result.getLotSize());
        assertFalse(result.getIsFractionalAllowed());
        assertTrue(result.getVisible());

        verify(productRepository).save(existingProduct);
    }
}
