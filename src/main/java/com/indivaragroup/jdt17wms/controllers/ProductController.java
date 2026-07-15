package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.AdminChangeVisibilityDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.services.ProductManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductManagementService productManagementService;

    public ProductController(ProductManagementService productManagementService) {
        this.productManagementService = productManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Product>>> getAllProducts(Pageable pageable) {
        Page<Product> result = productManagementService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.<Page<Product>>builder()
                .restApiResponseHttpCode(ApiSuccess.PRODUCTS_FETCHED.getCode())
                .restApiResponseMessage(ApiSuccess.PRODUCTS_FETCHED.getMessage())
                .restApiResponseResult(result)
                .restApiResponseError(null)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable UUID id,
            @RequestBody AdminChangeVisibilityDTO adminChangeVisibilityDTO) {
        Product result = productManagementService.updateProductVisibility(id, adminChangeVisibilityDTO.getVisibility());
        return ResponseEntity.ok(ApiResponse.<Product>builder()
                .restApiResponseHttpCode(ApiSuccess.PRODUCT_UPDATED.getCode())
                .restApiResponseMessage(ApiSuccess.PRODUCT_UPDATED.getMessage())
                .restApiResponseResult(result)
                .restApiResponseError(null)
                .build());
    }
}
