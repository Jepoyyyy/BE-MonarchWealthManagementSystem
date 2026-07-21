package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.dto.request.ProductQueryDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductManagementServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductManagementService productManagementService;

    private void mockAuthenticatedUser(UUID userId) {
        UserDTO principal = UserDTO.builder().id(userId).build();
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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

        Page<ProductResponseDTO> actualPage = productManagementService.getAllProducts(pageable);

        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        assertEquals(ProductResponseDTO.fromEntity(product), actualPage.getContent().getFirst());
    }

    @Test
    void updateProductVisibility_shouldUpdateAndReturnProduct_whenProductExists() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setVisible(false);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO updatedProduct = productManagementService.updateProductVisibility(id, true);

        assertNotNull(updatedProduct);
        assertTrue(updatedProduct.getVisible());
    }

    @Test
    void updateProductVisibility_shouldThrowNotFoundException_whenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> {
            productManagementService.updateProductVisibility(id, true);
        });
    }



    @Test
    void getProductsForUser_shouldFilterByVisibilityAndRiskProfile_forStandardUser() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("moderate") // moderate allows risk level <= 4
                .build();

        Product lowRiskVisible = Product.builder().riskLevel(2).visible(true).build();
        Product highRiskVisible = Product.builder().riskLevel(5).visible(true).build(); // Excluded (5 > 4)
        Product lowRiskHidden = Product.builder().riskLevel(2).visible(false).build(); // Excluded (hidden)

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(lowRiskVisible, highRiskVisible, lowRiskHidden));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(ProductResponseDTO.fromEntity(lowRiskVisible), result.getContent().getFirst());
    }

    @Test
    void getProductsForUser_shouldRespectShowAll_forStandardUser() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("moderate") // moderate allows risk level <= 4
                .build();

        Product lowRiskVisible = Product.builder().riskLevel(2).visible(true).build();
        Product highRiskVisible = Product.builder().riskLevel(5).visible(true).build(); // Included due to showAll = true

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(lowRiskVisible, highRiskVisible));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(null, null, true, false), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getProductsForUser_shouldFilterByTypeAndSearchQuery() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();

        Product matchTypeAndName = Product.builder().name("Danareksa Stock").issuer("Danareksa").type("stock").visible(true).build();
        Product matchTypeOnly = Product.builder().name("BCA Stock").issuer("Bank BCA").type("stock").visible(true).build();
        Product matchNameOnly = Product.builder().name("Danareksa Bond").issuer("Danareksa").type("bond").visible(true).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(matchTypeAndName, matchTypeOnly, matchNameOnly));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO("Danareksa", "stock", false, false), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(ProductResponseDTO.fromEntity(matchTypeAndName), result.getContent().getFirst());
    }

    @Test
    void getProductsForUser_shouldLimitToDashboardSummary() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();

        List<Product> tenProducts = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> Product.builder().visible(true).build())
                .toList();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(tenProducts);

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(null, null, false, true), PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
    }

    @Test
    void getProductsForUser_shouldNotFilterByVisibilityAndRiskLevel_forAdmin() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.ADMIN)
                .build();

        Product visibleLowRisk = Product.builder().riskLevel(2).visible(true).build();
        Product hiddenHighRisk = Product.builder().riskLevel(5).visible(false).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(visibleLowRisk, hiddenHighRisk));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getProductsForUser_shouldNotFilter_whenUserDoesNotExist() {
        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.empty());
        Product visibleLowRisk = Product.builder().riskLevel(2).visible(true).build();
        Product hiddenHighRisk = Product.builder().riskLevel(5).visible(false).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);
        when(productRepository.findAll()).thenReturn(List.of(visibleLowRisk, hiddenHighRisk));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getProductsForUser_shouldWork_whenQueryDtoIsNull() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();
        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        Product visibleLowRisk = Product.builder().riskLevel(2).visible(true).build();
        when(productRepository.findAll()).thenReturn(List.of(visibleLowRisk));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProductsForUser_shouldFilterByRiskAverse_forStandardUser() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_averse")
                .build();
        Product risk2 = Product.builder().riskLevel(2).visible(true).build();
        Product risk3 = Product.builder().riskLevel(3).visible(true).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(risk2, risk3));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(ProductResponseDTO.fromEntity(risk2), result.getContent().getFirst());
    }

    @Test
    void getProductsForUser_shouldReturnEmptyPage_whenOffsetIsGreaterThanProductListSize() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();
        Product product = Product.builder().visible(true).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(product));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(), PageRequest.of(1, 10));

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProductsForUser_shouldNotFilterType_whenTypeFilterIsBlank() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();
        Product product = Product.builder().type("stock").visible(true).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(product));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO(null, "   ", false, false), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(ProductResponseDTO.fromEntity(product), result.getContent().getFirst());
    }

    @Test
    void getProductsForUser_shouldNotFilterSearchQuery_whenSearchQueryIsBlank() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();
        Product product = Product.builder().name("Danareksa Stock").visible(true).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);


        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(product));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(new ProductQueryDTO("   ", null, false, false), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(ProductResponseDTO.fromEntity(product), result.getContent().getFirst());
    }

    @Test
    void getProductById_shouldReturnProduct_whenProductExistsAndIsVisible() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .visible(true)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponseDTO result = productManagementService.getProductById(productId);

        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("Test Product", result.getName());
        assertTrue(result.getVisible());
    }

    @Test
    void getProductById_shouldThrowException_whenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(CoreThrowHandler.class, () -> productManagementService.getProductById(productId));
    }

    @Test
    void getProductById_shouldThrowException_whenProductIsNotVisible() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Hidden Product")
                .visible(false)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(CoreThrowHandler.class, () -> productManagementService.getProductById(productId));
    }

    @Test
    void getProductsForUser_shouldHandleNullNameOrIssuer_whenSearching() {
        User user = User.builder()
                .id(SecurityUtils.STATIC_USER_ID)
                .role(UserRole.USER)
                .questionnaireCompleted(true)
                .riskProfile("risk_taker")
                .build();

        Product productWithNullName = Product.builder().name(null).issuer("Issuer").visible(true).build();
        Product productWithNullIssuer = Product.builder().name("Name").issuer(null).visible(true).build();

        mockAuthenticatedUser(SecurityUtils.STATIC_USER_ID);

        when(userRepository.findById(SecurityUtils.STATIC_USER_ID)).thenReturn(Optional.of(user));
        when(productRepository.findAll()).thenReturn(List.of(productWithNullName, productWithNullIssuer));

        Page<ProductResponseDTO> result = productManagementService.getProductsForUser(
                new ProductQueryDTO("searchQuery", null, false, false),
                PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }
}

