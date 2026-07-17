package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.dto.request.AdminChangeVisibilityDTO;
import com.indivaragroup.jdt17wms.dto.request.ProductQueryDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.services.ProductManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_PRODUCTS_PATH)
public class ProductController {

    private final ProductManagementService productManagementService;

    public ProductController(ProductManagementService productManagementService) {
        this.productManagementService = productManagementService;
    }

    @GetMapping
    public ApiResponse<Page<Product>> getAllProducts(
            ProductQueryDTO queryDTO,
            Pageable pageable) {
        return ApiResponse.success(ApiSuccess.PRODUCTS_FETCHED,
                productManagementService.getProductsForUser(queryDTO, pageable));
    }

    @PutMapping("/{id}")
    @AuditLogged(action = "UPDATE_PRODUCT", category = "PRODUCT")
    public ApiResponse<Product> updateProduct(
            @PathVariable UUID id,
            @RequestBody AdminChangeVisibilityDTO adminChangeVisibilityDTO) {
        return ApiResponse.success(ApiSuccess.PRODUCT_UPDATED,
                productManagementService.updateProductVisibility(id, adminChangeVisibilityDTO.getVisibility()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(
            @PathVariable UUID id
    ) {
        return ApiResponse.success(ApiSuccess.PRODUCT_FETCHED,
                productManagementService.getProductById(id));
    }
}
